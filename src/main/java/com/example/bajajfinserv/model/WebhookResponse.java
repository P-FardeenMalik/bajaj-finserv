package com.example.bajajfinserv.model;

import lombok.Data;

@Data
public class WebhookResponse {
    private String webhook;
    private String accessToken;
    private Object data;
}