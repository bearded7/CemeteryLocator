package com.example.cemeterylocator.api

import com.example.cemeterylocator.model.AuthResponse
import com.example.cemeterylocator.model.GeoGravesResponse
import com.example.cemeterylocator.model.GraveRecord
import com.example.cemeterylocator.model.MySubmissionsResponse
import com.example.cemeterylocator.model.SearchResponse
import com.example.cemeterylocator.model.SubmitGraveResponse
import retrofit2.Response
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface CemeteryApi {

    /** GET /api/search.php?q=... */
    @GET("api/search.php")
    suspend fun search(@Query("q") query: String): Response<SearchResponse>

    /** GET /api/person.php?graveno=...&location=... */
    @GET("api/person.php")
    suspend fun getPerson(
        @Query("graveno") graveNo: String,
        @Query("location") location: String? = null
    ): Response<GraveRecord>

    /** GET /api/graves_geo.php?location=... (location optional - omit for the whole cemetery) */
    @GET("api/graves_geo.php")
    suspend fun getGeoGraves(@Query("location") location: String? = null): Response<GeoGravesResponse>

    /** POST /api/register.php */
    @FormUrlEncoded
    @POST("api/register.php")
    suspend fun register(
        @Field("email") email: String,
        @Field("password") password: String,
        @Field("full_name") fullName: String
    ): Response<AuthResponse>

    /** POST /api/login.php */
    @FormUrlEncoded
    @POST("api/login.php")
    suspend fun login(
        @Field("email") email: String,
        @Field("password") password: String
    ): Response<AuthResponse>

    /** POST /api/submit_grave.php - requires auth (attached automatically by ApiClient) */
    @FormUrlEncoded
    @POST("api/submit_grave.php")
    suspend fun submitGrave(
        @Field("name") name: String,
        @Field("lat") lat: Double,
        @Field("lng") lng: Double,
        @Field("sex") sex: String? = null,
        @Field("borndate") bornDate: String? = null,
        @Field("dieddate") diedDate: String? = null,
        @Field("location") location: String? = null,
        @Field("notes") notes: String? = null
    ): Response<SubmitGraveResponse>

    /** GET /api/my_submissions.php - requires auth (attached automatically by ApiClient) */
    @GET("api/my_submissions.php")
    suspend fun mySubmissions(): Response<MySubmissionsResponse>
}
