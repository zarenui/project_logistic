package org.example.project_logistic.service;

import org.example.project_logistic.model.AssignmentResult;
import org.example.project_logistic.model.Driver;
import org.example.project_logistic.model.Order;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalTime;
import java.util.*;

@Service
public class AssignmentService {

    // Координаты базы (депо), откуда все водители начинают путь
    private static final double DEPOT_LAT = 55.7558;
    private static final double DEPOT_LON = 37.6173;

    public AssignmentResult assign(List<Driver> drivers, List<Order> orders) {
        Map<String, List<Order>> assignments = new HashMap<>();
        List<Order> unassignedOrders = new ArrayList<>();

        // 1. Подготовка водителей
        for (Driver driver : drivers) {
            // Условие: уровень опыта = макс. остановок
            driver.setMaxStops(driver.getExperienceLevel());
            // Условие: макс. вес (например, 100кг на 1 уровень опыта, или возьми из поля)
            // Если в классе Driver есть getVehicleCapacity(), используй его
            assignments.put(driver.getDriverId(), new ArrayList<>());
        }

        // 2. Сортировка: сначала самые сложные заказы (требующие высокого опыта)
        orders.sort(Comparator.comparingInt(Order::getRequiredExperienceLevel).reversed());

        for (Order order : orders) {
            Driver bestDriver = null;
            double minCost = Double.MAX_VALUE;

            // Фактор пробок для текущего времени заказа
            double trafficFactor = getSmoothTrafficFactor(order.getOpenTime());

            for (Driver driver : drivers) {
                List<Order> currentOrders = assignments.get(driver.getDriverId());

                // ПРОВЕРКА 1: Опыт
                boolean expMatch = driver.getExperienceLevel() >= order.getRequiredExperienceLevel();

                // ПРОВЕРКА 2: Лимит по количеству остановок
                boolean stopsMatch = currentOrders.size() < driver.getMaxStops();

                // ПРОВЕРКА 3: Лимит по весу (используем наш метод)
                double currentWeight = calculateCurrentLoad(currentOrders);
                // Предположим, лимит веса = уровень опыта * 50 (или другое поле)
                double maxWeightLimit = driver.getExperienceLevel() * 50.0;
                boolean weightMatch = (currentWeight + order.getWeight()) <= maxWeightLimit;

                if (expMatch && stopsMatch && weightMatch) {
                    // РАСЧЕТ: Расстояние по дорогам через OSRM
                    // Считаем от депо до точки заказа (или от последней точки водителя)
                    double roadDist = calculateRoadDistance(DEPOT_LAT, DEPOT_LON, order.getLat(), order.getLon());

                    // Итоговая стоимость: (Дороги * Пробки) + Штраф за занятость
                    double loadPenalty = currentOrders.size() * 0.8;
                    double totalCost = (roadDist * trafficFactor) + loadPenalty;

                    if (totalCost < minCost) {
                        minCost = totalCost;
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

    // Вспомогательный метод: теперь работает с List<Order> напрямую
    private double calculateCurrentLoad(List<Order> assignedOrders) {
        return assignedOrders.stream()
                .mapToDouble(Order::getWeight)
                .sum();
    }

    // Запасной метод: прямая линия
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // Упрощенная формула (в градусах), для точности лучше Haversine
        return Math.sqrt(Math.pow(lat2 - lat1, 2) + Math.pow(lon2 - lon1, 2)) * 111.0;
    }

    // Основной метод: реальные дороги
    private double calculateRoadDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            // ВАЖНО: OSRM ждет параметры в формате (lon,lat;lon,lat)
            String url = String.format(Locale.US, "http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                    lon1, lat1, lon2, lat2);

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            JSONObject json = new JSONObject(response);
            return json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getDouble("distance") / 1000.0; // в километры
        } catch (Exception e) {
            // Если OSRM недоступен или лимит запросов превышен
            return calculateDistance(lat1, lon1, lat2, lon2) * 1.4; // +40% на изгибы дорог
        }
    }

    private double getSmoothTrafficFactor(LocalTime time) {
        if (time == null) return 1.0;
        int minutes = time.getHour() * 60 + time.getMinute();

        // Пики: 08:30 (510 мин) и 18:30 (1110 мин)
        double morning = 0.7 * Math.exp(-Math.pow(minutes - 510, 2) / (2 * Math.pow(80, 2)));
        double evening = 0.7 * Math.exp(-Math.pow(minutes - 1110, 2) / (2 * Math.pow(100, 2)));

        return Math.min(1.0 + morning + evening, 1.7);
    }
}