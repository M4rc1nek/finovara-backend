package com.finovara.securitymonitoring.clientdata.repository;

import com.finovara.securitymonitoring.clientdata.model.ClientData;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientDataRepository extends JpaRepository<ClientData, Long> {
}
