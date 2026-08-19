export interface SalaryRecord {
  id: number;
  employeeId: number;
  baseSalary: number;
  currencyCode: string;
  bonus: number;
  allowances: number;
  effectiveDate: string;
  endDate: string | null;
  changeReason: string;
  createdBy: string;
}

export interface SalaryRecordRequest {
  baseSalary: number;
  currencyCode: string;
  bonus?: number;
  allowances?: number;
  effectiveDate: string;
  changeReason: string;
}
