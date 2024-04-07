package com.koeltv.cottagemanager.controller

import com.koeltv.cottagemanager.data.Client
import com.koeltv.cottagemanager.toNote
import com.koeltv.cottagemanager.toPlusNote
import javafx.fxml.FXML
import java.net.URL
import java.time.LocalDate
import java.util.*


class ReservationUpdateController(private val reservationId: String) : ReservationCreateController() {
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        super.initialize(location, resources)

        arrivalDateField.isDisable = true
        cottageField.isDisable = true
        confirmationCodeField.isDisable = true

        fieldValidityMap.replaceAll { _, _ -> true }
        addButton.text = "Editer"

        reservationService.read(reservationId)?.let {
            cottageField.value = it.cottage.alias
            arrivalDateField.value = it.arrivalDate
            departureDateField.value = it.departureDate
            nameField.text = it.client.name
            nationalityField.text = it.client.nationality
            phoneNumberField.text = it.client.phoneNumber
            confirmationCodeField.text = it.code
            priceField.text = it.price.toString()
            adultCountField.text = it.adultCount.toString()
            childCountField.text = it.childCount.toString()
            babyCountField.text = it.babyCount.toString()
            noteField.value = it.note?.toUByte()?.toPlusNote() ?: "+"
            commentsArea.text = it.comments
        }
    }

    @FXML
    override fun onConfirmButtonClick() {
        val oldReservation = reservationService.read(reservationId)!!
        if (nameField.text == oldReservation.client.name) { // Update
            clientService.update(nameField.text) {
                phoneNumber = phoneNumberField.text
                nationality = nationalityField.text
            }
        } else { // Create new and delete old if necessary
            val client = clientService.create(
                Client(
                    name = nameField.text,
                    phoneNumber = phoneNumberField.text,
                    nationality = nationalityField.text
                )
            )
            if (clientService.reservationCount(client.name) <= 0) {
                clientService.delete(client.name)
            }
            reservationService.updateClient(reservationId, client.name)
        }

        reservationService.update(confirmationCodeField.text) {
            status = if (arrivalDateField.value < LocalDate.now()) "Ancien voyageur" else "Confirmée"
            adultCount = adultCountField.text.toUByte()
            childCount = childCountField.text.toUByteOrNull() ?: 0U
            babyCount = babyCountField.text.toUByteOrNull() ?: 0U
            departureDate = departureDateField.value
            nightCount = arrivalDateField.value.until(departureDateField.value).days.toUShort()
            reservationDate = LocalDate.now()
            price = priceField.text.let {
                Regex("(\\d+)([,.](\\d{0,2}))?")
                    .find(it)!!.destructured
                    .let { (euros, _, cents) -> "$euros${cents.padEnd(2, '0')}" }
            }.toUInt()
            note = noteField.value.toNote()
            comments = commentsArea.text
        }

        unstack()
    }
}