package com.koeltv.cottagemanager

import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.util.*

open class ClientUpdateController(private val clientId: String): Initializable {
    @FXML
    lateinit var root: VBox
    @FXML
    lateinit var nameField: TextField
    @FXML
    lateinit var nationalityField: TextField
    @FXML
    lateinit var phoneNumberField: TextField
    @FXML
    lateinit var addButton: Button

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        nameField.isDisable = true
        addButton.isDisable = false

        addButton.text = "Éditer"

        transaction {
            val client = Client.findById(clientId)!!
            nameField.text = client.name
            nationalityField.text = client.nationality
            phoneNumberField.text = client.phoneNumber
        }

        phoneNumberField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 12 || !Regex("\\+\\d{0,12}").matches(source.text)) source.deletePreviousChar()
        }
    }

    @FXML
    open fun onConfirmButtonClick() {
        transaction {
            val client = Client.findById(clientId)!!
            client.phoneNumber = phoneNumberField.text
            client.nationality = nationalityField.text
        }

        (root.parent as Pane).children.remove(root)
    }

    @FXML
    fun onCancelButtonClick() {
        (root.parent as Pane).children.remove(root)
    }
}
