package org.example.project_logistic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@NoArgsConstructor // Создает пустой конструктор
@AllArgsConstructor // Создает конструктор со всеми полями
@Data
public class AssignmentResult {
    // Карта: ID водителя -> Список объектов заказов
    private Map<String, List<Order>> assignments;
    // Список объектов заказов, которые не удалось назначить
    private List<Order> unassignedOrders;
}