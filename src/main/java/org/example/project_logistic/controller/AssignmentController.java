package org.example.project_logistic.controller;

import org.example.project_logistic.model.*;
import org.example.project_logistic.serivce.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/assignment")
public class AssignmentController {

    @Autowired
    private CsvParserService csvParserService;

    @Autowired
    private AssignmentService assignmentService;

    @PostMapping("/run")
    public AssignmentResult runAssignment(
            @RequestParam("orders") MultipartFile ordersFile,
            @RequestParam("drivers") MultipartFile driversFile) throws Exception {

        List<Order> orders = csvParserService.parseOrders(ordersFile);
        List<Driver> drivers = csvParserService.parseDrivers(driversFile);

        return assignmentService.assign(orders, drivers);
    }
}