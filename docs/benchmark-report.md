# ZestSSO Benchmark Report

| Item | Value |
|------|-------|
| Started | 2026-06-14 01:16:29 |
| Base URL | http://localhost:9000 |
| Requests per endpoint | 500 |
| Concurrency | 20 |

## Results

| Endpoint | Total | Errors | Avg(ms) | P50(ms) | P99(ms) | QPS |
|----------|-------|--------|---------|---------|---------|-----|
| /api/public/health | 500 | 0 | 12.9 | 3.93 | 230.81 | 77.5 |
| /oauth2/jwks | 500 | 0 | 11.59 | 2.74 | 242.11 | 86.29 |
| /api/public/.well-known/openid-configuration | 500 | 0 | 11.35 | 3.01 | 218.73 | 88.13 |
| /scim/v2/ServiceProviderConfig | 500 | 0 | 10.97 | 2.64 | 222.02 | 91.18 |
| /login | 500 | 0 | 16.53 | 7.04 | 260.8 | 60.51 |

## Summary

- All benchmark endpoints succeeded under baseline concurrency.
- Re-run on production-like MySQL + Redis and monitor OAuth token endpoint P99.
