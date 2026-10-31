package com.brightloop.repository;

import com.brightloop.entity.QuotationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuotationTemplateRepository extends JpaRepository<QuotationTemplate, Long> {

    List<QuotationTemplate> findByActiveTrue();

    Optional<QuotationTemplate> findByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCase(String name);
}
