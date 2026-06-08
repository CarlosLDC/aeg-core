package com.aeg.core.fiscalbookuser;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FiscalBookUserRepository extends JpaRepository<FiscalBookUser, Long> {

    Optional<FiscalBookUser> findByUsername(String username);

    @Query("SELECT u FROM FiscalBookUser u LEFT JOIN FETCH u.employee e LEFT JOIN FETCH e.branch WHERE u.username = :username")
    Optional<FiscalBookUser> findByUsernameWithRelations(@Param("username") String username);
}
