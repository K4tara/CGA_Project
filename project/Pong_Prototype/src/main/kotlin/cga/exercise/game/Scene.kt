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
import java.io.File
import java.lang.Math.abs
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip

import kotlin.math.sin
import kotlin.random.Random

class Scene(private val window: GameWindow) {

    //Shaders
    private val classicStaticShader: ShaderProgram
    //private val staticCelShader: ShaderProgram
    private val effectShader: ShaderProgram
    private var useShader: ShaderProgram
    private val skyboxShader: ShaderProgram

    private val skybox: Skybox

    //Game variables
    var player1Score = 0
    var player2Score = 0
    private var swapControls = false
    private var rolling = true
    private var singlePlayer = false
    private var resetFactor = 1.668f
    private var itemX = 0f
    private var itemZ = 0f
    private var itemBoundsX = 18
    private var itemBoundsZ = 12
    private var itemSpawn = false
    private var placeItem = true
    private var effectNumber = 0
    private var effectTime = 0f
    private var spawnTime = 0f
    private val EFFECT_TIME = 14.5f
    private val SPAWN_TIME = 7f
    private var chaos = 0
    private var confuse = 0
    private var shake = 0

    //Objects + Camera
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
    private var camera = PongCamera()

    /* TODO:
        3. Hintergrund raussuchen, anpassen und einbinden
    */

    /* TODO:
        4. Anpasssung möglicher weiterer Lichtquellen bspw. mit Fokus auf Ball oder Spieler
    */

    //Lighting
    private var pointLight_ball = Pointlight(Vector3f(),Vector3f())
    private var pointLight_player1 = Pointlight(Vector3f(),Vector3f())
    private var pointLight_player2 = Pointlight(Vector3f(),Vector3f())
    private var spotLight = Spotlight(Vector3f(),Vector3f())

    //Mouse movement, Camera
    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var bool: Boolean = false
    private var view1: Boolean = false
    private var view2: Boolean = false
    private var view3: Boolean = false
    private var viewActive = 1 //welche Ansicht ist gerade aktiv (1,2 oder 3)

    //Player + ball movement parameters
    private var speed_player = 9.0f
    private var bounds_player_z = 5.5f
    private var inBounds1ZUp = false
    private var inBounds2ZUp = false
    private var inBounds1ZDown = false
    private var inBounds2ZDown = false
    private var keyW = false
    private var keyS = false
    private var keyUp = false
    private var keyDown = false
    private var pause = true
    private var speedZ = 0
    private var speedX = 0
    private var START_SPEEDX = 14
    private var maxSpeedZ = 20
    private var maxSpeedX = 22
    private var speed_ai_player = 0f
    private var max_speed_ai_player = 8.5f

    private var sound_1 : Clip
    private var sound_2 : Clip
    var audioInputStream2 : AudioInputStream = AudioSystem.getAudioInputStream(File("assets/sounds/pong.wav"))


