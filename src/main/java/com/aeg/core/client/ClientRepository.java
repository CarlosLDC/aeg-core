package com.aeg.core.client;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
	/** Navega la relación {@code distributor}, no un atributo {@code distributorId}. */
	List<Client> findByDistributor_Id(Long distributorId);
}