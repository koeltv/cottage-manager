package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.db.ClientService
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.control.TextInputControl
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL
import java.util.*

open class ClientUpdateController(private val clientId: String): Initializable, KoinComponent {
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

    val clientService: ClientService by inject()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        nameField.isDisable = true
        addButton.isDisable = false

        addButton.text = "Éditer"

        clientService.read(clientId)?.let {
            nameField.text = it.name
            nationalityField.text = it.nationality
            phoneNumberField.text = it.phoneNumber
        }

        phoneNumberField.setOnKeyTyped { event ->
            val source = event.source as TextInputControl
            if (source.text.length > 12 || !Regex("\\+\\d{0,12}").matches(source.text)) source.deletePreviousChar()
        }
    }

    @FXML
    open fun onConfirmButtonClick() {
        clientService.update(clientId) {
            phoneNumber = phoneNumberField.text
            nationality = nationalityField.text
        }

        (root.parent as Pane).children.remove(root)
    }

    @FXML
    fun onCancelButtonClick() {
        (root.parent as Pane).children.remove(root)
    }
}
