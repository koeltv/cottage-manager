package com.koeltv.cottagemanager

import javafx.fxml.FXML
import javafx.scene.layout.Pane
import java.net.URL
import java.util.*

class CottageUpdateController(private val cottageId: String) : CottageCreateController() {

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        nameField.isDisable = true
        addButton.isDisable = false

        addButton.text = "Éditer"

        cottageService.read(cottageId)?.let {
            nameField.text = it.name
            aliasField.text = it.alias
        }
    }

    @FXML
    override fun onConfirmButtonClick() {
        cottageService.update(cottageId) {
            alias = aliasField.text.ifBlank { nameField.text }
        }

        (root.parent as Pane).children.remove(root)
    }
}
