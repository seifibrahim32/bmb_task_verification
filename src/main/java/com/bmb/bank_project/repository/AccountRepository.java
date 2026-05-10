package com.bmb.bank_project.repository;

import com.bmb.bank_project.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccountRepository extends JpaRepository<Account, Long> {

    /** Check if an account number is already taken (globally unique). */
    boolean existsByAccountNumber(String accountNumber);

    /** All accounts owned by a specific CIF. */
    List<Account> findByCifCifNumber(String cifNumber);

    /** Single account that also belongs to the given CIF (prevents cross-CIF access). */
    Optional<Account> findByIdAndCifCifNumber(Long id, String cifNumber);
}
