package com.example.cemeterylocator.model

import com.google.gson.annotations.SerializedName

data class User(
    @SerializedName("userid") val userId: Int,
    @SerializedName("email") val email: String,
    @SerializedName("full_name") val fullName: String
)

data class AuthResponse(
    @SerializedName("token") val token: String,
    @SerializedName("user") val user: User
)

data class SubmitGraveResponse(
    @SerializedName("requestid") val requestId: Int,
    @SerializedName("status") val status: String,
    @SerializedName("message") val message: String
)

data class GraveRequest(
    @SerializedName("requestid") val requestId: Int,
    @SerializedName("name") val name: String,
    @SerializedName("sex") val sex: String?,
    @SerializedName("borndate") val bornDate: String?,
    @SerializedName("dieddate") val diedDate: String?,
    @SerializedName("location") val location: String?,
    @SerializedName("lat") val lat: Double,
    @SerializedName("lng") val lng: Double,
    @SerializedName("notes") val notes: String?,
    @SerializedName("status") val status: String, // pending | approved | rejected
    @SerializedName("admin_note") val adminNote: String?,
    @SerializedName("resulting_graveno") val resultingGraveNo: String?,
    @SerializedName("submitted_at") val submittedAt: String?,
    @SerializedName("reviewed_at") val reviewedAt: String?
)

data class MySubmissionsResponse(
    @SerializedName("submissions") val submissions: List<GraveRequest>
)

data class ApiError(
    @SerializedName("error") val error: String
)
