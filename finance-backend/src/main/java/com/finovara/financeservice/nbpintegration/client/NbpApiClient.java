package com.finovara.financeservice.nbpintegration.client;

import com.finovara.financeservice.nbpintegration.dto.NbpTableDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "nbp-api", url = "${nbp.api.url}")
public interface NbpApiClient {

    @GetMapping("/exchangerates/tables/A")
    List<NbpTableDto> getAllRates(@RequestParam("format") String format);

}
