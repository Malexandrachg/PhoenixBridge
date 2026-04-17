package com.hansa.phoenixbridge.api

import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

// Modelo de datos a enviar
data class PhoenixPayload(
    val orden: String,
    val ot: String,
    val nodo: String,
    val estado: String,
    val nombre: String
)

data class PhoenixResponse(
    val status: String,
    val message: String?
)

interface GoogleSheetsApi {
    @POST("macros/s/AKfycbyy5yvuYAs1L0D9OdNw3NOCQxrWnEyv9QB7btA0G7pndgrrfhArkQcsU3E2d0kwZNWrkg/exec")
    fun sendData(@Body payload: PhoenixPayload): Call<PhoenixResponse>
}

object RetrofitClient {
    private const val BASE_URL = "https://script.google.com/"

    val instance: GoogleSheetsApi by lazy {
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        retrofit.create(GoogleSheetsApi::class.java)
    }
}
