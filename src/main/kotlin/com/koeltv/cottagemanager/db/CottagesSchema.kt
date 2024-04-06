package com.koeltv.cottagemanager.db

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityChangeType
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.EntityHook
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import com.koeltv.cottagemanager.data.Cottage as DataCottage

class CottageService(private val database: Database) {
    internal object Cottages : IdTable<String>() {
        val name = varchar("name", 50)
        val alias = varchar("alias", 50).uniqueIndex()

        override val id = name.entityId()
        override val primaryKey = PrimaryKey(name)
    }

    class Cottage(id: EntityID<String>) : Entity<String>(id) {
        companion object : EntityClass<String, Cottage>(Cottages)

        var name by Cottages.name
        var alias by Cottages.alias

        fun toView(): DataCottage =
            DataCottage(name, alias)
    }

    init {
        transaction(database) {
            SchemaUtils.create(Cottages)
        }
    }

    fun create(cottage: DataCottage): String = transaction(database) {
        Cottage.new(cottage.name) {
            alias = cottage.alias
        }.name
    }

    fun read(id: String): DataCottage? = transaction(database) {
        Cottage.findById(id)?.toView()
    }

    fun update(id: String, updater: Cottage.() -> Unit) = transaction(database) {
        Cottage.findByIdAndUpdate(id, updater)
    }

    fun delete(id: String) = transaction(database) {
        Cottage.findById(id)?.delete()
    }

    fun readAll(): List<DataCottage> = transaction {
        Cottage.all().map { it.toView() }
    }

    fun subscribe(subscriber: (String, EntityChangeType) -> Unit) {
        EntityHook.subscribe { change ->
            if (change.entityClass == Cottage) {
                subscriber(change.entityId.value as String, change.changeType)
            }
        }
    }
}