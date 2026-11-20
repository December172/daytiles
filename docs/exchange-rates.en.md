# Official exchange rates without setup

[简体中文（默认）](exchange-rates.md) | [English](exchange-rates.en.md)

The finance screen and expense editor offer conversion between CNY, HKD, TWD, and USD. Opening the converter automatically loads public official data. No registration, API key, or configuration is needed. Snapshots downloaded within the last 24 hours are reused; **Refresh rates** forces an update.

Downloaded tables support offline conversion across all covered pairs. The screen separates the official observation date from the local download time and links to the source. **Use converted amount** fills the current expense editor; saving remains a separate action. Its target currency is fixed to that expense’s ledger.

## Official sources and research

The following information comes from official pages, public endpoints, and documentation accessed on 2026-11-20.

| Source | Free public data and coverage | Role and limits |
| --- | --- | --- |
| [State Administration of Foreign Exchange](https://www.safe.gov.cn/safe/rmbhlzjj/index.html) | Its official RMB central parity page explicitly identifies CFETS as the data source | Confirms the mainland source. The webpage and JSON express units differently; the app does not scrape this table |
| [China Foreign Exchange Trade System, CFETS](https://www.chinamoney.com.cn/chinese/bkccpr/) | ChinaMoney’s [public JSON](https://www.chinamoney.com.cn/r/cms/www/chinamoney/data/fx/ccpr.json) needs no key and returned central parity data dated 2026-11-20 | Preferred for CNY/HKD/USD; excludes TWD. This is a public website data feed, rather than a versioned developer API with a compatibility commitment |
| [Hong Kong Monetary Authority, HKMA](https://apidocs.hkma.gov.hk/documentation/market-data-and-statistics/monthly-statistical-bulletin/er-ir/er-eeri-daily/) | Documented free public HTTPS JSON API, including CNY, HKD, USD, and TWD, without a key | Used for TWD pairs and as a fallback. Daily observations belong to the monthly statistical bulletin and publication can lag. The latest returned observation was 2026-10-31; these are not described as live market prices |
| [Bank of China](https://www.boc.cn/sourcedb/whpj/) | Public official bank webpage with cash, telegraphic transfer, and conversion quotes; no key | Not selected: HTML bank quotations differ in meaning and are not a government open API |
| [Bank of Taiwan](https://rate.bot.com.tw/xrt/flcsv/0/day) | Official bank CSV download endpoint | Returned a browser verification page during research, so it is not an app dependency |

No commercial service requiring user credentials, bundled shared key, proxy backend, or account system is integrated. Source website terms still apply. HKMA’s [terms of use](https://www.hkma.gov.hk/eng/other-information/terms-and-conditions.shtml) include noncommercial use and attribution requirements. The converter identifies its sources and links to their official sites; this does not imply official endorsement.

## Conversion and caching

- CFETS `USD/CNY` and `HKD/CNY` mean CNY per unit of foreign currency, with CNY fixed at 1. Other quote conventions are ignored. The app does not confuse the webpage’s per-100-unit format with these JSON fields.
- HKMA `usd`, `cny`, and `twd` mean HKD per unit of foreign currency, with HKD fixed at 1. The newest complete, valid observation in the response is selected.
- A cross-rate is the source currency’s base value divided by the target currency’s base value. Each calculation uses one source and observation date; tables are never mixed. Displayed conversion rates are calculated locally from reference data.
- `BigDecimal` preserves decimal arithmetic. Cross-rates use 12 decimal places; the final amount rounds to minor units. Invalid, nonpositive, excessive-precision, and out-of-range data are rejected.
- Separate SharedPreferences snapshots store each source’s entire table, supporting reverse and other pairs offline. When both sources cover a pair, the newer observation wins, with CFETS preferred on equal dates.
- Automatic requests happen only when a cached quote is absent or its download is at least 24 hours old. Manual refresh bypasses this limit. CFETS failure triggers HKMA fallback. Network or parsing failures preserve the last successful snapshots and show an error.
- Requests use fixed official URLs and send no entered amounts, expenses, journals, notes, or budgets. There is no background polling. HTTPS requests disable redirects and enforce timeouts and a 128 KiB response limit.
- Room schema, existing records, budgets, and currency totals are unchanged. **Clear saved rates** removes only exchange rate snapshots.

Reference data can be old because of weekends, holidays, bulletin schedules, or endpoint outages. These are personal budgeting estimates, not bank/card settlement prices, and exclude fees. Check the displayed observation date before use.

## Verification

Automated coverage includes official parsing, rate direction and unit conventions, complete observations, invalid data, first use without configuration, fallback, whole-table caching, daily automatic refresh, forced refresh, offline retention, and applying an amount in the expense editor.

```shell
ANDROID_HOME=/opt/android-sdk ANDROID_SDK_ROOT=/opt/android-sdk \
  ANDROID_SERIAL=emulator-5554 ./gradlew --offline \
  assembleDebug testDebugUnitTest connectedDebugAndroidTest lintDebug \
  --no-configuration-cache
```

Verified on 2026-11-20: debug build, all 23 JVM tests, all 54 device tests on the API 37 AVD, and Lint (zero errors; seven existing dependency/tool update notices).

Separate live emulator checks opened the converter without a rate cache and automatically retrieved CFETS data dated 2026-11-20 09:15. Selecting TWD automatically retrieved HKMA data dated 2026-10-31. After disabling Wi-Fi and mobile data and restarting the app, downloaded rates remained usable; a failed manual refresh retained the original quote and observation date. Screenshots from both sources and the offline failure state were read and visually inspected, with no clipped text or controls.

All 54 device tests also passed on a vivo V2425A (Android 16 / API 36) using an isolated installation. The existing DayTiles installation was then updated in place. All eight database, preference, and app files matched byte for byte before and after the update. The updated app retrieved rates from CFETS and HKMA, and both screenshots were visually checked in dark mode with Simplified Chinese.

The first run needs network access to resolve build and test dependencies. Fixed test quotes are fixtures, not current prices.
