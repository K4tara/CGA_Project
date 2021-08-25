package cga.exercise.game

import cga.exercise.components.camera.PongCamera
import cga.exercise.components.geometry.Material
import cga.exercise.components.geometry.Mesh
import cga.exercise.components.geometry.Renderable
import cga.exercise.components.geometry.VertexAttribute
import cga.exercise.components.light.Pointlight
import cga.exercise.components.light.Spotlight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Skybox
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.OBJLoader
import org.joml.*
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.opengl.GL11.*

class Scene(private val window: GameWindow) {

    //Shaders
    private val classicStaticShader: ShaderProgram
    private val effectShader: ShaderProgram
    private var useShader: ShaderProgram
    private val skyboxShader: ShaderProgram

    //Game Objects
    private var ground = Renderable()
    private var text = Renderable()
    private var text_score = Renderable()
    private var text_p1_won = Renderable()
    private var text_p2_won = Renderable()
    private var score_bar = Renderable()
    private var score_p1 = Renderable()
    private var score_p2 = Renderable()
    private var ball = Renderable()
    private var player1 = Renderable()
    private var player2 = Renderable()
    private var playerAI = Renderable()
    private var item = Renderable()
    private var wallUp = Renderable()
    private var wallDown = Renderable()

    //Game, Skybox, Camera
    private var gamelogic: Gamelogic
    private val skybox: Skybox
    private var camera = PongCamera()

    //Lights
    private var pointLight_ball = Pointlight(Vector3f(),Vector3f())
    private var pointLight_player1 = Pointlight(Vector3f(),Vector3f())
    private var pointLight_player2 = Pointlight(Vector3f(),Vector3f())
    private var spotLight = Spotlight(Vector3f(),Vector3f())

    //Mouse movement (unused)
    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var bool: Boolean = false


    //Scene setup
    init {
        //initial opengl state
        glClearColor(0f, 0f, 0f, 1.0f); GLError.checkThrow()
        glEnable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW); GLError.checkThrow()
        glCullFace(GL_BACK); GLError.checkThrow()
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

        //shader setup
        classicStaticShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")
        effectShader = ShaderProgram("assets/shaders/effect3_vert.glsl", "assets/shaders/effect3_frag.glsl") //all effects in one shader
        skyboxShader = ShaderProgram("assets/shaders/skybox_vert.glsl", "assets/shaders/skybox_frag.glsl")
        useShader = effectShader

        //skybox setup
        skybox = Skybox()
        val skyboxFaces = listOf(
                "assets/textures/Skybox/posx.jpg",
                "assets/textures/Skybox/negx.jpg",
                "assets/textures/Skybox/posy.jpg",
                "assets/textures/Skybox/negy.jpg",
                "assets/textures/Skybox/posz.jpg",
                "assets/textures/Skybox/negz.jpg"
        )
        skybox.setupVaoVbo()
        skybox.loadCubemap(skyboxFaces)

        //game objects setup
        setupGameObjectsandLighting()
        transformations()

