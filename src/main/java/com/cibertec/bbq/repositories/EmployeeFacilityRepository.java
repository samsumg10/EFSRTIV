package com.cibertec.bbq.repositories;

import com.cibertec.bbq.models.EmployeeFacility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmployeeFacilityRepository extends JpaRepository<EmployeeFacility, Long> {

    List<EmployeeFacility> findAllByEmployeeIdAndDeletedAtIsNull(Long employeeId);

    @Query("SELECT ef.facility.id FROM EmployeeFacility ef " +
           "WHERE ef.employee.id = :employeeId AND ef.deletedAt IS NULL AND ef.facility.deletedAt IS NULL")
    List<Long> findFacilityIds(Long employeeId);

    boolean existsByEmployeeIdAndFacilityIdAndDeletedAtIsNull(Long employeeId, Long facilityId);
}
