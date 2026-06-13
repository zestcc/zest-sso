# ZestSSO 鐢熶骇绾ч獙鏀舵姤鍛?
| 椤圭洰 | 鍐呭 |
|------|------|
| 鐗堟湰 | 1.0.0-SNAPSHOT |
| 寮€濮嬫椂闂?| 2026-06-14 01:14:59 |
| 缁撴潫鏃堕棿 | 2026-06-14 01:17:16 |
| 鑰楁椂 | 2.3 鍒嗛挓 |
| 鐜 | JDK 17, MySQL 8, Redis 7, Windows 鏈湴 |
| 鏈嶅姟鍦板潃 | http://localhost:9000 |
| **楠屾敹缁撹** | **閫氳繃 (PASS)** |

## 1. 鐧界洅娴嬭瘯锛圵hite-box锛?
浠ｇ爜绾у崟鍏冩祴璇曚笌 Spring Boot 闆嗘垚娴嬭瘯锛岃鐩栧畨鍏ㄣ€丱IDC銆丼CIM銆丼AML銆丄dmin 绛夋牳蹇冩ā鍧椼€?
| 绫诲埆 | 鐘舵€?| 鐢ㄤ緥鏁?| 澶辫触 | 閿欒 |
|------|------|--------|------|------|
| 鍗曞厓娴嬭瘯 (mvn test) | PASS | 2 | 0 | 0 |
| 闆嗘垚娴嬭瘯 (mvn test -Pmysql-it) | PASS | 4 | 0 | 0 |

闆嗘垚娴嬭瘯鍚細AdminSessionChainIT銆丱idcPublicApiIT銆丼cimApiIT銆丼cimBulkPatchIT銆丄dminAuthIT銆丄dminIdentityProviderIT銆乄ebAuthnPublicApiIT銆?
## 2. 榛戠洅娴嬭瘯锛圔lack-box锛?
瀵硅繍琛屼腑鏈嶅姟杩涜澶栭儴 API 鍐掔儫锛屼笉渚濊禆鍐呴儴瀹炵幇銆?
| 椤圭洰 | 鐘舵€?| 閫氳繃椤?| 澶辫触椤?|
|------|------|--------|--------|
| E2E 鍐掔儫 (e2e-local.ps1) | PASS | 10 | 0 |

瑕嗙洊锛氬仴搴锋鏌ャ€丱IDC Discovery銆丣WKS銆丼CIM 閰嶇疆銆丄dmin 鐧诲綍+浼氳瘽銆丼CIM Token銆佺櫥鍑?URL銆佺櫥褰曢〉銆乄ebAuthn 鐧诲綍閫夐」銆?
## 3. 閾捐矾娴嬭瘯锛圕hain / E2E Flow锛?
| 閾捐矾 | 鐘舵€?| 姝ラ鏁?|
|------|------|--------|
| 鍏ㄩ摼璺?(chain-local.ps1) | PASS | 22 |

| 閾捐矾鍚嶇О | 楠岃瘉姝ラ |
|----------|----------|
| OIDC-Public | Discovery 鈫?JWKS 鈫?Client Credentials Token |
| Admin-Session | Login 鈫?/me 鈫?Clients 鈫?Users 鈫?Dashboard 鈫?Logout 鈫?401 |
| SCIM-Lifecycle | Token 鈫?Config 鈫?Create User 鈫?PATCH 鍋滅敤 鈫?楠岃瘉 鈫?Delete |
| Security | 閿欒瀵嗙爜鎷掔粷銆佹棤 Token SCIM 401 |
| OIDC-Authorize | PKCE 鎺堟潈璇锋眰閲嶅畾鍚戣嚦鐧诲綍 |
| WebAuthn-SLO | WebAuthn 鐧诲綍閫夐」銆丷P logout URI 閰嶇疆銆丏iscovery backchannel 澹版槑 |

## 4. 鍘嬪姏娴嬭瘯锛圫tress锛?
| 鎸囨爣 | 闃堝€?| 瀹炴祴 | 鐘舵€?|
|------|------|------|------|
| 閿欒鐜?| 0% | 0 errors | PASS |
| Health P99 | < 500ms | 230.81 ms | PASS |
| 骞跺彂 | 20 | 500 req/endpoint | 鈥?|

璇︾粏鍘嬫祴鏁版嵁瑙?[benchmark-report.md](benchmark-report.md)銆?
## 5. 鐢熶骇鍑嗗叆妫€鏌ユ竻鍗?
| 妫€鏌ラ」 | 鐘舵€?| 璇存槑 |
|--------|------|------|
| MySQL 鎸佷箙鍖栵紙绂佺敤 H2锛?| PASS | MysqlOnlyDataSourceGuard 寮哄埗 MySQL |
| Redis Session | PASS | Spring Session Redis |
| OAuth2/OIDC 鏍囧噯绔偣 | PASS | SAS 瀹樻柟瀹炵幇 |
| Admin 浼氳瘽璁よ瘉 | PASS | 鐧诲綍鍚?Session 鎸佷箙鍖?|
| SCIM 2.0 PATCH/Bulk | PASS | 閾捐矾娴嬭瘯楠岃瘉 |
| SAML 鍏冩暟鎹鍏?| PASS | 闆嗘垚娴嬭瘯楠岃瘉 |
| 鐧诲綍闄愭祦 | PASS | 20娆?鍒嗛挓/IP |
| JWT RS256 | PASS | 2048-bit RSA |
| Flyway 杩佺Щ V1-V8 | PASS | 鑷姩鎵ц |
| HTTPS / 鎸佷箙鍖栧瘑閽?| 寰呯敓浜ч厤缃?| 閮ㄧ讲鏃堕厤缃?application-prod.yml |

## 6. 鎵ц鍛戒护

    $env:MYSQL_PASSWORD = '123456'
    powershell -File scripts/acceptance.ps1

## 7. 缁撹

PASS: ZestSSO v1.0.0-SNAPSHOT production acceptance completed.
