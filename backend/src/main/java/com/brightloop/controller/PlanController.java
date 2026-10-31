package com.brightloop.controller;

import com.brightloop.dto.PlanRequest;
import com.brightloop.dto.PlanResponse;
import com.brightloop.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/plans")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;

    @PostMapping
    public ResponseEntity<PlanResponse> createPlan(@Valid @RequestBody PlanRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planService.createPlan(request));
    }

    @GetMapping
    public ResponseEntity<Page<PlanResponse>> getPlans(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(planService.getPlans(productId, activeOnly, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlanResponse> getPlanById(@PathVariable Long id) {
        return ResponseEntity.ok(planService.getPlanById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PlanResponse> updatePlan(
            @PathVariable Long id,
            @Valid @RequestBody PlanRequest request) {
        return ResponseEntity.ok(planService.updatePlan(id, request));
    }
}