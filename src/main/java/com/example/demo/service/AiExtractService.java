package com.example.demo.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.example.demo.model.CompanyInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.net.SocketException;

@Service
public class AiExtractService {
    private static final String DEFAULT_OLLAMA_BASE_URL = "http://localhost:11434";
    private static final Pattern FIRST_JSON_OBJECT = Pattern.compile("\\{[\\s\\S]*\\}");
    private static final Pattern FIRST_JSON_ARRAY = Pattern.compile("\\[[\\s\\S]*\\]");
    private static final int MAX_JSON_RETRY = 1;

    private static final String[] REQUIRED_KEYS = new String[] {
        "companyName",
        "entityIdentifier",
        "countryISOCode",
        "companyType"
    };

    private static final Pattern ISO_ALPHA_3 = Pattern.compile("^[A-Z]{3}$");

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String ollamaBaseUrl;

    private static final int CONNECT_TIMEOUT_MS = 60_000;
    // LLM generation can take longer, especially on CPU.
    private static final int READ_TIMEOUT_MS = 180_000;

    public AiExtractService(@Value("${ollama.base-url:" + DEFAULT_OLLAMA_BASE_URL + "}") String ollamaBaseUrl) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(CONNECT_TIMEOUT_MS);
    factory.setReadTimeout(READ_TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
        this.ollamaBaseUrl = ollamaBaseUrl;

        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
        this.objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    public CompanyInfo extract(String prompt) {
    return extractWithJsonRetry("llama3", prompt);
    }

    private CompanyInfo extractWithJsonRetry(String model, String prompt) {
        String currentPrompt = prompt;
        Exception lastParseException = null;

        for (int attempt = 0; attempt <= MAX_JSON_RETRY; attempt++) {
            Map<String, Object> request = Map.of(
                "model", model,
                "prompt", currentPrompt,
                "stream", false
            );

            try {
                @SuppressWarnings("rawtypes")
                ResponseEntity<Map> response = restTemplate.postForEntity(
                    ollamaBaseUrl + "/api/generate",
                    request,
                    Map.class
                );

                Map<?, ?> body = response.getBody();
                if (body == null) {
                    throw new RuntimeException("Empty response from AI service");
                }
                String raw = (String) body.get("response");

                System.out.println("Raw AI response (attempt " + (attempt + 1) + "): " + raw);

                try {
                    String cleanedJson = cleanJsonResponse(raw);
                    System.out.println("Cleaned JSON (attempt " + (attempt + 1) + "): " + cleanedJson);

                    // Enforce: must be an object (not array)
                    String repaired = repairLikelyTruncatedJsonObject(cleanedJson);
                    JsonNode node = objectMapper.readTree(repaired);
                    if (!node.isObject()) {
                        throw new RuntimeException("Expected a JSON object but got: " + node.getNodeType());
                    }

                    validateSchemaStrict(node);
                    validateCountryIsoAlpha3(node);

                    return objectMapper.treeToValue(node, CompanyInfo.class);
                } catch (Exception parseEx) {
                    lastParseException = parseEx;

                    if (attempt >= MAX_JSON_RETRY) {
                        System.err.println("Failed to parse AI response after retry: " + raw);
                        throw new RuntimeException("AI response parsing failed: " + parseEx.getMessage(), parseEx);
                    }

                    // Follow-up instruction: force JSON-only response
                    currentPrompt = currentPrompt +
                        "\n\nThe previous response was NOT valid JSON. " +
                        "Retry once and respond with ONLY ONE valid JSON object that contains EXACTLY these 4 keys and no others: " +
                        "companyName, entityIdentifier, countryISOCode, companyType. " +
                        "countryISOCode MUST be ISO 3166-1 alpha-3 (exactly 3 uppercase letters, e.g., HKG, USA, GBR, CHN). " +
                        "If a field is missing, use null. " +
                        "No explanations, no markdown, no code fences. Start with { and end with }.";
                }
            } catch (ResourceAccessException e) {
                Throwable root = e.getMostSpecificCause();
                if (root instanceof SocketException) {
                    System.err.println("Ollama connection dropped (SocketException): " + root.getMessage());
                }
                throw new RuntimeException(
                    "Cannot connect to Ollama at " + ollamaBaseUrl + ". Please ensure Ollama is running and reachable.",
                    e
                );
            }
        }

        throw new RuntimeException("AI response parsing failed", lastParseException);
    }

    private void validateSchemaStrict(JsonNode node) {
        // Must have exactly 4 keys
        if (node.size() != REQUIRED_KEYS.length) {
            throw new RuntimeException(
                "JSON object must contain exactly " + REQUIRED_KEYS.length + " keys (" + String.join(", ", REQUIRED_KEYS) + ") but got " + node.size()
            );
        }

        for (String key : REQUIRED_KEYS) {
            if (!node.has(key)) {
                throw new RuntimeException("JSON object missing required key: " + key);
            }
        }
    }

    private void validateCountryIsoAlpha3(JsonNode node) {
        JsonNode v = node.get("countryISOCode");
        if (v == null || v.isNull()) {
            return;
        }
        if (!v.isTextual()) {
            throw new RuntimeException("countryISOCode must be a string or null");
        }
        String code = v.asText().trim();
        if (!ISO_ALPHA_3.matcher(code).matches()) {
            throw new RuntimeException("countryISOCode must be ISO 3166-1 alpha-3 (3 uppercase letters). Got: " + code);
        }
    }

    /**
     * Small best-effort repair for a common Ollama failure mode: the model emits a JSON object
     * but truncates the final closing brace '}'.
     *
     * We only repair when it looks like a single JSON object starting with '{' and missing
     * one or more closing braces. We do NOT attempt complex healing.
     */
    private String repairLikelyTruncatedJsonObject(String json) {
        if (json == null) {
            return "";
        }
        String s = json.trim();
        if (!s.startsWith("{")) {
            return s;
        }

        int open = 0;
        int close = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);

            if (inString) {
                if (escaped) {
                    escaped = false;
                    continue;
                }
                if (c == '\\') {
                    escaped = true;
                    continue;
                }
                if (c == '"') {
                    inString = false;
                }
                continue;
            }

            if (c == '"') {
                inString = true;
                continue;
            }
            if (c == '{') {
                open++;
            } else if (c == '}') {
                close++;
            }
        }

