package com.koeltv.cottagemanager.data

import com.koeltv.cottagemanager.Cottage

data class CottageView(
    val name: String
)

fun Cottage.toView(): CottageView {
    return CottageView(name)
}
