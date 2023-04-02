package com.example.customcamera

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface AuthService {
    @POST("api/v1/auth/sign_in")
    @Headers(
        "Content-type:application/json"
    )
    fun signIn(@Body signInRequest: SignInRequest): Call<SignInResponse>
}

data class SignInRequest(val email: String, val password: String)

data class SignInResponse(
    val uid: String,
    val client: String,
    val accessToken: String
)
