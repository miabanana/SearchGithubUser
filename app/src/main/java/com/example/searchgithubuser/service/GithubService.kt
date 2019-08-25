package com.example.searchgithubuser.service

import com.example.searchgithubuser.model.SearchUserResponse
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface GithubService {
    @GET("search/users")
    fun searchUsers(@Query("q") userName: String,
                    @Query("page") page: Int,
                    @Query("per_page") perPage: Int): Call<SearchUserResponse>
}

class GithubServiceManager private constructor() {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.github.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    companion object {
        val instance : GithubService = GithubServiceManager().retrofit.create(GithubService::class.java)
    }

}