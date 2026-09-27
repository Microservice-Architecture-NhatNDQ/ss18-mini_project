package com.rikkeibank.customer.service;

import com.rikkeibank.customer.dto.StaffDto;
import com.rikkeibank.customer.entity.Staff;
import com.rikkeibank.customer.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class StaffService {

    private final StaffRepository staffRepository;

    @Transactional
    public StaffDto createStaff(StaffDto dto) {
        log.info("Creating staff with role: {}, branch: {}", dto.getRole(), dto.getBranchCode());

        String code = dto.getStaffCode();
        if (code == null || code.isBlank()) {
            code = "STF" + (System.currentTimeMillis() % 100000);
        }

        if (staffRepository.existsByStaffCode(code)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Staff code already exists");
        }

        Staff staff = Staff.builder()
                .staffCode(code)
                .userId(dto.getUserId())
                .fullName(dto.getFullName())
                .email(dto.getEmail())
                .phone(dto.getPhone())
                .branchCode(dto.getBranchCode())
                .department(dto.getDepartment())
                .role(dto.getRole().toUpperCase())
                .status("ACTIVE")
                .build();

        Staff saved = staffRepository.save(staff);
        return mapToDto(saved);
    }

    public List<StaffDto> getAllStaff() {
        return staffRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Cacheable(value = "staff", key = "#id")
    public StaffDto getStaffById(Long id) {
        log.info("[Cache-Aside] Cache MISS! Querying DB for Staff ID: {}", id);
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with ID: " + id));
        return mapToDto(staff);
    }

    public StaffDto getStaffByUserId(Long userId) {
        Staff staff = staffRepository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff profile not found for user ID: " + userId));
        return mapToDto(staff);
    }

    @Transactional
    @CacheEvict(value = "staff", key = "#id")
    public StaffDto updateStaff(Long id, StaffDto dto) {
        log.info("[Cache-Aside] Evicting cache & updating Staff ID: {}", id);
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with ID: " + id));

        staff.setFullName(dto.getFullName());
        staff.setPhone(dto.getPhone());
        staff.setEmail(dto.getEmail());
        staff.setBranchCode(dto.getBranchCode());
        staff.setDepartment(dto.getDepartment());
        if (dto.getRole() != null) {
            staff.setRole(dto.getRole().toUpperCase());
        }
        if (dto.getStatus() != null) {
            staff.setStatus(dto.getStatus());
        }

        Staff updated = staffRepository.save(staff);
        return mapToDto(updated);
    }

    @Transactional
    @CacheEvict(value = "staff", key = "#id")
    public void deleteStaff(Long id) {
        Staff staff = staffRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Staff not found with ID: " + id));
        staff.setStatus("INACTIVE");
        staffRepository.save(staff);
    }

    public StaffDto mapToDto(Staff entity) {
        return StaffDto.builder()
                .id(entity.getId())
                .staffCode(entity.getStaffCode())
                .userId(entity.getUserId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .branchCode(entity.getBranchCode())
                .department(entity.getDepartment())
                .role(entity.getRole())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
