package com.koeltv.cottagemanager

import javafx.scene.Node
import javafx.scene.layout.Pane

/**
 * Interface for managing a stack of views in a JavaFX application.
 *
 * This interface provides methods for adding views to the stack and removing the current view from the stack.
 * It assumes that the UI is structured as a tree of Panes, where each Pane represents a screen or view in the application.
 * The root property represents the current view, and the stack and unstack methods are used to manage the stack of views.
 */
interface Stackable {
    /**
     * The root Pane of the current view.
     *
     * This property should be overridden by classes implementing this interface to return the root Pane of the current view.
     */
    val root: Pane

    /**
     * Removes the current view from the stack.
     *
     * This method removes the root Pane of the current view from its parent Pane, effectively navigating back to the previous view.
     * It assumes that the parent of the root Pane is also a Pane, and that the current view is a child of its parent view.
     */
    fun unstack() = (root.parent as Pane).children.remove(root)

    /**
     * Adds a new view to the stack.
     *
     * This method adds a new Node to the children of the parent Pane of the current view, effectively navigating forward to a new view.
     * It assumes that the parent of the root Pane is also a Pane, and that the new view will be a child of its parent view.
     *
     * @param element The Node to be added to the stack.
     */
    fun <T : Node> stack(element: T) = (root.parent as Pane).children.add(element)
}