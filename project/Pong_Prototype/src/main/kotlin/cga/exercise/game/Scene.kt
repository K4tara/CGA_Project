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
import kotlin.math.sin
import kotlin.random.Random

class Scene(private val window: GameWindow) {

    /* TODO:
        1. Potenzielle Shader/Animationen/Effekte erzeugen um den Look zu verändern bspw. temporär bei einem Pickup
    */
    //Shaders
    private val staticShader: ShaderProgram
    private val staticShader2: ShaderProgram

    //Game variables
    var player1Score = 0
    var player2Score = 0
    private var rolling = true
    private var itemX = 0f
    private var itemZ = 0f
    private var itemBoundsX = 18
    private var itemBoundsZ = 12
    private var bouncesTillItemSpawn = 2
    private var paddleReverseCount = 0
    private var itemSpawn = false
    private var placeItem = true

    /* TODO:
          2. Einbindung weiterer Texturen
    */
    //Objects + Camera
    private var mesh4: Mesh
    private var ball2_mesh: Mesh
    private var ground = Renderable()
    private var ball2 = Renderable()
    private var camera = PongCamera()

    /* TODO:
        3. Angemessene Modelle für Spieler, Ball, Pickups und Hintergrund raussuchen, anpassen und einbinden
    */
    //Models
    private var player1 = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f), Math.toRadians(90.0f),0.0f) ?: throw IllegalArgumentException("loading failed")
    private var player2 = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),Math.toRadians(90.0f),0.0f) ?: throw IllegalArgumentException("loading failed")
    private var playerAI = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),Math.toRadians(90.0f),0.0f) ?: throw IllegalArgumentException("loading failed")
    private var wallDown = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    private var wallUp = ModelLoader.loadModel("assets/Light Cycle/HQ_Movie cycle.obj",Math.toRadians(-90.0f),0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    private var ball = ModelLoader.loadModel("assets/models/sphere.obj",0.0f,0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    private var item = ModelLoader.loadModel("assets/models/sphere.obj",0.0f,0.0f,0.0f) ?: throw IllegalArgumentException("loading failed")
    /* TODO:
        4. Anpasssung möglicher weiterer Lichtquellen bspw. mit Fokus auf Ball oder Spieler
    */

    //Lighting
    private var pointLight = Pointlight(Vector3f(),Vector3f())
    private var spotLight = Spotlight(Vector3f(),Vector3f())

    //Mouse movement
    private var oldMousePosX: Double = -1.0
    private var oldMousePosY: Double = -1.0
    private var bool: Boolean = false

    //Player + ball movement parameters
    private var speed_player = 9.0f;
    private var bounds_player_z = 5.5f;

    private var speedZ = 0
    private var speedX = -6
    private var maxSpeedZ = 8
    private var maxSpeedX = 7
    private var speed_ai_player = 0f
    private var max_speed_ai_player = 7f


    //scene setup
    init {

        staticShader = ShaderProgram("assets/shaders/simple_vert.glsl", "assets/shaders/simple_frag.glsl")
        staticShader2 = ShaderProgram("assets/shaders/tron_vert.glsl", "assets/shaders/tron_frag.glsl")

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
        val ball2_obj: OBJLoader.OBJResult = OBJLoader.loadOBJ("assets/models/sphere.obj")
        val ball2_obj_mesh: OBJLoader.OBJMesh = ball2_obj.objects[0].meshes[0]

        //Material
        val texture_emit = Texture2D("assets/textures/ground_emit.png",true)
        val texture_diff = Texture2D("assets/textures/ground_diff.png",true)
        val texture_spec = Texture2D("assets/textures/ground_spec.png",true)

        texture_emit.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_diff.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)
        texture_spec.setTexParams(GL_REPEAT, GL_REPEAT, GL_LINEAR_MIPMAP_LINEAR, GL_LINEAR)

        val groundMaterial = Material(texture_diff, texture_emit, texture_spec,60.0f, Vector2f(64.0f,64.0f))

        //Groundmesh
        mesh4 = Mesh(objMesh2.vertexData, objMesh2.indexData, vertexAttributes, groundMaterial)
        ground.list.add(mesh4)
        ball2_mesh = Mesh(ball2_obj_mesh.vertexData, ball2_obj_mesh.indexData, vertexAttributes, groundMaterial)
        ball2.list.add(ball2_mesh)

        //Lighting
        pointLight = Pointlight(camera.getWorldPosition(), Vector3f(1f,1f,0f))
        spotLight = Spotlight(Vector3f(0.0f, 1.0f, 12.0f), Vector3f(1.0f))

        //Transformations
        player1.scaleLocal(Vector3f(0.8f))
        player1.translateLocal(Vector3f(-12.0f, 0.0f, 0.0f))
        player2.scaleLocal(Vector3f(0.8f))
        player2.translateLocal(Vector3f(12.0f, 0.0f, 0.0f))
        playerAI.scaleLocal(Vector3f(0.8f))
        playerAI.translateLocal(Vector3f(12.0f, 0.0f, 0.0f))

        ball.scaleLocal(Vector3f(0.8f))
        ball.translateLocal(Vector3f(0.0f, 0.0f, 0.0f))
        ball2.scaleLocal(Vector3f(0.6f))
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

        staticShader2.use()
        staticShader2.setUniform("sceneColour", Vector3f(1.0f,1.0f,1.0f))

        camera.bind(staticShader2)
        ground.render(staticShader2)

        //staticShader2.setUniform("sceneColour", Vector3f(abs(sin(t/1)),abs(sin(t/3)),abs(sin(t/2))))

        player1.render(staticShader2)
        player2.render(staticShader2)
        ball2.render(staticShader2)
        wallDown.render(staticShader2)
        wallUp.render(staticShader2)

        if (paddleReverseCount >= bouncesTillItemSpawn) {
            itemSpawn = true
            item.render(staticShader2)
        }

        pointLight.bind(staticShader2,"cyclePoint")
        spotLight.bind(staticShader2,"cycleSpot", camera.getCalculateViewMatrix())
    }

    fun update(dt: Float, t: Float) {

        pointLight.lightCol = Vector3f(abs(sin(t/1)),abs(sin(t/3)),abs(sin(t/2)))

        player_movement(dt)
        ball_movement(dt)
        camera_switch(dt)
        winner()

        if (itemSpawn && placeItem) {
            placeItem()
        }
    }


    private fun player_movement(dt: Float) {
        //playerAI.translateLocal(Vector3f(0.0f, 0.0f, speed_ai_player  * dt))
        //AI_chase()

        if (window.getKeyState(GLFW_KEY_W) && player1.getPosition().z >= -bounds_player_z ) {
            player1.translateLocal(Vector3f(0.0f, 0.0f, -speed_player * dt))
        }

        if (window.getKeyState(GLFW_KEY_S) && player1.getPosition().z <= bounds_player_z ) {
            player1.translateLocal(Vector3f(0.0f, 0.0f, speed_player  * dt))
        }

        if (window.getKeyState(GLFW_KEY_UP) && player2.getPosition().z >= -bounds_player_z) {
            player2.translateLocal(Vector3f(0.0f, 0.0f, -speed_player  * dt))
        }

        if (window.getKeyState(GLFW_KEY_DOWN) && player2.getPosition().z <= bounds_player_z ) {
            player2.translateLocal(Vector3f(0.0f, 0.0f, speed_player  * dt))
        }
    }

    private fun ball_movement(dt: Float){

        if (rolling == true) {
            ball2.translateLocal(Vector3f(speedX * dt,0.0f,speedZ * dt))
        }

        //println("X Player1: " + player1.getPosition().x)
        //println("X Player2: " + player2.getPosition().x)
        //println("X Ball: " + ball2.getPosition().x)
        //println("Z Player1: " + player1.getPosition().z)
        //println("Z Player2: " + player2.getPosition().z)
        //println("Z Ball: " + ball2.getPosition().z)
        //println("Z WallUp: " + wallUp.getPosition().z)
        //println("Z WallDown: " + wallDown.getPosition().z)
        //println("X Item: " + item.getPosition().x)
        //println("Z Item: " + item.getPosition().z)

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

        //AI intersection -> same as player 2
        /*if (ball2.getPosition().x >= playerAI.getPosition().x-0.5 && ball2.getPosition().x <= playerAI.getPosition().x+0.5 &&
                ball2.getPosition().z+0.5 <= playerAI.getPosition().z+2 && ball2.getPosition().z-0.5 >= playerAI.getPosition().z-2) {
            reverse(playerAI)
        }*/

        //item intersection (großzügig gewählte Parameter -> man soll das Item auch treffen können)
        if (itemSpawn &&
                item.getPosition().x >= ball2.getPosition().x-0.5 && item.getPosition().x <= ball2.getPosition().x+0.5 &&
                item.getPosition().z+0.5 <= ball2.getPosition().z+1 && item.getPosition().z-0.5 >= ball2.getPosition().z-1) {

            itemSpawn = false
            paddleReverseCount = -2 //next item spawn in bouncesTillItemSpawn +2
            placeItem = true
            println("item activated!")
            //TODO: change shader, effects ...
        }

        //bei den Wänden interessiert uns nur eine Seite
        if (ball2.getPosition().z >= (wallDown.getPosition().z)-0.5) {
            reverse(wallDown)
        }

        if (ball2.getPosition().z <= (wallUp.getPosition().z)+0.5) {
            reverse(wallUp)
        }
    }

    private fun camera_switch(dt: Float){
        /* TODO:
            6. Ein/Ausschalten einer anderen Perspektive bspw. freie Bewegung der Kamera mit der Maus (wäre direkt auch bei Entwicklung hilfreich)
        */
    }

    fun onKey(key: Int, scancode: Int, action: Int, mode: Int) {}

    fun onMouseMove(xpos: Double, ypos: Double) {
        val deltaX: Double = xpos - oldMousePosX
        val deltaY: Double = ypos - oldMousePosY

        oldMousePosX = xpos
        oldMousePosY = ypos

        if (bool) {
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

    private fun AI_chase() {
        val p_center = (7 + playerAI.getPosition().z) + (4/2)
        val b_center = (9 + ball2.getPosition().z) + (1/2)

        speed_ai_player += ((b_center - p_center) * 1.2).toInt()

        if (speed_ai_player > max_speed_ai_player) {
            speed_ai_player = max_speed_ai_player
        }

        if (speed_ai_player < -max_speed_ai_player) {
            speed_ai_player = -max_speed_ai_player
        }
    }

    private fun resetGame(posX: Float, posZ: Float) {
        rolling = false
        speedZ = 0
        speedX *= -1
        paddleReverseCount = 0

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
        if (item.getPosition().z+itemZ > -13 && item.getPosition().z+itemZ < 13 &&
                item.getPosition().x+itemX > -19 && item.getPosition().x+itemX < 19) {

            item.translateLocal(Vector3f(itemX, 0.0f, itemZ))
            placeItem = false
            println("new item has appeared!")
        }

        itemBoundsX--
        itemBoundsZ--
        if (itemBoundsX <= 4) itemBoundsX = 4
        if (itemBoundsZ <= 3) itemBoundsZ = 3
    }

}
