package com.a2411520007.mahasiswa.api

import com.a2411520007.mahasiswa.model.MahasiswaDetailResponse
import com.a2411520007.mahasiswa.model.MahasiswaPostResponse
import com.a2411520007.mahasiswa.model.MahasiswaResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query

interface ApiInterface {
    @GET("mahasiswa.php")
    suspend fun getDaftarMahasiswa(): Response<MahasiswaResponse>

    @GET("mahasiswa.php")
    suspend fun searchMahasiswa(
        @Query("search") keyword: String
    ): Response<MahasiswaResponse>

    @GET("mahasiswa.php")
    suspend fun getDetailMahasiswa(
        @Query("id") id: String
    ): Response<MahasiswaDetailResponse>

    @POST("mahasiswa.php")
    suspend fun addMahasiswa(
        @Body body: Map<String, String>
    ): Response<MahasiswaPostResponse>

    @PUT("mahasiswa.php")
    suspend fun updateMahasiswa(
        @Query("id") id: String,
        @Body body: Map<String, String>
    ): Response<MahasiswaPostResponse>

    @DELETE("mahasiswa.php")
    suspend fun deleteMahasiswa(
        @Query("id") id: String
    ): Response<MahasiswaPostResponse>
}