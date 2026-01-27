package com.example.demo.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildForVision() {
        return """
        You are an information extraction assistant analyzing a document image.

        Extract the following fields from the image:
        - companyName: The name of the company
        - brNumber: The business registration number
        - companyAddress: If the document mentions a company address, extract it and return ONLY the ISO 3166-1 alpha-3 country code (3-letter code). For example: HKG for Hong Kong, USA for United States, GBR for United Kingdom, CHN for China, etc.
        - companyType: The type of the company. If the document mentions any of the following company types (or similar terms), output the corresponding value:
          * PRIVATE_COMPANY_LIMITED_BY_SHARES (e.g., "private limited", "私人有限公司")
          * PUBLIC_COMPANY_LIMITED_BY_SHARES (e.g., "public limited", "公眾有限公司")
          * PUBLIC_COMPANY_LIMITED_BY_GUARANTEE (e.g., "limited by guarantee")
          * LIMITED_LIABILITY_COMPANY (e.g., "LLC", "有限責任公司")
          * LIMITED_PARTNERSHIP (e.g., "LP", "有限合伙")
          * EXEMPTED_LIMITED_PARTNERSHIP (e.g., "ELP")
          * SEGREGATED_PORTFOLIO_COMPANY (e.g., "SPC")
          * EXEMPTED_COMPANY (e.g., "exempted company")
          * EXEMPTED_LIMITED_COMPANY (e.g., "exempted limited")
          * UNLIMITED_COMPANY (e.g., "unlimited")
          * GENERAL_PARTNERSHIP (e.g., "GP", "普通合伙")
          * SOLE_PROPRIETORSHIP (e.g., "sole proprietor", "獨資")
          * If none of the above match, return "OTHERS"

        Rules:
        - Return ONLY valid JSON
        - If a field is missing, return null
        - No explanation or additional text

        Example:
        {
          "companyName": "ACME TECHNOLOGY LIMITED",
          "brNumber": "12345678",
          "companyAddress": "HKG",
          "companyType": "PRIVATE_COMPANY_LIMITED_BY_SHARES"
        }
        """;
    }

    public String build(String documentText) {
        return """
        You are an information extraction assistant.

        Extract the following fields from the document:
        - companyName: The name of the company
        - brNumber: The business registration number
        - companyAddress: If the document mentions a company address, extract it and return ONLY the ISO 3166-1 alpha-3 country code (3-letter code). For example: HKG for Hong Kong, USA for United States, GBR for United Kingdom, CHN for China, etc.
        - companyType: The type of the company. If the document mentions any of the following company types (or similar terms), output the corresponding value:
          * PRIVATE_COMPANY_LIMITED_BY_SHARES (e.g., "private limited", "私人有限公司")
          * PUBLIC_COMPANY_LIMITED_BY_SHARES (e.g., "public limited", "公眾有限公司")
          * PUBLIC_COMPANY_LIMITED_BY_GUARANTEE (e.g., "limited by guarantee")
          * LIMITED_LIABILITY_COMPANY (e.g., "LLC", "有限責任公司")
          * LIMITED_PARTNERSHIP (e.g., "LP", "有限合伙")
          * EXEMPTED_LIMITED_PARTNERSHIP (e.g., "ELP")
          * SEGREGATED_PORTFOLIO_COMPANY (e.g., "SPC")
          * EXEMPTED_COMPANY (e.g., "exempted company")
          * EXEMPTED_LIMITED_COMPANY (e.g., "exempted limited")
          * UNLIMITED_COMPANY (e.g., "unlimited")
          * GENERAL_PARTNERSHIP (e.g., "GP", "普通合伙")
          * SOLE_PROPRIETORSHIP (e.g., "sole proprietor", "獨資")
          * If none of the above match, return "OTHERS"

        Rules:
        - Return ONLY valid JSON
        - If a field is missing, return null
        - No explanation or additional text

        Example:
        {
          "companyName": "ACME TECHNOLOGY LIMITED",
          "brNumber": "12345678",
          "companyAddress": "HKG",
          "companyType": "PRIVATE_COMPANY_LIMITED_BY_SHARES"
        }

        Document text:
        %s
        """.formatted(documentText);
    }
}
