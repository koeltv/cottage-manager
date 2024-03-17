package com.koeltv.cottagemanager.data

import com.koeltv.cottagemanager.Cottage

data class CottageView(
    val name: String,
    val alias: String,
)

fun Cottage.toView(): CottageView {
    return CottageView(
        name,
        alias,
    )
}
