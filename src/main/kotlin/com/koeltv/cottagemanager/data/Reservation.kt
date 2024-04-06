package com.koeltv.cottagemanager.data

import java.time.LocalDate

data class Reservation(
    val code: String,
    val client: Client,
    val adultCount: Int,
    val childCount: Int,
    val babyCount: Int,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val reservationDate: LocalDate?,
    val cottage: Cottage,
    val price: Int,
    val note: Int?,
    val comments: String,
)