import { DecimalPipe } from '@angular/common';
import { Component, OnInit, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { AnalyticsService } from '../analytics.service';
import { AnalyticsSummary } from '../analytics.model';

interface ChartDatum {
  name: string;
  value: number;
}

@Component({
  selector: 'app-analytics-dashboard',
  imports: [DecimalPipe, MatCardModule, MatProgressSpinnerModule, NgxChartsModule],
  templateUrl: './analytics-dashboard.html',
  styleUrl: './analytics-dashboard.scss'
})
export class AnalyticsDashboard implements OnInit {
  readonly loading = signal(true);
  readonly summary = signal<AnalyticsSummary | null>(null);
  readonly headcountByDepartment = signal<ChartDatum[]>([]);
  readonly payrollByCountry = signal<ChartDatum[]>([]);
  readonly distribution = signal<ChartDatum[]>([]);

  constructor(private readonly analyticsService: AnalyticsService) {}

  ngOnInit(): void {
    this.analyticsService.summary().subscribe((summary) => {
      this.summary.set(summary);
      this.loading.set(false);
    });

    this.analyticsService
      .byDimension('department')
      .subscribe((rows) =>
        this.headcountByDepartment.set(rows.map((r) => ({ name: r.dimension, value: r.headcount })))
      );

    this.analyticsService
      .byDimension('country')
      .subscribe((rows) => this.payrollByCountry.set(rows.map((r) => ({ name: r.dimension, value: r.totalPayrollUsd }))));

    this.analyticsService
      .distribution()
      .subscribe((rows) => this.distribution.set(rows.map((r) => ({ name: r.bucket, value: r.headcount }))));
  }
}
