package com.finovara.reportservice.feignclient;

import feign.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

/**
 * DEV ONLY - Disables SSL certificate verification for Feign clients.
 * Required because microservices use a self-signed mkcert certificate
 * which is not trusted by the JVM inside the Docker container.
 * Remove this class before deploying to production.
 */

@Configuration
public class FeignSslConfig {

    @Bean
    public Client feignClient() throws Exception {
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
        }, new java.security.SecureRandom());

        return new Client.Default(
                sslContext.getSocketFactory(),
                (hostname, session) -> true
        );
    }
}