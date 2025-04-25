package com.example.bajajfinserv.runner;

import com.example.bajajfinserv.model.WebhookRequest;
import com.example.bajajfinserv.model.WebhookResponse;
import com.example.bajajfinserv.model.WebhookResult;
import com.example.bajajfinserv.service.WebhookService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class WebhookRunner implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(WebhookRunner.class);
    
    private final WebhookService webhookService;
    
    @Autowired
    public WebhookRunner(WebhookService webhookService) {
        this.webhookService = webhookService;
    }
    
    @Override
    public void run(ApplicationArguments args) {
        logger.info("Starting application and calling webhook...");
        
        try {
            // Create the webhook request
            WebhookRequest request = new WebhookRequest();
            request.setName("John Doe");
            request.setRegNo("REG12347");
            request.setEmail("john@example.com");
            
            // Call the webhook generation endpoint
            WebhookResponse response = webhookService.generateWebhook(request);
            logger.info("Received webhook URL: {}", response.getWebhook());
            
            // Solve the assigned problem
            Object outcome = webhookService.solveAssignedProblem(response, request.getRegNo());
            
            // Create the result object
            WebhookResult result = new WebhookResult();
            result.setRegNo(request.getRegNo());
            result.setOutcome(outcome);
            
            // Send the result to the provided webhook
            logger.info("Sending result to webhook...");
            webhookService.sendResultToWebhook(response.getWebhook(), response.getAccessToken(), result);
            
            logger.info("Webhook process completed successfully.");
        } catch (Exception e) {
            logger.error("Error during webhook process", e);
        }
    }
}