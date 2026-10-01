package com.finovara.activitylogservice.internal.security.util.clientinfo;

import com.finovara.activitylogservice.activitylog.accountactivity.secure.login.activity.repository.LoginActivityRepository;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.BrowserCountDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.LocationCountDto;
import com.finovara.activitylogservice.internal.security.util.clientinfo.dto.ClientInfoDto;
import com.finovara.contracts.model.activity.LoginActivityStatus;
import com.finovara.contracts.percentage.CalculatePercentage;
import com.finovara.contracts.report.dto.security.ShareStatDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToLongFunction;

@Service
@RequiredArgsConstructor
public class ClientInfoResolver {

    private final LoginActivityRepository loginActivityRepository;

    public ClientInfoDto getClientContext(Long userId, LocalDateTime from, LocalDateTime to) {

        List<LocationCountDto> locationCounts = loginActivityRepository.findLocationCounts(userId, LoginActivityStatus.SUCCESSFUL, from, to);
        List<BrowserCountDto> browserCounts = loginActivityRepository.findBrowserCounts(userId, LoginActivityStatus.SUCCESSFUL, from, to);

        return new ClientInfoDto(
                toShares(locationCounts, LocationCountDto::location, LocationCountDto::count),
                toShares(browserCounts, BrowserCountDto::browser, BrowserCountDto::count)
        );
    }

    private <T> List<ShareStatDto> toShares(List<T> entries, Function<T, String> labelExtractor, ToLongFunction<T> countExtractor) {
        long totalCount = entries.stream().mapToLong(countExtractor).sum();

        return entries.stream()
                .map(entry -> new ShareStatDto(
                        labelExtractor.apply(entry),
                        CalculatePercentage.calculatePercentage(BigDecimal.valueOf(countExtractor.applyAsLong(entry)), BigDecimal.valueOf(totalCount))))
                .toList();
    }
}
