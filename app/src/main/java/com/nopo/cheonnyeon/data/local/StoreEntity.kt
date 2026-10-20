package com.nopo.cheonnyeon.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Entity(tableName = "stores", indices = [Index("lat", "lng"), Index("sido", "sigungu")])
data class StoreEntity(
    @PrimaryKey val id: String,
    val name: String,
    val aliases: List<String>,
    val programs: List<String>,
    val categoryGroup: String,
    val isFood: Boolean,
    val sido: String,
    val sigungu: String,
    val address: String,
    val phone: String?,
    val foundedYear: Int?,
    val foundedDate: String?,
    val lat: Double?,
    val lng: Double?,
    val menuItems: List<String>,
    val hours: String?,
    val amenities: List<String>,
    val summary: String?,
    val generation: Int?,
    val sourceUrl: String?,
    val photoUrls: List<String>,
    val isTest: Boolean = false,
    val story: String? = null,
) {
    fun toDomain() = Store(
        id, name, aliases, programs, categoryGroup, isFood, sido, sigungu, address, phone, foundedYear, foundedDate,
        lat, lng, menuItems, hours, amenities, summary, generation, sourceUrl, photoUrls, isTest, story,
    )

    companion object {
        fun from(s: Store) = StoreEntity(
            s.id, s.name, s.aliases, s.programs, s.categoryGroup, s.isFood, s.sido, s.sigungu, s.address, s.phone,
            s.foundedYear, s.foundedDate, s.lat, s.lng, s.menuItems, s.hours, s.amenities, s.summary, s.generation,
            s.sourceUrl, s.photoUrls, s.isTest, s.story,
        )
    }
}

/** 목록 열은 JSON 문자열로 저장한다 */
class StringListConverter {
    private val serializer = ListSerializer(String.serializer())

    @TypeConverter fun toDb(list: List<String>): String = Json.encodeToString(serializer, list)

    @TypeConverter fun fromDb(raw: String): List<String> = Json.decodeFromString(serializer, raw)
}
