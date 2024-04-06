package com.koeltv.cottagemanager.db

import com.koeltv.cottagemanager.db.ReservationService.Reservation
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

data class ClientView(val name: String, val phoneNumber: String, val nationality: String)
data class ClientWithStatsView(
    val name: String,
    val phoneNumber: String,
    val nationality: String,
    val averageNote: Int?,
    val reservationCount: Int,
)

class ClientService(private val database: Database) {
    internal object Clients : IdTable<String>() {
        val name = varchar("name", 40)
        val phoneNumber = char("phoneNumber", 14).nullable()
        val nationality = char("nationality", 2).nullable()

        override val id = name.entityId()
        override val primaryKey = PrimaryKey(name)
    }

    class Client(name: EntityID<String>) : Entity<String>(name) {
        companion object : EntityClass<String, Client>(Clients)

        var name by Clients.name
        var phoneNumber by Clients.phoneNumber
        var nationality by Clients.nationality

        fun toView(): ClientView = ClientView(
            name,
            phoneNumber ?: "",
            nationality ?: ""
        )
    }

    init {
        transaction(database) {
            SchemaUtils.create(Clients)
        }
    }

    fun create(client: ClientView): ClientView = transaction(database) {
        Client.new(client.name) {
            phoneNumber = client.phoneNumber
            nationality = client.nationality
        }.toView()
    }

    fun read(id: String): ClientView? = transaction(database) {
        Client.findById(id)?.toView()
    }

    fun update(id: String, updater: Client.() -> Unit) = transaction(database) {
        Client.findByIdAndUpdate(id, updater)
    }

    fun delete(id: String) = transaction(database) {
        Client.findById(id)?.delete()
    }

    fun readAll(): List<ClientView> = transaction {
        Client.all().map { it.toView() }
    }

    fun subscribe(subscriber: (String, EntityChangeType) -> Unit) {
        EntityHook.subscribe { change ->
            if (change.entityClass == Client) {
                subscriber(change.entityId.value as String, change.changeType)
            }
        }
    }

    fun reservationCount(id: String): Long = transaction(database) {
        Reservation.find { ReservationService.Reservations.client eq id }.count()
    }

    fun notesOf(id: String): List<Byte> = transaction(database) {
        ReservationService.Reservation
            .find { ReservationService.Reservations.client eq id }
            .mapNotNull { it.note?.toByte() }
            .toList()
    }

    fun readWithStats(id: String): ClientWithStatsView? = transaction(database) {
        read(id)?.addStats()
    }

    fun readAllWithStats(): List<ClientWithStatsView> = transaction(database) {
        readAll().map { it.addStats() }
    }

    private fun ClientView.addStats(): ClientWithStatsView {
        val reservations = Reservation.find { ReservationService.Reservations.client eq name }.toList()
        val notes = reservations.mapNotNull { it.note?.toInt() }

        return ClientWithStatsView(
            name = name,
            phoneNumber = phoneNumber,
            nationality = nationality,
            averageNote = if (notes.isEmpty()) null else notes.average().toInt(),
            reservationCount = reservations.count()
        )
    }
}