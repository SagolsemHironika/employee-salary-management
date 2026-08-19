import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { SalaryRecord, SalaryRecordRequest } from './salary-record.model';

@Injectable({ providedIn: 'root' })
export class SalaryRecordService {
  constructor(private readonly http: HttpClient) {}

  history(employeeId: number): Observable<SalaryRecord[]> {
    return this.http.get<SalaryRecord[]>(`/employees/${employeeId}/salary-records`);
  }

  create(employeeId: number, request: SalaryRecordRequest): Observable<SalaryRecord> {
    return this.http.post<SalaryRecord>(`/employees/${employeeId}/salary-records`, request);
  }
}
