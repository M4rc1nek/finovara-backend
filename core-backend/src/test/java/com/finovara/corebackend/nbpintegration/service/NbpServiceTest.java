package com.finovara.corebackend.nbpintegration.service;

import com.finovara.corebackend.exception.badrequest.InvalidInputException;
import com.finovara.corebackend.exception.serviceunavailable.ServiceUnavailableException;
import com.finovara.corebackend.nbpintegration.client.NbpApiClient;
import com.finovara.corebackend.nbpintegration.dto.NbpTableDto;
import com.finovara.corebackend.nbpintegration.model.NbpConversionType;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NbpServiceTest {

    @Mock
    private NbpApiClient nbpApiClient;

    @InjectMocks
    private NbpService nbpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(nbpService, "scale", 4);
    }

    @Nested
    class SameCurrency {

        @Nested
        class GetAllRates {
            @Test
            void shouldThrowServiceUnavailableExceptionWhenNbpApiClientFails() {
                when(nbpApiClient.getAllRates("json")).thenThrow(new FeignException.ServiceUnavailable
                        ("NBP API unavailable", mock(Request.class), null, null));

                assertThrows(ServiceUnavailableException.class, () -> nbpService.getAllRates());
            }
        }

        @Test
        void shouldReturnSameAmountWhenFromAndToCurrencyAreEqual() {
            BigDecimal result = nbpService.convertCurrencies("USD", "USD", new BigDecimal("100"),
                    NbpConversionType.FOREIGN_CURRENCIES);

            assertEquals(new BigDecimal("100.0000"), result);
        }
    }

    @Nested
    class FromPln {

        @Test
        void shouldConvertFromPlnToUsd() {
            NbpTableDto.Rate usd = new NbpTableDto.Rate("US Dollar", "USD", new BigDecimal("4.0000"));
            NbpTableDto table = new NbpTableDto("A", "001/A/NBP/2024", "2024-01-01", List.of(usd));
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of(table));

            BigDecimal result = nbpService.convertCurrencies("PLN", "USD", new BigDecimal("100"),
                    NbpConversionType.FROM_PLN);

            assertEquals(new BigDecimal("25.0000"), result);
        }

        @Test
        void shouldConvertFromPlnToEur() {
            NbpTableDto.Rate eur = new NbpTableDto.Rate("Euro", "EUR", new BigDecimal("4.2000"));
            NbpTableDto table = new NbpTableDto("A", "001/A/NBP/2024", "2024-01-01", List.of(eur));
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of(table));

            BigDecimal result = nbpService.convertCurrencies("PLN", "EUR", new BigDecimal("100"),
                    NbpConversionType.FROM_PLN);

            assertEquals(new BigDecimal("23.8095"), result);
        }
    }

    @Nested
    class ToPln {

        @Test
        void shouldConvertUsdToPln() {
            NbpTableDto.Rate usd = new NbpTableDto.Rate("US Dollar", "USD", new BigDecimal("4.0000"));
            NbpTableDto table = new NbpTableDto("A", "001/A/NBP/2024", "2024-01-01", List.of(usd));
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of(table));

            BigDecimal result = nbpService.convertCurrencies("USD", "PLN", new BigDecimal("10"),
                    NbpConversionType.TO_PLN);

            assertEquals(new BigDecimal("40.0000"), result);
        }
    }

    @Nested
    class ForeignCurrencies {

        @Test
        void shouldConvertUsdToEur() {
            NbpTableDto.Rate usd = new NbpTableDto.Rate("US Dollar", "USD", new BigDecimal("4.0000"));
            NbpTableDto.Rate eur = new NbpTableDto.Rate("Euro", "EUR", new BigDecimal("4.2000"));
            NbpTableDto table = new NbpTableDto("A", "001/A/NBP/2024", "2024-01-01", List.of(usd, eur));
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of(table));

            BigDecimal result = nbpService.convertCurrencies("USD", "EUR", new BigDecimal("10"),
                    NbpConversionType.FOREIGN_CURRENCIES);

            assertEquals(new BigDecimal("9.5238"), result);
        }
    }

    @Nested
    class FindRateByCode {

        @Test
        void shouldFindRateCaseInsensitive() {
            NbpTableDto.Rate usd = new NbpTableDto.Rate("US Dollar", "USD", new BigDecimal("4.0000"));
            NbpTableDto table = new NbpTableDto("A", "001/A/NBP/2024", "2024-01-01", List.of(usd));
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of(table));

            BigDecimal result = nbpService.convertCurrencies("PLN", "usd", new BigDecimal("100"),
                    NbpConversionType.FROM_PLN);

            assertEquals(new BigDecimal("25.0000"), result);
        }

        @Test
        void shouldThrowExceptionWhenCurrencyNotFound() {
            NbpTableDto.Rate usd = new NbpTableDto.Rate("US Dollar", "USD", new BigDecimal("4.0000"));
            NbpTableDto table = new NbpTableDto("A", "001/A/NBP/2024", "2024-01-01", List.of(usd));
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of(table));

            assertThrows(InvalidInputException.class, () -> nbpService.convertCurrencies("PLN", "XYZ",
                    new BigDecimal("100"), NbpConversionType.FROM_PLN));
        }
    }

    @Nested
    class FetchExchangeRates {

        @Test
        void shouldThrowExceptionWhenExchangeRatesAreUnavailable() {
            when(nbpApiClient.getAllRates("json")).thenReturn(List.of());

            assertThrows(InvalidInputException.class, () -> nbpService.convertCurrencies("USD", "EUR",
                    new BigDecimal("10"), NbpConversionType.FOREIGN_CURRENCIES));
        }

        @Test
        void shouldThrowExceptionWhenExchangeRatesResponseIsNull() {
            when(nbpApiClient.getAllRates("json")).thenReturn(null);

            assertThrows(InvalidInputException.class, () -> nbpService.convertCurrencies("USD", "EUR",
                    new BigDecimal("10"), NbpConversionType.FOREIGN_CURRENCIES));
        }
    }
}