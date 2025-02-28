package com.seank.vatroutbuddy.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.seank.vatroutbuddy.domain.model.StockingInfo
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "stockings",
    indices = [Index(value = ["waterbody", "date"], unique = true)]
)
data class StockingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: LocalDate,
    val county: String,
    val waterbody: String,
    val category: String,
    val species: List<String>,
    val isNationalForest: Boolean,
    val lastUpdated: LocalDateTime
) {
    fun toStockingInfo() = StockingInfo(
        date = date,
        county = county,
        waterbody = waterbody,
        category = category,
        species = species,
        isNationalForest = isNationalForest
    )

    companion object {
        fun fromStockingInfo(
            info: StockingInfo,
            lastUpdated: LocalDateTime = LocalDateTime.now()
        ) = StockingEntity(
            date = info.date,
            county = info.county,
            waterbody = info.waterbody,
            category = info.category,
            species = info.species,
            isNationalForest = info.isNationalForest,
            lastUpdated = lastUpdated
        )
    }
} 