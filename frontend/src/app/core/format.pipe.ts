import { Pipe, PipeTransform } from '@angular/core';

/**
 * Compact currency for summary tiles: 12_400_000 -> "$12.4M".
 *
 * A total-payroll figure is read for magnitude, not for cents. Rendering all
 * nine digits forces the eye to count commas; the full value stays available
 * in the element's `title` at the call site.
 */
@Pipe({ name: 'compactMoney' })
export class CompactMoneyPipe implements PipeTransform {
  transform(value: number | null | undefined, currency = 'USD'): string {
    if (value === null || value === undefined || Number.isNaN(value)) {
      return '—';
    }
    return new Intl.NumberFormat('en-US', {
      style: 'currency',
      currency,
      notation: 'compact',
      maximumFractionDigits: 1
    }).format(value);
  }
}

/** Initials for the row monogram: "Ada", "Lovelace" -> "AL". */
@Pipe({ name: 'initials' })
export class InitialsPipe implements PipeTransform {
  transform(first: string | null | undefined, last: string | null | undefined): string {
    return `${first?.[0] ?? ''}${last?.[0] ?? ''}` || '—';
  }
}

/**
 * Renders a backend `LocalDate` ("2024-01-01") without a timezone shift.
 *
 * DatePipe would hand the string to `new Date()`, which reads a bare ISO date
 * as UTC midnight and then formats it in local time — west of Greenwich that
 * prints the previous day. An effective date that slides by one day is a
 * payroll defect, so the parts are read literally instead.
 */
@Pipe({ name: 'plainDate' })
export class PlainDatePipe implements PipeTransform {
  transform(value: string | null | undefined, style: 'short' | 'month' = 'short'): string {
    if (!value) {
      return '—';
    }
    const [year, month, day] = value.split('-').map(Number);
    if (!year || !month || !day) {
      return value;
    }
    const local = new Date(year, month - 1, day);
    return new Intl.DateTimeFormat('en-GB', {
      day: style === 'short' ? 'numeric' : undefined,
      month: 'short',
      year: 'numeric'
    }).format(local);
  }
}

/** "annual_review" -> "Annual review". Backend enums are not display copy. */
@Pipe({ name: 'humanize' })
export class HumanizePipe implements PipeTransform {
  transform(value: string | null | undefined): string {
    if (!value) {
      return '—';
    }
    const spaced = value.replace(/[_-]+/g, ' ').trim();
    return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
  }
}
