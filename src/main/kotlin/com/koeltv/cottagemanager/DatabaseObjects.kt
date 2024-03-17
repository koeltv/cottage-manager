package com.koeltv.cottagemanager

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.date

object Clients: IdTable<String>() {
    val name = varchar("name", 40)
    val phoneNumber = char("phoneNumber", 13).nullable()
    val nationality = char("nationality", 2).nullable()

    override val id = name.entityId()
    override val primaryKey = PrimaryKey(name)
}

object Reservations: IdTable<String>() {
    val confirmationCode = char("confirmationCode", 10)
    val status = varchar("status", 40)
    val client = reference("client", Clients)
    val adultCount = ubyte("adultCount")
    val childCount = ubyte("childCount")
    val babyCount = ubyte("babyCount")
    val arrivalDate = date("arrivalDate")
    val departureDate = date("departureDate")
    val nightCount = ushort("nightCount")
    val reservationDate = date("reservationDate").nullable()
    val cottage = reference("cottage", Cottages)
    val price = uinteger("price")
    val note = ubyte("note").nullable()
    val comments = text("comments").nullable()

    override val id = confirmationCode.entityId()
    override val primaryKey = PrimaryKey(confirmationCode)
}

object Cottages: IdTable<String>() {
    val name = varchar("name", 50)
    val alias = varchar("alias", 50).uniqueIndex()

    override val id = name.entityId()
    override val primaryKey = PrimaryKey(name)
}

class Client(name: EntityID<String>) : Entity<String>(name) {
    companion object : EntityClass<String, Client>(Clients)

    var name by Clients.name
    var phoneNumber by Clients.phoneNumber
    var nationality by Clients.nationality
}

class Reservation(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Reservation>(Reservations)

    var confirmationCode by Reservations.confirmationCode
    var status by Reservations.status

    var client by Client referencedOn Reservations.client

    var adultCount by Reservations.adultCount
    var childCount by Reservations.childCount
    var babyCount by Reservations.babyCount
    var arrivalDate by Reservations.arrivalDate
    var departureDate by Reservations.departureDate
    var nightCount by Reservations.nightCount
    var reservationDate by Reservations.reservationDate

    var cottage by Cottage referencedOn Reservations.cottage

    var price by Reservations.price
    var note by Reservations.note
    var comments by Reservations.comments
}

class Cottage(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, Cottage>(Cottages)

    var name by Cottages.name
    var alias by Cottages.alias
}