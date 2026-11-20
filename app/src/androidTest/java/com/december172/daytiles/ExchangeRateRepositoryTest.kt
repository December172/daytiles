package com.december172.daytiles

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.december172.daytiles.data.*
import com.december172.daytiles.domain.model.ExchangeRateSource
import java.io.IOException
import java.math.BigDecimal
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExchangeRateRepositoryTest {
    private val context = object : ContextWrapper(InstrumentationRegistry.getInstrumentation().targetContext) {
        override fun getSharedPreferences(name: String, mode: Int) = super.getSharedPreferences("test_$name", mode)
    }
    private val cfets = """{"head":{"rep_code":"200"},"data":{"lastDate":"2026-11-20 9:15"},"records":[{"vrtEName":"USD/CNY","price":"7.25"},{"vrtEName":"HKD/CNY","price":"0.9"},{"vrtEName":"100JPY/CNY","price":"4.1"}]}"""
    private val hkma = """{"header":{"success":true},"result":{"records":[{"end_of_day":"2026-10-31","usd":7.8,"cny":1.1,"twd":0.25}]}}"""
    private val calls = mutableListOf<ExchangeRateSource>()
    private val unavailable = mutableSetOf<ExchangeRateSource>()
    private var now = 1000L
    private val client = ExchangeRateClient { source ->
        calls += source
        if (source in unavailable) throw IOException("Offline")
        if (source == ExchangeRateSource.CFETS) OfficialExchangeRateClient.parseCfets(cfets, now)
        else OfficialExchangeRateClient.parseHkma(hkma, now)
    }
    private fun repository() = ExchangeRateRepository(context, client, clock = { now })

    @Before fun prepare() { repository().clearCachedRates() }
    @After fun clean() = prepare()

    @Test fun cfetsUsesCnyPerForeignUnitAndIgnoresOtherCurrencyConventions() {
        val table = OfficialExchangeRateClient.parseCfets(cfets, now)
        assertEquals("2026-11-20 09:15", table.updatedAt)
        assertEquals(725L, table.quote("USD", "CNY")!!.convertMinor(100))
        assertEquals(138L, table.quote("CNY", "USD")!!.convertMinor(1000))
        assertEquals(806L, table.quote("USD", "HKD")!!.convertMinor(100))
        assertNull(table.quote("TWD", "CNY"))
        assertEquals(ExchangeRateSource.CFETS, table.quote("USD", "CNY")!!.source)
    }

    @Test fun hkmaUsesHkdPerForeignUnitAndOneCompleteObservation() {
        val response = """{"header":{"success":true},"result":{"records":[
            {"end_of_day":"2026-11-02","usd":7.9,"cny":null,"twd":0.26},
            {"end_of_day":"2026-10-30","usd":7.7,"cny":1.0,"twd":0.24},
            {"end_of_day":"2026-10-31","usd":7.8,"cny":1.1,"twd":0.25}]}}"""
        val table = OfficialExchangeRateClient.parseHkma(response, now)
        assertEquals("2026-10-31", table.updatedAt)
        assertEquals(3120L, table.quote("USD", "TWD")!!.convertMinor(100))
        assertEquals(400L, table.quote("HKD", "TWD")!!.convertMinor(100))
        assertEquals(440L, table.quote("CNY", "TWD")!!.convertMinor(100))
        val floatArtifact = OfficialExchangeRateClient.parseHkma(hkma.replace("7.8", "7.8454999999999995"), now)
        assertEquals(0, BigDecimal("7.8455").compareTo(floatArtifact.ratesToBase.getValue("USD")))
    }

    @Test fun invalidOrIncompleteFeedsNeverBecomeQuotes() {
        for (value in listOf("0", "-7", "NaN", "1e100", "0.0000000000001", "1000000", "0.0000001")) {
            assertThrows(Exception::class.java) { OfficialExchangeRateClient.parseCfets(cfets.replace("7.25", value), now) }
        }
        assertThrows(Exception::class.java) { OfficialExchangeRateClient.parseCfets(cfets.replace("HKD/CNY", "CNY/HKD"), now) }
        assertThrows(Exception::class.java) { OfficialExchangeRateClient.parseCfets(cfets.replace("200", "500"), now) }
        assertThrows(Exception::class.java) { OfficialExchangeRateClient.parseHkma(hkma.replace("true", "false"), now) }
        for (value in listOf("null", "0", "-1", "1e-999999")) {
            assertThrows(Exception::class.java) { OfficialExchangeRateClient.parseHkma(hkma.replace("0.25", value), now) }
        }
        assertThrows(Exception::class.java) { OfficialExchangeRateClient.parseCfets("<html>Unavailable</html>", now) }
    }

    @Test fun officialRequestsUseFixedHttpsUrlsWithoutUserAmountsOrCredentials() {
        val client = OfficialExchangeRateClient(transport = { url ->
            assertEquals("https", url.protocol)
            assertFalse(url.toString().contains("key", ignoreCase = true))
            assertFalse(url.toString().contains("amount", ignoreCase = true))
            if (url.host == "www.chinamoney.com.cn") {
                assertEquals(OfficialExchangeRateClient.CFETS_URL, url.toString()); cfets
            } else {
                assertEquals(OfficialExchangeRateClient.HKMA_URL, url.toString()); hkma
            }
        }, clock = { now })
        assertEquals(now, client.fetch(ExchangeRateSource.CFETS).fetchedAt)
        assertEquals(now, client.fetch(ExchangeRateSource.HKMA).fetchedAt)
    }

    @Test fun firstUseNeedsNoConfigurationAndSnapshotServesAllSupportedPairs() = runBlocking<Unit> {
        val repo = repository()
        val quote = repo.refresh("USD", "CNY", force = false)
        assertEquals(listOf(ExchangeRateSource.CFETS), calls)
        assertEquals(quote, repository().cachedRate("USD", "CNY"))
        assertEquals(138L, repo.refresh("CNY", "USD", force = false).convertMinor(1000))
        assertNotNull(repo.refresh("HKD", "USD", force = false))
        assertEquals(1, calls.size)
        assertNull(repo.cachedRate("TWD", "CNY"))
    }

    @Test fun twdUsesHkmaDirectlyAndDoesNotMixTheCfetsObservation() = runBlocking<Unit> {
        val repo = repository()
        repo.refresh("USD", "CNY")
        val quote = repo.refresh("USD", "TWD", force = false)
        assertEquals(listOf(ExchangeRateSource.CFETS, ExchangeRateSource.HKMA), calls)
        assertEquals(ExchangeRateSource.HKMA, quote.source)
        assertEquals("2026-10-31", quote.updatedAt)
        assertEquals(3120L, quote.convertMinor(100))
        assertEquals(725L, repo.cachedRate("USD", "CNY")!!.convertMinor(100))
        assertEquals(440L, repo.cachedRate("CNY", "TWD")!!.convertMinor(100))
    }

    @Test fun primaryFailureFallsBackToOfficialHkmaFeed() = runBlocking<Unit> {
        unavailable += ExchangeRateSource.CFETS
        val quote = repository().refresh("USD", "CNY", force = false)
        assertEquals(listOf(ExchangeRateSource.CFETS, ExchangeRateSource.HKMA), calls)
        assertEquals(ExchangeRateSource.HKMA, quote.source)
        repository().refresh("HKD", "USD", force = false)
        assertEquals(2, calls.size)
    }

    @Test fun automaticRefreshIsLimitedToDailyButManualRefreshBypassesCache() = runBlocking<Unit> {
        val repo = repository()
        repo.refresh("CNY", "USD", force = false)
        now += ExchangeRateRepository.CACHE_AGE_MS - 1
        repo.refresh("HKD", "CNY", force = false)
        assertEquals(1, calls.size)
        now++
        repo.refresh("CNY", "USD", force = false)
        assertEquals(2, calls.size)
        repo.refresh("CNY", "USD")
        assertEquals(3, calls.size)
    }

    @Test fun networkFailurePreservesCacheAndClearRemovesEveryPair() = runBlocking<Unit> {
        val repo = repository()
        val quote = repo.refresh("CNY", "USD")
        unavailable += ExchangeRateSource.entries
        try { repo.refresh("CNY", "USD"); fail("Refresh should fail") } catch (_: IOException) { }
        assertEquals(quote, repository().cachedRate("CNY", "USD"))
        assertNotNull(repository().cachedRate("USD", "HKD"))
        repo.clearCachedRates()
        assertNull(repository().cachedRate("CNY", "USD"))
        assertNull(repository().cachedRate("USD", "HKD"))
    }

    @Test fun sameCurrencyNeedsNoNetworkAndCorruptCacheIsIgnored() = runBlocking<Unit> {
        assertEquals(123L, repository().refresh("CNY", "CNY").convertMinor(123))
        assertTrue(calls.isEmpty())
        context.getSharedPreferences("official_exchange_rates", Context.MODE_PRIVATE).edit()
            .putString("CFETS", "not json").commit()
        assertNull(repository().cachedRate("CNY", "USD"))
    }
}
