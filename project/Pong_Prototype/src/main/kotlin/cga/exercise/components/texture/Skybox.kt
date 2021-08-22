package cga.exercise.components.texture

import cga.exercise.components.camera.PongCamera
import cga.exercise.components.camera.SwitchCamera
import cga.exercise.components.shader.ShaderProgram
import org.joml.Matrix3f
import org.joml.Matrix4f
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL33.*
import org.lwjgl.stb.STBImage


class Skybox {

    private var skyboxVao = 0
    private var skyboxVbo = 0
    private var textureID = 0

    private val size = 500f

    private val vertices = floatArrayOf(
            -size, size, -size,
            -size, -size, -size,
            size, -size, -size,
            size, -size, -size,
            size, size, -size,
            -size, size, -size,
            -size, -size, size,
            -size, -size, -size,
            -size, size, -size,
            -size, size, -size,
            -size, size, size,
            -size, -size, size,
            size, -size, -size,
            size, -size, size,
            size, size, size,
            size, size, size,
            size, size, -size,
            size, -size, -size,
            -size, -size, size,
            -size, size, size,
            size, size, size,
            size, size, size,
            size, -size, size,
            -size, -size, size,
            -size, size, -size,
            size, size, -size,
            size, size, size,
            size, size, size,
            -size, size, size,
            -size, size, -size,
            -size, -size, -size,
            -size, -size, size,
            size, -size, -size,
            size, -size, -size,
            -size, -size, size,
            size, -size, size
    )

    fun loadCubemap(faces: List<String>) {
        val id = glGenTextures()
        glBindTexture(GL_TEXTURE_CUBE_MAP, id)
        if (id != -1) textureID = id

        for (i in faces.indices) {
            val width = BufferUtils.createIntBuffer(1)
            val height = BufferUtils.createIntBuffer(1)
            val nrChannels = BufferUtils.createIntBuffer(1)

            val data = STBImage.stbi_load(faces[i], width, height, nrChannels, 0) ?: throw Exception("Cubemap tex failed to load at path: ${faces[i]}")
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i,
                    0, GL_RGB, width.get(), height.get(),0, GL_RGB, GL_UNSIGNED_BYTE, data)

            STBImage.stbi_image_free(data)
        }

        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_LINEAR)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE)
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE)
    }

    fun setupVaoVbo() {
        skyboxVao = glGenVertexArrays()
        skyboxVbo = glGenBuffers()

        glBindVertexArray(skyboxVao)
        glBindBuffer(GL_ARRAY_BUFFER, skyboxVbo)
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW)
        glEnableVertexAttribArray(0)
        glVertexAttribPointer(0,3, GL_FLOAT, false, 3*4,0)
    }

    fun render(shader: ShaderProgram, camera: PongCamera) {
        glDepthFunc(GL_LEQUAL) //change depth function so depth test passes when values are equal to depth buffer's content

        shader.use()

        val viewMatrix = Matrix4f(Matrix3f(camera.getCalculateViewMatrix()))
        shader.setUniform("view", viewMatrix,false)
        shader.setUniform("projection", camera.getCalculateProjectionMatrix(),false)
        shader.setUniform("skybox", 0)

        glBindVertexArray(skyboxVao)
        glBindTexture(GL_TEXTURE_CUBE_MAP, textureID)
        glDrawArrays(GL_TRIANGLES,0,36)

        unbind()

        glDepthFunc(GL_LESS) //set depth function back to default
    }

    private fun unbind() {
        glBindTexture(0,0)
        glBindVertexArray(0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }
}