package com.aeg.core.branch;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends JpaRepository<Branch, Long> {

	@Query("""
			SELECT DISTINCT b FROM Client c
			INNER JOIN c.distributor d
			INNER JOIN c.branch b
			WHERE d.id = :distributorId
			""")
	List<Branch> findBranchesByDistributorId(@Param("distributorId") Long distributorId);

	List<Branch> findByCompany_Id(Long companyId);

	Optional<Branch> findFirstByCompany_IdAndCityIgnoreCaseAndStateIgnoreCase(
			Long companyId, String city, String state);

	List<Branch> findByIdIn(Iterable<Long> ids);

	@Query("""
			SELECT b FROM Branch b
			JOIN FETCH b.company
			WHERE b.id = :id
			""")
	Optional<Branch> findByIdWithCompany(@Param("id") Long id);

	/** Native update: JPQL on {@code isClient} triggers Hibernate "Binding property is null". */
	@Modifying(clearAutomatically = true)
	@Query(
			value = """
					UPDATE public.sucursales
					SET es_cliente = true
					WHERE id = :branchId
					  AND (es_cliente IS NULL OR es_cliente = false)
					""",
			nativeQuery = true)
	int markAsClient(@Param("branchId") Long branchId);
}
