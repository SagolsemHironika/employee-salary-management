package com.acme.salary.salary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.salary.common.InvalidRequestException;
import com.acme.salary.common.ResourceNotFoundException;
import com.acme.salary.employee.EmployeeRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SalaryRecordServiceTest {

    @Mock
    private SalaryRecordRepository salaryRecordRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private SalaryRecordService service;

    @BeforeEach
    void setUp() {
        service = new SalaryRecordService(salaryRecordRepository, employeeRepository);
    }

    @Test
    void createsHireRecordWhenNoCurrentRecordExists() {
        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(Optional.empty());
        when(salaryRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryRecordRequest request =
                new SalaryRecordRequest(new BigDecimal("90000"), "USD", null, null, LocalDate.of(2024, 1, 1), "hire");

        SalaryRecordSummary result = service.createRecord(1L, request, "tester");

        assertThat(result.employeeId()).isEqualTo(1L);
        assertThat(result.baseSalary()).isEqualByComparingTo("90000");
        assertThat(result.endDate()).isNull();
        assertThat(result.createdBy()).isEqualTo("tester");
        verify(salaryRecordRepository, times(1)).save(any());
    }

    @Test
    void closesPreviousCurrentRecordWhenNewOneIsCreated() {
        SalaryRecord current = new SalaryRecord();
        current.setEmployeeId(1L);
        current.setEffectiveDate(LocalDate.of(2023, 1, 1));
        current.setEndDate(null);

        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(Optional.of(current));
        when(salaryRecordRepository.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(salaryRecordRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SalaryRecordRequest request = new SalaryRecordRequest(
                new BigDecimal("105000"), "USD", null, null, LocalDate.of(2024, 1, 1), "promotion");

        service.createRecord(1L, request, "tester");

        // Order is load-bearing, not incidental: the close must reach the
        // database before the insert, or both rows momentarily have a null
        // end_date and uq_salary_records_open_per_employee (V4) rejects the
        // insert. Asserting the order here fails fast if someone downgrades
        // that saveAndFlush back to a plain save.
        ArgumentCaptor<SalaryRecord> inserted = ArgumentCaptor.forClass(SalaryRecord.class);
        InOrder inOrder = inOrder(salaryRecordRepository);
        inOrder.verify(salaryRecordRepository).saveAndFlush(current);
        inOrder.verify(salaryRecordRepository).save(inserted.capture());

        assertThat(current.getEndDate()).isEqualTo(LocalDate.of(2023, 12, 31));
        SalaryRecord newRecord = inserted.getValue();
        assertThat(newRecord.getEffectiveDate()).isEqualTo(LocalDate.of(2024, 1, 1));
        assertThat(newRecord.getEndDate()).isNull();
    }

    @Test
    void rejectsEffectiveDateNotAfterCurrentRecord() {
        SalaryRecord current = new SalaryRecord();
        current.setEmployeeId(1L);
        current.setEffectiveDate(LocalDate.of(2023, 1, 1));

        when(employeeRepository.existsById(1L)).thenReturn(true);
        when(salaryRecordRepository.findByEmployeeIdAndEndDateIsNull(1L)).thenReturn(Optional.of(current));

        SalaryRecordRequest sameDate = new SalaryRecordRequest(
                new BigDecimal("95000"), "USD", null, null, LocalDate.of(2023, 1, 1), "adjustment");

        assertThatThrownBy(() -> service.createRecord(1L, sameDate, "tester"))
                .isInstanceOf(InvalidRequestException.class);
        verify(salaryRecordRepository, never()).save(any());
    }

    @Test
    void throwsWhenEmployeeDoesNotExist() {
        when(employeeRepository.existsById(99L)).thenReturn(false);

        SalaryRecordRequest request =
                new SalaryRecordRequest(new BigDecimal("90000"), "USD", null, null, LocalDate.of(2024, 1, 1), "hire");

        assertThatThrownBy(() -> service.createRecord(99L, request, "tester"))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(salaryRecordRepository, never()).findByEmployeeIdAndEndDateIsNull(any());
    }
}
