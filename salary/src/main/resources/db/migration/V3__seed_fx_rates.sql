-- Static FX snapshot for USD-normalized analytics reporting. Illustrative
-- rates as of a single snapshot date; see docs/REQUIREMENTS.md for the
-- documented limitation (no live FX conversion in v1).
INSERT INTO fx_rates_snapshot (currency_code, rate_to_usd, as_of_date) VALUES
    ('USD', 1.00000000, '2026-01-01'),
    ('EUR', 1.08000000, '2026-01-01'),
    ('GBP', 1.27000000, '2026-01-01'),
    ('INR', 0.01200000, '2026-01-01'),
    ('BRL', 0.17000000, '2026-01-01'),
    ('NGN', 0.00062000, '2026-01-01'),
    ('JPY', 0.00680000, '2026-01-01'),
    ('PLN', 0.25000000, '2026-01-01');
