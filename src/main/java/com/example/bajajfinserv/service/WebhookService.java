package com.example.bajajfinserv.service;

import com.example.bajajfinserv.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class WebhookService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public WebhookService(RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
    }

    public WebhookResponse generateWebhook(WebhookRequest request) {
        String url = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook";
        HttpEntity<WebhookRequest> httpEntity = new HttpEntity<>(request);
        
        logger.info("Calling generateWebhook endpoint with request: {}", request);
        
        ResponseEntity<WebhookResponse> response = restTemplate.exchange(
                url,
                HttpMethod.POST,
                httpEntity,
                WebhookResponse.class
        );
        
        logger.info("Received webhook response: {}", response.getBody());
        return response.getBody();
    }

    @Retryable(maxAttempts = 4, backoff = @Backoff(delay = 1000))
    public void sendResultToWebhook(String webhookUrl, String accessToken, WebhookResult result) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", accessToken);
        headers.set("Content-Type", "application/json");
        
        HttpEntity<WebhookResult> httpEntity = new HttpEntity<>(result, headers);
        
        logger.info("Sending result to webhook: {}", result);
        
        ResponseEntity<String> response = restTemplate.exchange(
                webhookUrl,
                HttpMethod.POST,
                httpEntity,
                String.class
        );
        
        logger.info("Webhook response status: {}", response.getStatusCode());
    }

    public Object solveAssignedProblem(WebhookResponse webhookResponse, String regNo) {
        
        int lastTwoDigits;
        try {
            lastTwoDigits = Integer.parseInt(regNo.substring(regNo.length() - 2));
        } catch (NumberFormatException e) {
            logger.error("Could not parse last two digits of regNo: {}", regNo);
            return null;
        }
        
        boolean isOdd = lastTwoDigits % 2 != 0;
        logger.info("Registration number last two digits: {}, isOdd: {}", lastTwoDigits, isOdd);
        
        if (isOdd) {
            logger.info("Solving Problem 1: Mutual Followers");
            return solveProblem1(webhookResponse.getData());
        } else {
            logger.info("Solving Problem 2: Nth-Level Followers");
            return solveProblem2(webhookResponse.getData());
        }
    }

    private List<List<Integer>> solveProblem1(Object data) {
        Problem1Data problem1Data = objectMapper.convertValue(data, Problem1Data.class);
        List<User> users = problem1Data.getUsers();
        
        List<List<Integer>> mutualFollowers = new ArrayList<>();
        
        for (User user1 : users) {
            for (User user2 : users) {
                if (user1.getId() < user2.getId() && 
                    user1.getFollows().contains(user2.getId()) && 
                    user2.getFollows().contains(user1.getId())) {
                    
                    mutualFollowers.add(Arrays.asList(user1.getId(), user2.getId()));
                }
            }
        }
        
        logger.info("Problem 1 solution: {}", mutualFollowers);
        return mutualFollowers;
    }

    private List<Integer> solveProblem2(Object data) {
        Problem2Data problem2Data = objectMapper.convertValue(data, Problem2Data.class);
        
        int findId = problem2Data.getFindId();
        int n = problem2Data.getN();
        List<User> users = problem2Data.getUsers();
        
        logger.info("Problem 2 parameters: findId={}, n={}", findId, n);
        
        
        Map<Integer, User> userMap = new HashMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user);
        }
        
        
        Set<Integer> currentLevel = new HashSet<>();
        currentLevel.add(findId);
        
        
        Set<Integer> foundIds = new HashSet<>(currentLevel);
        
        
        for (int level = 0; level < n; level++) {
            Set<Integer> nextLevel = new HashSet<>();
            
            for (Integer id : currentLevel) {
                User user = userMap.get(id);
                if (user != null && user.getFollows() != null) {
                    nextLevel.addAll(user.getFollows());
                }
            }
            
            
            nextLevel.removeAll(foundIds);
            foundIds.addAll(nextLevel);
            currentLevel = nextLevel;
        }
        
        List<Integer> result = new ArrayList<>(currentLevel);
        Collections.sort(result);
        
        logger.info("Problem 2 solution: {}", result);
        return result;
    }
}