package com.bmb.bank_project.controller;

import com.bmb.bank_project.dto.AccountResponse;
import com.bmb.bank_project.dto.ApiResponse;
import com.bmb.bank_project.dto.CreateAccountRequest;
import com.bmb.bank_project.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

/**
 * Account endpoints — all require a valid JWT bearer token.
 * The token's subject (cifNumber) is used to scope every operation
 * so a CIF can only see and manage its own accounts.
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Account Management",
     description = "Account operations — requires Bearer JWT from POST /api/cif/authenticate")
@SecurityRequirement(name = "bearerAuth")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // POST /api/accounts
    @PostMapping
    @Operation(summary = "Open a new bank account for the authenticated CIF")
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody CreateAccountRequest request) {

        AccountResponse account = accountService.createAccount(authenticatedCif(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Account created successfully.", account));
    }

    // GET /api/accounts
    @GetMapping
    @Operation(summary = "List all accounts belonging to the authenticated CIF")
    public ResponseEntity<ApiResponse<List<AccountResponse>>> listAccounts() {
        List<AccountResponse> accounts = accountService.getAccountsByCif(authenticatedCif());
        return ResponseEntity.ok(ApiResponse.ok("Accounts retrieved.", accounts));
    }

    // GET /api/accounts/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get a specific account by ID (must belong to the authenticated CIF)")
    public ResponseEntity<ApiResponse<AccountResponse>> getAccount(@PathVariable Long id) {
        AccountResponse account = accountService.getAccountById(id, authenticatedCif());
        return ResponseEntity.ok(ApiResponse.ok("Account retrieved.", account));
    }

    // Helper
    private String authenticatedCif() {
        return (String) Objects.requireNonNull(SecurityContextHolder.getContext()
                .getAuthentication()).getPrincipal();
    }
}
