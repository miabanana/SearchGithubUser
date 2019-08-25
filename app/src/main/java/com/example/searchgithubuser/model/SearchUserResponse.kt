package com.example.searchgithubuser.model

import com.google.gson.annotations.SerializedName

data class SearchUserResponse (
    @SerializedName("total_count") val totalCount: Int = 0,
    @SerializedName("incomplete_results") val isIncomplete: Boolean = false,
    @SerializedName("items") val users: List<User>)

data class User (
    @SerializedName("login") val account: String,
    val id: Int,
    @SerializedName("avatar_url") val avatar: String
)