    //Scene setup
    init {
        classicStaticShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")
        //staticCelShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/effect_frag.glsl") //Toon effect (Cel shading)
        effectShader = ShaderProgram("assets/shaders/effect3_vert.glsl", "assets/shaders/effect3_frag.glsl") //all effects in one shader
        skyboxShader = ShaderProgram("assets/shaders/skybox_vert.glsl", "assets/shaders/skybox_frag.glsl")

        useShader = classicStaticShader
        effectTime = EFFECT_TIME
        spawnTime = SPAWN_TIME
        speedX = -START_SPEEDX

        skybox = Skybox()
        val skyboxFaces = listOf(
                "assets/textures/Skybox/right.jpg",
                "assets/textures/Skybox/left.jpg",
                "assets/textures/Skybox/top.jpg",
                "assets/textures/Skybox/bottom.jpg",
                "assets/textures/Skybox/front.jpg",
                "assets/textures/Skybox/back.jpg"
        )
        skybox.setupVaoVbo()
        skybox.loadCubemap(skyboxFaces)

        //Set Background Sound
        var audioInputStream : AudioInputStream = AudioSystem.getAudioInputStream(File("assets/sounds/pong_atmo.wav"))

        sound_1 = AudioSystem.getClip()
        sound_1.open(audioInputStream)
        sound_1.start()
        sound_1.loop(Clip.LOOP_CONTINUOUSLY)

        sound_2 = AudioSystem.getClip()
        sound_2.open(audioInputStream2)

        //initial opengl state
        glClearColor(0f, 0f, 0f, 1.0f); GLError.checkThrow()
        glEnable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW); GLError.checkThrow()
        glCullFace(GL_BACK); GLError.checkThrow()
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

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
            "assets/textures/item_emit.png",
            "assets/textures/item_diff.png",
            "assets/textures/item_spec.png")

        load_models_assign_textures(vertexAttributes, text_score,
            "assets/models/score_text.obj",
            "assets/textures/item_emit.png",
            "assets/textures/item_diff.png",
            "assets/textures/item_spec.png")

        load_models_assign_textures(vertexAttributes, text_p1_won,
            "assets/models/text_p1_won.obj",
            "assets/textures/item_emit.png",
            "assets/textures/item_diff.png",
            "assets/textures/item_spec.png")

        load_models_assign_textures(vertexAttributes, text_p2_won,
            "assets/models/text_p2_won.obj",
            "assets/textures/item_emit.png",
            "assets/textures/item_diff.png",
            "assets/textures/item_spec.png")

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
            "assets/textures/item_emit.png",
            "assets/textures/item_diff.png",
            "assets/textures/item_spec.png")


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
        pointLight_ball = Pointlight(camera.getWorldPosition(), Vector3f(1f,1f,1f))
        pointLight_player1 = Pointlight(camera.getWorldPosition(), Vector3f(1f,0f,0f), Vector3f(1.0f,0.4f,0.1f))
        pointLight_player2 = Pointlight(camera.getWorldPosition(), Vector3f(0f,1f,0f), Vector3f(1.0f,0.4f,0.1f))

        spotLight = Spotlight(Vector3f(0.0f, 30.0f, 60.0f), Vector3f(70.0f))

        pointLight_ball.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        pointLight_player1.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        pointLight_player2.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        spotLight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(),0.0f) //rotateLocal(Math.toRadians(-45.0f), Math.PI.toFloat(),0.0f)   -10

        pointLight_ball.parent = ball
        pointLight_player1.parent = player1
        pointLight_player2.parent = player2
        //spotLight.parent = ball2
        //camera.parent = cycle

        //Transformations
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
        item.translateLocal(Vector3f(0.0f, 1.0f, 0.0f))

        text_p1_won.translateLocal(Vector3f(0.0f, -2f, 0.0f))
        text_p2_won.translateLocal(Vector3f(0.0f, -2f, 0.0f))

        camera.rotateLocal(Math.toRadians(-90.0f), 0.0f, 0.0f)
        camera.translateLocal(Vector3f(0.0f,0.0f,8.0f))
    }


    fun update(dt: Float, t: Float) {

        start_game(dt)
        restart_game(dt)
        player_movement(dt)

        if (!pause) ball_movement(dt)

        camera_switch(dt,t)
        changeMode()
        controlBallspeed()
        winner()

        if (singlePlayer) {
            playerAI(dt,t)
        }

        item.rotateLocal(Math.toRadians(0.0f), 0.01f, -0.0f)

        //end effects after some time
        if (effectNumber > 0) {
            countDownEffect(dt)
        }

        //place item if there´s no effect and if new item should be placed
        if (effectNumber == 0 && placeItem) {
            countDownSpawn(dt)
        }
    }

    fun render(dt: Float, t: Float) {
        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        useShader.use()
        useShader.setUniform("sceneColour", Vector3f(1.0f,1.0f,1.0f))
        useShader.setUniform("time",t)
        useShader.setUniform("chaos", chaos) //LSD effect
        useShader.setUniform("confuse", confuse) //inverted colors & player movement
        useShader.setUniform("shake", shake) //earthquake + light switching

        camera.bind(useShader)
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

        if (singlePlayer) {
            playerAI.render(useShader)
        } else {
            player2.render(useShader)
        }

        if (itemSpawn) {
            item.render(useShader)
        }

        ball.render(useShader)

        pointLight_player1.bind(useShader,"cyclePoint2")
        pointLight_player2.bind(useShader,"cyclePoint3")
        pointLight_ball.bind(useShader,"cyclePoint")
        spotLight.bind(useShader,"cycleSpot", camera.getCalculateViewMatrix())

        skybox.render(skyboxShader, camera)
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

    private fun start_game(dt: Float) {

        if (window.getKeyState(GLFW_KEY_ENTER) && pause) {
            text.translateLocal(Vector3f(0.0f, -5.0f, 0.0f))
            pause = false
            text_score.translateLocal(Vector3f(0.0f, 0.01f, 0.0f))
        }
    }

    private fun restart_game(dt: Float){

        if(window.getKeyState(GLFW_KEY_R)){

            pause = false
            text.translateLocal(Vector3f(0.0f, -5.0f, 0.0f))

            if(player1Score==1){
                score_p2.translateLocal(Vector3f(0.2f, 0.0f,0.0f))
                player1Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player1Score==2){
                score_p2.translateLocal(Vector3f(0.6f, 0.0f,0.0f))
                player1Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player1Score==3){
                score_p2.translateLocal(Vector3f(1.2f, 0.0f,0.0f))
                player1Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player1Score==4){
                score_p2.translateLocal(Vector3f(2.0f, 0.0f,0.0f))
                player1Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player1Score==5){
                score_p2.translateLocal(Vector3f(2.9f, 0.0f,0.0f))
                text_p1_won.translateLocal(Vector3f(0.0f, -2f, 0.0f))
                player1Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }

            if(player2Score==1){
                score_p1.translateLocal(Vector3f(0.2f, 0.0f,0.0f))
                player2Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player2Score==2){
                score_p1.translateLocal(Vector3f(0.6f, 0.0f,0.0f))
                player2Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player2Score==3){
                score_p1.translateLocal(Vector3f(1.2f, 0.0f,0.0f))
                player2Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player2Score==4){
                score_p1.translateLocal(Vector3f(2.0f, 0.0f,0.0f))
                player2Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }
            if(player2Score==5){
                score_p1.translateLocal(Vector3f(2.9f, 0.0f,0.0f))
                text_p2_won.translateLocal(Vector3f(0.0f, -2f, 0.0f))
                player2Score = 0
                println("RESTART")
                println("SCORE  $player1Score : $player2Score")
            }

        }
    }

    private fun player_movement(dt: Float) {

        inBounds1ZUp = player1.getPosition().z >= -bounds_player_z
        inBounds1ZDown = player1.getPosition().z <= bounds_player_z
        inBounds2ZUp = player2.getPosition().z >= -bounds_player_z
        inBounds2ZDown = player2.getPosition().z <= bounds_player_z

        if (swapControls) {
            keyS = window.getKeyState(GLFW_KEY_W)
            keyW = window.getKeyState(GLFW_KEY_S)
            keyDown = window.getKeyState(GLFW_KEY_UP)
            keyUp = window.getKeyState(GLFW_KEY_DOWN)
        } else {
            keyW = window.getKeyState(GLFW_KEY_W)
            keyS = window.getKeyState(GLFW_KEY_S)
            keyUp = window.getKeyState(GLFW_KEY_UP)
            keyDown = window.getKeyState(GLFW_KEY_DOWN)
        }

        if (keyW && inBounds1ZUp) { // player1.getPosition().z >= -bounds_player_z) {
            player1.translateLocal(Vector3f(0.0f, 0.0f, -speed_player * dt))
        }

        if (keyS && inBounds1ZDown) { // player1.getPosition().z <= bounds_player_z) {
            player1.translateLocal(Vector3f(0.0f, 0.0f, speed_player  * dt))
        }

        if (!singlePlayer) {
            if (keyUp && inBounds2ZUp) { // player2.getPosition().z >= -bounds_player_z) {
                player2.translateLocal(Vector3f(0.0f, 0.0f, -speed_player  * dt))
            }

            if (keyDown && inBounds2ZDown) { // player2.getPosition().z <= bounds_player_z) {
                player2.translateLocal(Vector3f(0.0f, 0.0f, speed_player  * dt))
            }
        }
    }

    private fun ball_movement(dt: Float){

        if (rolling) {
            ball.translateLocal(Vector3f(speedX * dt,0.0f,speedZ * dt))
        }

        //check for intersection (cheat mode)
        //-> jede Seite des Objekts auf Überschneidung überprüfen (Zahlen = Hälfte der Breite der Objekte)
        if (ball.getPosition().x <= player1.getPosition().x+0.5 && ball.getPosition().x >= player1.getPosition().x-0.5 &&
                ball.getPosition().z+0.5 <= player1.getPosition().z+2 && ball.getPosition().z-0.5 >= player1.getPosition().z-2) {
            reverse(player1)
        }

        if (!singlePlayer) {
            if (ball.getPosition().x >= player2.getPosition().x - 0.5 && ball.getPosition().x <= player2.getPosition().x + 0.5 &&
                    ball.getPosition().z + 0.5 <= player2.getPosition().z + 2 && ball.getPosition().z - 0.5 >= player2.getPosition().z - 2) {
                reverse(player2)
            }
        }

        //item intersection (großzügig gewählte Parameter -> man soll das Item auch treffen können)
        if (itemSpawn &&
                item.getPosition().x >= ball.getPosition().x-0.5 && item.getPosition().x <= ball.getPosition().x+0.5 &&
                item.getPosition().z+0.5 <= ball.getPosition().z+1 && item.getPosition().z-0.5 >= ball.getPosition().z-1) {

            println("item activated!")
            itemSpawn = false
            placeItem = true
            effect()
        }

        //bei den Wänden interessiert uns nur eine Seite
        if (ball.getPosition().z >= (wallDown.getPosition().z)-0.5) {
            reverse(wallDown)
        }

        if (ball.getPosition().z <= (wallUp.getPosition().z)+0.5) {
            reverse(wallUp)
        }
    }

    private fun camera_switch(dt: Float, t:Float) {
        //Boolwerte zum sicherstellen, dass die Operationen jeweils nur einmal hintereinander ausgeführt werden
        //wenn in Position 2 oder 3: zuerst zurück nach 1

        //Top/Down-Ansicht (Standard)
        if (window.getKeyState(GLFW_KEY_1)) {
            if (view2 && view1 == false) {
                camera.rotateAroundPoint(Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,-4.0f,-3.0f))
                viewActive = 1
                view1 = true
                view2 = false
            }

            if (view3 && view1 == false) {
                camera.rotateAroundPoint(0f,Math.toRadians(90f),0.0f,Vector3f(0.0f))
                camera.rotateAroundPoint(Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,-2.0f,-5.0f))
                viewActive = 1
                view1 = true
                view3 = false
            }
        }

        //Seitenansicht
        if (window.getKeyState(GLFW_KEY_2) && viewActive == 1) {
            if (view2 == false) {
                camera.rotateAroundPoint(Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,4.0f,3.0f))
                viewActive = 2
                view2 = true
                view1 = false
            }
        }

        //3rd-Person Ansicht (für Spieler 1)
        if (window.getKeyState(GLFW_KEY_3) && viewActive == 1) {
            if (view3 == false) {
                camera.rotateAroundPoint(Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                camera.rotateAroundPoint(0f,Math.toRadians(-90f),0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,2.0f,5.0f))
                viewActive = 3
                view3 = true
                view1 = false
            }
        }
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

    private fun reverse(obj: Renderable) {

        if (obj == wallDown || obj == wallUp) {
            println("WALL_REVERSE")
            speedZ *= -1
        } else {
            println("PADDLE_REVERSE")

            sound_2.start()

            val o_center = (7 + obj.getPosition().z) + (4 / 2) // 7 & 9 = z = Abstand zum unteren Spielfeldrand
            val b_center = (9 + ball.getPosition().z) + (1 / 2) // (4/2) & (1/2) = Hälfte der Länge der Objekte

            speedX *= -1
            speedZ += ((b_center - o_center) * 2).toInt()  // centerBall - centerPaddle; 2 = Konstante zur Erhöhung des Abprallwinkels
        }
    }

    private fun controlBallspeed() {
        if (speedZ > maxSpeedZ) {
            speedZ = maxSpeedZ
        }

        if (speedZ < -maxSpeedZ) {
            speedZ = -maxSpeedZ
        }

        if (speedX > maxSpeedX) {
            speedX = maxSpeedX
        }

        if (speedX < -maxSpeedX) {
            speedX = -maxSpeedX
        }
    }

    private fun resetGame(posX: Float, posZ: Float) {
        rolling = false
        speedZ = 0
        speedX *= -1
        itemSpawn = false
        placeItem = true
        spawnTime = SPAWN_TIME
        endEffect()

        ball.translateLocal(Vector3f(-posX * resetFactor, 0.0f, -posZ * resetFactor))
        rolling = true
    }

    private fun winner() {
        if (ball.getPosition().x > player2.getPosition().x+2) {
            player1Score++

            if(player1Score==1) score_p2.translateLocal(Vector3f(-0.2f, 0.0f,0.0f))
            if(player1Score==2) score_p2.translateLocal(Vector3f(-0.4f, 0.0f,0.0f))
            if(player1Score==3) score_p2.translateLocal(Vector3f(-0.6f, 0.0f,0.0f))
            if(player1Score==4) score_p2.translateLocal(Vector3f(-0.8f, 0.0f,0.0f))
            if(player1Score==5) { score_p2.translateLocal(Vector3f(-0.9f, 0.0f,0.0f))
                                  text_p1_won.translateLocal(Vector3f(0.0f, 2f, 0.0f))
                                  pause = true}

            println("SCORE  $player1Score : $player2Score")
            val posX = ball.getPosition().x
            val posZ = ball.getPosition().z
            resetGame(posX,posZ)
        }

        if (ball.getPosition().x < player1.getPosition().x-2) {
            player2Score++

            if(player2Score==1) score_p1.translateLocal(Vector3f(-0.2f, 0.0f,0.0f))
            if(player2Score==2) score_p1.translateLocal(Vector3f(-0.4f, 0.0f,0.0f))
            if(player2Score==3) score_p1.translateLocal(Vector3f(-0.6f, 0.0f,0.0f))
            if(player2Score==4) score_p1.translateLocal(Vector3f(-0.8f, 0.0f,0.0f))
            if(player2Score==5) { score_p1.translateLocal(Vector3f(-0.9f, 0.0f,0.0f))
                                text_p2_won.translateLocal(Vector3f(0.0f, 2f, 0.0f))
                              pause = true}

            println("SCORE  $player1Score : $player2Score")
            val posX = ball.getPosition().x
            val posZ = ball.getPosition().z
            resetGame(posX,posZ)
        }
    }

    private fun placeItem() {
        itemX = Random.nextInt(-itemBoundsX,itemBoundsX).toFloat()
        itemZ = Random.nextInt(-itemBoundsZ,itemBoundsZ).toFloat()

        //can´t move out of bounds
        if (item.getPosition().z+itemZ >= -6 && item.getPosition().z+itemZ <= 6 &&
                item.getPosition().x+itemX >= -11 && item.getPosition().x+itemX <= 11) {

            item.translateLocal(Vector3f(itemX, 0.0f, itemZ))
            itemSpawn = true
            placeItem = false
            println("new item has appeared!")
            println("Item Position X: ${item.getPosition().x}, Z: ${item.getPosition().z}")
        }
    }

    private fun effect() {
        val effect = Random.nextInt(1,5) //1 bis 4

        when (effect) {
            1 -> {
                useShader = effectShader
                chaos = 1
            }
            2 -> {
                useShader = effectShader
                confuse = 1
                swapControls = true
            }
            3 -> {
                useShader = effectShader
                shake = 1
            }
            4 -> speedUp()
        }

        effectNumber = effect
    }

    private fun endEffect() {
        when (effectNumber) {
            1 -> {
                useShader = classicStaticShader
                chaos = 0
            }
            2 -> {
                useShader = classicStaticShader
                confuse = 0
                swapControls = false
            }
            3 -> {
                useShader = classicStaticShader
                shake = 0
            }
            4 -> slowDown()
        }

        effectNumber = 0
    }

    private fun speedUp() {
        if (speedX > 0) {
            speedX = maxSpeedX
        } else {
            speedX = -maxSpeedX
        }

        speedZ * 1.4
    }

    private fun slowDown() {
        if (speedX > 0) {
            speedX = START_SPEEDX
        } else {
            speedX = -START_SPEEDX
        }

        speedZ / 1.4
    }

    private fun playerAI(dt: Float, t: Float) {
        val p_center = (8 + playerAI.getPosition().z) + (4/2)
        val b_center = (9 + ball.getPosition().z) + (2/2)
        var move = ((b_center - p_center)*1.2).toInt()

        if (playerAI.getPosition().z+move >= -bounds_player_z && playerAI.getPosition().z+move <= bounds_player_z) {
            playerAI.translateLocal(Vector3f(0.0f, 0.0f, speed_ai_player  * dt))
        }

        speed_ai_player += move

        if (speed_ai_player > max_speed_ai_player) {
            speed_ai_player = max_speed_ai_player
        }

        if (speed_ai_player < -max_speed_ai_player) {
            speed_ai_player = -max_speed_ai_player
        }

        //AI intersection -> same as player 2
        if (ball.getPosition().x >= playerAI.getPosition().x-0.5 && ball.getPosition().x <= playerAI.getPosition().x+0.5 &&
                ball.getPosition().z+0.5 <= playerAI.getPosition().z+2 && ball.getPosition().z-0.5 >= playerAI.getPosition().z-2) {
            reverse(playerAI)
        }
    }

    private fun changeMode() {
        if (window.getKeyState(GLFW_KEY_F1)) {
            singlePlayer = true
        }

        if (window.getKeyState(GLFW_KEY_F2)) {
            singlePlayer = false
        }

        if (window.getKeyState(GLFW_KEY_F3)) {
            sound_1.start()
            sound_1.loop(Clip.LOOP_CONTINUOUSLY)
            //sound_2.start()
        }

        if (window.getKeyState(GLFW_KEY_F4)) {
            sound_1.stop()
            //sound_2.stop()
        }
    }

    private fun countDownEffect(dt: Float) {
        if (effectTime > 0f) {
            effectTime -= dt

            if (effectTime <= 0f){
                endEffect()
                effectTime = EFFECT_TIME
            }
        }
    }

    private fun countDownSpawn(dt: Float) {
        if (spawnTime > 0f) {
            spawnTime -= dt

            if (spawnTime <= 0f){
                placeItem()
                spawnTime = SPAWN_TIME
            }
        }
    }
}
