package com.koeltv.cottagemanager.data

data class Client(
    override val name: String,
    override val phoneNumber: String,
    override val nationality: String
) : ClientBase(name, phoneNumber, nationality)