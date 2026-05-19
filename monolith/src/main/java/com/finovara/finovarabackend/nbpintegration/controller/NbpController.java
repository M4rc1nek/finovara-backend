package com.finovara.finovarabackend.nbpintegration.controller;

import com.finovara.finovarabackend.nbpintegration.dto.NbpTableDto;
import com.finovara.finovarabackend.nbpintegration.model.NbpConversionType;
import com.finovara.finovarabackend.nbpintegration.service.NbpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/currencies")
@RequiredArgsConstructor
public class NbpController {
    private final NbpService nbpService;

    @GetMapping
    public ResponseEntity<List<NbpTableDto>> getAllRates() {
        return ResponseEntity.ok(nbpService.getAllRates());
    }

    @GetMapping("/convert")
    public BigDecimal convert(@RequestParam BigDecimal amount, @RequestParam String from, @RequestParam String to,
                          @RequestParam(defaultValue = "FROM_PLN") NbpConversionType nbpConversionType) {
        return nbpService.convertCurrencies(from, to, amount, nbpConversionType);
    }
}