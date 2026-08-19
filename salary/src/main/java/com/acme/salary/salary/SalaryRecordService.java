package com.acme.salary.salary;

import com.acme.salary.common.InvalidRequestException;
import com.acme.salary.common.ResourceNotFoundException;
import com.acme.salary.employee.EmployeeRepository;
import java.util.List;
import java.util.Optional;
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
            salaryRecordRepository.save(currentRecord);
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

        return SalaryRecordSummary.from(salaryRecordRepository.save(record));
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
