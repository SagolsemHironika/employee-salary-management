import { DOCUMENT } from '@angular/common';
import { Injectable, computed, inject, signal } from '@angular/core';

export type ThemePreference = 'light' | 'dark' | 'system';

const STORAGE_KEY = 'ledger.theme';

/**
 * Owns the light/dark preference.
 *
 * Three states, not two: "system" is a real choice, and it is the default.
 * The `data-theme` attribute is only written for an explicit preference — when
 * the user is on "system" we remove it entirely and let the
 * `prefers-color-scheme` block in _tokens.scss decide, which keeps the OS in
 * charge without any JS media-query plumbing.
 */
@Injectable({ providedIn: 'root' })
export class ThemeService {
  private readonly document = inject(DOCUMENT);

  readonly preference = signal<ThemePreference>(this.read());

  /** Tracks the OS setting live, so "system" reacts without a reload. */
  private readonly systemDark = signal(this.prefersDark());

  /** What the user will actually see right now — resolves "system". */
  readonly resolved = computed<'light' | 'dark'>(() => {
    const preference = this.preference();
    if (preference !== 'system') {
      return preference;
    }
    return this.systemDark() ? 'dark' : 'light';
  });

  constructor() {
    this.apply(this.preference());
    this.document.defaultView
      ?.matchMedia?.('(prefers-color-scheme: dark)')
      .addEventListener('change', (event) => this.systemDark.set(event.matches));
  }

  set(preference: ThemePreference): void {
    this.preference.set(preference);
    this.apply(preference);
    try {
      if (preference === 'system') {
        localStorage.removeItem(STORAGE_KEY);
      } else {
        localStorage.setItem(STORAGE_KEY, preference);
      }
    } catch {
      // Private browsing: the preference simply will not survive a reload.
    }
  }

  /** Cycles light -> dark -> system, for the single-button toolbar control. */
  cycle(): void {
    const next: Record<ThemePreference, ThemePreference> = {
      light: 'dark',
      dark: 'system',
      system: 'light'
    };
    this.set(next[this.preference()]);
  }

  private apply(preference: ThemePreference): void {
    const root = this.document.documentElement;
    if (preference === 'system') {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', preference);
    }
  }

  private prefersDark(): boolean {
    return this.document.defaultView?.matchMedia?.('(prefers-color-scheme: dark)').matches ?? false;
  }

  private read(): ThemePreference {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      return stored === 'dark' || stored === 'light' ? stored : 'system';
    } catch {
      return 'system';
    }
  }
}
