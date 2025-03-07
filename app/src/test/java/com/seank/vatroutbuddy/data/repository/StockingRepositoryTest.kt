package com.seank.vatroutbuddy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.seank.vatroutbuddy.AppConfig
import com.seank.vatroutbuddy.data.db.AppDatabase
import com.seank.vatroutbuddy.data.db.StockingDao
import com.seank.vatroutbuddy.data.db.StockingEntity
import com.seank.vatroutbuddy.data.network.StockingNetworkDataSource
import com.seank.vatroutbuddy.data.preferences.AppPreferences
import com.seank.vatroutbuddy.util.TestFactory
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.Month

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class StockingRepositoryTest {
    private lateinit var repository: StockingRepository
    private lateinit var networkDataSource: StockingNetworkDataSource
    private lateinit var stockingDao: StockingDao
    private lateinit var preferences: AppPreferences
    private lateinit var datastore: DataStore<Preferences>
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher + Job())

    @Before
    fun setup() {
        networkDataSource = mockk()
        stockingDao = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build().stockingDao()
        datastore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { context.preferencesDataStoreFile("Test_Settings") }
        )
        preferences = AppPreferences(datastore)
        repository = StockingRepository(
            networkDataSource,
            stockingDao,
            preferences,
            testDispatcher
        )
    }

    private suspend fun populateDatabase() {
        val today = LocalDate.of(2025, 3, 4)
        val stockings = mutableListOf<StockingEntity>()
        for (i in 0L..10L) {
            stockings.add(
                StockingEntity(
                    id = i,
                    date = LocalDate.now(),
                    county = "Test County $i",
                    waterbody = "Test Waterbody $i",
                    category = "A",
                    species = listOf("Rainbow Trout"),
                    isNationalForest = false,
                    lastUpdated = today.minusDays(i / 2).atStartOfDay(),
                    isNsf = false,
                    isHeritageDayWater = false,
                    isDelayedHarvest = false,
                )
            )
        }
        stockingDao.insertStockings(
            stockings
        )
    }

    @Test
    fun `refreshSinceLastStocking uses most recent stocking date minus one day when available`() =
        runTest {
            populateDatabase()
            val lastStockingDate = stockingDao.getMostRecentStockingDate()
            val expectedStartDate = lastStockingDate!!.minusDays(1)

            coEvery { networkDataSource.fetchStockings(expectedStartDate) } returns Result.success(
                emptyList()
            )

            repository.fetchLatestStockings()

            coVerify { networkDataSource.fetchStockings(expectedStartDate) }
        }

    @Test
    fun `refreshSinceLastStocking uses default months past when no stockings exist`() = runTest {
        val expectedStartDate = LocalDate.now().minusMonths(AppConfig.DEFAULT_MONTHS_PAST)
        assertEquals(null, stockingDao.getMostRecentStockingDate())

        coEvery { networkDataSource.fetchStockings(expectedStartDate) } returns Result.success(
            emptyList()
        )

        repository.fetchLatestStockings()

        coVerify { networkDataSource.fetchStockings(expectedStartDate) }
    }

    @Test
    fun `refreshSinceLastStocking returns failure when network call fails`() = runTest {
        val expectedStartDate = LocalDate.now().minusMonths(AppConfig.DEFAULT_MONTHS_PAST)
        val expectedException = Exception("Network error")

        coEvery { networkDataSource.fetchStockings(expectedStartDate) } returns Result.failure(
            expectedException
        )

        val result = repository.fetchLatestStockings()

        assert(result.isFailure)
        assertEquals(expectedException, result.exceptionOrNull())
    }

    @Test
    fun `refreshSinceDate updates database with fetched stockings`() = runTest {
        val startDate = LocalDate.now().minusDays(5)

        coEvery { networkDataSource.fetchStockings(startDate) } returns Result.success(
            listOf(
                TestFactory.createStockingInfo(
                    id = 1,
                    date = LocalDate.now(),
                    waterbody = "New Waterbody"
                )
            )
        )

        repository.fetchStockingsInDateRange(startDate)

        val savedStockings = stockingDao.getMostRecentStockings(2)
        assertEquals(1, savedStockings.size)
        assertEquals("New Waterbody", savedStockings[0].waterbody)
    }

    @Test
    fun `fetchHistoricalData fetches from Oct 2018 to earliest stocking date`() = runTest {
        populateDatabase()
        val earliestDate = stockingDao.getEarliestStockingDate()
        val expectedStartDate = LocalDate.of(2018, Month.OCTOBER, 1)
        val stockings = listOf(
            TestFactory.createStockingInfo(
                id = 1,
                date = LocalDate.of(2018, 10, 2),
                waterbody = "Historical Waterbody"
            )
        )

        coEvery { networkDataSource.fetchStockings(any(), any()) } returns Result.success(stockings)

        val existingStockings = stockingDao.getMostRecentStockings(15)
        assertEquals(10, existingStockings.size)
        assertEquals(false, repository.hasHistoricalData.first())

        repository.fetchHistoricalData()

        val updatedStockings = stockingDao.getMostRecentStockings(15)
        assertEquals(11, updatedStockings.size)
        assertEquals("Historical Waterbody", updatedStockings[10].waterbody)

        coVerify {
            networkDataSource.fetchStockings(
                startDate = expectedStartDate,
                endDate = earliestDate
            )
        }
        assertEquals(true, repository.hasHistoricalData.first())
        assertEquals(
            LocalDate.of(2018, 10, 2),
            stockingDao.getEarliestStockingDate()
        )
    }

    @Test
    fun `fetchHistoricalData uses current date when no stockings exist`() = runTest {
        val today = LocalDate.now()
        val expectedStartDate = LocalDate.of(2018, Month.OCTOBER, 1)
        val stockings = listOf(
            TestFactory.createStockingInfo(
                id = 1,
                date = today,
                waterbody = "Historical Waterbody"
            )
        )

        assertEquals(null, stockingDao.getEarliestStockingDate())
        assertEquals(0, stockingDao.getMostRecentStockings(1).size)
        assertEquals(false, repository.hasHistoricalData.first())
        coEvery { networkDataSource.fetchStockings(any(), any()) } returns Result.success(stockings)

        repository.fetchHistoricalData()

        coVerify {
            networkDataSource.fetchStockings(
                startDate = expectedStartDate,
                endDate = today
            )
        }
        assertEquals(true, repository.hasHistoricalData.first())
        assertEquals(1, stockingDao.getMostRecentStockings(2).size)
    }

    @Test
    fun `fetchHistoricalData sets preference flag on success`() = runTest {
        coEvery {
            networkDataSource.fetchStockings(
                any(),
                any()
            )
        } returns Result.success(emptyList())
        assertEquals(false, repository.hasHistoricalData.first())
        repository.fetchHistoricalData()
        assertEquals(true, repository.hasHistoricalData.first())
    }

    @Test
    fun `fetchHistoricalData does not set preference flag on failure`() = runTest {
        coEvery {
            networkDataSource.fetchStockings(
                any(),
                any()
            )
        } returns Result.failure(Exception())

        assertEquals(false, repository.hasHistoricalData.first())

        repository.fetchHistoricalData()

        assertEquals(false, repository.hasHistoricalData.first())
    }

    @After
    fun tearDown() = runTest {
        datastore.edit { it.clear() }
    }
} 