        int missing = open - close;
        if (missing <= 0) {
            return s;
        }
        if (missing > 3) {
            // If it's missing too many braces, it's probably not a simple truncation.
            return s;
        }

        StringBuilder sb = new StringBuilder(s);
        for (int i = 0; i < missing; i++) {
            sb.append('}');
        }
        return sb.toString();
    }

    /**
     * Cleans the AI response to extract valid JSON.
     * Handles various formats like:
     * - Plain JSON
     * - JSON wrapped in markdown code blocks (```json ... ```)
     * - JSON preceded by explanatory text (e.g., "Here is the JSON: {...}")
     */
    private String cleanJsonResponse(String response) {
        if (response == null || response.isEmpty()) {
            return "";
        }
        
        // Remove markdown code blocks
        String cleaned = response.replaceAll("```json\\s*", "")
                                 .replaceAll("```\\s*", "")
                                 .trim();
        
        // Extract the first complete JSON object/array found anywhere in the string.
        // This is more reliable than naive first/last brace slicing.
        Optional<String> extracted = extractFirstJson(cleaned);
        if (extracted.isPresent()) {
            cleaned = extracted.get();
        }
        
        // Fix common JSON issues: convert empty arrays to null for string fields
        // This handles cases where AI returns "remarks": [] instead of "remarks": null
        cleaned = cleaned.replaceAll("\"remarks\"\\s*:\\s*\\[\\s*\\]", "\"remarks\": null");
        
        return cleaned.trim();
    }

    private Optional<String> extractFirstJson(String text) {
        if (text == null) {
            return Optional.empty();
        }

        Matcher obj = FIRST_JSON_OBJECT.matcher(text);
        if (obj.find()) {
            String candidate = obj.group();
            if (isValidJson(candidate)) {
                return Optional.of(candidate.trim());
            }
        }

        Matcher arr = FIRST_JSON_ARRAY.matcher(text);
        if (arr.find()) {
            String candidate = arr.group();
            if (isValidJson(candidate)) {
                return Optional.of(candidate.trim());
            }
        }

        // If we still have braces but it's invalid, surface a clearer message.
        if (text.contains("{") || text.contains("[")) {
            throw new RuntimeException(
                "AI response did not contain a complete valid JSON payload. " +
                "Make sure the model outputs a single JSON object that ends with the matching closing brace." +
                " Raw fragment: " + abbreviate(text, 600)
            );
        }

        return Optional.empty();
    }

    private boolean isValidJson(String candidate) {
        try {
            objectMapper.readTree(candidate);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String abbreviate(String s, int maxLen) {
        if (s == null) {
            return "";
        }
        if (s.length() <= maxLen) {
            return s;
        }
        return s.substring(0, maxLen) + "...";
    }
}