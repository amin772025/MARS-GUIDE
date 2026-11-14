package com.marsguide.vzd.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class NasaImageSearchResponse(
    @param:Json(name = "collection") val collection: NasaCollection?
)

@JsonClass(generateAdapter = true)
data class NasaCollection(
    @param:Json(name = "items") val items: List<NasaItem>?
)

@JsonClass(generateAdapter = true)
data class NasaItem(
    @param:Json(name = "data") val data: List<NasaItemData>?,
    @param:Json(name = "links") val links: List<NasaItemLink>?
)

@JsonClass(generateAdapter = true)
data class NasaItemData(
    @param:Json(name = "nasa_id") val nasaId: String?,
    @param:Json(name = "title") val title: String?,
    @param:Json(name = "description") val description: String?,
    @param:Json(name = "date_created") val dateCreated: String?,
    @param:Json(name = "center") val center: String?,
    @param:Json(name = "keywords") val keywords: List<String>?
)

@JsonClass(generateAdapter = true)
data class NasaItemLink(
    @param:Json(name = "href") val href: String?,
    @param:Json(name = "rel") val rel: String?,
    @param:Json(name = "render") val render: String?
)
