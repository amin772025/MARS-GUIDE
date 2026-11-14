package com.marsguide.vzd.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

@JsonClass(generateAdapter = true)
data class NasaImageSearchResponse(
    val collection: NasaCollection?
)

@JsonClass(generateAdapter = true)
data class NasaCollection(
    val version: String?,
    val href: String?,
    val items: List<NasaItem>?
)

@JsonClass(generateAdapter = true)
data class NasaItem(
    val href: String?,
    val data: List<NasaItemData>?,
    val links: List<NasaItemLink>?
)

@JsonClass(generateAdapter = true)
data class NasaItemData(
    @param:Json(name = "nasa_id") val nasaId: String?,
    val title: String?,
    val description: String?,
    val center: String?,
    @param:Json(name = "date_created") val dateCreated: String?
)

@JsonClass(generateAdapter = true)
data class NasaItemLink(
    val href: String?,
    val rel: String?,
    val render: String?
)

@JsonClass(generateAdapter = true)
data class MarsLatestPhotosResponse(
    @param:Json(name = "latest_photos") val latestPhotos: List<MarsPhotoItem>
)

@JsonClass(generateAdapter = true)
data class MarsPhotoItem(
    val id: Long,
    val sol: Int,
    val camera: MarsCamera,
    @param:Json(name = "img_src") val imgSrc: String,
    @param:Json(name = "earth_date") val earthDate: String,
    val rover: MarsRoverInfo
)

@JsonClass(generateAdapter = true)
data class MarsCamera(
    val id: Long,
    val name: String,
    @param:Json(name = "full_name") val fullName: String
)

@JsonClass(generateAdapter = true)
data class MarsRoverInfo(
    val id: Long,
    val name: String,
    @param:Json(name = "landing_date") val landingDate: String,
    @param:Json(name = "launch_date") val launchDate: String,
    val status: String
)

interface NasaApiService {
    @GET
    suspend fun searchImages(
        @Url url: String = "https://images-api.nasa.gov/search",
        @Query("q") query: String,
        @Query("media_type") mediaType: String = "image"
    ): NasaImageSearchResponse

    @GET
    suspend fun getPerseveranceLatestPhotos(
        @Url url: String = "https://api.nasa.gov/mars-photos/api/v1/rovers/perseverance/latest_photos",
        @Query("api_key") apiKey: String = com.marsguide.vzd.BuildConfig.NASA_API_KEY
    ): MarsLatestPhotosResponse

    companion object {
        fun create(): NasaApiService {
            val client = OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(12, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
                .build()

            return Retrofit.Builder()
                .baseUrl("https://images-api.nasa.gov/")
                .addConverterFactory(MoshiConverterFactory.create())
                .client(client)
                .build()
                .create(NasaApiService::class.java)
        }

        val instance: NasaApiService by lazy {
            create()
        }
    }
}
