package com.finovara.authbackend.util.email;

import com.finovara.contracts.exception.badrequest.InvalidInputException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EmailDomainValidatorTest {

    private final EmailDomainValidator validator = new EmailDomainValidator();

    private EmailDomainValidator validatorWithContext(DirContext ctx) {
        return new EmailDomainValidator() {
            @Override
            protected DirContext createDirContext() {
                return ctx;
            }
        };
    }

    private EmailDomainValidator validatorThrowingOnCreate(NamingException ex) {
        return new EmailDomainValidator() {
            @Override
            protected DirContext createDirContext() throws NamingException {
                throw ex;
            }
        };
    }

    @Nested
    class ExtractDomain {

        @Test
        void shouldThrowExceptionWhenEmailIsNull() {
            assertThrows(InvalidInputException.class, () -> validator.validateDomainHasMxRecord(null));
        }

        @Test
        void shouldThrowExceptionWhenEmailHasNoAtSign() {
            assertThrows(InvalidInputException.class, () -> validator.validateDomainHasMxRecord("invalidemail.com"));
        }

        @Test
        void shouldThrowExceptionWhenAtSignIsAtTheBeginning() {
            assertThrows(InvalidInputException.class, () -> validator.validateDomainHasMxRecord("@domain.com"));
        }

        @Test
        void shouldThrowExceptionWhenAtSignIsAtTheEnd() {
            assertThrows(InvalidInputException.class, () -> validator.validateDomainHasMxRecord("user@"));
        }

        @Test
        void shouldThrowExceptionWhenDomainContainsSpace() {
            assertThrows(InvalidInputException.class, () -> validator.validateDomainHasMxRecord("user@domain .com"));
        }
    }

    @Nested
    class ValidateDomainHasMxRecord {

        @Test
        void shouldNotThrowWhenMxRecordExists() throws Exception {
            DirContext ctx = mock(DirContext.class);
            BasicAttributes attributes = new BasicAttributes();
            attributes.put(new BasicAttribute("MX", "mail.domain.com"));
            when(ctx.getAttributes(any(String.class), any(String[].class))).thenReturn(attributes);

            assertDoesNotThrow(() -> validatorWithContext(ctx).validateDomainHasMxRecord("user@domain.com"));
        }

        @Test
        void shouldThrowExceptionWhenMxRecordIsNull() throws Exception {
            DirContext ctx = mock(DirContext.class);
            when(ctx.getAttributes(any(String.class), any(String[].class))).thenReturn(new BasicAttributes());

            assertThrows(InvalidInputException.class, () -> validatorWithContext(ctx).validateDomainHasMxRecord("user@domain.com"));
        }

        @Test
        void shouldThrowExceptionWhenDomainDoesNotExist() throws Exception {
            DirContext ctx = mock(DirContext.class);
            when(ctx.getAttributes(any(String.class), any(String[].class)))
                    .thenThrow(new NameNotFoundException("Domain not found"));

            assertThrows(InvalidInputException.class, () -> validatorWithContext(ctx).validateDomainHasMxRecord("user@nonexistent.xyz"));
        }

        @Test
        void shouldThrowExceptionWhenDnsLookupFails() {
            assertThrows(InvalidInputException.class, () -> validatorThrowingOnCreate(new NamingException("DNS timeout"))
                            .validateDomainHasMxRecord("user@domain.com"));
        }
    }
}