package com.aeg.core.security;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByBranchId(Long branchId);

    Optional<User> findByUsername(String username);

    Optional<User> findByNationalId(String nationalId);

    boolean existsByNationalIdAndIdNot(String nationalId, Long id);

    @Query("""
            SELECT u FROM User u
            LEFT JOIN FETCH u.distributor
            LEFT JOIN FETCH u.branch
            WHERE u.username = :username
            """)
    Optional<User> findByUsernameWithRelations(@Param("username") String username);
}
