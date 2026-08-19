import { DOCUMENT } from '@angular/common';
import { Injectable, Signal, computed, inject } from '@angular/core';
import { Color, ScaleType } from '@swimlane/ngx-charts';
import { ThemeService } from './theme.service';

const CATEGORICAL = ['--chart-1', '--chart-2', '--chart-3', '--chart-4', '--chart-5', '--chart-6'];

/**
 * Bridges SCSS tokens into ngx-charts.
 *
 * ngx-charts writes its series colour straight into an SVG `fill` and also
 * feeds it to d3-color for hover/active shading, so `var(--chart-1)` cannot be
 * passed through — d3 needs a parseable colour. We therefore resolve the custom
 * properties off the document element at read time.
 *
 * Reading computed style rather than duplicating hexes in TypeScript keeps
 * _tokens.scss the single source of truth; the computed signal re-resolves
 * whenever the theme flips, which is exactly when the values change.
 */
@Injectable({ providedIn: 'root' })
export class ChartTheme {
  private readonly document = inject(DOCUMENT);
  private readonly theme = inject(ThemeService);

  /** Categorical palette, ordered. Recomputes on every theme change. */
  readonly scheme: Signal<Color> = computed(() => {
    this.theme.resolved(); // dependency: re-resolve when the palette flips
    return {
      name: 'ledger',
      selectable: false,
      group: ScaleType.Ordinal,
      domain: CATEGORICAL.map((token) => this.token(token))
    };
  });

  /** Single-hue scheme, for charts where every bar means the same thing. */
  readonly monoScheme: Signal<Color> = computed(() => {
    this.theme.resolved();
    return {
      name: 'ledger-mono',
      selectable: false,
      group: ScaleType.Ordinal,
      domain: [this.token('--chart-1')]
    };
  });

  private token(name: string): string {
    const value = this.document.defaultView
      ?.getComputedStyle(this.document.documentElement)
      .getPropertyValue(name)
      .trim();
    // Fallback keeps charts rendering if styles have not landed yet.
    return value || '#4b45e0';
  }
}
