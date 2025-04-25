package com.example.bajajfinserv.model;

import lombok.Data;

@Data
public class WebhookResult {
    private String regNo;
    private Object outcome;
}