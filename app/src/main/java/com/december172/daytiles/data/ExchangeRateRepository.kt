package com.december172.daytiles.data

import android.content.Context
import androidx.core.content.edit
import com.december172.daytiles.domain.model.ExchangeRate
import com.december172.daytiles.domain.model.ExchangeRateSource
import com.december172.daytiles.domain.model.Money
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** All cross rates use one source and one observation, never mismatched dates. */
data class ExchangeRateTable(
    val source: ExchangeRateSource,
    val updatedAt: String,
    val fetchedAt: Long,
    val ratesToBase: Map<String, BigDecimal>,
) {
    init {
        val expected = if (source == ExchangeRateSource.CFETS) setOf("CNY", "HKD", "USD") else Money.currencies.toSet()
        require(ratesToBase.keys == expected)
        require(ratesToBase[if (source == ExchangeRateSource.CFETS) "CNY" else "HKD"] == BigDecimal.ONE)
        require(updatedAt.length <= 32 && fetchedAt >= 0)
        LocalDate.parse(updatedAt.take(10))
        ratesToBase.values.forEach {
            require(it > BigDecimal.ZERO && it <= BigDecimal("1000000"))
            require(it.precision() <= 24 && it.scale() in 0..12)
        }
        // Every cross-rate must fit the quote model before this snapshot can be cached.
        require(ratesToBase.values.max() <= ratesToBase.values.min().multiply(BigDecimal("1000000")))
    }

    fun quote(from: String, to: String): ExchangeRate? {
        val fromRate = ratesToBase[from] ?: return null
        val toRate = ratesToBase[to] ?: return null
        return ExchangeRate(from, to, fromRate.divide(toRate, 12, RoundingMode.HALF_UP), updatedAt, fetchedAt, source)
    }
}

fun interface ExchangeRateClient {
    fun fetch(source: ExchangeRateSource): ExchangeRateTable
}

/** Public official feeds. No account, credentials, or user data are sent. */
class OfficialExchangeRateClient(
    private val transport: (URL) -> String = ::downloadRates,
    private val clock: () -> Long = System::currentTimeMillis,
) : ExchangeRateClient {
    override fun fetch(source: ExchangeRateSource): ExchangeRateTable {
        val response = transport(URL(if (source == ExchangeRateSource.CFETS) CFETS_URL else HKMA_URL))
        return when (source) {
            ExchangeRateSource.CFETS -> parseCfets(response, clock())
            ExchangeRateSource.HKMA -> parseHkma(response, clock())
        }
    }

    companion object {
        const val CFETS_URL = "https://www.chinamoney.com.cn/r/cms/www/chinamoney/data/fx/ccpr.json"
        const val HKMA_URL = "https://api.hkma.gov.hk/public/market-data-and-statistics/monthly-statistical-bulletin/er-ir/er-eeri-daily?pagesize=10&sortby=end_of_day&sortorder=desc"

        fun parseCfets(response: String, fetchedAt: Long): ExchangeRateTable {
            val body = JSONObject(response)
            require(body.getJSONObject("head").getString("rep_code") == "200")
            val date = LocalDateTime.parse(body.getJSONObject("data").getString("lastDate"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd H:mm", Locale.ROOT))
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT))
            val rates = mutableMapOf("CNY" to BigDecimal.ONE)
            val records = body.getJSONArray("records")
            for (index in 0 until records.length()) {
                val row = records.getJSONObject(index)
                val code = when (row.getString("vrtEName")) {
                    "USD/CNY" -> "USD"
                    "HKD/CNY" -> "HKD"
                    else -> continue
                }
                require(code !in rates)
                rates[code] = row.getString("price").toBigDecimal()
            }
            return ExchangeRateTable(ExchangeRateSource.CFETS, date, fetchedAt, rates)
        }

        fun parseHkma(response: String, fetchedAt: Long): ExchangeRateTable {
            val body = JSONObject(response)
            require(body.getJSONObject("header").getBoolean("success"))
            val records = body.getJSONObject("result").getJSONArray("records")
            val tables = (0 until records.length()).mapNotNull { index ->
                try {
                    val row = records.getJSONObject(index)
                    val date = LocalDate.parse(row.getString("end_of_day")).toString()
                    val rates = Money.currencies.associateWith { code ->
                        if (code == "HKD") BigDecimal.ONE else row.getString(code.lowercase(Locale.ROOT))
                            .toBigDecimal().also {
                                require(it > BigDecimal.ZERO && it <= BigDecimal("1000000"))
                                require(it.precision() <= 24 && it.scale() in 0..18)
                            }.setScale(12, RoundingMode.HALF_UP)
                    }
                    ExchangeRateTable(ExchangeRateSource.HKMA, date, fetchedAt, rates)
                } catch (_: Exception) { null }
            }
            return tables.maxByOrNull { it.updatedAt } ?: throw IOException("No complete official exchange rate observation")
        }
    }
}

