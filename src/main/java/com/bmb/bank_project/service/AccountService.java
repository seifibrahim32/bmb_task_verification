package com.bmb.bank_project.service;

import com.bmb.bank_project.dto.AccountResponse;
import com.bmb.bank_project.dto.CreateAccountRequest;
import com.bmb.bank_project.exception.BankException;
import com.bmb.bank_project.model.Account;
import com.bmb.bank_project.model.Cif;
import com.bmb.bank_project.repository.AccountRepository;
import com.bmb.bank_project.repository.CifRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final CifRepository     cifRepository;

    public AccountService(AccountRepository accountRepository,
                          CifRepository cifRepository) {
        this.accountRepository = accountRepository;
        this.cifRepository     = cifRepository;
    }

    // Create account for the authenticated CIF
    @Transactional
    public AccountResponse createAccount(String cifNumber, CreateAccountRequest request) {
        Cif cif = cifRepository.findByCifNumber(cifNumber)
                .orElseThrow(() -> new BankException(
                    "CIF not found: " + cifNumber, HttpStatus.NOT_FOUND));

        if (accountRepository.existsByAccountNumber(request.getAccountNumber())) {
            throw new BankException(
                "Account number '" + request.getAccountNumber() + "' is already in use.",
                HttpStatus.CONFLICT);
        }

        Account account = new Account(
                request.getAccountNumber(),
                request.getHolderName(),
                request.getBalance(),
                request.getAccountType());
        account.setCif(cif);

        return toResponse(accountRepository.save(account));
    }

    // List all accounts for the authenticated CIF
    @Transactional(readOnly = true)
    public List<AccountResponse> getAccountsByCif(String cifNumber) {
        return accountRepository.findByCifCifNumber(cifNumber)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // Get a single account (must belong to the authenticated CIF)
    @Transactional(readOnly = true)
    public AccountResponse getAccountById(Long id, String cifNumber) {
        Account account = accountRepository.findByIdAndCifCifNumber(id, cifNumber)
                .orElseThrow(() -> new BankException(
                    "Account not found or does not belong to this CIF.", HttpStatus.NOT_FOUND));
        return toResponse(account);
    }

    // Mapper
    private AccountResponse toResponse(Account account) {
        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setHolderName(account.getHolderName());
        response.setBalance(account.getBalance());
        response.setAccountType(account.getAccountType());
        response.setCifNumber(account.getCif().getCifNumber());
        return response;
    }
}
