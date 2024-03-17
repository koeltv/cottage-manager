package com.koeltv.cottagemanager.data

import com.koeltv.cottagemanager.Reservation
import com.koeltv.cottagemanager.toPlusNote
import com.koeltv.cottagemanager.toPriceString
import java.time.LocalDate

data class ReservationView(
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val name: String,
    val repartition: String,
    val nationality: String,
    val price: String,
    val note: String,
    val code: String,
    val comments: String,
)

fun Reservation.toView(): ReservationView {
    return ReservationView(
        arrivalDate,
        departureDate,
        client.name,
        "${adultCount}A, ${childCount}E, ${babyCount}BB",
        client.nationality ?: "",
        price.toPriceString(),
        note?.toPlusNote() ?: "",
        confirmationCode,
        comments ?: ""
    )
}
