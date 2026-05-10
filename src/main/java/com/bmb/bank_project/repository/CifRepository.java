package com.bmb.bank_project.repository;

import com.bmb.bank_project.model.Cif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CifRepository extends JpaRepository<Cif, Long> {
    Optional<Cif> findByCifNumber(String cifNumber);
    boolean existsByEmail(String email);
}
