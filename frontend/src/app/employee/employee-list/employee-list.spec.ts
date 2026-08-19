import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { of } from 'rxjs';
import { EmployeeList } from './employee-list';
import { EmployeeService } from '../employee.service';
import { EmployeeSummary, PagedResponse } from '../employee.model';

function summary(id: number): EmployeeSummary {
  return {
    id,
    employeeCode: `EMP-${id}`,
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
}

describe('EmployeeList', () => {
  let employeeServiceMock: { list: ReturnType<typeof vi.fn> };
  let routerMock: { navigate: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    employeeServiceMock = {
      list: vi.fn().mockReturnValue(
        of({ items: [summary(1), summary(2)], totalElements: 2, page: 0, size: 20 } satisfies PagedResponse<EmployeeSummary>)
      )
    };
    routerMock = { navigate: vi.fn() };

    TestBed.configureTestingModule({
      imports: [EmployeeList],
      providers: [
        { provide: EmployeeService, useValue: employeeServiceMock },
        { provide: Router, useValue: routerMock },
        // The name cell renders a real routerLink so keyboard users get proper
        // link semantics; RouterLink resolves ActivatedRoute for relative
        // resolution, so the stub has to be present even though every link here
        // is absolute.
        { provide: ActivatedRoute, useValue: {} }
      ]
    });
  });

  it('loads the first page of employees on init', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();

    expect(employeeServiceMock.list).toHaveBeenCalledWith(
      0,
      20,
      { country: undefined, department: undefined },
      undefined
    );
    expect(fixture.componentInstance.employees()).toHaveLength(2);
    expect(fixture.componentInstance.totalElements()).toBe(2);
  });

  it('re-fetches page 0 with filter values when filters are applied', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    employeeServiceMock.list.mockClear();

    fixture.componentInstance.filterForm.setValue({ country: 'US', department: 'Engineering' });
    fixture.componentInstance.applyFilters();

    expect(employeeServiceMock.list).toHaveBeenCalledWith(
      0,
      20,
      { country: 'US', department: 'Engineering' },
      undefined
    );
  });

  it('requests the next page with the paginator page size on page change', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    employeeServiceMock.list.mockClear();

    fixture.componentInstance.onPage({ pageIndex: 1, pageSize: 10, length: 2 });

    expect(employeeServiceMock.list).toHaveBeenCalledWith(
      1,
      10,
      { country: undefined, department: undefined },
      undefined
    );
  });

  it('requests salary,desc sort and resets to page 0 when the salary header is sorted', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    fixture.componentInstance.pageIndex = 3;
    employeeServiceMock.list.mockClear();

    fixture.componentInstance.onSortChange({ active: 'salary', direction: 'desc' });

    expect(fixture.componentInstance.pageIndex).toBe(0);
    expect(employeeServiceMock.list).toHaveBeenCalledWith(
      0,
      20,
      { country: undefined, department: undefined },
      'salary,desc'
    );
  });

  it('clears the sort when Material cycles back to the unsorted state', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    fixture.componentInstance.onSortChange({ active: 'salary', direction: 'desc' });
    employeeServiceMock.list.mockClear();

    fixture.componentInstance.onSortChange({ active: 'salary', direction: '' });

    expect(employeeServiceMock.list).toHaveBeenCalledWith(
      0,
      20,
      { country: undefined, department: undefined },
      undefined
    );
  });

  it('navigates to the employee detail route when a row is opened', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();

    fixture.componentInstance.openEmployee(summary(7));

    expect(routerMock.navigate).toHaveBeenCalledWith(['/employees', 7]);
  });
});
