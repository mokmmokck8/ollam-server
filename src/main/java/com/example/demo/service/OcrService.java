package com.example.demo.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.io.ByteArrayResource;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OcrService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private static final String PADDLEOCR_URL = "http://localhost:8866/predict/ocr_system";

    public OcrService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(60000);
        factory.setReadTimeout(60000);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 使用 PaddleOCR 从图片中提取文字
     * @param imageData 图片字节数据
     * @param filename 文件名
     * @return 提取的文字内容
     */
    public String extractTextFromImage(byte[] imageData, String filename) {
        try {
            if (imageData == null || imageData.length == 0) {
                throw new IllegalArgumentException("Image data is empty");
            }
            
            System.out.println("Processing image: " + filename + " (size: " + imageData.length + " bytes)");
            
            // 准备请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            // 准备文件数据
            ByteArrayResource fileResource = new ByteArrayResource(imageData) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            // 构建 multipart 请求
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("images", fileResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            System.out.println("Calling PaddleOCR service at: " + PADDLEOCR_URL);
            
            // 调用 PaddleOCR API
            ResponseEntity<String> response = restTemplate.postForEntity(
                PADDLEOCR_URL,
                requestEntity,
                String.class
            );

            String responseBody = response.getBody();
            System.out.println("PaddleOCR response status: " + response.getStatusCode());
            System.out.println("PaddleOCR response body: " + responseBody);

            // 检查响应是否包含错误
            if (responseBody != null && responseBody.contains("\"error\"")) {
                JsonNode errorNode = objectMapper.readTree(responseBody);
                String errorMessage = errorNode.path("error").asText();
                String errorDetails = errorNode.path("details").asText("");
                System.err.println("PaddleOCR service returned error: " + errorMessage);
                if (!errorDetails.isEmpty()) {
                    System.err.println("Error details: " + errorDetails);
                }
                throw new RuntimeException("PaddleOCR service error: " + errorMessage);
            }

            // 解析 OCR 结果
            return parseOcrResponse(responseBody);
            
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.err.println("PaddleOCR service error (HTTP " + e.getStatusCode() + "): " + e.getResponseBodyAsString());
            throw new RuntimeException("PaddleOCR service encountered an error. Please check if the image is valid and the service is running properly.", e);
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.err.println("Cannot connect to PaddleOCR service: " + e.getMessage());
            throw new RuntimeException("Cannot connect to PaddleOCR service at " + PADDLEOCR_URL + ". Please ensure the service is running.", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error during OCR extraction: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to extract text using PaddleOCR", e);
        }
    }

    /**
     * 解析 PaddleOCR 的 JSON 响应，提取所有识别的文字
     */
    private String parseOcrResponse(String jsonResponse) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode resultsNode = rootNode.path("results");

            if (resultsNode.isMissingNode() || !resultsNode.isArray()) {
                System.err.println("Invalid OCR response format - missing or invalid 'results' field");
                System.err.println("Response was: " + jsonResponse);
                throw new RuntimeException("Invalid OCR response format: missing 'results' field");
            }

            StringBuilder extractedText = new StringBuilder();
            int totalTextBlocks = 0;

            // 遍历所有图片的结果（通常只有一张）
            for (JsonNode imageResult : resultsNode) {
                JsonNode dataNode = imageResult.path("data");
                
                if (dataNode.isArray()) {
                    // 遍历所有识别的文本行
                    for (JsonNode textBlock : dataNode) {
                        String text = textBlock.path("text").asText();
                        double confidence = textBlock.path("confidence").asDouble(0.0);
                        
                        if (!text.isEmpty()) {
                            extractedText.append(text).append("\n");
                            totalTextBlocks++;
                            System.out.println("Extracted text line (confidence: " + 
                                String.format("%.2f", confidence) + "): " + text);
                        }
                    }
                }
            }

            String result = extractedText.toString().trim();
            
            if (result.isEmpty()) {
                System.err.println("No text was extracted from the image. The image might be:");
                System.err.println("  - Empty or blank");
                System.err.println("  - Contains no recognizable text");
                System.err.println("  - In an unsupported format or corrupted");
                System.err.println("  - Too low quality for OCR recognition");
                throw new RuntimeException("No text could be extracted from the image. Please ensure the image contains readable text.");
            }

            System.out.println("Successfully extracted " + totalTextBlocks + " text blocks from OCR");
            System.out.println("Total extracted text length: " + result.length() + " characters");

            return result;
            
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Error parsing OCR JSON response: " + e.getMessage());
            System.err.println("Response was: " + jsonResponse);
            throw new RuntimeException("Failed to parse OCR response JSON", e);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            System.err.println("Unexpected error parsing OCR response: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to parse OCR response", e);
        }
    }
}
