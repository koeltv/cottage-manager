package com.koeltv.cottagemanager.data

data class ClientWithStats(
    val name: String,
    val phoneNumber: String,
    val nationality: String,
    val averageNote: Int?,
    val reservationCount: Int,
)