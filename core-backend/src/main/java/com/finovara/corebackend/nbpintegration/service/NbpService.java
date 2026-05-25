package com.finovara.corebackend.nbpintegration.service;

import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.corebackend.nbpintegration.client.NbpApiClient;
import com.finovara.corebackend.nbpintegration.dto.NbpTableDto;
import com.finovara.corebackend.nbpintegration.model.NbpConversionType;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NbpService {

    @Value("${nbp.properties.scale}")
    private int scale;


    private static final RoundingMode ROUNDING_MODE = RoundingMode.HALF_UP;
    private final NbpApiClient nbpApiClient;

    public List<NbpTableDto> getAllRates() {
        try{
            return nbpApiClient.getAllRates("json");
        }catch (FeignException exception){
            log.error("Failed to fetch rates from NBP API", exception);
            throw new ServiceUnavailableException("Failed to get all rates", exception);
        }
    }

    public BigDecimal convertCurrencies(String fromCurrency, String toCurrency, BigDecimal amount, NbpConversionType conversionType) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount.setScale(scale, ROUNDING_MODE);
        }

        List<NbpTableDto.Rate> exchangeRates = fetchExchangeRates();

        return switch (conversionType) {
            case FROM_PLN -> convertFromPln(exchangeRates, toCurrency, amount);
            case TO_PLN -> convertToPln(exchangeRates, fromCurrency, amount);
            case FOREIGN_CURRENCIES -> convertBetweenForeignCurrencies(exchangeRates, fromCurrency, toCurrency, amount);
        };
    }

    private BigDecimal convertFromPln(List<NbpTableDto.Rate> exchangeRates, String toCurrency, BigDecimal amount) {
        BigDecimal toRate = findRateByCode(exchangeRates, toCurrency);
        return amount.divide(toRate, scale, ROUNDING_MODE);
    }

    private BigDecimal convertToPln(List<NbpTableDto.Rate> exchangeRates, String fromCurrency, BigDecimal amount) {
        BigDecimal fromRate = findRateByCode(exchangeRates, fromCurrency);
        return amount.multiply(fromRate).setScale(scale, ROUNDING_MODE);
    }

    private BigDecimal convertBetweenForeignCurrencies(List<NbpTableDto.Rate> exchangeRates, String fromCurrency, String toCurrency, BigDecimal amount) {
        BigDecimal fromRate = findRateByCode(exchangeRates, fromCurrency);
        BigDecimal toRate = findRateByCode(exchangeRates, toCurrency);
        BigDecimal amountInPln = amount.multiply(fromRate);
        return amountInPln.divide(toRate, scale, ROUNDING_MODE);
    }

    private BigDecimal findRateByCode(List<NbpTableDto.Rate> exchangeRates, String currencyCode) {
        return exchangeRates.stream()
                .filter(rate -> rate.currencyCode().equalsIgnoreCase(currencyCode))
                .findFirst()
                .map(NbpTableDto.Rate::averageRate)
                .orElseThrow(() -> new InvalidInputException("Unsupported currency: " + currencyCode));
    }

    private List<NbpTableDto.Rate> fetchExchangeRates() {
        List<NbpTableDto> tables = getAllRates();
        if (tables == null || tables.isEmpty()) {
            throw new InvalidInputException("Exchange rates are currently unavailable.");
        }
        return tables.getFirst().rates();
    }
}