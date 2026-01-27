package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.model.CompanyInfo;
import java.util.Base64;
import java.util.Map;
import java.util.List;
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

        try {
            return objectMapper.readValue(rawJson, CompanyInfo.class);
        } catch (Exception e) {
            throw new RuntimeException("AI response parsing failed", e);
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
            
            // Clean up markdown formatting (remove backticks and json markers)
            responseText = responseText.replaceAll("```json\\s*", "")
                                       .replaceAll("```\\s*", "")
                                       .trim();
            
            System.out.println("Cleaned response text: " + responseText);
            
            // Now parse the actual JSON data
            JsonNode dataNode = mapper.readTree(responseText);
            
            return mapper.treeToValue(dataNode, CompanyInfo.class);
        } catch (Exception e) {
            System.err.println("Error parsing Ollama response: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse AI response", e);
        }
    }
}