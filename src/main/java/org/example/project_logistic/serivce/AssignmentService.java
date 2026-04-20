package org.example.project_logistic.serivce;

import org.example.project_logistic.model.AssignmentResult;
import org.example.project_logistic.model.Driver;
import org.example.project_logistic.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssignmentService {

    public AssignmentResult assign(List<Order> orders, List<Driver> drivers) {
        Map<String, List<String>> assignments = new HashMap<>();
        List<String> unassigned = new ArrayList<>();

        // Инициализируем списки для каждого водителя
        for (Driver d : drivers) assignments.put(d.getDriverId(), new ArrayList<>());

        // Простейшая реализация жадного алгоритма
        for (Order order : orders) {
            boolean assigned = false;
            for (Driver driver : drivers) {
                if (driver.getCurrentStops() < driver.getMaxStops() &&
                        driver.getExperienceLevel() >= order.getRequiredExperienceLevel()) {

                    assignments.get(driver.getDriverId()).add(order.getOrderId());
                    driver.setCurrentStops(driver.getCurrentStops() + 1);
                    assigned = true;
                    break;
                }
            }
            if (!assigned) unassigned.add(order.getOrderId());
        }

        return new AssignmentResult(assignments, unassigned);
    }

    private double calculateHaversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371; // Радиус Земли в км
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}