package com.example.commuterx_java.models

data class TripReview(
    val userId: String = "",
    val rating: Float = 0f,
    val feedback: String = "",
    val transportType: String = "",
    val origin: String = "",
    val destination: String = "",
    val timestamp: Long = System.currentTimeMillis()
)