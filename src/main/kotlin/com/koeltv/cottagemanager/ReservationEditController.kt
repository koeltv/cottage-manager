package com.koeltv.cottagemanager

import javafx.collections.FXCollections
import javafx.collections.MapChangeListener
import javafx.collections.ObservableMap
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.*
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.time.LocalDate
import java.util.*


class ReservationEditController : Initializable {
    @FXML
    lateinit var root: VBox

    @FXML
    lateinit var cottageField: ChoiceBox<String>

    @FXML
    lateinit var arrivalDateField: DatePicker

    @FXML
    lateinit var departureDateField: DatePicker

    @FXML
    lateinit var nameField: AutoCompleteTextField<String>

    @FXML
    lateinit var nationalityField: TextField

    @FXML
    lateinit var phoneNumberField: TextField

    @FXML
    lateinit var confirmationCodeField: TextField

    @FXML
    lateinit var priceField: TextField

    @FXML
    lateinit var adultCountField: TextField

    @FXML
    lateinit var childCountField: TextField

    @FXML
    lateinit var babyCountField: TextField

    @FXML
    lateinit var noteField: ChoiceBox<String>

    @FXML
    lateinit var commentsArea: TextArea

    @FXML
    lateinit var addButton: Button

    private val fieldValidityMap: ObservableMap<Control, Boolean> = FXCollections.observableHashMap()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        fieldValidityMap.putAll(
            mapOf(
                nameField to false,
                confirmationCodeField to false,
                priceField to false,
                adultCountField to false,
            )
        )
        fieldValidityMap.addListener(MapChangeListener { _ ->
            addButton.isDisable = fieldValidityMap.any { !it.value }
        })

        arrivalDateField.value = LocalDate.now()
        departureDateField.value = LocalDate.now().plusDays(1)

        departureDateField.setDayCellFactory { _ ->
            object : DateCell() {
                override fun updateItem(item: LocalDate, empty: Boolean) {
                    super.updateItem(item, empty)
                    isDisable = item.isBefore(arrivalDateField.value.plusDays(1))
                }
            }
        }

        noteField.items.setAll("-----", "----", "---", "--", "-", "+", "++", "+++", "++++", "+++++")
        noteField.value = "+"

        transaction {
            Cottage.all()
                .map { it.name }
                .forEach { cottageField.items.add(it) }
        }
        cottageField.value = cottageField.items.first()

        // Name field setup with autocomplete
        val entries = TreeSet<String>()
        transaction {
            Client.all().forEach { entries.add(it.name) }
        }
        nameField.entries.addAll(entries)
        nameField.entryMenu.setOnAction { event ->
            (event.target as MenuItem).addEventHandler(Event.ANY) {
                nameField.text = nameField.lastSelectedObject
                fieldValidityMap.replace(nameField, nameField.text.length > 2)
            }
        }

        confirmationCodeField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 10 || event.character.matches(Regex("[^0-9A-Z\b]"))) source.deletePreviousChar()
            fieldValidityMap.replace(confirmationCodeField, source.text.length == 10)
        }

        nameField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            fieldValidityMap.replace(nameField, source.text.length > 2)
        }
        priceField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (!Regex("\\d+([.,]\\d{0,2})?").matches(source.text)) source.deletePreviousChar()
            fieldValidityMap.replace(priceField, source.text.isNotEmpty())
        }
        adultCountField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 2 || event.character.matches(Regex("[^0-9\b]"))) source.deletePreviousChar()
            fieldValidityMap.replace(adultCountField, source.text.isNotEmpty())
        }
        childCountField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 2 || event.character.matches(Regex("[^0-9\b]"))) source.deletePreviousChar()
        }
        babyCountField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 2 || event.character.matches(Regex("[^0-9\b]"))) source.deletePreviousChar()
        }
        phoneNumberField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 12 || !Regex("\\+\\d{0,12}").matches(source.text)) source.deletePreviousChar()
        }
    }

    @FXML
    fun onCancelButtonClick() {
        (root.parent as Pane).children.remove(root)
    }

    @FXML
    private fun onConfirmButtonClick() {
        val clientName = nameField.text

        transaction {
            val knownClient = Client.findById(clientName) ?: Client.new {
                name = clientName
                phoneNumber = phoneNumberField.text
                nationality = nationalityField.text
            }

            Reservation.new(confirmationCodeField.text) {
//                confirmationCode = confirmationCodeField.text
                status = if (arrivalDateField.value < LocalDate.now()) "Ancien voyageur" else "Confirmée"
                client = knownClient
                adultCount = adultCountField.text.toUByte()
                childCount = childCountField.text.toUByte()
                babyCount = babyCountField.text.toUByte()
                arrivalDate = arrivalDateField.value
                departureDate = departureDateField.value
                nightCount = arrivalDateField.value.until(departureDateField.value).days.toUShort()
                reservationDate = LocalDate.now()
                cottage = Cottage.findById(cottageField.value)!!
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