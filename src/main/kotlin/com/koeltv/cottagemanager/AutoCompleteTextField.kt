package com.koeltv.cottagemanager

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.collections.ObservableList
import javafx.event.EventHandler
import javafx.geometry.Side
import javafx.scene.control.ContextMenu
import javafx.scene.control.CustomMenuItem
import javafx.scene.control.TextField
import javafx.scene.text.Text
import javafx.scene.text.TextFlow
import java.util.*
import java.util.regex.Pattern
import kotlin.math.min


/**
 * This class is a TextField which implements an "autocomplete" functionality,
 * based on a supplied list of entries.
 *
 * If the entered text matches a part of any of the supplied entries these are
 * going to be displayed in a popup. Further the matching part of the entry is
 * going to be displayed in a special style, defined by
 * [textOccurenceStyle][.textOccurenceStyle]. The maximum number of
 * displayed entries in the popup is defined by
 * [maxEntries][.maxEntries].<br></br>
 * By default the pattern matching is not case-sensitive. This behaviour is
 * defined by the [caseSensitive][.caseSensitive].
 *
 * The AutoCompleteTextField also has a List of
 * [filteredEntries][.filteredEntries] that is equal to the search results
 * if search results are not empty, or [filteredEntries][.filteredEntries]
 * is equal to [entries][.entries] otherwise. If
 * [popupHidden][.popupHidden] is set to true no popup is going to be
 * shown. This list can be used to bind all entries to another node (a ListView
 * for example) in the following way:
 * <pre>
 * `
 * AutoCompleteTextField auto = new AutoCompleteTextField(entries);
 * auto.setPopupHidden(true);
 * SimpleListProperty filteredEntries = new SimpleListProperty(auto.getFilteredEntries());
 * listView.itemsProperty().bind(filteredEntries);
` *
</pre> *
 *
 * More [here](https://stackoverflow.com/questions/36861056/javafx-textfield-auto-suggestions)
 *
 * @author Caleb Brinkman
 * @author Fabian Ochmann
 */
class AutoCompleteTextField<S>(entrySet: SortedSet<S> = TreeSet()) : TextField() {
    private val lastSelectedItem: ObjectProperty<S> = SimpleObjectProperty()

    /**
     * Get the existing set of autocomplete entries.
     *
     * @return The existing autocomplete entries.
     */
    /**
     * The existing autocomplete entries.
     */
    val entries: SortedSet<S> = entrySet

    /**
     * The set of filtered entries:<br></br>
     * Equal to the search results if search results are not empty, equal to
     * [entries][.entries] otherwise.
     */
    val filteredEntries: ObservableList<S> = FXCollections.observableArrayList()

    /**
     * The popup used to select an entry.
     */
    val entryMenu: ContextMenu

    /**
     * Indicates whether the search is case sensitive or not. <br></br>
     * Default: false
     */
    var isCaseSensitive: Boolean = false

    /**
     * Indicates whether the Popup should be hidden or displayed. Use this if
     * you want to filter an existing list/set (for example values of a
     * [ListView][javafx.scene.control.ListView]). Do this by binding
     * [getFilteredEntries()][.getFilteredEntries] to the list/set.
     */
    var isPopupHidden: Boolean = false

    /**
     * The CSS style that should be applied on the parts in the popup that match
     * the entered text. <br></br>
     * Default: "-fx-font-weight: bold; -fx-fill: red;"
     *
     *
     * Note: This style is going to be applied on an
     * [Text][javafx.scene.text.Text] instance. See the *JavaFX CSS
     * Reference Guide* for available CSS Propeties.
     */
    var textOccurenceStyle: String = ("-fx-font-weight: bold; "
            + "-fx-fill: red;")

    /**
     * The maximum Number of entries displayed in the popup.<br></br>
     * Default: 10
     */
    var maxEntries: Int = 10

    init {
        filteredEntries.addAll(entries)

        entryMenu = ContextMenu()

        textProperty().addListener { _: ObservableValue<out String?>?, _: String?, _: String? ->
            if (text == null || text.isEmpty()) {
                filteredEntries.clear()
                filteredEntries.addAll(entries)
                entryMenu.hide()
            } else {
                val searchResult = LinkedList<S>()
                //Check if the entered Text is part of some entry
                val text1 = text
                val pattern = if (isCaseSensitive) {
                    Pattern.compile(".*$text1.*")
                } else {
                    Pattern.compile(".*$text1.*", Pattern.CASE_INSENSITIVE)
                }
                for (entry in entries) {
                    val matcher = pattern.matcher(entry.toString())
                    if (matcher.matches()) {
                        searchResult.add(entry)
                    }
                }
                if (!entries.isEmpty()) {
                    filteredEntries.clear()
                    filteredEntries.addAll(searchResult)
                    //Only show popup if not in filter mode
                    if (!isPopupHidden) {
                        populatePopup(searchResult, text1)
                        if (!entryMenu.isShowing) {
                            entryMenu.show(
                                this@AutoCompleteTextField,
                                Side.BOTTOM,
                                0.0,
                                0.0
                            )
                        }
                    }
                } else {
                    entryMenu.hide()
                }
            }
        }

        focusedProperty().addListener { _: ObservableValue<out Boolean?>?, _: Boolean?, _: Boolean? ->
            entryMenu.hide()
        }
    }

    /**
     * Populate the entry set with the given search results. Display is limited
     * to 10 entries, for performance.
     *
     * @param searchResult The set of matching strings.
     */
    private fun populatePopup(searchResult: List<S>, text: String) {
        val menuItems: MutableList<CustomMenuItem> = LinkedList()
        val count = min(searchResult.size.toDouble(), maxEntries.toDouble()).toInt()
        for (i in 0 until count) {
            val result = searchResult[i].toString()
            val itemObject = searchResult[i]

            val occurrence = if (isCaseSensitive) {
                result.indexOf(text)
            } else {
                result.lowercase(Locale.getDefault()).indexOf(text.lowercase(Locale.getDefault()))
            }
            if (occurrence < 0) {
                continue
            }
            //Part before occurrence (might be empty)
            val pre = Text(result.substring(0, occurrence))
            //Part of (first) occurrence
            val `in` = Text(result.substring(occurrence, occurrence + text.length))
            `in`.style = textOccurenceStyle
            //Part after occurrence
            val post = Text(result.substring(occurrence + text.length, result.length))

            val entryFlow = TextFlow(pre, `in`, post)

            val item = CustomMenuItem(entryFlow, true)
            item.onAction = EventHandler {
                lastSelectedItem.set(itemObject)
                entryMenu.hide()
            }
            menuItems.add(item)
        }
        entryMenu.items.clear()
        entryMenu.items.addAll(menuItems)
    }

    val lastSelectedObject: S
        get() = lastSelectedItem.get()
}