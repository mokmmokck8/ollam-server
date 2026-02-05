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
    private final OcrService ocrService;

    public DocumentUploadController(
        PdfParseService pdfParseService,
        AiExtractService aiExtractService,
        PromptBuilder promptBuilder,
        OcrService ocrService
    ) {
        this.pdfParseService = pdfParseService;
        this.aiExtractService = aiExtractService;
        this.promptBuilder = promptBuilder;
        this.ocrService = ocrService;
    }

    @PostMapping("/upload")
    public CompanyInfo upload(@RequestParam("file") MultipartFile file) {
        String contentType = file.getContentType();
        
        if (contentType == null) {
            throw new IllegalArgumentException("Unable to determine file type");
        }
        
        // Handle image files with OCR + Llama (two-step process)
        if (contentType.startsWith("image/")) {
            try {
                // Step 1: Use PaddleOCR to extract text from image
                byte[] imageData = file.getBytes();
                String extractedText = ocrService.extractTextFromImage(imageData, file.getOriginalFilename());
                
                System.out.println("OCR extracted text, now processing with Llama...");
                
                // Step 2: Use Llama to analyze the extracted text
                String prompt = promptBuilder.build(extractedText);
                return aiExtractService.extract(prompt);
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