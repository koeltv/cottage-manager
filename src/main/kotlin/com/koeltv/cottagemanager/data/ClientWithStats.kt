package com.koeltv.cottagemanager.data

data class ClientWithStats(
    override val name: String,
    override val phoneNumber: String,
    override val nationality: String,
    val averageNote: Int?,
    val reservationCount: Int,
) : ClientBase(name, phoneNumber, nationality)