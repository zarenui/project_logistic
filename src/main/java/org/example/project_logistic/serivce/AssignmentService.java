package org.example.project_logistic.serivce;

import org.example.project_logistic.model.AssignmentResult;
import org.example.project_logistic.model.Driver;
import org.example.project_logistic.model.Order;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssignmentService {

    public AssignmentResult assign(List<Driver> drivers, List<Order> orders) {
        Map<String, List<Order>> assignments = new HashMap<>();
        List<Order> unassignedOrders = new ArrayList<>();

        // 1. Инициализация: Опыт = Лимит остановок
        for (Driver driver : drivers) {
            driver.setMaxStops(driver.getExperienceLevel());
            assignments.put(driver.getDriverId(), new ArrayList<>());
        }

        // 2. Сортировка заказов по сложности (требуемому опыту) для честного распределения
        orders.sort(Comparator.comparingInt(Order::getRequiredExperienceLevel).reversed());

        for (Order order : orders) {
            Driver bestDriver = null;
            double minDistance = Double.MAX_VALUE;

            for (Driver driver : drivers) {
                // Проверка 1: Подходит ли опыт водителя для этого заказа
                boolean experienceMatch = driver.getExperienceLevel() >= order.getRequiredExperienceLevel();
                // Проверка 2: Есть ли место в машине (лимит по опыту)
                boolean hasCapacity = assignments.get(driver.getDriverId()).size() < driver.getMaxStops();

                if (experienceMatch && hasCapacity) {
                    // Расчет расстояния (упрощенный или по формуле)
                    double dist = calculateDistance(55.75, 37.62, order.getLat(), order.getLon());

                    // Штраф за загруженность, чтобы распределять заказы "графом" (равномерно)
                    double loadFactor = assignments.get(driver.getDriverId()).size() * 0.5;
                    double totalScore = dist + loadFactor;

                    if (totalScore < minDistance) {
                        minDistance = totalScore;
                        bestDriver = driver;
                    }
                }
            }

            if (bestDriver != null) {
                assignments.get(bestDriver.getDriverId()).add(order);
            } else {
                unassignedOrders.add(order);
            }
        }

        AssignmentResult result = new AssignmentResult();
        result.setAssignments(assignments);
        result.setUnassignedOrders(unassignedOrders);
        return result;
    }
    // Вспомогательный метод для подсчета текущего веса в машине
    private double calculateCurrentLoad(List<String> assignedOrderIds, List<Order> allOrders) {
        return allOrders.stream()
                .filter(o -> assignedOrderIds.contains(o.getOrderId()))
                .mapToDouble(Order::getWeight)
                .sum();
    }

    // Метод расчета расстояния (формула гаверсинусов или упрощенная)
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2));
    }
}