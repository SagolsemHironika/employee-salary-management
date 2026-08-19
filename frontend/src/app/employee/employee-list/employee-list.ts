import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSortModule, Sort } from '@angular/material/sort';
import { EmployeeService } from '../employee.service';
import { EmployeeSummary } from '../employee.model';

@Component({
  selector: 'app-employee-list',
  imports: [
    DecimalPipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSortModule
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss'
})
export class EmployeeList implements OnInit {
  readonly displayedColumns = [
    'employeeCode',
    'firstName',
    'lastName',
    'countryCode',
    'department',
    'jobTitle',
    'band',
    'status',
    'salary'
  ];

  private readonly fb = inject(FormBuilder);

  readonly filterForm = this.fb.group({
    country: [''],
    department: ['']
  });

  readonly employees = signal<EmployeeSummary[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(true);
  pageIndex = 0;
  pageSize = 20;
  private sort: string | undefined;

  constructor(
    private readonly employeeService: EmployeeService,
    private readonly router: Router
  ) {}

  ngOnInit(): void {
    this.fetchPage();
  }

  applyFilters(): void {
    this.pageIndex = 0;
    this.fetchPage();
  }

  onPage(event: PageEvent): void {
    this.pageIndex = event.pageIndex;
    this.pageSize = event.pageSize;
    this.fetchPage();
  }

  openEmployee(employee: EmployeeSummary): void {
    this.router.navigate(['/employees', employee.id]);
  }

  onSortChange(sort: Sort): void {
    this.sort = sort.direction ? `${sort.active},${sort.direction}` : undefined;
    this.pageIndex = 0;
    this.fetchPage();
  }

  private fetchPage(): void {
    this.loading.set(true);
    const { country, department } = this.filterForm.getRawValue();
    this.employeeService
      .list(
        this.pageIndex,
        this.pageSize,
        { country: country || undefined, department: department || undefined },
        this.sort
      )
      .subscribe((response) => {
        this.employees.set(response.items);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      });
  }
}
