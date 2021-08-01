package cga.exercise.components.geometry

import cga.exercise.components.shader.ShaderProgram
import org.lwjgl.opengl.GL11

class Renderable(val list: MutableList <Mesh> = mutableListOf()): IRenderable, Transformable() {

    override fun render(shaderProgram: ShaderProgram) {

        shaderProgram.setUniform("model_matrix", getWorldModelMatrix(), false)
        list.forEach{
            it.render(shaderProgram)
        }
    }
}