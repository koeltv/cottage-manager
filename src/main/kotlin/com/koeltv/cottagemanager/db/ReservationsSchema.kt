package com.koeltv.cottagemanager.db

import com.koeltv.cottagemanager.data.AirbnbReservation
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.transactions.transaction
import com.koeltv.cottagemanager.data.Reservation as DataReservation

class ReservationService(private val database: Database) {
    internal object Reservations : IdTable<String>() {
        val confirmationCode = char("confirmationCode", 10)
        val status = varchar("status", 40)
        val client = reference("client", ClientService.Clients)
        val adultCount = ubyte("adultCount")
        val childCount = ubyte("childCount")
        val babyCount = ubyte("babyCount")
        val arrivalDate = date("arrivalDate")
        val departureDate = date("departureDate")
        val nightCount = ushort("nightCount")
        val reservationDate = date("reservationDate").nullable()
        val cottage = reference("cottage", CottageService.Cottages)
        val price = uinteger("price")
        val note = ubyte("note").nullable()
        val comments = text("comments").nullable()

        override val id = confirmationCode.entityId()
        override val primaryKey = PrimaryKey(confirmationCode)
    }

    class Reservation(id: EntityID<String>) : Entity<String>(id) {
        companion object : EntityClass<String, Reservation>(Reservations)

        var confirmationCode by Reservations.confirmationCode
        var status by Reservations.status

        var client by ClientService.Client referencedOn Reservations.client

        var adultCount by Reservations.adultCount
        var childCount by Reservations.childCount
        var babyCount by Reservations.babyCount
        var arrivalDate by Reservations.arrivalDate
        var departureDate by Reservations.departureDate
        var nightCount by Reservations.nightCount
        var reservationDate by Reservations.reservationDate

        var cottage by CottageService.Cottage referencedOn Reservations.cottage

        var price by Reservations.price
        var note by Reservations.note
        var comments by Reservations.comments

        fun toView(): DataReservation = DataReservation(
            code = confirmationCode,
            client = client.toView(),
            adultCount = adultCount.toInt(),
            childCount = childCount.toInt(),
            babyCount = babyCount.toInt(),
            arrivalDate = arrivalDate,
            departureDate = departureDate,
            reservationDate = reservationDate,
            cottage = cottage.toView(),
            price = price.toInt(),
            note = note?.toInt(),
            comments = comments ?: ""
        )
    }

    init {
        transaction(database) {
            SchemaUtils.create(Reservations)
        }
    }

    fun create(reservation: DataReservation): String = transaction(database) {
        Reservation.new(reservation.code) {
            client = ClientService.Client.findById(reservation.client.name)!!
            adultCount = reservation.adultCount.toUByte()
            childCount = reservation.childCount.toUByte()
            babyCount = reservation.babyCount.toUByte()
            arrivalDate = reservation.arrivalDate
            departureDate = reservation.departureDate
            reservationDate = reservation.reservationDate
            cottage = CottageService.Cottage.findById(reservation.cottage.name)!!
            price = reservation.price.toUInt()
            note = reservation.note?.toUByte()
            comments = reservation.comments.ifBlank { null }
        }.confirmationCode
    }

    fun read(id: String): DataReservation? = transaction(database) {
        Reservation.findById(id)?.toView()
    }

    fun update(id: String, updater: Reservation.() -> Unit) = transaction(database) {
        Reservation.findByIdAndUpdate(id, updater)
    }

    fun delete(id: String) = transaction(database) {
        Reservation.findById(id)?.delete()
    }

    fun readAll(): List<DataReservation> = transaction(database) {
        Reservation.all().map { it.toView() }
    }

    fun subscribe(subscriber: (String, EntityChangeType) -> Unit) {
        EntityHook.subscribe { change ->
            if (change.entityClass == Reservation) {
                subscriber(change.entityId.value as String, change.changeType)
            }
        }
    }

    fun updateClient(id: String, clientName: String) = transaction(database) {
        Reservation.findById(id)?.apply {
            client = ClientService.Client.findById(clientName)!!
        }
    }

    fun importAirbnbReservation(reservation: AirbnbReservation) = transaction(database) {
        val reservationCottage =
            CottageService.Cottage.findById(reservation.cottage)
                ?: CottageService.Cottage.new(reservation.cottage) {
                    alias = reservation.cottage
                }

        val reservationClient = ClientService.Client.findByIdAndUpdate(reservation.name) {
            if (reservation.phoneNumber.isNotBlank()) {
                it.phoneNumber = reservation.phoneNumber
            }
        } ?: ClientService.Client.new(reservation.name) {
            phoneNumber = reservation.phoneNumber
        }

        val reservationUpdater: Reservation.() -> Unit = {
            status = reservation.status
            client = reservationClient
            adultCount = reservation.adultCount.toUByte()
            childCount = reservation.childCount.toUByte()
            babyCount = reservation.babyCount.toUByte()
            arrivalDate = reservation.arrivalDate
            departureDate = reservation.departureDate
            nightCount = reservation.nightCount.toUShort()
            reservationDate = reservation.reservationDate
            cottage = reservationCottage
            price = reservation.price.toUInt()
        }

        Reservation.findByIdAndUpdate(reservation.confirmationCode, reservationUpdater)
            ?: Reservation.new(reservation.confirmationCode, reservationUpdater)
    }
}