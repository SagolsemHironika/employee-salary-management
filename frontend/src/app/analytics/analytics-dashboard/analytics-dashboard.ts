import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
// BarChartModule, not NgxChartsModule: the umbrella module drags in pie, line,
// area, heatmap, treemap and gauge, none of which this dashboard renders.
import { BarChartModule } from '@swimlane/ngx-charts';
import { AnalyticsService } from '../analytics.service';
import { AnalyticsSummary } from '../analytics.model';
import { ChartTheme } from '../../core/chart-theme';
import { CompactMoneyPipe } from '../../core/format.pipe';

interface ChartDatum {
  name: string;
  value: number;
}

@Component({
  selector: 'app-analytics-dashboard',
  imports: [DecimalPipe, BarChartModule, CompactMoneyPipe],
  templateUrl: './analytics-dashboard.html',
  styleUrl: './analytics-dashboard.scss'
})
export class AnalyticsDashboard implements OnInit {
  readonly chartTheme = inject(ChartTheme);

  readonly loading = signal(true);
  readonly summary = signal<AnalyticsSummary | null>(null);
  readonly headcountByDepartment = signal<ChartDatum[]>([]);
  readonly payrollByCountry = signal<ChartDatum[]>([]);
  readonly distribution = signal<ChartDatum[]>([]);

  /** Derived, not fetched: one fewer round trip and it can never disagree. */
  readonly averageBaseUsd = computed(() => {
    const s = this.summary();
    return s && s.headcount > 0 ? s.totalPayrollUsd / s.headcount : null;
  });

  readonly departmentCount = computed(() => this.headcountByDepartment().length);

  constructor(private readonly analyticsService: AnalyticsService) {}

  /** Axis formatter — a payroll axis in raw digits is unreadable. */
  readonly formatMoneyTick = (value: number): string =>
    new Intl.NumberFormat('en-US', { notation: 'compact', maximumFractionDigits: 1 }).format(value);

  ngOnInit(): void {
    this.analyticsService.summary().subscribe((summary) => {
      this.summary.set(summary);
      this.loading.set(false);
    });

    this.analyticsService.byDimension('department').subscribe((rows) =>
      this.headcountByDepartment.set(
        // Sorted descending: a ranked bar chart is read top-to-bottom, and
        // alphabetical order hides the very comparison the chart exists for.
        rows
          .map((r) => ({ name: r.dimension, value: r.headcount }))
          .sort((a, b) => b.value - a.value)
      )
    );

    this.analyticsService.byDimension('country').subscribe((rows) =>
      this.payrollByCountry.set(
        rows
          .map((r) => ({ name: r.dimension, value: r.totalPayrollUsd }))
          .sort((a, b) => b.value - a.value)
      )
    );

    // Distribution buckets keep their server order: they are an ordered scale,
    // so re-sorting by height would destroy the histogram's meaning.
    this.analyticsService
      .distribution()
      .subscribe((rows) => this.distribution.set(rows.map((r) => ({ name: r.bucket, value: r.headcount }))));
  }
}
