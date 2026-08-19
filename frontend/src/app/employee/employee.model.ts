export interface EmployeeSummary {
  id: number;
  employeeCode: string;
  firstName: string;
  lastName: string;
  email: string;
  countryCode: string;
  department: string;
  jobTitle: string;
  band: string;
  hireDate: string;
  status: string;
}

export interface PagedResponse<T> {
  items: T[];
  totalElements: number;
  page: number;
  size: number;
}
