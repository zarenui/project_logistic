package org.example.project_logistic.serivce;

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

    public AssignmentResult assign(List<Driver> drivers, List<Order> orders) {
        Map<String, List<Order>> assignments = new HashMap<>();
        List<Order> unassignedOrders = new ArrayList<>();

        for (Driver driver : drivers) {
            driver.setMaxStops(driver.getExperienceLevel());
            assignments.put(driver.getDriverId(), new ArrayList<>());
        }

        orders.sort(Comparator.comparingInt(Order::getRequiredExperienceLevel).reversed());

        for (Order order : orders) {
            Driver bestDriver = null;
            double minCost = Double.MAX_VALUE; // Теперь это не просто дистанция, а "стоимость" пути

            // РАСЧЕТ ЗАРАНЕЕ: Получаем коэффициент для времени конкретного заказа
            double trafficFactor = getSmoothTrafficFactor(order.getOpenTime());

            for (Driver driver : drivers) {
                boolean experienceMatch = driver.getExperienceLevel() >= order.getRequiredExperienceLevel();
                boolean hasCapacity = assignments.get(driver.getDriverId()).size() < driver.getMaxStops();

                if (experienceMatch && hasCapacity) {
                    // Базовое расстояние
                    double baseDist = calculateDistance(55.75, 37.62, order.getLat(), order.getLon());

                    // Эффективное расстояние (с учетом пробок)
                    double effectiveDist = baseDist * trafficFactor;

                    // Штраф за загруженность (балансировка между водителями)
                    double loadFactor = assignments.get(driver.getDriverId()).size() * 0.5;

                    // Итоговая стоимость назначения
                    double totalCost = effectiveDist + loadFactor;

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

    private double calculateRoadDistance(double lat1, double lon1, double lat2, double lon2) {
        try {
            // Используем публичный демо-сервер OSRM (для продакшена лучше поднять свой в Docker)
            String url = String.format("http://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=false",
                    lon1, lat1, lon2, lat2);

            RestTemplate restTemplate = new RestTemplate();
            String response = restTemplate.getForObject(url, String.class);

            // Достаем дистанцию в метрах из JSON ответа
            JSONObject json = new JSONObject(response);
            return json.getJSONArray("routes")
                    .getJSONObject(0)
                    .getDouble("distance") / 1000.0; // Переводим в км
        } catch (Exception e) {
            // Если сервис упал, откатываемся к обычной математике (с запасом 30%)
            return calculateDistance(lat1, lon1, lat2, lon2) * 1.3;
        }
    }
    // Расчет плавного коэффициента пробок (от 1.0 до 1.7)
    private double getSmoothTrafficFactor(LocalTime time) {
        if (time == null) return 1.0; // Защита от NPE

        // Переводим время в минуты от полуночи (0 - 1440)
        int minutes = time.getHour() * 60 + time.getMinute();

        // Утренний час пик (пик в 08:30 = 510 минут). Ширина пика (sigma) = 90 минут.
        double morningPeak = 0.7 * Math.exp(-Math.pow(minutes - 510, 2) / (2 * Math.pow(90, 2)));

        // Вечерний час пик (пик в 18:30 = 1110 минут). Ширина пика (sigma) = 120 минут.
        double eveningPeak = 0.7 * Math.exp(-Math.pow(minutes - 1110, 2) / (2 * Math.pow(120, 2)));

        // Итоговый коэффициент (база 1.0 + влияние пиков)
        double factor = 1.0 + morningPeak + eveningPeak;

        // Жестко ограничиваем максимум, чтобы из-за наложений не превысить 1.7
        return Math.min(factor, 1.7);
    }
}