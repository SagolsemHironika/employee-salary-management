import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { AnalyticsSummary, DimensionBreakdown, DistributionBucket } from './analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  constructor(private readonly http: HttpClient) {}

  summary(): Observable<AnalyticsSummary> {
    return this.http.get<AnalyticsSummary>('/analytics/summary');
  }

  byDimension(groupBy: 'country' | 'department' | 'band'): Observable<DimensionBreakdown[]> {
    return this.http.get<DimensionBreakdown[]>('/analytics/by-dimension', {
      params: new HttpParams().set('groupBy', groupBy)
    });
  }

  distribution(): Observable<DistributionBucket[]> {
    return this.http.get<DistributionBucket[]>('/analytics/distribution');
  }
}
