import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { EmployeeDetail } from './employee-detail';
import { EmployeeService } from '../employee.service';
import { SalaryRecordService } from '../../salary/salary-record.service';

const EMPLOYEE = {
  id: 1,
  employeeCode: 'EMP-00001',
  firstName: 'Ada',
  lastName: 'Lovelace',
  email: 'ada@acme.example',
  countryCode: 'US',
  department: 'Engineering',
  jobTitle: 'Software Engineer',
  band: 'ENG-L3',
  hireDate: '2021-01-01',
  status: 'active',
  currentBaseSalary: 95000,
  currentCurrencyCode: 'USD'
};

describe('EmployeeDetail', () => {
  let employeeServiceMock: { get: ReturnType<typeof vi.fn> };
  let salaryRecordServiceMock: { history: ReturnType<typeof vi.fn>; create: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    employeeServiceMock = { get: vi.fn().mockReturnValue(of(EMPLOYEE)) };
    salaryRecordServiceMock = {
      history: vi.fn().mockReturnValue(of([])),
      create: vi.fn()
    };

    TestBed.configureTestingModule({
      imports: [EmployeeDetail],
      providers: [
        { provide: EmployeeService, useValue: employeeServiceMock },
        { provide: SalaryRecordService, useValue: salaryRecordServiceMock },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } }
        }
      ]
    });
  });

  it('rejects submission when required fields are missing', () => {
    const fixture = TestBed.createComponent(EmployeeDetail);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({ baseSalary: null, effectiveDate: '' });

    fixture.componentInstance.submit();

    expect(salaryRecordServiceMock.create).not.toHaveBeenCalled();
  });

  it('rejects a zero or negative base salary', () => {
    const fixture = TestBed.createComponent(EmployeeDetail);
    fixture.detectChanges();
    fixture.componentInstance.form.patchValue({
      baseSalary: 0,
      currencyCode: 'USD',
      effectiveDate: '2024-01-01',
      changeReason: 'adjustment'
    });

    expect(fixture.componentInstance.form.valid).toBe(false);
  });

  it('submits a valid form and refreshes history on success', () => {
    salaryRecordServiceMock.create.mockReturnValue(
      of({
        id: 10,
        employeeId: 1,
        baseSalary: 95000,
        currencyCode: 'USD',
        bonus: 0,
        allowances: 0,
        effectiveDate: '2024-01-01',
        endDate: null,
        changeReason: 'adjustment',
        createdBy: 'admin@acme.example'
      })
    );
    const fixture = TestBed.createComponent(EmployeeDetail);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      baseSalary: 95000,
      currencyCode: 'USD',
      effectiveDate: '2024-01-01',
      changeReason: 'adjustment'
    });
    salaryRecordServiceMock.history.mockClear();

    fixture.componentInstance.submit();

    expect(salaryRecordServiceMock.create).toHaveBeenCalledWith(1, {
      baseSalary: 95000,
      currencyCode: 'USD',
      effectiveDate: '2024-01-01',
      changeReason: 'adjustment'
    });
    expect(salaryRecordServiceMock.history).toHaveBeenCalledWith(1);
  });

  it('surfaces the server error message when creation fails', () => {
    salaryRecordServiceMock.create.mockReturnValue(
      throwError(() => ({ error: { message: 'New effective date must be after the current record' } }))
    );
    const fixture = TestBed.createComponent(EmployeeDetail);
    fixture.detectChanges();
    fixture.componentInstance.form.setValue({
      baseSalary: 95000,
      currencyCode: 'USD',
      effectiveDate: '2020-01-01',
      changeReason: 'adjustment'
    });

    fixture.componentInstance.submit();

    expect(fixture.componentInstance.formError()).toBe('New effective date must be after the current record');
  });
});
