package com.agriscope.notification_service.service;

import com.agriscope.notification_service.model.Recommendation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EmailTemplateServiceTest {

    private EmailTemplateService emailTemplateService;

    @BeforeEach
    void setUp() {
        emailTemplateService = new EmailTemplateService();
    }

    @Test
    @DisplayName("Should generate Welcome Email with correct name")
    void buildWelcomeEmailHtml_Success() {
        String firstName = "Nemanja";
        String html = emailTemplateService.buildWelcomeEmailHtml(firstName);

        assertNotNull(html);
        assertTrue(html.contains("Hi Nemanja, Welcome Aboard!"), "Email should contain personalized greeting");
        assertTrue(html.contains("http://localhost:4200/login"), "Email should contain login link");

        assertTrue(html.contains("automated email from Agriscope"), "Email should contain footer signature");
    }

    @Test
    @DisplayName("Should generate Alert Email with all details present")
    void buildAlertEmailHtml_FullDetails() {
        Recommendation rec = new Recommendation();
        rec.setRecommendationType("FROST_ALERT");
        rec.setRecommendedSeed("CORN");
        rec.setReasoning("Temperature dropping below zero.");
        rec.setAdvice("Cover crops immediately.");

        String fieldName = "North Field";

        String html = emailTemplateService.buildAlertEmailHtml(rec, fieldName);

        assertNotNull(html);

        assertTrue(html.contains("Frost Alert") || html.contains("FROST ALERT"), "Title should be formatted correctly");
        assertTrue(html.contains("Corn") || html.contains("CORN"), "Seed name should be formatted");

        assertTrue(html.contains("Temperature dropping below zero."), "Reasoning should be present");
        assertTrue(html.contains("Cover crops immediately."), "Advice should be present");
    }

    @Test
    @DisplayName("Should handle Alert Email with missing reasoning/advice gracefully")
    void buildAlertEmailHtml_MissingData() {
        Recommendation rec = new Recommendation();
        rec.setRecommendationType("IRRIGATE_NOW");
        rec.setRecommendedSeed("WHEAT");
        rec.setReasoning(null);
        rec.setAdvice(null);

        String fieldName = "South Field";

        String html = emailTemplateService.buildAlertEmailHtml(rec, fieldName);

        assertTrue(html.contains("No details provided."), "Should handle null reasoning");
    }
}