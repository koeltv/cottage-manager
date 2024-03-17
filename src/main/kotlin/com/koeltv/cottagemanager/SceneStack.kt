package com.koeltv.cottagemanager

import javafx.scene.Scene
import javafx.stage.Stage
import java.util.*

object SceneStack {
    private lateinit var stage: Stage

    private val sceneStack = Stack<Scene>()

    fun setStage(stage: Stage) {
        this.stage = stage
        sceneStack.clear()
    }

    fun openScene(scene: Scene) {
        sceneStack.push(scene)
        stage.scene = scene
    }

    fun back() {
        sceneStack.pop()
        stage.scene = sceneStack.peek()
    }
}