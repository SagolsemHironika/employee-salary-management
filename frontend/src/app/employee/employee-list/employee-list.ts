import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmployeeService } from '../employee.service';
import { EmployeeSummary } from '../employee.model';

@Component({
  selector: 'app-employee-list',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressSpinnerModule
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
    'status'
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

  private fetchPage(): void {
    this.loading.set(true);
    const { country, department } = this.filterForm.getRawValue();
    this.employeeService
      .list(this.pageIndex, this.pageSize, { country: country || undefined, department: department || undefined })
      .subscribe((response) => {
        this.employees.set(response.items);
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
      });
  }
}
