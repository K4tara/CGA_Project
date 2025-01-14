package cga.exercise.game

import cga.framework.GameWindow

class Game(width: Int,
           height: Int,
           fullscreen: Boolean = false,
           vsync: Boolean = false,
           title: String = "AT0M1C-JUNGL3-W0RM3-BATLL3",
           GLVersionMajor: Int = 3,
           GLVersionMinor: Int = 3) : GameWindow(width, height, fullscreen, vsync, GLVersionMajor, GLVersionMinor, title, 4, 120.0f) {

    private val scene: Scene
    init {
        setCursorVisible(false)
        scene = Scene(this)
    }

    override fun shutdown() = scene.cleanup()

    override fun update(dt: Float, t: Float) = scene.update(dt, t)

    override fun render(dt: Float, t: Float) = scene.render(dt, t)

    override fun onMouseMove(xpos: Double, ypos: Double) = scene.onMouseMove(xpos, ypos)

    override fun onKey(key: Int, scancode: Int, action: Int, mode: Int) = scene.onKey(key, scancode, action, mode)

}