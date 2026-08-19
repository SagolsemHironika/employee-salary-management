import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
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
    status: 'active'
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
        { provide: Router, useValue: routerMock }
      ]
    });
  });

  it('loads the first page of employees on init', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();

    expect(employeeServiceMock.list).toHaveBeenCalledWith(0, 20, { country: undefined, department: undefined });
    expect(fixture.componentInstance.employees()).toHaveLength(2);
    expect(fixture.componentInstance.totalElements()).toBe(2);
  });

  it('re-fetches page 0 with filter values when filters are applied', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    employeeServiceMock.list.mockClear();

    fixture.componentInstance.filterForm.setValue({ country: 'US', department: 'Engineering' });
    fixture.componentInstance.applyFilters();

    expect(employeeServiceMock.list).toHaveBeenCalledWith(0, 20, { country: 'US', department: 'Engineering' });
  });

  it('requests the next page with the paginator page size on page change', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();
    employeeServiceMock.list.mockClear();

    fixture.componentInstance.onPage({ pageIndex: 1, pageSize: 10, length: 2 });

    expect(employeeServiceMock.list).toHaveBeenCalledWith(1, 10, { country: undefined, department: undefined });
  });

  it('navigates to the employee detail route when a row is opened', () => {
    const fixture = TestBed.createComponent(EmployeeList);
    fixture.detectChanges();

    fixture.componentInstance.openEmployee(summary(7));

    expect(routerMock.navigate).toHaveBeenCalledWith(['/employees', 7]);
  });
});
