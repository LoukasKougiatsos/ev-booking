package com.evbooking.server.admin.controller;

import com.evbooking.server.admin.dto.ConnectorAdminRequest;
import com.evbooking.server.admin.dto.ConnectorAdminResponse;
import com.evbooking.server.admin.dto.StationAdminRequest;
import com.evbooking.server.admin.dto.StationAdminResponse;
import com.evbooking.server.admin.service.AdminService;
import com.evbooking.server.booking.dto.BookingResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/bookings")
    public List<BookingResponse> listBookings() {
        return adminService.listAllBookings();
    }

    @GetMapping("/stations")
    public List<StationAdminResponse> listStations() {
        return adminService.listStations();
    }

    @PostMapping("/stations")
    public StationAdminResponse createStation(@RequestBody StationAdminRequest request) {
        return adminService.createStation(request);
    }

    @PutMapping("/stations/{id}")
    public StationAdminResponse updateStation(@PathVariable Long id, @RequestBody StationAdminRequest request) {
        return adminService.updateStation(id, request);
    }

    @DeleteMapping("/stations/{id}")
    public void deleteStation(@PathVariable Long id) {
        adminService.deleteStation(id);
    }

    @GetMapping("/connectors")
    public List<ConnectorAdminResponse> listConnectors() {
        return adminService.listConnectors();
    }

    @PostMapping("/connectors")
    public ConnectorAdminResponse createConnector(@RequestBody ConnectorAdminRequest request) {
        return adminService.createConnector(request);
    }

    @PutMapping("/connectors/{id}")
    public ConnectorAdminResponse updateConnector(@PathVariable Long id, @RequestBody ConnectorAdminRequest request) {
        return adminService.updateConnector(id, request);
    }

    @DeleteMapping("/connectors/{id}")
    public void deleteConnector(@PathVariable Long id) {
        adminService.deleteConnector(id);
    }
}
