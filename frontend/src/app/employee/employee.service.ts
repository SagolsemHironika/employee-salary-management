import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { EmployeeSummary, PagedResponse } from './employee.model';

export interface EmployeeListFilter {
  country?: string;
  department?: string;
}

@Injectable({ providedIn: 'root' })
export class EmployeeService {
  constructor(private readonly http: HttpClient) {}

  list(
    page: number,
    size: number,
    filter: EmployeeListFilter = {},
    sort?: string
  ): Observable<PagedResponse<EmployeeSummary>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (filter.country) {
      params = params.set('country', filter.country);
    }
    if (filter.department) {
      params = params.set('department', filter.department);
    }
    if (sort) {
      params = params.set('sort', sort);
    }
    return this.http.get<PagedResponse<EmployeeSummary>>('/employees', { params });
  }

  get(id: number): Observable<EmployeeSummary> {
    return this.http.get<EmployeeSummary>(`/employees/${id}`);
  }
}
