package com.finovara.securitymonitoring.clientdata.repository;

import com.finovara.securitymonitoring.clientdata.model.ClientData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientDataRepository extends JpaRepository<ClientData, Long> {
}
