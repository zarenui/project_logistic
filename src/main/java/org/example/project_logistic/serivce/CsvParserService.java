package org.example.project_logistic.serivce;

import org.example.project_logistic.model.Driver;
import org.example.project_logistic.model.Order;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvParserService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("[H:m[:s]]");

    private String getValue(CSVRecord record, String column, String defaultValue) {
        return record.isMapped(column) ? record.get(column) : defaultValue;
    }

    public List<Order> parseOrders(MultipartFile file) throws Exception {
        List<Order> orders = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                Order order = new Order();
                order.setOrderId(record.get("order_id"));
                order.setLat(Double.parseDouble(record.get("lat")));
                order.setLon(Double.parseDouble(record.get("lon")));
                order.setOpenTime(LocalTime.parse(record.get("open_time"), TIME_FORMATTER));
                order.setCloseTime(LocalTime.parse(record.get("close_time"), TIME_FORMATTER));
                order.setWeight(Double.parseDouble(getValue(record, "weight", "0")));
                order.setRequiredExperienceLevel(Integer.parseInt(getValue(record, "required_experience_level", "1")));
                orders.add(order);
            }
        }
        return orders;
    }

    public List<Driver> parseDrivers(MultipartFile file) throws Exception {
        List<Driver> drivers = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader().withIgnoreHeaderCase().withTrim())) {

            for (CSVRecord record : csvParser) {
                Driver driver = new Driver();
                driver.setDriverId(record.get("driver_id"));
                driver.setName(getValue(record, "name", "Unknown Driver"));
                driver.setExperienceLevel(Integer.parseInt(getValue(record, "experience_level", "1")));
                driver.setMaxStops(Integer.parseInt(getValue(record, "max_stops", "8")));
                driver.setVehicleCapacity(Double.parseDouble(getValue(record, "vehicle_capacity", "9999")));
                drivers.add(driver);
            }
        }
        return drivers;
    }
}