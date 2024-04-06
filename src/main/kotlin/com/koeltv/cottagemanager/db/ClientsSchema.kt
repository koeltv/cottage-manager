package com.koeltv.cottagemanager.db

import com.koeltv.cottagemanager.data.ClientWithStats
import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.koeltv.cottagemanager.data.Client as DataClient

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

        fun toView(): DataClient = DataClient(
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

    fun create(client: DataClient): DataClient = transaction(database) {
        Client.new(client.name) {
            phoneNumber = client.phoneNumber
            nationality = client.nationality
        }.toView()
    }

    fun read(id: String): DataClient? = transaction(database) {
        Client.findById(id)?.toView()
    }

    fun update(id: String, updater: Client.() -> Unit) = transaction(database) {
        Client.findByIdAndUpdate(id, updater)
    }

    fun delete(id: String) = transaction(database) {
        Client.findById(id)?.delete()
    }

    fun readAll(): List<DataClient> = transaction {
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
        ReservationService.Reservation.find { ReservationService.Reservations.client eq id }.count()
    }

    fun readWithStats(id: String): ClientWithStats? = transaction(database) {
        read(id)?.addStats()
    }

    fun readAllWithStats(): List<ClientWithStats> = transaction(database) {
        readAll().map { it.addStats() }
    }

    private fun DataClient.addStats(): ClientWithStats {
        val reservations = ReservationService.Reservation.find { ReservationService.Reservations.client eq name }.toList()
        val notes = reservations.mapNotNull { it.note?.toInt() }

        return ClientWithStats(
            name = name,
            phoneNumber = phoneNumber,
            nationality = nationality,
            averageNote = if (notes.isEmpty()) null else notes.average().toInt(),
            reservationCount = reservations.count()
        )
    }
}