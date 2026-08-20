package com.acme.salary.salary;

import com.acme.salary.common.ConflictException;
import com.acme.salary.common.InvalidRequestException;
import com.acme.salary.common.ResourceNotFoundException;
import com.acme.salary.employee.EmployeeRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SalaryRecordService {

    private final SalaryRecordRepository salaryRecordRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryRecordService(SalaryRecordRepository salaryRecordRepository, EmployeeRepository employeeRepository) {
        this.salaryRecordRepository = salaryRecordRepository;
        this.employeeRepository = employeeRepository;
    }

    @Transactional
    public SalaryRecordSummary createRecord(Long employeeId, SalaryRecordRequest request, String createdBy) {
        requireEmployee(employeeId);

        Optional<SalaryRecord> current = salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(employeeId);
        if (current.isPresent()) {
            SalaryRecord currentRecord = current.get();
            if (!request.effectiveDate().isAfter(currentRecord.getEffectiveDate())) {
                throw new InvalidRequestException(
                        "New effective date must be after the current record's effective date ("
                                + currentRecord.getEffectiveDate() + ")");
            }
            currentRecord.setEndDate(request.effectiveDate().minusDays(1));
            // saveAndFlush, not save: the close has to reach the database
            // before the insert below, not merely before the commit.
            //
            // SalaryRecord uses IDENTITY generation, so Hibernate must execute
            // the INSERT immediately to read the generated key back, while a
            // plain save() leaves this UPDATE queued in the persistence context
            // until flush. That ordering leaves both rows with end_date IS NULL
            // at the moment of the insert, which the partial unique index added
            // in V4 rejects -- turning an ordinary raise into a 409.
            //
            // The invariant was only ever true at commit; making it true
            // statement-by-statement is what lets the database enforce it.
            salaryRecordRepository.saveAndFlush(currentRecord);
        }

        SalaryRecord record = new SalaryRecord();
        record.setEmployeeId(employeeId);
        record.setBaseSalary(request.baseSalary());
        record.setCurrencyCode(request.currencyCode());
        record.setBonus(request.bonusOrZero());
        record.setAllowances(request.allowancesOrZero());
        record.setEffectiveDate(request.effectiveDate());
        record.setChangeReason(request.changeReason());
        record.setCreatedBy(createdBy);

        try {
            return SalaryRecordSummary.from(salaryRecordRepository.save(record));
        } catch (DataIntegrityViolationException ex) {
            // uq_salary_records_open_per_employee (V4) rejected a second open
            // record. Reaching here means another request closed and replaced
            // this employee's current salary between our read above and this
            // insert -- the read-modify-write race that @Transactional alone
            // cannot prevent under READ COMMITTED. The caller's request was
            // well-formed, so this is 409 and not 400, and a retry against the
            // now-current record is the correct next step.
            throw new ConflictException(
                    "This employee's salary was changed by someone else while you were editing. "
                            + "Reload the history and reapply your change.");
        }
    }

    public List<SalaryRecordSummary> history(Long employeeId) {
        requireEmployee(employeeId);
        return salaryRecordRepository.findByEmployeeIdOrderByEffectiveDateDesc(employeeId).stream()
                .map(SalaryRecordSummary::from)
                .toList();
    }

    private void requireEmployee(Long employeeId) {
        if (!employeeRepository.existsById(employeeId)) {
            throw new ResourceNotFoundException("Employee " + employeeId + " not found");
        }
    }
}
