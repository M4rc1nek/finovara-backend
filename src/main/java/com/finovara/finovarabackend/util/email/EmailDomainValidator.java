package com.finovara.finovarabackend.util.email;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.Context;
import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Slf4j
@Component
public class EmailDomainValidator {

    public void validateDomainHasMxRecord(String email) {
        String domain = extractDomain(email);

        DirContext ctx = null;
        try {
            ctx = createDirContext();
            Attributes attrs = ctx.getAttributes(domain, new String[]{"MX"});

            if (attrs == null || attrs.get("MX") == null) {
                throw new InvalidInputException("Invalid email domain: " + domain);
            }

        } catch (InvalidInputException e) {
            throw e;
        } catch (NameNotFoundException e) {
            throw new InvalidInputException("Invalid email domain: " + domain);
        } catch (Exception e) {
            log.warn("Could not validate MX record for domain: {}", domain, e);
            throw new InvalidInputException("Could not validate email domain: " + domain);
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (Exception e) {
                    log.debug("Failed to close DirContext", e);
                }
            }
        }
    }

    protected DirContext createDirContext() throws Exception {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        env.put(Context.PROVIDER_URL, "dns:");
        env.put("com.sun.jndi.dns.timeout.initial", "2000");
        env.put("com.sun.jndi.dns.timeout.retries", "2");
        return new InitialDirContext(env);
    }

    private String extractDomain(String email) {
        if (email == null) {
            throw new InvalidInputException("Email cannot be null");
        }

        int atIndex = email.lastIndexOf('@');

        if (atIndex <= 0 || atIndex == email.length() - 1) {
            throw new InvalidInputException("Invalid email format");
        }

        String domain = email.substring(atIndex + 1).trim();

        if (domain.isEmpty() || domain.contains(" ")) {
            throw new InvalidInputException("Invalid email domain");
        }

        return domain.toLowerCase();
    }
}