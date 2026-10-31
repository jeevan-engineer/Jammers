package com.brightloop.repository;

import com.brightloop.entity.Tax;
import com.brightloop.enums.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaxRepository extends JpaRepository<Tax, Long> {

    List<Tax> findByActiveTrue();

    List<Tax> findByApplicabilityAndActiveTrue(ProductType applicability);
}