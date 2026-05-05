package com.finovara.finovarabackend.util.email;

import com.finovara.finovarabackend.exception.badrequest.InvalidInputException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.naming.NameNotFoundException;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

@Slf4j
@Component
public class EmailDomainValidator {

    public void validateDomainHasMxRecord(String email) {
        String domain = email.substring(email.indexOf('@') + 1);
        try {
            Hashtable<String, String> env = new Hashtable<>();
            env.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            env.put("java.naming.provider.url", "dns:");

            Attributes attrs = new InitialDirContext(env).getAttributes(domain, new String[]{"MX"});

            if (attrs.get("MX") == null) {
                throw new InvalidInputException("Invalid email domain: " + domain);
            }
        } catch (InvalidInputException e) {
            throw e;
        } catch (NameNotFoundException e) {
            throw new InvalidInputException("Invalid email domain: " + domain);
        } catch (Exception e) {
            log.warn("Could not validate MX record for domain: {}", domain, e);
        }
    }
}