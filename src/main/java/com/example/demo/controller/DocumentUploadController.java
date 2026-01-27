package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.model.CompanyInfo;
import com.example.demo.service.*;

@RestController
@RequestMapping("/api")
public class DocumentUploadController {

    private final PdfParseService pdfParseService;
    private final AiExtractService aiExtractService;
    private final PromptBuilder promptBuilder;

    public DocumentUploadController(
        PdfParseService pdfParseService,
        AiExtractService aiExtractService,
        PromptBuilder promptBuilder
    ) {
        this.pdfParseService = pdfParseService;
        this.aiExtractService = aiExtractService;
        this.promptBuilder = promptBuilder;
    }

    @PostMapping("/upload")
    public CompanyInfo upload(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        
        if (contentType == null) {
            throw new IllegalArgumentException("Unable to determine file type");
        }
        
        // Handle image files with Ollama vision model
        if (contentType.startsWith("image/")) {
            try {
                byte[] imageData = file.getBytes();
                String prompt = promptBuilder.buildForVision();
                return aiExtractService.extractFromImage(prompt, imageData);
            } catch (Exception e) {
                throw new RuntimeException("Failed to process image file", e);
            }
        }
        // Handle PDF files with text extraction
        else if (contentType.equals("application/pdf")) {
            String text = pdfParseService.extractText(file);
            String prompt = promptBuilder.build(text);
            return aiExtractService.extract(prompt);
        }
        else {
            throw new IllegalArgumentException("Unsupported file type: " + contentType + ". Only PDF and images are supported.");
        }
    }
}