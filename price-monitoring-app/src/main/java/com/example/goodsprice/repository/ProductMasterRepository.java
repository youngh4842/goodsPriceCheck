package com.example.goodsprice.repository;

import com.example.goodsprice.domain.ProductMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductMasterRepository extends JpaRepository<ProductMaster, Long> {

	Optional<ProductMaster> findByProductCode(String productCode);
}
