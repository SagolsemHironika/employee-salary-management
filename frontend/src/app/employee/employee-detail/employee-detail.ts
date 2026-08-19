import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { EmployeeService } from '../employee.service';
import { EmployeeSummary } from '../employee.model';
import { SalaryRecordService } from '../../salary/salary-record.service';
import { SalaryRecord } from '../../salary/salary-record.model';
import { HumanizePipe, InitialsPipe, PlainDatePipe } from '../../core/format.pipe';

/** One rendered node on the salary timeline. */
export interface TimelineEntry {
  record: SalaryRecord;
  current: boolean;
  /** null when there is no prior record, or when the currency changed. */
  deltaAbsolute: number | null;
  deltaPercent: number | null;
  direction: 'up' | 'down' | 'flat' | 'none';
  /** True when the previous record used a different currency. */
  currencyChanged: boolean;
}

@Component({
  selector: 'app-employee-detail',
  imports: [
    DecimalPipe,
    RouterLink,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    InitialsPipe,
    HumanizePipe,
    PlainDatePipe
  ],
  templateUrl: './employee-detail.html',
  styleUrl: './employee-detail.scss'
})
export class EmployeeDetail implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly changeReasons = ['hire', 'promotion', 'annual_review', 'adjustment'];

  readonly employee = signal<EmployeeSummary | null>(null);
  readonly history = signal<SalaryRecord[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);

  /**
   * History arrives newest-first, so the *next* element is the previous salary.
   *
   * A delta is only meaningful within one currency: 95,000 USD following
   * 88,000 EUR is not a 7.9% rise. Rather than silently converting at today's
   * FX rate — which would restate history every time rates move — the entry is
   * flagged and the delta suppressed.
   */
  readonly timeline = computed<TimelineEntry[]>(() => {
    const records = this.history();
    return records.map((record, index) => {
      const previous = records[index + 1];
      const currencyChanged = Boolean(previous && previous.currencyCode !== record.currencyCode);

      if (!previous || currencyChanged) {
        return {
          record,
          current: record.endDate === null,
          deltaAbsolute: null,
          deltaPercent: null,
          direction: 'none' as const,
          currencyChanged
        };
      }

      const deltaAbsolute = record.baseSalary - previous.baseSalary;
      const deltaPercent = previous.baseSalary ? (deltaAbsolute / previous.baseSalary) * 100 : null;
      return {
        record,
        current: record.endDate === null,
        deltaAbsolute,
        deltaPercent,
        direction: deltaAbsolute > 0 ? ('up' as const) : deltaAbsolute < 0 ? ('down' as const) : ('flat' as const),
        currencyChanged: false
      };
    });
  });

  readonly form = this.fb.group({
    baseSalary: [null as number | null, [Validators.required, Validators.min(0.01)]],
    currencyCode: ['USD', [Validators.required, Validators.minLength(3), Validators.maxLength(3)]],
    effectiveDate: ['', Validators.required],
    changeReason: ['adjustment', Validators.required]
  });

  private employeeId!: number;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly employeeService: EmployeeService,
    private readonly salaryRecordService: SalaryRecordService
  ) {}

  ngOnInit(): void {
    this.employeeId = Number(this.route.snapshot.paramMap.get('id'));
    this.loadEmployee();
    this.loadHistory();
  }

  statusTone(status: string | undefined): 'active' | 'pending' | 'inactive' {
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

  submit(): void {
    if (this.form.invalid) {
      return;
    }
    this.submitting.set(true);
    this.formError.set(null);
    const { baseSalary, currencyCode, effectiveDate, changeReason } = this.form.getRawValue();
    this.salaryRecordService
      .create(this.employeeId, {
        baseSalary: baseSalary!,
        currencyCode: currencyCode!,
        effectiveDate: effectiveDate!,
        changeReason: changeReason!
      })
      .subscribe({
        next: () => {
          this.submitting.set(false);
          this.form.reset({ currencyCode: 'USD', changeReason: 'adjustment' });
          this.loadHistory();
        },
        error: (err) => {
          this.submitting.set(false);
          this.formError.set(err?.error?.message ?? 'Could not create salary record');
        }
      });
  }

  private loadEmployee(): void {
    this.employeeService.get(this.employeeId).subscribe((employee) => {
      this.employee.set(employee);
      this.loading.set(false);
    });
  }

  private loadHistory(): void {
    this.salaryRecordService.history(this.employeeId).subscribe((records) => this.history.set(records));
  }
}
