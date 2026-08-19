export interface AnalyticsSummary {
  headcount: number;
  totalPayrollUsd: number;
}

export interface DimensionBreakdown {
  dimension: string;
  headcount: number;
  totalPayrollUsd: number;
}

export interface DistributionBucket {
  bucket: string;
  headcount: number;
}
