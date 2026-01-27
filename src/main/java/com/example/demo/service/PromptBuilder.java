package com.example.demo.service;

import org.springframework.stereotype.Component;

@Component
public class PromptBuilder {

    public String buildForVision() {
        return """
        You are an information extraction assistant analyzing a document image.

        Extract the following fields from the image:
        - companyName: The name of the company
        - entityIdentifier: The company's unique identification number. Look for any of the following:
          * Business Registration Number (BR Number)
          * Tax ID Number (TIN)
          * Unified Social Credit Code (USCC) - used in China (统一社会信用代码)
          * Corporate Number - used in Japan (法人番号)
          * Unique Entity Number (UEN) - used in Singapore
          * Company Registration Number (CRN)
          * VAT Number
          * Employer Identification Number (EIN)
          * Any similar official company identification number
          Extract only the number itself, without any prefix labels or explanations.
        - countryISOCode: If the document mentions a company address, extract it and return ONLY the ISO 3166-1 alpha-3 country code. This MUST be exactly 3 uppercase letters. For example: HKG for Hong Kong, USA for United States, GBR for United Kingdom, CHN for China, SGP for Singapore, JPN for Japan, etc. Do not return the full address, city name, or any other format - only the 3-letter country code.
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

        CRITICAL RULES:
        - Return ONLY valid JSON - NO explanations, NO introductory text, NO markdown formatting
        - Do NOT start your response with phrases like "Here is", "The JSON is", etc.
        - Do NOT wrap the JSON in code blocks or backticks
        - Start your response directly with { and end with }
        - If a field is missing, return null
        - Your entire response must be parseable as JSON

        Example:
        {
          "companyName": "ACME TECHNOLOGY LIMITED",
          "entityIdentifier": "12345678",
          "countryISOCode": "HKG",
          "companyType": "PRIVATE_COMPANY_LIMITED_BY_SHARES"
        }
        """;
    }

    public String build(String documentText) {
        return """
        You are an information extraction assistant.

        Extract the following fields from the document:
        - companyName: The name of the company
        - entityIdentifier: The company's unique identification number. Look for any of the following:
          * Business Registration Number (BR Number)
          * Tax ID Number (TIN)
          * Unified Social Credit Code (USCC) - used in China (统一社会信用代码)
          * Corporate Number - used in Japan (法人番号)
          * Unique Entity Number (UEN) - used in Singapore
          * Company Registration Number (CRN)
          * VAT Number
          * Employer Identification Number (EIN)
          * Any similar official company identification number
          Extract only the number itself, without any prefix labels or explanations.
        - countryISOCode: If the document mentions a company address, extract it and return ONLY the ISO 3166-1 alpha-3 country code. This MUST be exactly 3 uppercase letters. For example: HKG for Hong Kong, USA for United States, GBR for United Kingdom, CHN for China, SGP for Singapore, JPN for Japan, etc. Do not return the full address, city name, or any other format - only the 3-letter country code.
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

        CRITICAL RULES:
        - Return ONLY valid JSON - NO explanations, NO introductory text, NO markdown formatting
        - Do NOT start your response with phrases like "Here is", "The JSON is", etc.
        - Do NOT wrap the JSON in code blocks or backticks
        - Start your response directly with { and end with }
        - If a field is missing, return null
        - Your entire response must be parseable as JSON

        Example:
        {
          "companyName": "ACME TECHNOLOGY LIMITED",
          "entityIdentifier": "12345678",
          "countryISOCode": "HKG",
          "companyType": "PRIVATE_COMPANY_LIMITED_BY_SHARES"
        }

        Document text:
        %s
        """.formatted(documentText);
    }
}
