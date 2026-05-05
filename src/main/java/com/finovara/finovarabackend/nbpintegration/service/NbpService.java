package com.finovara.finovarabackend.nbpintegration.service;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import com.finovara.finovarabackend.nbpintegration.client.NbpApiClient;
import com.finovara.finovarabackend.nbpintegration.dto.NbpTableDto;
import com.finovara.finovarabackend.nbpintegration.model.NbpConversionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NbpService {
    private final NbpApiClient nbpApiClient;

    public List<NbpTableDto> getAllRates() {
        return nbpApiClient.getAllRates("json");
    }

    public double convertCurrencies(String fromCurrency, String toCurrency, double amount, NbpConversionType conversionType) {
        if (fromCurrency.equalsIgnoreCase(toCurrency)) {
            return amount;
        }

        List<NbpTableDto.Rate> exchangeRates = fetchExchangeRates();

        return switch (conversionType) {
            case FROM_PLN -> convertFromPln(exchangeRates, toCurrency, amount);
            case TO_PLN -> convertToPln(exchangeRates, fromCurrency, amount);
            case FOREIGN_CURRENCIES -> convertBetweenForeignCurrencies(exchangeRates, fromCurrency, toCurrency, amount);
        };
    }

    private double convertFromPln(List<NbpTableDto.Rate> exchangeRates, String toCurrency, double amount) {
        double toRate = findRateByCode(exchangeRates, toCurrency);
        return amount / toRate;
    }

    private double convertToPln(List<NbpTableDto.Rate> exchangeRates, String fromCurrency, double amount) {
        double fromRate = findRateByCode(exchangeRates, fromCurrency);
        return amount * fromRate;
    }

    private double convertBetweenForeignCurrencies(List<NbpTableDto.Rate> exchangeRates, String fromCurrency, String toCurrency, double amount) {
        double fromRate = findRateByCode(exchangeRates, fromCurrency);
        double toRate = findRateByCode(exchangeRates, toCurrency);
        double amountInPln = amount * fromRate;
        return amountInPln / toRate;
    }

    private double findRateByCode(List<NbpTableDto.Rate> exchangeRates, String currencyCode) {
        return exchangeRates.stream()
                .filter(rate -> rate.currencyCode().equalsIgnoreCase(currencyCode))
                .findFirst()
                .map(NbpTableDto.Rate::averageRate)
                .orElseThrow(() -> new InvalidInputException("Unsupported currency: " + currencyCode));
    }

    private List<NbpTableDto.Rate> fetchExchangeRates() {
        return getAllRates().getFirst().rates();
    }
}
