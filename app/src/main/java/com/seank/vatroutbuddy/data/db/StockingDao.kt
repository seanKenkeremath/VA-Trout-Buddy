package com.seank.vatroutbuddy.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface StockingDao {

    @Query("SELECT MAX(date) FROM stockings")
    suspend fun getMostRecentStockingDate(): LocalDate?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertStockings(stockings: List<StockingEntity>): List<Long>

    @Query("SELECT * FROM stockings WHERE id IN (:ids)")
    suspend fun getStockingsById(ids: List<Long>): List<StockingEntity>

    @Transaction
    suspend fun insertAndReturnStockings(entities: List<StockingEntity>): List<StockingEntity> {
        val ids = insertStockings(entities)
        return getStockingsById(ids)
    }

    @Query("SELECT MIN(date) FROM stockings")
    suspend fun getEarliestStockingDate(): LocalDate?

    @Query("SELECT * FROM stockings WHERE waterbody = :waterbody ORDER BY date DESC LIMIT :limit")
    suspend fun getStockingsByWaterbody(waterbody: String, limit: Int): List<StockingEntity>

    @Query("""
    SELECT * FROM stockings
    WHERE (:counties IS NULL OR ',' || :counties || ',' LIKE '%,' || county || ',%')
    AND (:isNationalForest IS NULL OR isNationalForest = :isNationalForest)
    AND (:isHeritageDayWater IS NULL OR isHeritageDayWater = :isHeritageDayWater)
    AND (:isNsf IS NULL OR isNsf = :isNsf)
    AND (:isDelayedHarvest IS NULL OR isDelayedHarvest = :isDelayedHarvest)
    ORDER BY date DESC, waterbody, id
    LIMIT :limit
""")
    suspend fun getMostRecentStockings(
        counties: List<String>? = null,
        isNationalForest: Boolean? = null,
        isHeritageDayWater: Boolean? = null,
        isNsf: Boolean? = null,
        isDelayedHarvest: Boolean? = null,
        limit: Int
    ): List<StockingEntity>

    @Query("""
    SELECT * FROM stockings
    WHERE (:counties IS NULL OR (',' || :counties || ',' LIKE '%,' || county || ',%'))
    AND (:isNationalForest IS NULL OR isNationalForest = :isNationalForest)
    AND (:isHeritageDayWater IS NULL OR isHeritageDayWater = :isHeritageDayWater)
    AND (:isNsf IS NULL OR isNsf = :isNsf)
    AND (:isDelayedHarvest IS NULL OR isDelayedHarvest = :isDelayedHarvest)
    AND (
        (date < :lastDate)
        OR (date = :lastDate AND waterbody > :lastWaterbody)
        OR (date = :lastDate AND waterbody = :lastWaterbody AND id > :lastId)
    )
    ORDER BY date DESC, waterbody, id
    LIMIT :pageSize
""")
    suspend fun getStockingsPaged(
        lastDate: LocalDate,
        lastWaterbody: String,
        lastId: Long,
        counties: List<String>? = null,
        isNationalForest: Boolean? = null,
        isHeritageDayWater: Boolean? = null,
        isNsf: Boolean? = null,
        isDelayedHarvest: Boolean? = null,
        pageSize: Int
    ): List<StockingEntity>

    @Query("SELECT DISTINCT county FROM stockings ORDER BY county")
    suspend fun getAllCounties(): List<String>

    @Query("SELECT DISTINCT category FROM stockings ORDER BY category")
    suspend fun getAllCategories(): List<String>
} 