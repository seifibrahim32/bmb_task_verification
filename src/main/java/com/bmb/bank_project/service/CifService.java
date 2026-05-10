package com.bmb.bank_project.service;

import com.bmb.bank_project.dto.*;
import com.bmb.bank_project.exception.BankException;
import com.bmb.bank_project.model.Cif;
import com.bmb.bank_project.model.CifStatus;
import com.bmb.bank_project.repository.CifRepository;
import com.bmb.bank_project.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class CifService {

    private static final int MAX_FAILED_ATTEMPTS = 3;

    private final CifRepository   cifRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;

    public CifService(CifRepository cifRepository,
                      PasswordEncoder passwordEncoder,
                      JwtUtil jwtUtil) {
        this.cifRepository   = cifRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil         = jwtUtil;
    }

    // Register
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (cifRepository.existsByEmail(request.getEmail())) {
            throw new BankException("A CIF with this email already exists.", HttpStatus.CONFLICT);
        }

        Cif cif = new Cif();
        cif.setCifNumber(generateCifNumber());
        cif.setFullName(request.getFullName());
        cif.setEmail(request.getEmail());
        cif.setPhoneNumber(request.getPhoneNumber());
        cif.setStatus(CifStatus.PENDING_TPIN);
        cif.setFailedAttempts(0);

        cifRepository.save(cif);
        return new RegisterResponse(cif.getCifNumber(), cif.getFullName());
    }

    // Set TPIN
    @Transactional
    public void setTpin(SetTpinRequest request) {
        Cif cif = findCifOrThrow(request.getCifNumber());

        if (cif.getStatus() != CifStatus.PENDING_TPIN) {
            throw new BankException(
                "TPIN has already been set for this CIF. Use Reset TPIN to change it.",
                HttpStatus.BAD_REQUEST);
        }
        if (!request.getTpin().equals(request.getConfirmTpin())) {
            throw new BankException("TPIN and confirmation TPIN do not match.", HttpStatus.BAD_REQUEST);
        }

        cif.setTpinHash(passwordEncoder.encode(request.getTpin()));
        cif.setStatus(CifStatus.ACTIVE);
        cif.setFailedAttempts(0);
        cifRepository.save(cif);
    }

    // Authenticate
    // noRollbackFor: BankException is thrown AFTER the BLOCKED status is saved.
    // Without this, Spring rolls back the transaction on any RuntimeException,
    // which would discard the status update and leave the CIF permanently un-blockable.
    @Transactional(noRollbackFor = BankException.class)
    public AuthenticateResponse authenticate(AuthenticateRequest request) {
        Cif cif = findCifOrThrow(request.getCifNumber());

        if (cif.getStatus() == CifStatus.PENDING_TPIN) {
            throw new BankException(
                "CIF has no TPIN set yet. Please set a TPIN first.", HttpStatus.BAD_REQUEST);
        }
        if (cif.getStatus() == CifStatus.BLOCKED) {
            throw new BankException(
                "This CIF is blocked due to too many failed attempts. Please use Reset TPIN.",
                HttpStatus.FORBIDDEN);
        }

        boolean match = passwordEncoder.matches(request.getTpin(), cif.getTpinHash());

        if (match) {
            cif.setFailedAttempts(0);
            cifRepository.save(cif);
            // Issue a JWT valid for 24 hours
            String token = jwtUtil.generateToken(cif.getCifNumber());
            return new AuthenticateResponse(true, cif.getCifNumber(), null, token);
        }

        int newFailed = cif.getFailedAttempts() + 1;
        cif.setFailedAttempts(newFailed);

        if (newFailed >= MAX_FAILED_ATTEMPTS) {
            cif.setStatus(CifStatus.BLOCKED);
            cifRepository.save(cif);
            throw new BankException(
                "Incorrect TPIN. CIF has been blocked after " + MAX_FAILED_ATTEMPTS + " failed attempts.",
                HttpStatus.FORBIDDEN);
        }

        cifRepository.save(cif);
        int remaining = MAX_FAILED_ATTEMPTS - newFailed;
        return new AuthenticateResponse(false, cif.getCifNumber(), remaining, null);
    }

    // Reset TPIN
    @Transactional
    public void resetTpin(ResetTpinRequest request) {
        Cif cif = findCifOrThrow(request.getCifNumber());

        if (cif.getStatus() == CifStatus.PENDING_TPIN) {
            throw new BankException(
                "CIF has no previous TPIN. Use the Set TPIN API instead.",
                HttpStatus.BAD_REQUEST);
        }
        if (cif.getStatus() != CifStatus.BLOCKED) {
            throw new BankException(
                "CIF is not blocked. Reset TPIN is only for blocked CIFs.",
                HttpStatus.BAD_REQUEST);
        }
        if (!request.getNewTpin().equals(request.getConfirmTpin())) {
            throw new BankException("New TPIN and confirmation TPIN do not match.", HttpStatus.BAD_REQUEST);
        }

        cif.setTpinHash(passwordEncoder.encode(request.getNewTpin()));
        cif.setStatus(CifStatus.ACTIVE);
        cif.setFailedAttempts(0);
        cifRepository.save(cif);
    }

    // Helpers
    private Cif findCifOrThrow(String cifNumber) {
        return cifRepository.findByCifNumber(cifNumber)
                .orElseThrow(() -> new BankException(
                    "CIF not found: " + cifNumber, HttpStatus.NOT_FOUND));
    }

    private String generateCifNumber() {
        String unique;
        do {
            unique = "CIF-" + UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 8)
                    .toUpperCase();
        } while (cifRepository.findByCifNumber(unique).isPresent());
        return unique;
    }
}
