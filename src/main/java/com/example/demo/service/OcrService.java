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

            System.out.println("Calling PaddleOCR service...");
            
            // 调用 PaddleOCR API
            ResponseEntity<String> response = restTemplate.postForEntity(
                PADDLEOCR_URL,
                requestEntity,
                String.class
            );

            String responseBody = response.getBody();
            System.out.println("PaddleOCR response: " + responseBody);

            // 解析 OCR 结果
            return parseOcrResponse(responseBody);
            
        } catch (Exception e) {
            System.err.println("PaddleOCR extraction failed: " + e.getMessage());
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
                throw new RuntimeException("Invalid OCR response format");
            }

            StringBuilder extractedText = new StringBuilder();

            // 遍历所有图片的结果（通常只有一张）
            for (JsonNode imageResult : resultsNode) {
                JsonNode dataNode = imageResult.path("data");
                
                if (dataNode.isArray()) {
                    // 遍历所有识别的文本行
                    for (JsonNode textBlock : dataNode) {
                        String text = textBlock.path("text").asText();
                        if (!text.isEmpty()) {
                            extractedText.append(text).append("\n");
                        }
                    }
                }
            }

            String result = extractedText.toString().trim();
            System.out.println("Extracted text from OCR:\n" + result);
            
            if (result.isEmpty()) {
                throw new RuntimeException("No text extracted from image");
            }

            return result;
            
        } catch (Exception e) {
            System.err.println("Error parsing OCR response: " + e.getMessage());
            throw new RuntimeException("Failed to parse OCR response", e);
        }
    }
}
