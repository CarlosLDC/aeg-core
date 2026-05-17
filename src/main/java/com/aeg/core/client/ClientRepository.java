package com.aeg.core.client;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
	List<Client> findByDistributorId(Long distributorId);
}