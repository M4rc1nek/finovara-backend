package com.finovara.reportservice.report.smartreport.service.loader;

import com.finovara.contracts.exception.serviceunavailable.ServiceUnavailableException;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
class SmartReportResourceLoader {

    List<String> readLines(String path) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("File not found: " + path);
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            return reader.lines()
                    .filter(line -> !line.isBlank())
                    .toList();
        } catch (Exception e) {
            throw new ServiceUnavailableException("Failed to load resource: " + path, e);
        }
    }
}