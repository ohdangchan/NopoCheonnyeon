package com.nopo.cheonnyeon.data.catalog

import com.nopo.cheonnyeon.domain.model.Store
import kotlinx.serialization.Serializable

/** assets/stores.json 최상위 */
@Serializable
data class CatalogJson(val version: Long, val stores: List<StoreJson>)

/** stores.json의 가게 한 곳. 알 수 없는 필드는 무시하고, 없는 필드는 기본값을 쓴다. */
@Serializable
data class StoreJson(
    val id: String,
    val name: String,
    val aliases: List<String> = emptyList(),
    val programs: List<String> = emptyList(),
    val categoryGroup: String = "",
    val isFood: Boolean = true,
    val sido: String = "",
    val sigungu: String = "",
    val address: String? = null,
    val phone: String? = null,
    val foundedYear: Int? = null,
    val foundedDate: String? = null,
    val lat: Double? = null,
    val lng: Double? = null,
    val menuItems: List<String> = emptyList(),
    val hours: String? = null,
    val amenities: List<String> = emptyList(),
    val summary: String? = null,
    val generation: Int? = null,
    val sourceUrl: String? = null,
    val photoUrls: List<String> = emptyList(),
    val story: String? = null,
) {
    fun toDomain() = Store(
        id = id, name = name, aliases = aliases, programs = programs, categoryGroup = categoryGroup, isFood = isFood,
        sido = sido, sigungu = sigungu, address = address.orEmpty(), phone = phone, foundedYear = foundedYear,
        foundedDate = foundedDate, lat = lat, lng = lng, menuItems = menuItems, hours = hours, amenities = amenities,
        summary = summary, generation = generation, sourceUrl = sourceUrl, photoUrls = photoUrls, story = story,
    )
}