        //actual game setup
        gamelogic = Gamelogic(window, camera, ball, player1, player2, playerAI, item, wallUp,
                wallDown, text, text_score, text_p1_won, text_p2_won, score_p1, score_p2, score_bar)
    }


    fun update(dt: Float, t: Float) {
        //continuous rotation of items
        item.rotateLocal(Math.toRadians(0.0f), 0.01f, -0.0f)

        gamelogic.start_game(dt)
        gamelogic.restart_game(dt)
        gamelogic.camera_switch(dt,t)
        gamelogic.changeMode()
        gamelogic.winner(dt)
        gamelogic.colorSwap()

        //disable movement when game is paused
        if (!gamelogic.pause) {
            gamelogic.ball_movement(dt)
            gamelogic.player_movement(dt)
            gamelogic.controlBallspeed()
        }

        //if single player mode activate AI
        if (gamelogic.singlePlayer) {
            gamelogic.playerAI(dt)
        }

        //end effect after some time
        if (gamelogic.effectNumber > 0) {
            gamelogic.countDownEffect(dt)
        }

        //place item if there´s no effect and if new item should be placed (after some time)
        if (gamelogic.effectNumber == 0 && gamelogic.placeItem) {
            gamelogic.countDownSpawn(dt)
        }
    }

    fun render(dt: Float, t: Float) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        //set shader uniforms
        useShader.use()
        useShader.setUniform("sceneColour", Vector3f(1.0f,1.0f,1.0f))
        useShader.setUniform("time",t)
        useShader.setUniform("chaos", gamelogic.chaos) //LSD effect (jetzt eher Bildrauschen o.ä.)
        useShader.setUniform("confuse", gamelogic.confuse) //inverted colors & player movement
        useShader.setUniform("shake", gamelogic.shake) //earthquake

        //bind camera
        camera.bind(useShader)

        //render game objects
        ground.render(useShader)
        text.render(useShader)
        text_score.render(useShader)
        text_p1_won.render(useShader)
        text_p2_won.render(useShader)
        score_bar.render(useShader)
        score_p1.render(useShader)
        score_p2.render(useShader)
        wallDown.render(useShader)
        wallUp.render(useShader)
        player1.render(useShader)
        ball.render(useShader)

        //render player2 if multiplayer, AI if singleplayer
        if (gamelogic.singlePlayer) {
            playerAI.render(useShader)
            pointLight_player2.parent = playerAI
        } else {
            player2.render(useShader)
            pointLight_player2.parent = player2
        }

        //render item
        if (gamelogic.itemSpawn) {
            item.render(useShader)
        }

        //bind lights
        pointLight_player1.bind(useShader,"cyclePoint2")
        pointLight_player2.bind(useShader,"cyclePoint3")
        pointLight_ball.bind(useShader,"cyclePoint")
        spotLight.bind(useShader,"cycleSpot", camera.getCalculateViewMatrix())

        //render skybox
        skybox.render(skyboxShader, camera, t, gamelogic.chaos, gamelogic.confuse, gamelogic.shake)
    }

    fun load_models_assign_textures (vertexAttributes : Array<VertexAttribute>, renderable: Renderable, modelPath: String,
                                     texture_emit_path: String, texture_diff_path: String, texture_spec_path: String){
        var mesh: Mesh

        val obj: OBJLoader.OBJResult = OBJLoader.loadOBJ(modelPath)
        val obj_mesh: OBJLoader.OBJMesh = obj.objects[0].meshes[0]

        val texture_emit = Texture2D(texture_emit_path,true)
        val texture_diff = Texture2D(texture_diff_path,true)
        val texture_spec = Texture2D(texture_spec_path,true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT,  GL_NEAREST,GL_NEAREST)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_NEAREST,GL_NEAREST)

        val material = Material(texture_diff, texture_emit, texture_spec,10.0f, Vector2f(64.0f,64.0f))

        mesh = Mesh(obj_mesh.vertexData, obj_mesh.indexData, vertexAttributes, material)
        renderable.list.add(mesh)
    }

    private fun setupGameObjectsandLighting() {
        //Atrributes
        val stride: Int = 8 * 4
        val attrPos = VertexAttribute(0,3, GL_FLOAT, false, stride, 0)
        val attrTC = VertexAttribute(1,2, GL_FLOAT, false, stride, 3 * 4)
        val attrNorm = VertexAttribute(2,3, GL_FLOAT, false, stride, 5 * 4)
        val vertexAttributes = arrayOf(attrPos, attrTC, attrNorm)

        //Load Models + Assign Textures
        load_models_assign_textures(vertexAttributes, ground,
                "assets/models/ground2.obj",
                "assets/textures/ground_emit.png",
                "assets/textures/ground_diff.png",
                "assets/textures/ground_spec.png")

        load_models_assign_textures(vertexAttributes, ball,
                "assets/models/ball.obj",
                "assets/textures/ball_emit.png",
                "assets/textures/ball_diff.png",
                "assets/textures/ball_spec.png")

        load_models_assign_textures(vertexAttributes, player1,
                "assets/models/player.obj",
                "assets/textures/player1_emit.png",
                "assets/textures/player1_diff.png",
                "assets/textures/player1_spec.png")

        load_models_assign_textures(vertexAttributes, player2,
                "assets/models/player.obj",
                "assets/textures/player2_emit.png",
                "assets/textures/player2_diff.png",
                "assets/textures/player2_spec.png")

        load_models_assign_textures(vertexAttributes, playerAI,
                "assets/models/player.obj",
                "assets/textures/player2_emit.png",
                "assets/textures/player2_diff.png",
                "assets/textures/player2_spec.png")

        load_models_assign_textures(vertexAttributes, item,
                "assets/models/item.obj",
                "assets/textures/item_emit.png",
                "assets/textures/item_diff.png",
                "assets/textures/item_spec.png")

        load_models_assign_textures(vertexAttributes, text,
                "assets/models/text.obj",
                "assets/textures/white.png",
                "assets/textures/white.png",
                "assets/textures/white.png")

        load_models_assign_textures(vertexAttributes, text_score,
                "assets/models/score_text.obj",
                "assets/textures/white.png",
                "assets/textures/white.png",
                "assets/textures/white.png")

        load_models_assign_textures(vertexAttributes, text_p1_won,
                "assets/models/text_p1_won.obj",
                "assets/textures/white.png",
                "assets/textures/white.png",
                "assets/textures/white.png")

        load_models_assign_textures(vertexAttributes, text_p2_won,
                "assets/models/text_p2_won.obj",
                "assets/textures/white.png",
                "assets/textures/white.png",
                "assets/textures/white.png")

        load_models_assign_textures(vertexAttributes, score_p1,
                "assets/models/score_p1.obj",
                "assets/textures/ground_emit.png",
                "assets/textures/ground_diff.png",
                "assets/textures/ground_spec.png")

        load_models_assign_textures(vertexAttributes, score_p2,
                "assets/models/score_p2.obj",
                "assets/textures/ground_emit.png",
                "assets/textures/ground_diff.png",
                "assets/textures/ground_spec.png")

        load_models_assign_textures(vertexAttributes, score_bar,
                "assets/models/score_bar.obj",
                "assets/textures/white.png",
                "assets/textures/white.png",
                "assets/textures/white.png")


        load_models_assign_textures(vertexAttributes, wallUp,
                "assets/models/wall.obj",
                "assets/textures/wall_emit.png",
                "assets/textures/wall_diff.png",
                "assets/textures/wall_spec.png")

        load_models_assign_textures(vertexAttributes, wallDown,
                "assets/models/wall.obj",
                "assets/textures/wall_emit.png",
                "assets/textures/wall_diff.png",
                "assets/textures/wall_spec.png")

        //Setting Lighting
        pointLight_ball = Pointlight(camera.getWorldPosition(), Vector3f(1f,1f,1f), Vector3f(1.0f,0.6f,0.1f))
        pointLight_player1 = Pointlight(camera.getWorldPosition(), Vector3f(1f,0f,0f), Vector3f(1.0f,0.4f,0.1f))
        pointLight_player2 = Pointlight(camera.getWorldPosition(), Vector3f(0f,0f,1f), Vector3f(1.0f,0.4f,0.1f))

        spotLight = Spotlight(Vector3f(0.0f, 30.0f, 60.0f), Vector3f(70.0f))
    }

    private fun transformations() {
        pointLight_ball.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        pointLight_player1.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        pointLight_player2.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        spotLight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(),0.0f) //rotateLocal(Math.toRadians(-45.0f), Math.PI.toFloat(),0.0f)   -10

        pointLight_ball.parent = ball
        pointLight_player1.parent = player1

        player1.scaleLocal(Vector3f(0.8f))
        player1.translateLocal(Vector3f(-12.0f, 0.0f, 0.0f))
        player2.scaleLocal(Vector3f(0.8f))
        player2.translateLocal(Vector3f(12.0f, 0.0f, 0.0f))
        playerAI.scaleLocal(Vector3f(0.8f))
        playerAI.translateLocal(Vector3f(12.0f, 0.0f, 0.0f))

        ball.scaleLocal(Vector3f(0.4f))
        ball.translateLocal(Vector3f(0.0f, 0.0f, 0.0f))
        wallDown.translateLocal(Vector3f(0.0f, 0.0f, 7.0f))
        wallUp.translateLocal(Vector3f(0.0f, 0.0f, -7.0f))
        wallDown.scaleLocal(Vector3f(7f,1f,1f))
        wallUp.scaleLocal(Vector3f(7f,1f,1f))
        item.scaleLocal(Vector3f(0.4f))
        item.translateLocal(Vector3f(0f, 1.0f, 0f))

        text_p1_won.translateLocal(Vector3f(0.0f, -2f, 0.0f))
        text_p2_won.translateLocal(Vector3f(0.0f, -2f, 0.0f))

        score_bar.translateLocal(Vector3f(0.5f, 0f, 0.0f))
        score_p1.translateLocal(Vector3f(0.5f, 0f, 0.0f))
        score_p2.translateLocal(Vector3f(0.5f, 0f, 0.0f))
        text_score.translateLocal(Vector3f(0.5f, 0f, 0.0f))

        camera.rotateLocal(Math.toRadians(-90.0f), 0.0f, 0.0f)
        camera.translateLocal(Vector3f(0.0f,0.0f,8.0f))
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {
        val deltaX: Double = xpos - oldMousePosX
        val deltaY: Double = ypos - oldMousePosY

        oldMousePosX = xpos
        oldMousePosY = ypos

        if (bool) {
            //camera.rotateAroundPoint(Math.toRadians(deltaY.toFloat() * 0.05f),0.0f,0.0f, Vector3f(0.0f))
            //camera.rotateAroundPoint(0.0f, Math.toRadians(deltaX.toFloat() * 0.05f), 0.0f, Vector3f(0.0f))
        }
        bool = true
    }

    fun cleanup() {}
}
