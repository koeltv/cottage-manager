package com.koeltv.cottagemanager

fun String.toNote(): UByte { // "----" --> 1
    return if ('-' in this) {
        (5 - length).toUByte()
    } else {
        (4 + length).toUByte()
    }
}

fun UByte.toPlusNote(): String { // 5 --> "+"
    return if (this < 5u) {
        "-".repeat((5u - this).toInt())
    } else {
        "+".repeat((this - 4u).toInt())
    }
}

fun UInt.toPriceString(): String { // 10502 --> 105.02€
    return toString().let { "${it.dropLast(2)}.${it.takeLast(2)}€" }
}

fun String.uppercaseFirst(): String = this.toCharArray() // test --> Test
    .apply { this[0] = this[0].uppercaseChar() }
    .concatToString()