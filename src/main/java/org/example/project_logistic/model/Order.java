package org.example.project_logistic.model;

import lombok.Data;
import java.time.LocalTime;

@Data
public class Order {
    private String orderId;
    private double lat;
    private double lon;
    private LocalTime openTime;
    private LocalTime closeTime;
    private int requiredExperienceLevel;
}