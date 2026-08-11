package com.finovara.authservice.util.authorization;

import com.finovara.contracts.authorization.additionalcode.resolver.AdditionalAuthorizationCodeResolver;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdditionalAuthorizationCodeResolverTest {

    private AdditionalAuthorizationCodeResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new AdditionalAuthorizationCodeResolver();
    }

    @Nested
    class ResolveFromRawCode {

        @Test
        void shouldReturnDtoWithGivenCodeWhenRawCodeIsProvided() {
            ConfirmAuthorizationCodeDto result = resolver.resolve("raw-code");

            assertThat(result.code()).isEqualTo("raw-code");
        }

        @Test
        void shouldReturnDtoWithNullCodeWhenRawCodeIsNull() {
            ConfirmAuthorizationCodeDto result = resolver.resolve((String) null);

            assertThat(result.code()).isNull();
        }

        @Test
        void shouldReturnDtoWithEmptyCodeWhenRawCodeIsEmpty() {
            ConfirmAuthorizationCodeDto result = resolver.resolve("");

            assertThat(result.code()).isEmpty();
        }
    }

    @Nested
    class ResolveFromDto {

        @Test
        void shouldReturnNewDtoWithSameCodeWhenDtoIsProvided() {
            ConfirmAuthorizationCodeDto input = new ConfirmAuthorizationCodeDto("existing-code");

            ConfirmAuthorizationCodeDto result = resolver.resolve(input);

            assertThat(result.code()).isEqualTo("existing-code");
        }

        @Test
        void shouldReturnDtoWithNullCodeWhenInputDtoIsNull() {
            ConfirmAuthorizationCodeDto result = resolver.resolve((ConfirmAuthorizationCodeDto) null);

            assertThat(result.code()).isNull();
        }

        @Test
        void shouldReturnDtoWithNullCodeWhenInputDtoHasNullCode() {
            ConfirmAuthorizationCodeDto input = new ConfirmAuthorizationCodeDto(null);

            ConfirmAuthorizationCodeDto result = resolver.resolve(input);

            assertThat(result.code()).isNull();
        }
    }
}