private fun downloadRates(url: URL): String {
    val connection = (url.openConnection() as HttpURLConnection).apply {
        connectTimeout = 10_000
        readTimeout = 10_000
        instanceFollowRedirects = false
        setRequestProperty("Accept", "application/json")
    }
    try {
        if (connection.responseCode != HttpURLConnection.HTTP_OK) throw IOException("Official exchange rate feed unavailable")
        return connection.inputStream.use { stream ->
            val bytes = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val count = stream.read(buffer)
                if (count < 0) break
                if (bytes.size() + count > 131_072) throw IOException("Exchange rate response too large")
                bytes.write(buffer, 0, count)
            }
            bytes.toString("UTF-8")
        }
    } finally { connection.disconnect() }
}

/** Source snapshots serve every pair offline and are refreshed at most daily on opening. */
class ExchangeRateRepository(
    context: Context,
    private val client: ExchangeRateClient = OfficialExchangeRateClient(),
    private val clock: () -> Long = System::currentTimeMillis,
) {
    // Store complete source snapshots separately from finance records.
    private val preferences = context.getSharedPreferences("official_exchange_rates", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    private fun cachedTable(source: ExchangeRateSource): ExchangeRateTable? = try {
        preferences.getString(source.name, null)?.let { json ->
            val body = JSONObject(json)
            val rates = body.getJSONObject("rates")
            ExchangeRateTable(source, body.getString("updatedAt"), body.getLong("fetchedAt"),
                rates.keys().asSequence().associateWith { rates.getString(it).toBigDecimal() })
        }
    } catch (_: Exception) { null }

    fun cachedRate(from: String, to: String): ExchangeRate? {
        require(from in Money.currencies && to in Money.currencies)
        if (from == to) return ExchangeRate(from, to, BigDecimal.ONE, "", 0)
        return ExchangeRateSource.entries.mapNotNull { cachedTable(it)?.quote(from, to) }
            .maxByOrNull { it.updatedAt.take(10) }
    }

    suspend fun refresh(from: String, to: String, force: Boolean = true): ExchangeRate = withContext(Dispatchers.IO) {
        mutex.withLock {
            require(from in Money.currencies && to in Money.currencies)
            val cached = cachedRate(from, to)
            if (from == to) return@withLock requireNotNull(cached)
            if (!force && cached != null && clock() - cached.fetchedAt in 0 until CACHE_AGE_MS) return@withLock cached
            val sources = if (from == "TWD" || to == "TWD") listOf(ExchangeRateSource.HKMA) else ExchangeRateSource.entries
            for (source in sources) {
                try {
                    val table = client.fetch(source)
                    require(table.source == source && table.quote(from, to) != null)
                    currentCoroutineContext().ensureActive()
                    preferences.edit {
                        val rates = JSONObject()
                        table.ratesToBase.forEach { (code, rate) -> rates.put(code, rate.toPlainString()) }
                        putString(source.name, JSONObject().put("rates", rates).put("updatedAt", table.updatedAt)
                            .put("fetchedAt", table.fetchedAt).toString())
                    }
                    return@withLock requireNotNull(cachedRate(from, to))
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    // A failed primary feed can fall back to the other official source.
                }
            }
            throw IOException("Official exchange rate feeds unavailable")
        }
    }

    fun clearCachedRates() { preferences.edit { clear() } }

    companion object { const val CACHE_AGE_MS = 24 * 60 * 60 * 1000L }
}
