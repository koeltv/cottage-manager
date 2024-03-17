package com.koeltv.cottagemanager.data

import com.koeltv.cottagemanager.Client
import com.koeltv.cottagemanager.Reservation
import com.koeltv.cottagemanager.Reservations
import com.koeltv.cottagemanager.toPlusNote

data class ClientView(
    val name: String,
    val phoneNumber: String,
    val nationality: String,
    val averageNote: String,
    val reservationCount: Int,
    val comments: String,
)

fun Client.toView(): ClientView {
    val notes = Reservation
        .find { Reservations.client eq (this@toView).id }
        .mapNotNull { it.note?.toInt() }

    return ClientView(
        name = name,
        phoneNumber = phoneNumber ?: "",
        nationality = nationality ?: "",
        averageNote = notes.average().toInt().toUByte().toPlusNote(),
        reservationCount = notes.count(),
        comments = ""
    )
}