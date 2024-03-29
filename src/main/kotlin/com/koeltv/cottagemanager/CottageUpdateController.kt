package com.koeltv.cottagemanager

import javafx.fxml.FXML
import javafx.scene.layout.Pane
import org.jetbrains.exposed.sql.transactions.transaction
import java.net.URL
import java.util.*

class CottageUpdateController(private val cottageId: String): CottageCreateController() {

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        nameField.isDisable = true
        addButton.isDisable = false

        addButton.text = "Éditer"

        transaction {
            val cottage = Cottage.findById(cottageId)!!
            nameField.text = cottage.name
            aliasField.text = cottage.alias
        }
    }

    @FXML
    override fun onConfirmButtonClick() {
        transaction {
            val cottage = Cottage.findById(cottageId)!!
            cottage.alias = aliasField.text.ifBlank { nameField.text }
        }

        (root.parent as Pane).children.remove(root)
    }
}
