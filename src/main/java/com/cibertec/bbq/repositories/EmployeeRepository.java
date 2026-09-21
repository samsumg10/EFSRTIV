package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT e FROM Employee e JOIN FETCH e.company JOIN FETCH e.role " +
           "WHERE e.email = :email AND e.deletedAt IS NULL")
    Optional<Employee> findActiveByEmail(String email);

    List<Employee> findAllByCompanyIdAndDeletedAtIsNullOrderByIdDesc(Long companyId);
    Optional<Employee> findByIdAndCompanyIdAndDeletedAtIsNull(Long id, Long companyId);

    /** Representante = empleado de tipo 1 más antiguo. */
    Optional<Employee> findFirstByCompanyIdAndRole_TypeAndDeletedAtIsNullOrderByIdAsc(Long companyId, Integer type);
    long countByCompanyIdAndRole_TypeAndDeletedAtIsNull(Long companyId, Integer type);
    long countByRoleIdAndDeletedAtIsNull(Long roleId);

    List<Employee> findAllByCompanyId(Long companyId);

    /** Incluye eliminados: el UNIQUE de la BD también los cuenta. */
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
}
