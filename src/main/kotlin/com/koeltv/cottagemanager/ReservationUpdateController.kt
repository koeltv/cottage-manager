package com.koeltv.cottagemanager

import javafx.fxml.FXML
import javafx.scene.layout.Pane
import org.jetbrains.exposed.sql.transactions.transaction
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

        transaction {
            val reservation = Reservation.findById(reservationId)!!

            cottageField.value = reservation.cottage.alias
            arrivalDateField.value = reservation.arrivalDate
            departureDateField.value = reservation.departureDate
            nameField.text = reservation.client.name
            nationalityField.text = reservation.client.nationality
            phoneNumberField.text = reservation.client.phoneNumber
            confirmationCodeField.text = reservation.confirmationCode
            priceField.text = reservation.price.toPriceString().removeSuffix("€")
            adultCountField.text = reservation.adultCount.toString()
            childCountField.text = reservation.childCount.toString()
            babyCountField.text = reservation.babyCount.toString()
            noteField.value = reservation.note?.toPlusNote() ?: "+"
            commentsArea.text = reservation.comments
        }
    }

    @FXML
    override fun onConfirmButtonClick() {
        val clientName = nameField.text

        transaction {
            val knownClient = Client.findById(clientName) ?: Client.new(clientName) {}
            knownClient.apply {
                phoneNumber = phoneNumberField.text
                nationality = nationalityField.text
            }

            Reservation.findById(confirmationCodeField.text)!!.apply {
                status = if (arrivalDateField.value < LocalDate.now()) "Ancien voyageur" else "Confirmée"
                client = knownClient
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
        }

        (root.parent as Pane).children.remove(root)
    }
}