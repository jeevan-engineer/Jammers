package com.brightloop.controller;

import com.brightloop.dto.QuotationTemplateDto;
import com.brightloop.entity.QuotationTemplate;
import com.brightloop.entity.QuotationTemplateLine;
import com.brightloop.exception.ResourceNotFoundException;
import com.brightloop.repository.QuotationTemplateRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/quotation-templates")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class QuotationTemplateController {

    private final QuotationTemplateRepository templateRepository;

    @PostMapping
    public ResponseEntity<QuotationTemplateDto> createTemplate(@Valid @RequestBody QuotationTemplateDto dto) {
        QuotationTemplate template = QuotationTemplate.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .recurringInterval(dto.getRecurringInterval())
                .duration(dto.getDuration())
                .durationUnit(dto.getDurationUnit())
                .autoClose(dto.getAutoClose() != null ? dto.getAutoClose() : false)
                .active(dto.getActive() != null ? dto.getActive() : true)
                .termsAndConditions(dto.getTermsAndConditions())
                .notes(dto.getNotes())
                .lines(new ArrayList<>())
                .build();

        if (dto.getLines() != null) {
            for (QuotationTemplateDto.LineDto lineDto : dto.getLines()) {
                QuotationTemplateLine line = QuotationTemplateLine.builder()
                        .productId(lineDto.getProductId())
                        .productName(lineDto.getProductName())
                        .description(lineDto.getDescription())
                        .quantity(lineDto.getQuantity())
                        .unitPrice(lineDto.getUnitPrice())
                        .discount(lineDto.getDiscount())
                        .sequence(lineDto.getSequence())
                        .build();
                template.addLine(line);
            }
        }

        QuotationTemplate saved = templateRepository.save(template);
        return ResponseEntity.status(HttpStatus.CREATED).body(mapToDto(saved));
    }

    @GetMapping
    public ResponseEntity<List<QuotationTemplateDto>> getAllTemplates() {
        List<QuotationTemplateDto> templates = templateRepository.findAll().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuotationTemplateDto> getTemplateById(@PathVariable Long id) {
        QuotationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuotationTemplate", "id", id));
        return ResponseEntity.ok(mapToDto(template));
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuotationTemplateDto> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody QuotationTemplateDto dto
    ) {
        QuotationTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("QuotationTemplate", "id", id));

        template.setName(dto.getName());
        template.setDescription(dto.getDescription());
        template.setRecurringInterval(dto.getRecurringInterval());
        template.setDuration(dto.getDuration());
        template.setDurationUnit(dto.getDurationUnit());
        if (dto.getAutoClose() != null) template.setAutoClose(dto.getAutoClose());
        if (dto.getActive() != null) template.setActive(dto.getActive());
        template.setTermsAndConditions(dto.getTermsAndConditions());
        template.setNotes(dto.getNotes());

        if (dto.getLines() != null) {
            template.getLines().clear();
            for (QuotationTemplateDto.LineDto lineDto : dto.getLines()) {
                QuotationTemplateLine line = QuotationTemplateLine.builder()
                        .productId(lineDto.getProductId())
                        .productName(lineDto.getProductName())
                        .description(lineDto.getDescription())
                        .quantity(lineDto.getQuantity())
                        .unitPrice(lineDto.getUnitPrice())
                        .discount(lineDto.getDiscount())
                        .sequence(lineDto.getSequence())
                        .build();
                template.addLine(line);
            }
        }

        QuotationTemplate updated = templateRepository.save(template);
        return ResponseEntity.ok(mapToDto(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        if (!templateRepository.existsById(id)) {
            throw new ResourceNotFoundException("QuotationTemplate", "id", id);
        }
        templateRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private QuotationTemplateDto mapToDto(QuotationTemplate entity) {
        List<QuotationTemplateDto.LineDto> lines = entity.getLines().stream()
                .map(line -> QuotationTemplateDto.LineDto.builder()
                        .id(line.getId())
                        .productId(line.getProductId())
                        .productName(line.getProductName())
                        .description(line.getDescription())
                        .quantity(line.getQuantity())
                        .unitPrice(line.getUnitPrice())
                        .discount(line.getDiscount())
                        .subtotal(line.getSubtotal())
                        .sequence(line.getSequence())
                        .build())
                .collect(Collectors.toList());

        return QuotationTemplateDto.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .recurringInterval(entity.getRecurringInterval())
                .duration(entity.getDuration())
                .durationUnit(entity.getDurationUnit())
                .autoClose(entity.getAutoClose())
                .active(entity.getActive())
                .termsAndConditions(entity.getTermsAndConditions())
                .notes(entity.getNotes())
                .lines(lines)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
