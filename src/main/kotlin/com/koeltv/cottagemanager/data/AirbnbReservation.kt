package com.koeltv.cottagemanager.data

import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AirbnbReservation(
    val confirmationCode: String,
    val status: String,
    val name: String, //
    val phoneNumber: String,
    val adultCount: Int,
    val childCount: Int,
    val babyCount: Int,
    val arrivalDate: LocalDate,
    val departureDate: LocalDate,
    val nightCount: Int,
    val reservationDate: LocalDate,
    val cottage: String,
    val price: Int, // 123.45€ --> 12345
) {
    enum class Status(val text: String) {
        FORMER_TRAVELER("Ancien voyageur"),
        CANCELED("Annulée par le voyageur"),
        CONFIRMED("Confirmée"),
        ONGOING("Séjour en cours");

        fun match(status: String): Boolean {
            return text == status
        }
    }

    companion object {
        private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

        fun fromCsv(inputStream: InputStream): List<AirbnbReservation> {
            val reader = inputStream.bufferedReader()
            reader.readLine() // Ignore header line
            return reader.lineSequence()
                .filter { it.isNotBlank() }
                .map { line ->
                    val data = line
                        .split(',', ignoreCase = false)
                        .map { it.trim().removeSurrounding("\"") }

                    AirbnbReservation(
                        confirmationCode = data[0],
                        status = data[1],
                        name = data[2],
                        phoneNumber = data[3].replace(Regex("[ -]"), ""),
                        adultCount = data[4].toInt(),
                        childCount = data[5].toInt(),
                        babyCount = data[6].toInt(),
                        arrivalDate = LocalDate.parse(data[7], dateFormatter),
                        departureDate = LocalDate.parse(data[8], dateFormatter),
                        nightCount = data[9].toInt(),
                        reservationDate = LocalDate.parse(data[10]),
                        cottage = data[11],
                        price = (data[12].removePrefix("\"") + data[13]).replace(Regex("[^0-9]"), "").toInt()
                    )
                }.toList()
        }
    }
}
