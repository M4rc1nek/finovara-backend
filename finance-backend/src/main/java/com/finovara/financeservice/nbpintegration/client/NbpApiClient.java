package com.finovara.financeservice.nbpintegration.client;

import com.finovara.financeservice.nbpintegration.dto.NbpTableDto;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "nbp-api", url = "${nbp.api.url}")
public interface NbpApiClient {

    @Cacheable(value = "nbpRates", key = "'all'", unless = "#result == null")
    @GetMapping("/exchangerates/tables/A")
    List<NbpTableDto> getAllRates(@RequestParam("format") String format);

}
