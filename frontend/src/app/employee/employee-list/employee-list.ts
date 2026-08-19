import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule, Sort } from '@angular/material/sort';
import { EmployeeService } from '../employee.service';
import { EmployeeSummary } from '../employee.model';
import { HumanizePipe, InitialsPipe } from '../../core/format.pipe';

@Component({
  selector: 'app-employee-list',
  imports: [
    DecimalPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    InitialsPipe,
    HumanizePipe
  ],
  templateUrl: './employee-list.html',
  styleUrl: './employee-list.scss'
})
export class EmployeeList implements OnInit {
  // `employeeCode` and `salary` are the only server-sortable keys, so those
  // column defs keep their names; `employee` is a presentation-only merge of
  // first/last name and is deliberately not given a sort header.
  readonly displayedColumns = [
    'employeeCode',
    'employee',
    'department',
    'jobTitle',
    'countryCode',
    'status',
    'salary'
  ];

  readonly skeletonRows = Array.from({ length: 8 }, (_, i) => i);

  private readonly fb = inject(FormBuilder);

  readonly filterForm = this.fb.group({
    country: [''],
    department: ['']
  });

  readonly employees = signal<EmployeeSummary[]>([]);
  readonly totalElements = signal(0);
  readonly loading = signal(true);

  /**
   * Set when the request itself failed.
   *
   * Kept distinct from "zero results" on purpose: rendering the empty-state
   * copy after a network or server error tells the user their filters matched
   * nothing, which is a guess, and usually a wrong one.
   */
  readonly loadError = signal<string | null>(null);

  /** Separates "first paint" (skeleton) from "refetch" (dim the existing grid). */
  readonly hasLoadedOnce = signal(false);

  readonly appliedFilters = signal<{ country?: string; department?: string }>({});
  readonly isFiltered = computed(() => {
    const { country, department } = this.appliedFilters();
    return Boolean(country || department);
  });

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

  clearFilters(): void {
    this.filterForm.reset({ country: '', department: '' });
    this.applyFilters();
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

  /** Maps a backend status string onto one of the three status tones. */
  statusTone(status: string): 'active' | 'pending' | 'inactive' {
    switch (status?.toLowerCase()) {
      case 'active':
        return 'active';
      case 'on_leave':
      case 'pending':
        return 'pending';
      default:
        return 'inactive';
    }
  }

  /** Re-runs the current page/filter/sort after a failure. */
  retry(): void {
    this.fetchPage();
  }

  private fetchPage(): void {
    this.loading.set(true);
    this.loadError.set(null);
    const { country, department } = this.filterForm.getRawValue();
    const filter = { country: country || undefined, department: department || undefined };
    this.employeeService.list(this.pageIndex, this.pageSize, filter, this.sort).subscribe({
      next: (response) => {
        this.employees.set(response.items);
        this.totalElements.set(response.totalElements);
        this.appliedFilters.set(filter);
        this.loading.set(false);
        this.hasLoadedOnce.set(true);
      },
      error: (err) => {
        // Clear stale rows: leaving the previous page visible under an error
        // banner invites someone to act on numbers that may no longer be current.
        this.employees.set([]);
        this.totalElements.set(0);
        this.loadError.set(
          err?.status === 0
            ? 'Could not reach the server. Check your connection and try again.'
            : `Could not load employees (error ${err?.status ?? 'unknown'}).`
        );
        this.loading.set(false);
        this.hasLoadedOnce.set(true);
      }
    });
  }
}
