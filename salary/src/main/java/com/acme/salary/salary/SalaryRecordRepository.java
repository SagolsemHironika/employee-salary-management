package com.acme.salary.salary;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalaryRecordRepository extends JpaRepository<SalaryRecord, Long> {

    List<SalaryRecord> findByEmployeeIdOrderByEffectiveDateDesc(Long employeeId);

    Optional<SalaryRecord> findByEmployeeIdAndEndDateIsNull(Long employeeId);
}
