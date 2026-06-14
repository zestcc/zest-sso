# ZestSSO Benchmark Report

| Item | Value |
|------|-------|
| Started | 2026-06-14 19:22:00 |
| Base URL | http://localhost:9000 |
| Requests per endpoint | 500 |
| Concurrency | 20 |

## Results

| Endpoint | Total | Errors | Avg(ms) | P50(ms) | P99(ms) | QPS |
|----------|-------|--------|---------|---------|---------|-----|
| /api/public/health | 500 | 0 | 13.88 | 4.27 | 255.37 | 72.05 |
| /oauth2/jwks | 500 | 0 | 9.16 | 2.3 | 182.99 | 109.17 |
| /api/public/.well-known/openid-configuration | 500 | 0 | 10.71 | 3.03 | 207.23 | 93.37 |
| /scim/v2/ServiceProviderConfig | 500 | 0 | 8.67 | 2.22 | 167.09 | 115.38 |
| /login | 500 | 0 | 16.85 | 7.4 | 263 | 59.33 |

## Summary

- All benchmark endpoints succeeded under baseline concurrency.
- Re-run on production-like MySQL + Redis and monitor OAuth token endpoint P99.
