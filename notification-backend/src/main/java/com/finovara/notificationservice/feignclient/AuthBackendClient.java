package com.finovara.notificationservice.feignclient;

import com.finovara.contracts.authorization.dto.UserDataResponse;
import com.finovara.contracts.authorization.dto.ConfirmAuthorizationCodeDto;
import com.finovara.contracts.notification.email.digest.report.finance.WeeklyFinanceDigestReportDto;
import com.finovara.contracts.notification.email.digest.report.security.WeeklySecurityDigestReportDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "auth-backend", url = "${auth-backend.url}")
public interface AuthBackendClient {

    @GetMapping("internal/user-data")
    UserDataResponse getUserEmailData(@RequestHeader("X-User-Id") Long userId);

    @PostMapping("/internal/confirm-authorization-code")
    Void confirmAuthorizationCode(@RequestHeader("X-User-Id") Long userId, @RequestBody ConfirmAuthorizationCodeDto dto);

    @GetMapping("/internal/security/digest/weekly-email/report")
    List<WeeklySecurityDigestReportDto> getWeeklySecurityDigestReports();
}