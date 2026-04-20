package org.example.project_logistic.model;

import lombok.Data;

@Data
public class Driver {
    private String driverId;
    private String name;
    private int experienceLevel;
    private int maxStops;
    private double vehicleCapacity;

    // Поля для состояния во время работы алгоритма
    private int currentStops;
    private Double lastLat;
    private Double lastLon;
}