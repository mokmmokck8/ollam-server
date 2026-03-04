package com.example.demo.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.example.demo.model.CompanyInfo;
import com.example.demo.service.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

@RestController
@RequestMapping("/api")
public class DocumentUploadController {

    private final AiExtractService aiExtractService;
    private final PromptBuilder promptBuilder;
    private final OcrService ocrService;

    public DocumentUploadController(
        AiExtractService aiExtractService,
        PromptBuilder promptBuilder,
        OcrService ocrService
    ) {
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
        
        try {
            // Unified flow: regardless of image or PDF, run OCR first, then Llama.
            //
            // For PDFs, we OCR the raw bytes (works best for scan PDFs). For text PDFs this is
            // slower and may be worse than PDF text extraction, but matches your requested behavior.

            byte[] data = file.getBytes();
            String filename = file.getOriginalFilename();

            if (contentType.startsWith("image/")) {
                String extractedText = ocrService.extractTextFromImage(data, filename);
                System.out.println("OCR extracted text (image), now processing with Llama...");
                String prompt = promptBuilder.build(extractedText);
                return aiExtractService.extract(prompt);
            }

            if (contentType.equals("application/pdf")) {
                // PaddleOCR endpoint expects image bytes, so for PDFs we render pages to images then OCR.
                String extractedText = extractTextFromPdfViaOcr(data, filename);
                System.out.println("OCR extracted text (pdf), now processing with Llama...");
                String prompt = promptBuilder.build(extractedText);
                return aiExtractService.extract(prompt);
            }

            throw new IllegalArgumentException(
                "Unsupported file type: " + contentType + ". Only PDF and images are supported."
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to process uploaded file", e);
        }
    }

    private String extractTextFromPdfViaOcr(byte[] pdfBytes, String filename) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF data is empty");
        }

        try (PDDocument document = PDDocument.load(pdfBytes)) {
            PDFRenderer renderer = new PDFRenderer(document);
            int pages = document.getNumberOfPages();
            int maxPages = Math.min(pages, 5); // safety cap to avoid very large requests

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxPages; i++) {
                // 200 DPI is a reasonable balance between OCR accuracy and payload size.
                BufferedImage image = renderer.renderImageWithDPI(i, 200);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                ImageIO.write(image, "png", out);
                byte[] png = out.toByteArray();

                String pageText = ocrService.extractTextFromImage(png, (filename == null ? "document" : filename) + "-page-" + (i + 1) + ".png");
                if (pageText != null && !pageText.isBlank()) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(pageText);
                }
            }

            String result = sb.toString().trim();
            if (result.isEmpty()) {
                throw new RuntimeException("No text could be extracted from PDF via OCR");
            }
            return result;
        } catch (Exception e) {
            throw new RuntimeException("Failed to OCR PDF", e);
        }
    }
}