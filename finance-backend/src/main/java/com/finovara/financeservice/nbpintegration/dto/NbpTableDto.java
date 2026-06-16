package com.finovara.financeservice.nbpintegration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NbpTableDto(
        @JsonProperty("table")
        String tableType,

        @JsonProperty("no")
        String tableNumber,

        @JsonProperty("effectiveDate")
        String publishDate,
        @JsonProperty("rates")
        List<Rate> rates
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rate(
            @JsonProperty("currency")
            String name,

            @JsonProperty("code")
            String currencyCode,

            @JsonProperty("mid")
            BigDecimal averageRate
    ) {
    }
}
