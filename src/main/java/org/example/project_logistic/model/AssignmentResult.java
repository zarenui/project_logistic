package org.example.project_logistic.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
public class AssignmentResult {
    private Map<String, List<String>> assignments; // driverId -> list of orderIds
    private List<String> unassignedOrders;
}