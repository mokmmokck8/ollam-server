package com.example.demo.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Simplified CompanyInfo model matching the prompt definition
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CompanyInfo {
    private String companyName;
    private String entityIdentifier;
    private String countryISOCode;
    private String companyType;

    // Default constructor
    public CompanyInfo() {
    }

    // Constructor with all fields
    public CompanyInfo(String companyName, String entityIdentifier, String countryISOCode, String companyType) {
        this.companyName = companyName;
        this.entityIdentifier = entityIdentifier;
        this.countryISOCode = countryISOCode;
        this.companyType = companyType;
    }

    // Getters and Setters
    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getEntityIdentifier() {
        return entityIdentifier;
    }

    public void setEntityIdentifier(String entityIdentifier) {
        this.entityIdentifier = entityIdentifier;
    }

    public String getCountryISOCode() {
        return countryISOCode;
    }

    public void setCountryISOCode(String countryISOCode) {
        this.countryISOCode = countryISOCode;
    }

    public String getCompanyType() {
        return companyType;
    }

    public void setCompanyType(String companyType) {
        this.companyType = companyType;
    }

    @Override
    public String toString() {
        return "CompanyInfo{" +
                "companyName='" + companyName + '\'' +
                ", entityIdentifier='" + entityIdentifier + '\'' +
                ", countryISOCode='" + countryISOCode + '\'' +
                ", companyType='" + companyType + '\'' +
                '}';
    }
}