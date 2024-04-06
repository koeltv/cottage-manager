package com.koeltv.cottagemanager

import com.koeltv.cottagemanager.db.CottageService
import com.koeltv.cottagemanager.db.CottageView
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Button
import javafx.scene.control.TextField
import javafx.scene.layout.Pane
import javafx.scene.layout.VBox
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.net.URL
import java.util.*

open class CottageCreateController: Initializable, KoinComponent {
    @FXML
    lateinit var root: VBox
    @FXML
    lateinit var nameField: TextField
    @FXML
    lateinit var aliasField: TextField
    @FXML
    lateinit var addButton: Button

    val cottageService: CottageService by inject()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        nameField.setOnAction {
            addButton.isDisable = nameField.text.length >= 2
        }
    }

    @FXML
    open fun onConfirmButtonClick() {
        cottageService.create(CottageView(
            name = nameField.text,
            alias = aliasField.text.ifBlank { nameField.text }
        ))

        (root.parent as Pane).children.remove(root)
    }

    @FXML
    fun onCancelButtonClick() {
        (root.parent as Pane).children.remove(root)
    }
}
