import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { EmployeeService } from '../employee.service';
import { EmployeeSummary } from '../employee.model';
import { SalaryRecordService } from '../../salary/salary-record.service';
import { SalaryRecord } from '../../salary/salary-record.model';

@Component({
  selector: 'app-employee-detail',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './employee-detail.html',
  styleUrl: './employee-detail.scss'
})
export class EmployeeDetail implements OnInit {
  private readonly fb = inject(FormBuilder);

  readonly changeReasons = ['hire', 'promotion', 'annual_review', 'adjustment'];
  readonly historyColumns = ['effectiveDate', 'endDate', 'baseSalary', 'currencyCode', 'changeReason', 'createdBy'];

  readonly employee = signal<EmployeeSummary | null>(null);
  readonly history = signal<SalaryRecord[]>([]);
  readonly loading = signal(true);
  readonly submitting = signal(false);
  readonly formError = signal<string | null>(null);

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
