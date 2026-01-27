package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.model.CompanyInfo;
import java.util.Base64;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class AiExtractService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CompanyInfo extract(String prompt) {
        Map<String, Object> request = Map.of(
            "model", "llama3",
            "prompt", prompt,
            "stream", false
        );

        ResponseEntity<Map> response = restTemplate.postForEntity(
            "http://localhost:11434/api/generate",
            request,
            Map.class
        );

        String rawJson = (String) response.getBody().get("response");
        
        System.out.println("Raw AI response: " + rawJson);

        try {
            // Clean up the response to extract JSON
            String cleanedJson = cleanJsonResponse(rawJson);
            System.out.println("Cleaned JSON: " + cleanedJson);
            
            return objectMapper.readValue(cleanedJson, CompanyInfo.class);
        } catch (Exception e) {
            System.err.println("Failed to parse AI response: " + rawJson);
            throw new RuntimeException("AI response parsing failed: " + e.getMessage(), e);
        }
    }

    public CompanyInfo extractFromImage(String prompt, byte[] imageData) {
        // Encode image to base64
        String base64Image = Base64.getEncoder().encodeToString(imageData);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "llava");
        requestBody.put("prompt", prompt);
        requestBody.put("images", new String[]{base64Image});
        requestBody.put("stream", false);
        
        System.out.println("Processing image with llava model...");
        
        ResponseEntity<String> response = restTemplate.postForEntity(
            "http://localhost:11434/api/generate",
            requestBody,
            String.class
        );
        
        String responseBody = response.getBody();
        System.out.println("Raw Ollama response: " + responseBody);
        
        // Parse the response - llava returns the response in a different format
        return parseOllamaResponse(responseBody);
    }

    private CompanyInfo parseOllamaResponse(String jsonResponse) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode rootNode = mapper.readTree(jsonResponse);
            
            // Extract the actual response text from Ollama's response
            String responseText = rootNode.path("response").asText();
            
            System.out.println("Extracted response text: " + responseText);
            
            // Clean up the response to extract JSON
            String cleanedJson = cleanJsonResponse(responseText);
            
            System.out.println("Cleaned response text: " + cleanedJson);
            
            // Check if responseText is empty or not valid JSON
            if (cleanedJson.isEmpty()) {
                throw new RuntimeException("Empty response from AI model");
            }
            
            // Now parse the actual JSON data
            return mapper.readValue(cleanedJson, CompanyInfo.class);
        } catch (Exception e) {
            System.err.println("Error parsing Ollama response: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse AI response", e);
        }
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
        
        // Try to find JSON object in the response
        // Look for the first { and last }
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            cleaned = cleaned.substring(firstBrace, lastBrace + 1);
        }
        
        // Try to find JSON array in the response if no object found
        if (!cleaned.startsWith("{")) {
            int firstBracket = cleaned.indexOf('[');
            int lastBracket = cleaned.lastIndexOf(']');
            
            if (firstBracket >= 0 && lastBracket > firstBracket) {
                cleaned = cleaned.substring(firstBracket, lastBracket + 1);
            }
        }
        
        return cleaned.trim();
    }
}