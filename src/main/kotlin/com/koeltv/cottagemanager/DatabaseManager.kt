package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.data.AirbnbReservation
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import java.io.File

object DatabaseManager {
    fun init() {
        File("./db").mkdirs()

        Database.connect(
            url = "jdbc:sqlite:./db/testDb.db",
            user = "root",
            password = "pass"
        )

        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Cottages, Clients, Reservations)
        }
    }

    fun importAirbnbReservation(reservation: AirbnbReservation) {
        transaction {
            Cottages.insertIgnore {
                it[name] = reservation.cottage
                it[alias] = reservation.cottage
            }
            Client.findById(reservation.name)?.let {
                if (reservation.phoneNumber.isNotBlank()) {
                    it.phoneNumber = reservation.phoneNumber
                }
            } ?: Client.new(reservation.name) {
                phoneNumber = reservation.phoneNumber
            }
            Reservations.insertIgnore {
                it[confirmationCode] = reservation.confirmationCode
                it[status] = reservation.status
                it[client] = Clients.select(Clients.name).where { Clients.name eq reservation.name }
                it[adultCount] = reservation.adultCount.toUByte()
                it[childCount] = reservation.childCount.toUByte()
                it[babyCount] = reservation.babyCount.toUByte()
                it[arrivalDate] = reservation.arrivalDate
                it[departureDate] = reservation.departureDate
                it[nightCount] = reservation.nightCount.toUShort()
                it[reservationDate] = reservation.reservationDate
                it[cottage] = Cottages.select(Cottages.name).where { Cottages.name eq reservation.cottage }
                it[price] = reservation.price.toUInt()
            }
        }
    }
}