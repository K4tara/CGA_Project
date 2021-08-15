package cga.exercise.game

import cga.exercise.components.camera.PongCamera
import cga.exercise.components.geometry.*
import cga.exercise.components.light.Pointlight
import cga.exercise.components.light.Spotlight
import cga.exercise.components.shader.ShaderProgram
import cga.exercise.components.texture.Texture2D
import cga.framework.GLError
import cga.framework.GameWindow
import cga.framework.ModelLoader
import cga.framework.OBJLoader
import org.joml.Vector3f
import org.joml.Math
import org.joml.Vector2f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.glfw.GLFW.*
import java.lang.IllegalArgumentException
import java.lang.Math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class Scene(private val window: GameWindow) {

    //Shaders
    private val classicStaticShader: ShaderProgram
    private val staticCelShader: ShaderProgram
    private val effectShader: ShaderProgram
    private var useShader: ShaderProgram

    //Game variables
    var player1Score = 0
    var player2Score = 0
    private var rolling = true
    private var itemX = 0f
    private var itemZ = 0f
    private var itemBoundsX = 18
    private var itemBoundsZ = 12
    private var bouncesTillItemSpawn = 5
    private var paddleReverseCount = 0
    private var itemSpawn = false
    private var placeItem = true
    private var effectNumber = 0
    private var effectTime = 15f
    private var chaos = 0
    private var confuse = 0
    private var shake = 0

    /* TODO:
          2. Einbindung weiterer Texturen
    */
    //Objects + Camera
    private var mesh4: Mesh
    private var ball2_mesh: Mesh
    private var player1_mesh: Mesh
    private var player2_mesh: Mesh
    private var item_mesh: Mesh
    private var wall_mesh: Mesh
    private var wallUp_mesh: Mesh
    private var wallDown_mesh: Mesh

    private var ground = Renderable()
    private var ball2 = Renderable()
    private var player1 = Renderable()
    private var player2 = Renderable()
    private var item = Renderable()
    private var wall = Renderable()
    private var wallUp = Renderable()
    private var wallDown = Renderable()
    private var camera = PongCamera()

    /* TODO:
        3. Angemessene Modelle für Spieler, Ball, Pickups und Hintergrund raussuchen, anpassen und einbinden
    */
    //Models
   // private var player1 = ModelLoader.loadModel("assets/models/player.obj",Math.toRadians(-90.0f), Math.toRadians(90.0f),0.0f) ?: throw IllegalArgumentException("loading failed")
    // private var player2 = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),Math.toRadians(90.0f),0.0f) ?: throw IllegalArgumentException("loading failed")
    private var playerAI = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),Math.toRadians(90.0f),0.0f) ?: throw IllegalArgumentException("loading failed")
    //private var wallDown = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    //private var wallUp = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    private var ball = ModelLoader.loadModel("assets/models/sphere.obj",0.0f,0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    // private var item = ModelLoader.loadModel("assets/models/sphere.obj",0.0f,0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    /* TODO:
        4. Anpasssung möglicher weiterer Lichtquellen bspw. mit Fokus auf Ball oder Spieler
    */

    //Lighting
    private var pointLight = Pointlight(Vector3f(),Vector3f())
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
    private var speed_player = 9.0f;
    private var bounds_player_z = 5.5f;
    private var inBounds1 = false
    private var inBounds2 = false
    private var inBounds1_1 = false
    private var inBounds2_1 = false
    private var keyW = false
    private var keyS = false
    private var keyUp = false
    private var keyDown = false
    private var speedZ = 0
    private var speedX = -20
    private var maxSpeedZ = 11
    private var maxSpeedX = 10
    private var speed_ai_player = 0f
    private var max_speed_ai_player = 7f


    //scene setup
    init {

        classicStaticShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")
        staticCelShader = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/effect_frag.glsl") //Toon effect (Cel shading)
        effectShader = ShaderProgram("assets/shaders/effect3_vert.glsl", "assets/shaders/effect3_frag.glsl") //all effects in one shader

        useShader = classicStaticShader

        //initial opengl state
        glClearColor(0f, 0f, 0f, 1.0f); GLError.checkThrow()
        glEnable(GL_CULL_FACE); GLError.checkThrow()
        glFrontFace(GL_CCW); GLError.checkThrow()
        glCullFace(GL_BACK); GLError.checkThrow()
        glEnable(GL_DEPTH_TEST); GLError.checkThrow()
        glDepthFunc(GL_LESS); GLError.checkThrow()

        //Attributes
        //val textstride: Int = 8 * 4
        //val textattrPos = VertexAttribute(0,2, GL_FLOAT, false, textstride, 0)
        //val textattrTC = VertexAttribute(1,2, GL_FLOAT, false, textstride, 2 * 4)
        //val textvertexAttributes = arrayOf(textattrPos, textattrTC)

        val stride: Int = 8 * 4
        val attrPos = VertexAttribute(0,3, GL_FLOAT, false, stride, 0)
        val attrTC = VertexAttribute(1,2, GL_FLOAT, false, stride, 3 * 4)
        val attrNorm = VertexAttribute(2,3, GL_FLOAT, false, stride, 5 * 4)
        val vertexAttributes = arrayOf(attrPos, attrTC, attrNorm)

        //Ground + ball
        val res2: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/ground.obj")
        val objMesh2: OBJLoader.OBJMesh = res2.objects[0].meshes[0]

        val ball2_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/test/untitled.obj")
        val ball2_obj_mesh: OBJLoader.OBJMesh = ball2_obj.objects[0].meshes[0]

        val player1_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/player.obj")
        val player1_obj_mesh: OBJLoader.OBJMesh = player1_obj.objects[0].meshes[0]

        val player2_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/player.obj")
        val player2_obj_mesh: OBJLoader.OBJMesh = player2_obj.objects[0].meshes[0]

        val item_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/item.obj")
        val item_obj_mesh: OBJLoader.OBJMesh = item_obj.objects[0].meshes[0]

        val wall_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/wall.obj")
        val wall_obj_mesh: OBJLoader.OBJMesh = wall_obj.objects[0].meshes[0]

        val wallUp_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/wall.obj")
        val wallUp_obj_mesh: OBJLoader.OBJMesh = wallUp_obj.objects[0].meshes[0]

        val wallDown_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/wall.obj")
        val wallDown_obj_mesh: OBJLoader.OBJMesh = wallDown_obj.objects[0].meshes[0]

        //Material Ground
        val texture_emit = Texture2D("assets/textures/grass_emit.png",true)
        val texture_diff = Texture2D("assets/textures/grass_diff.png",true)
        val texture_spec = Texture2D("assets/textures/grass_spec.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val groundMaterial = Material(texture_diff, texture_emit, texture_spec,90.0f, Vector2f(64.0f,64.0f))

        //Material ball
        val texture_emit2 = Texture2D("assets/textures/ball.png",true)
        val texture_diff2 = Texture2D("assets/textures/ball.png",true)
        val texture_spec2 = Texture2D("assets/textures/ball.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val ballMaterial = Material(texture_diff2, texture_emit2, texture_spec2,90.0f, Vector2f(64.0f,64.0f))

        //Material Player1
        val texture_emit3 = Texture2D("assets/textures/player1.png",true)
        val texture_diff3 = Texture2D("assets/textures/player1.png",true)
        val texture_spec3 = Texture2D("assets/textures/player1.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val player1Material = Material(texture_diff3, texture_emit3, texture_spec3,30.0f, Vector2f(64.0f,64.0f))

        //Material Player2
        val texture_emit4 = Texture2D("assets/textures/player2.png",true)
        val texture_diff4 = Texture2D("assets/textures/grass_diff.png",true)
        val texture_spec4 = Texture2D("assets/textures/grass_spec.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val player2Material = Material(texture_diff4, texture_emit4, texture_spec4,30.0f, Vector2f(64.0f,64.0f))

        //Material Item
        val texture_emit6 = Texture2D("assets/textures/item.png",true)
        val texture_diff6 = Texture2D("assets/textures/item.png",true)
        val texture_spec6 = Texture2D("assets/textures/item.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val itemMaterial = Material(texture_diff6, texture_emit6, texture_spec6,30.0f, Vector2f(64.0f,64.0f))

        //Material Wall
        val texture_emit5 = Texture2D("assets/textures/wall.png",true)
        val texture_diff5 = Texture2D("assets/textures/wall.png",true)
        val texture_spec5 = Texture2D("assets/textures/wall.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val wallMaterial = Material(texture_diff5, texture_emit5, texture_spec5,90.0f, Vector2f(64.0f,64.0f))

        //Assign Meshes
        mesh4 = Mesh(objMesh2.vertexData, objMesh2.indexData, vertexAttributes, groundMaterial)
        ground.list.add(mesh4)

        ball2_mesh = Mesh(ball2_obj_mesh.vertexData, ball2_obj_mesh.indexData, vertexAttributes, ballMaterial)
        ball2.list.add(ball2_mesh)

        player1_mesh = Mesh(player1_obj_mesh.vertexData, player1_obj_mesh.indexData, vertexAttributes, player1Material)
        player1.list.add(player1_mesh)

        player2_mesh = Mesh(player2_obj_mesh.vertexData, player2_obj_mesh.indexData, vertexAttributes, player2Material)
        player2.list.add(player2_mesh)

        item_mesh = Mesh( item_obj_mesh.vertexData,  item_obj_mesh.indexData, vertexAttributes,  itemMaterial)
        item.list.add(item_mesh)

        wall_mesh = Mesh(wall_obj_mesh.vertexData, wall_obj_mesh.indexData, vertexAttributes, wallMaterial)
        wall.list.add(wall_mesh)

        wallUp_mesh = Mesh(wall_obj_mesh.vertexData, wall_obj_mesh.indexData, vertexAttributes, wallMaterial)
        wallUp.list.add(wallUp_mesh)

        wallDown_mesh = Mesh(wall_obj_mesh.vertexData, wall_obj_mesh.indexData, vertexAttributes, wallMaterial)
        wallDown.list.add(wallDown_mesh)


        //Lighting
        pointLight = Pointlight(camera.getWorldPosition(), Vector3f(1f,1f,0f))
        spotLight = Spotlight(Vector3f(0.0f, 1.0f, 30.0f), Vector3f(70.0f))

        //Transformations
        player1.scaleLocal(Vector3f(0.8f))
        player1.translateLocal(Vector3f(-12.0f, 0.0f, 0.0f))
        player2.scaleLocal(Vector3f(0.8f))
        player2.translateLocal(Vector3f(12.0f, 0.0f, 0.0f))
        playerAI.scaleLocal(Vector3f(0.8f))
        playerAI.translateLocal(Vector3f(12.0f, 0.0f, 0.0f))

        ball.scaleLocal(Vector3f(0.8f))
        ball.translateLocal(Vector3f(0.0f, 0.0f, 0.0f))
        ball2.scaleLocal(Vector3f(0.4f))
        ball2.translateLocal(Vector3f(0.0f, 0.0f, 0.0f))
        wallDown.translateLocal(Vector3f(0.0f, 0.0f, 7.0f))
        wallUp.translateLocal(Vector3f(0.0f, 0.0f, -7.0f))
        wallDown.scaleLocal(Vector3f(7f,1f,1f))
        wallUp.scaleLocal(Vector3f(7f,1f,1f))
        item.scaleLocal(Vector3f(0.4f))

        camera.rotateLocal(Math.toRadians(-90.0f), 0.0f, 0.0f)
        camera.translateLocal(Vector3f(0.0f,0.0f,8.0f))

        pointLight.translateLocal(Vector3f(0.0f,4.0f,0.0f))
        spotLight.rotateLocal(Math.toRadians(-10.0f), Math.PI.toFloat(),0.0f)

        //Parents
        pointLight.parent = ball2
        //spotLight.parent = ball2
        //camera.parent = cycle

    }

    fun render(dt: Float, t: Float) {

        glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

        useShader.use()
        useShader.setUniform("sceneColour", Vector3f(1.0f,1.0f,1.0f))
        useShader.setUniform("time",t)
        useShader.setUniform("chaos", chaos) //LSD effect
        useShader.setUniform("confuse", confuse) //inverted colors & TODO: player movement
        useShader.setUniform("shake", shake) //earthquake + light switching

        camera.bind(useShader)
        ground.render(useShader)
        wallDown.render(useShader)
        wallUp.render(useShader)
        player1.render(useShader)
        player2.render(useShader)

        if (paddleReverseCount >= bouncesTillItemSpawn) {
            itemSpawn = true
            item.render(useShader)
        }

        ball2.render(useShader)

        pointLight.bind(useShader,"cyclePoint")
        spotLight.bind(useShader,"cycleSpot", camera.getCalculateViewMatrix())
    }

    fun update(dt: Float, t: Float) {

        pointLight.lightCol = Vector3f(abs(sin(t/1)),abs(sin(t/3)),abs(sin(t/2)))

        player_movement(dt)
        ball_movement(dt)
        camera_switch(dt,t)
        winner()
        //playerAI(dt,t)
        controlBallspeed()

        if (itemSpawn && placeItem) {
            placeItem()
        }

        //end effects
        if (effectNumber > 0) {
            countDownEffect(dt)
        }
    }

    private fun player_movement(dt: Float) {
        inBounds1 = player1.getPosition().z >= -bounds_player_z
        inBounds1_1 = player1.getPosition().z <= bounds_player_z
        inBounds2 = player2.getPosition().z >= -bounds_player_z
        inBounds2_1 = player2.getPosition().z <= bounds_player_z

        keyW = window.getKeyState(GLFW_KEY_W)
        keyS = window.getKeyState(GLFW_KEY_S)
        keyUp = window.getKeyState(GLFW_KEY_UP)
        keyDown = window.getKeyState(GLFW_KEY_DOWN)


        if (keyW && inBounds1) { //player1.getPosition().z >= -bounds_player_z) {
            player1.translateLocal(Vector3f(0.0f, 0.0f, -speed_player * dt))
        }

        if (keyS && inBounds1_1) { //player1.getPosition().z <= bounds_player_z) {
            player1.translateLocal(Vector3f(0.0f, 0.0f, speed_player  * dt))
        }

        if (keyUp && inBounds2) { //player2.getPosition().z >= -bounds_player_z) {
            player2.translateLocal(Vector3f(0.0f, 0.0f, -speed_player  * dt))
        }

        if (keyDown && inBounds2_1) { //player2.getPosition().z <= bounds_player_z) {
            player2.translateLocal(Vector3f(0.0f, 0.0f, speed_player  * dt))
        }
    }

    private fun ball_movement(dt: Float){

        if (rolling) {
            ball2.translateLocal(Vector3f(speedX * dt,0.0f,speedZ * dt))
        }

        //check for intersection (cheat mode)
        //-> jede Seite des Objekts auf Überschneidung überprüfen (Zahlen = Hälfte der Breite der Objekte)
        if (ball2.getPosition().x <= player1.getPosition().x+0.5 && ball2.getPosition().x >= player1.getPosition().x-0.5 &&
                ball2.getPosition().z+0.5 <= player1.getPosition().z+2 && ball2.getPosition().z-0.5 >= player1.getPosition().z-2) {
            reverse(player1)
        }

        if (ball2.getPosition().x >= player2.getPosition().x-0.5 && ball2.getPosition().x <= player2.getPosition().x+0.5 &&
                ball2.getPosition().z+0.5 <= player2.getPosition().z+2 && ball2.getPosition().z-0.5 >= player2.getPosition().z-2) {
            reverse(player2)
        }

        //item intersection (großzügig gewählte Parameter -> man soll das Item auch treffen können)
        if (itemSpawn &&
                item.getPosition().x >= ball2.getPosition().x-0.5 && item.getPosition().x <= ball2.getPosition().x+0.5 &&
                item.getPosition().z+0.5 <= ball2.getPosition().z+1 && item.getPosition().z-0.5 >= ball2.getPosition().z-1) {

            println("item activated!")
            itemSpawn = false
            paddleReverseCount = -3
            placeItem = true
            effect()
        }

        //bei den Wänden interessiert uns nur eine Seite
        if (ball2.getPosition().z >= (wallDown.getPosition().z)-0.5) {
            reverse(wallDown)
        }

        if (ball2.getPosition().z <= (wallUp.getPosition().z)+0.5) {
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
            paddleReverseCount++

            val o_center = (7 + obj.getPosition().z) + (4 / 2) // 7 & 9 = z = Abstand zum unteren Spielfeldrand
            val b_center = (9 + ball2.getPosition().z) + (1 / 2) // (4/2) & (1/2) = Hälfte der Länge der Objekte

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
        paddleReverseCount = 0
        itemSpawn = false
        endEffect()

        ball2.translateLocal(Vector3f(-posX*1.668f, 0.0f, -posZ*1.668f)) //keine Ahnung wieso dieser Faktor..
        rolling = true
    }

    private fun winner() {
        if (ball2.getPosition().x > player2.getPosition().x+2) {
            player1Score++
            println("SCORE  $player1Score : $player2Score")
            val posX = ball2.getPosition().x
            val posZ = ball2.getPosition().z
            resetGame(posX,posZ)
        }

        if (ball2.getPosition().x < player1.getPosition().x-2) {
            player2Score++
            println("SCORE  $player1Score : $player2Score")
            val posX = ball2.getPosition().x
            val posZ = ball2.getPosition().z
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
                //speed_player *= -1
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
                //speed_player *= -1
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
            speedX = 6
        } else {
            speedX = -6
        }

        speedZ / 1.4
    }

    private fun playerAI(dt: Float, t: Float) {
        val p_center = (8 + playerAI.getPosition().z) + (4/2)
        val b_center = (9 + ball2.getPosition().z) + (2/2)
        var move = ((b_center - p_center)*1).toInt()

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
        if (ball2.getPosition().x >= playerAI.getPosition().x-0.5 && ball2.getPosition().x <= playerAI.getPosition().x+0.5 &&
                ball2.getPosition().z+0.5 <= playerAI.getPosition().z+2 && ball2.getPosition().z-0.5 >= playerAI.getPosition().z-2) {
            reverse(playerAI)
        }
    }

    private fun countDownEffect(dt: Float) {
        if (effectTime > 0f) {
            effectTime -= dt

            if (effectTime <= 0f){
                endEffect()
                effectTime = 15f
            }
        }
    }
}
