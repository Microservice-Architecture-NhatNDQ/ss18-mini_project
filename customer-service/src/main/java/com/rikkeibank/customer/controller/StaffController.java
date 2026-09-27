package com.rikkeibank.customer.controller;

import com.rikkeibank.customer.dto.StaffDto;
import com.rikkeibank.customer.service.StaffService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/staff")
@RequiredArgsConstructor
@Slf4j
public class StaffController {

    private final StaffService staffService;

    @PostMapping
    public ResponseEntity<?> createStaff(
            @Valid @RequestBody StaffDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can create staff"));
        }
        StaffDto created = staffService.createStaff(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<?> getAllStaff(@RequestHeader(value = "X-User-Roles", required = false) String roles) {
        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can view staff list"));
        }
        List<StaffDto> list = staffService.getAllStaff();
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<StaffDto> getStaffById(@PathVariable Long id) {
        StaffDto staff = staffService.getStaffById(id);
        return ResponseEntity.ok(staff);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateStaff(
            @PathVariable Long id,
            @Valid @RequestBody StaffDto dto,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can update staff"));
        }
        StaffDto updated = staffService.updateStaff(id, dto);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteStaff(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {

        if (roles != null && !roles.contains("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Only ADMIN can delete staff"));
        }
        staffService.deleteStaff(id);
        return ResponseEntity.ok(Map.of("message", "Staff deactivated successfully", "id", id));
    }
}
