package com.example.cemeterylocator.model

import com.google.gson.annotations.SerializedName

data class GraveRecord(
    @SerializedName("graveno") val graveNo: String,
    @SerializedName("name") val name: String,
    @SerializedName("sex") val sex: String?,
    @SerializedName("borndate") val bornDate: String?,
    @SerializedName("dieddate") val diedDate: String?,
    @SerializedName("age") val age: Int? = null,
    @SerializedName("location") val location: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lng") val lng: Double?,
    @SerializedName("categories") val categories: String?,
    @SerializedName("photo_url") val photoUrl: String?,
    @SerializedName("gravepic_url") val gravePicUrl: String?,
    @SerializedName("memorabilia") val memorabilia: String?
)

data class SearchResponse(
    @SerializedName("results") val results: List<GraveRecord>
)

/** Lightweight shape used by api/graves_geo.php - just enough to place a map pin. */
data class GeoGrave(
    @SerializedName("graveno") val graveNo: String,
    @SerializedName("name") val name: String,
    @SerializedName("location") val location: String?,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double
)

data class GeoGravesResponse(
    @SerializedName("graves") val graves: List<GeoGrave>
)
