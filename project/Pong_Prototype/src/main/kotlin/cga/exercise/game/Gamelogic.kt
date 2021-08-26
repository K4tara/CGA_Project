package cga.exercise.game

import cga.exercise.components.camera.PongCamera
import cga.exercise.components.geometry.Material
import cga.exercise.components.geometry.Renderable
import cga.exercise.components.texture.Texture2D
import cga.framework.GameWindow
import org.joml.Vector3f
import org.lwjgl.glfw.GLFW
import org.lwjgl.opengl.GL11
import java.io.File
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.Clip
import kotlin.random.Random

class Gamelogic (val window: GameWindow,
                 val camera: PongCamera,
                 val ball: Renderable,
                 val player1: Renderable,
                 val player2: Renderable,
                 val playerBot: Renderable,
                 val item: Renderable,
                 val wallUp: Renderable,
                 val wallDown: Renderable,
                 val text: Renderable,
                 val text_score: Renderable,
                 val text_p1_won: Renderable,
                 val text_p2_won: Renderable,
                 val score_p1: Renderable,
                 val score_p2: Renderable,
                 val score_bar: Renderable) {

    //Game variables
    var player1Score = 0
    var player2Score = 0
    private var swapControls = false
    private var rolling = true
    var singlePlayer = false
    private var resetFactor = 2.49f
    private var itemX = 0f
    private var itemZ = 0f
    private var itemBoundsX = 11
    private var itemBoundsZ = 6
    var itemSpawn = false
    var placeItem = true
    var effectNumber = 0
    private var effectTime = 0f
    private var spawnTime = 0f
    private val EFFECT_TIME = 14.5f
    private val SPAWN_TIME = 7f
    var chaos = 0
    var confuse = 0
    var shake = 0
    private var time = 60f
    private var temp = 0f

    //Player + ball movement parameters
    private var speed_player = 9.0f
    private var bounds_player_z = 5.4f
    private var bounds_playerAI_z = 5.5f
    private var inBounds1ZUp = false
    private var inBounds2ZUp = false
    private var inBounds1ZDown = false
    private var inBounds2ZDown = false
    private var keyW = false
    private var keyS = false
    private var keyUp = false
    private var keyDown = false
    var pause = true
    private var speedZ = 0f
    private var speedX = 0f
    private var speedFactor = 1.1f
    private var START_SPEEDX = 14f
    private var mediSpeedX = START_SPEEDX + 2f
    private var maxSpeedZ = 20f
    private var maxSpeedX = 22f
    private var speed_ai_player = 0f
    private var max_speed_ai_player = 8.5f

    //Physics variables
    private var ballHalfWidth = 1f //2 breit
    private var ballHalfHeight = 1f //2 hoch
    private var playerWidth = 0.8f //1 breit
    private var playerHalfHeight = 2.6f //5 hoch
    private var itemHalfHeight = 0.5f //1 hoch
    private var wallWidth = 0.7f //0.5 breit

    //Camera
    private var view1: Boolean = false
    private var view2: Boolean = false
    private var view3: Boolean = false
    private var viewActive = 1 //welche Ansicht ist gerade aktiv (1,2 oder 3)

    //Texturing
    private val materialWhite: Material
    private val materialBlack: Material

    //Sounds
    private var sound_1: Clip
    private var sound_2: Clip
    private var pongSound = true


    init {
        //game variable setup
        effectTime = EFFECT_TIME
        spawnTime = SPAWN_TIME
        speedX = -START_SPEEDX

        //Set Background Sound
        val audioInputStream : AudioInputStream = AudioSystem.getAudioInputStream(File("assets/sounds/pong_atmo.wav"))
        val audioInputStream2 : AudioInputStream = AudioSystem.getAudioInputStream(File("assets/sounds/pong.wav"))

        sound_1 = AudioSystem.getClip()
        sound_1.open(audioInputStream)
        sound_1.start()
        sound_1.loop(Clip.LOOP_CONTINUOUSLY)
        sound_2 = AudioSystem.getClip()
        sound_2.open(audioInputStream2)

        //texture setup
        val texture_emit = Texture2D("assets/textures/white.png",true)
        val texture_diff = Texture2D("assets/textures/white.png",true)
        val texture_spec = Texture2D("assets/textures/white.png",true)
        texture_emit.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_NEAREST, GL11.GL_NEAREST)
        texture_diff.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR)
        texture_spec.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_NEAREST, GL11.GL_NEAREST)

        val texture_emit2 = Texture2D("assets/textures/black.jpg",true)
        val texture_diff2 = Texture2D("assets/textures/black.jpg",true)
        val texture_spec2 = Texture2D("assets/textures/black.jpg",true)
        texture_emit2.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_NEAREST, GL11.GL_NEAREST)
        texture_diff2.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_LINEAR_MIPMAP_LINEAR, GL11.GL_LINEAR)
        texture_spec2.setTexParams(GL11.GL_REPEAT, GL11.GL_REPEAT, GL11.GL_NEAREST, GL11.GL_NEAREST)

        materialWhite = Material(texture_diff, texture_emit, texture_spec)
        materialBlack = Material(texture_diff2, texture_emit2, texture_spec2)
    }


    fun start_game(dt: Float) {

        if (window.getKeyState(GLFW.GLFW_KEY_ENTER) && pause) {
            text.translateLocal(Vector3f(0.0f, -5.0f, 0.0f))
            pause = false
            text_score.translateLocal(Vector3f(0.0f, 0.01f, 0.0f))
        }
    }

    fun restart_game(dt: Float){

        if(window.getKeyState(GLFW.GLFW_KEY_R)){

            text.translateLocal(Vector3f(0.0f, -5.0f, 0.0f))
            resetGame(ball.getPosition().x, ball.getPosition().z, dt)

            player1.translateLocal(Vector3f(0f,0f, -player1.getPosition().z))
            player2.translateLocal(Vector3f(0f,0f, -player2.getPosition().z))
            playerBot.translateLocal(Vector3f(0f,0f, -playerBot.getPosition().z))

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
            pause = false
        }
    }

    fun player_movement(dt: Float) {

        inBounds1ZUp = player1.getPosition().z >= -bounds_player_z
        inBounds1ZDown = player1.getPosition().z <= bounds_player_z
        inBounds2ZUp = player2.getPosition().z >= -bounds_player_z
        inBounds2ZDown = player2.getPosition().z <= bounds_player_z

        if (swapControls) {
            keyS = window.getKeyState(GLFW.GLFW_KEY_W)
            keyW = window.getKeyState(GLFW.GLFW_KEY_S)
            keyDown = window.getKeyState(GLFW.GLFW_KEY_UP)
            keyUp = window.getKeyState(GLFW.GLFW_KEY_DOWN)
        } else {
            keyW = window.getKeyState(GLFW.GLFW_KEY_W)
            keyS = window.getKeyState(GLFW.GLFW_KEY_S)
            keyUp = window.getKeyState(GLFW.GLFW_KEY_UP)
            keyDown = window.getKeyState(GLFW.GLFW_KEY_DOWN)
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

    fun ball_movement(dt: Float){

        if (rolling) {
            ball.translateLocal(Vector3f(speedX * dt,0.0f,speedZ * dt))
        }

        //check for intersection
        //-> jede Seite des Objekts auf Überschneidung überprüfen
        if (ball.getPosition().x <= player1.getPosition().x + playerWidth
                && ball.getPosition().x >= player1.getPosition().x - playerWidth
                && ball.getPosition().z + ballHalfHeight <= player1.getPosition().z + playerHalfHeight
                && ball.getPosition().z - ballHalfHeight >= player1.getPosition().z - playerHalfHeight) {
            reverse(player1)
        }

        if (!singlePlayer) {
            if (ball.getPosition().x <= player2.getPosition().x + playerWidth
                    && ball.getPosition().x >= player2.getPosition().x - playerWidth
                    && ball.getPosition().z + ballHalfHeight <= player2.getPosition().z + playerHalfHeight
                    && ball.getPosition().z - ballHalfHeight >= player2.getPosition().z - playerHalfHeight) {
                reverse(player2)
            }
        }

        //item intersection (großzügig gewählte Parameter -> man soll das Item auch treffen können)
        if (itemSpawn &&
                item.getPosition().x >= ball.getPosition().x - ballHalfWidth
                && item.getPosition().x <= ball.getPosition().x + ballHalfWidth
                && item.getPosition().z + itemHalfHeight <= ball.getPosition().z + ballHalfHeight
                && item.getPosition().z - itemHalfHeight >= ball.getPosition().z - ballHalfHeight) {

            println("item activated!")
            itemSpawn = false
            placeItem = true
            effect()
        }

        //bei den Wänden interessiert uns nur eine Seite
        if (ball.getPosition().z >= (wallDown.getPosition().z) - wallWidth) {
            reverse(wallDown)
        }

        if (ball.getPosition().z <= (wallUp.getPosition().z) + wallWidth) {
            reverse(wallUp)
        }
    }

    private fun reverse(obj: Renderable) {

        if (obj == wallDown || obj == wallUp) {
            println("WALL_REVERSE")
            speedZ *= -1
        } else {
            println("PADDLE_REVERSE")

            if (pongSound) sound_2.start()

            speedX *= -1
            speedZ += (ball.getPosition().z - obj.getPosition().z) * 1.2f // centerBall - centerPaddle; 1.2 = Konstante zur Erhöhung des Abprallwinkels
        }
    }

    fun controlBallspeed() {

        //ball wird pro Minute schneller
        if (window.currentTime >= time && speedX != maxSpeedX && speedX != -maxSpeedX) {
            if (speedX > 0) {
                speedX = mediSpeedX
            } else {
                speedX = -mediSpeedX
            }
            speedZ * speedFactor

            time += 60
            mediSpeedX += 2
            speedFactor += 0.1f

            println("speedUp!")
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

    private fun resetGame(posX: Float, posZ: Float, dt: Float) {
        //reset nach einem Punkt/Tor
        rolling = false

        if (speedX > 0) {
            speedX = -START_SPEEDX
        } else {
            speedX = START_SPEEDX
        }
        speedZ = 0f

        time = window.currentTime + 60f
        mediSpeedX = START_SPEEDX + 2
        speedFactor = 1.1f
        itemSpawn = false
        placeItem = true
        spawnTime = SPAWN_TIME
        endEffect()

        ball.translateLocal(Vector3f(-posX * resetFactor, 0.0f, -posZ * resetFactor))

        rolling = true
    }

    fun winner(dt: Float) {
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
            resetGame(posX,posZ,dt)
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
            resetGame(posX,posZ,dt)
        }
    }

    private fun placeItem(dt: Float) {
        while (true) {
            itemX = Random.nextInt(-itemBoundsX,itemBoundsX).toFloat()
            itemZ = Random.nextInt(-itemBoundsZ,itemBoundsZ).toFloat()

            //println("item.oldPos.x: ${item.getPosition().x}")
            //println("item.oldPos.z: ${item.getPosition().z}")
            item.translateLocal(Vector3f(itemX, 0.0f, itemZ))
            //println("item.newPos.x: ${item.getPosition().x}")
            //println("item.newPos.z: ${item.getPosition().z}")

            //item can´t be rendered if out of bounds
            if (placeItem &&
                    item.getPosition().z >= -5 && item.getPosition().z <= 5 &&
                    item.getPosition().x >= -9 && item.getPosition().x <= 9) {

                itemSpawn = true
                placeItem = false
                println("new item has appeared!")
                println("Item Position X: ${item.getPosition().x}, Z: ${item.getPosition().z}")
                break
            }

            //get out of loop if something goes wrong
            if (item.getPosition().z <= -30 || item.getPosition().z >= 30 ||
                    item.getPosition().x >= 30 || item.getPosition().x <= -30) {

                println("unlucky..")
                println("Item Position X: ${item.getPosition().x}, Z: ${item.getPosition().z}")
                val posX = item.getPosition().x
                val posZ = item.getPosition().z
                item.translateLocal(Vector3f(-posX * resetFactor, 0.0f, -posZ * resetFactor))
                break
            }
        }
    }

    private fun effect() {
        val effect = Random.nextInt(1,5) //1 bis 4

        when (effect) {
            1 -> {
                //useShader = effectShader
                chaos = 1
            }
            2 -> {
                //useShader = effectShader
                confuse = 1
                swapControls = true
            }
            3 -> {
                //useShader = effectShader
                shake = 1
            }
            4 -> speedUp()
        }

        effectNumber = effect
    }

    private fun endEffect() {
        when (effectNumber) {
            1 -> {
                //useShader = classicStaticShader
                chaos = 0
            }
            2 -> {
                //useShader = classicStaticShader
                confuse = 0
                swapControls = false
            }
            3 -> {
                //useShader = classicStaticShader
                shake = 0
            }
            4 -> slowDown()
        }

        effectNumber = 0
    }

    fun colorSwap() {
        when (viewActive) {
            1 -> {
                text_score.list.first().material = materialWhite
                score_bar.list.first().material = materialWhite
                score_p1.list.first().material = materialBlack
                score_p2.list.first().material = materialBlack
            }

            2 -> {
                text_score.list.first().material = materialBlack
                score_bar.list.first().material = materialBlack
                score_p1.list.first().material = materialWhite
                score_p2.list.first().material = materialWhite
            }

            3 -> {
                text_score.list.first().material = materialBlack
                score_bar.list.first().material = materialBlack
                score_p1.list.first().material = materialWhite
                score_p2.list.first().material = materialWhite
            }
        }
    }

    private fun speedUp() {
        temp = Math.abs(speedX)

        if (speedX > 0) {
            speedX = maxSpeedX
        } else {
            speedX = -maxSpeedX
        }

        speedZ * 1.4
    }

    private fun slowDown() {
        if (speedX > 0) {
            speedX = temp
        } else {
            speedX = -temp
        }

        speedZ / 1.4
    }

    fun playerBot(dt: Float) {
        val move = (ball.getPosition().z - playerBot.getPosition().z)

        //stays in bounds
        if (playerBot.getPosition().z+move >= -bounds_playerAI_z && playerBot.getPosition().z+move <= bounds_playerAI_z) {
            playerBot.translateLocal(Vector3f(0.0f, 0.0f, (speed_ai_player) * dt))
            speed_ai_player = (move/dt)
        }

        if (speed_ai_player > max_speed_ai_player) {
            speed_ai_player = max_speed_ai_player
        }

        if (speed_ai_player < -max_speed_ai_player) {
            speed_ai_player = -max_speed_ai_player
        }

        //Bot intersection -> same as player 1 & 2
        if (ball.getPosition().x <= playerBot.getPosition().x + playerWidth
                && ball.getPosition().x >= playerBot.getPosition().x - playerWidth
                && ball.getPosition().z + ballHalfHeight <= playerBot.getPosition().z + playerHalfHeight
                && ball.getPosition().z - ballHalfHeight >= playerBot.getPosition().z - playerHalfHeight) {
            reverse(playerBot)
        }
    }

    fun changeMode() {
        if (window.getKeyState(GLFW.GLFW_KEY_F1)) {
            singlePlayer = true
        }

        if (window.getKeyState(GLFW.GLFW_KEY_F2)) {
            singlePlayer = false
        }

        if (window.getKeyState(GLFW.GLFW_KEY_F3)) {
            sound_1.start()
            sound_1.loop(Clip.LOOP_CONTINUOUSLY)
            pongSound = true
            //sound_2.start()
        }

        if (window.getKeyState(GLFW.GLFW_KEY_F4)) {
            sound_1.stop()
            pongSound = false
            //sound_2.stop()
        }
    }

    fun countDownEffect(dt: Float) {
        if (effectTime > 0f) {
            effectTime -= dt

            if (effectTime <= 0f){
                endEffect()
                effectTime = EFFECT_TIME
            }
        }
    }

    fun countDownSpawn(dt: Float) {
        if (spawnTime > 0f) {
            spawnTime -= dt

            if (spawnTime <= 0f){
                placeItem(dt)
                spawnTime = SPAWN_TIME
            }
        }
    }

    fun camera_switch(dt: Float, t:Float) {
        //Boolwerte zum sicherstellen, dass die Operationen jeweils nur einmal hintereinander ausgeführt werden
        //wenn in Position 2 oder 3: zuerst zurück nach 1

        //Top/Down-Ansicht (Standard)
        if (window.getKeyState(GLFW.GLFW_KEY_1) || pause) {
            if (view2 && view1 == false) {
                camera.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,-4.0f,-3.0f))

                text_score.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                score_bar.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                score_p1.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                score_p2.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                text_score.translateLocal(Vector3f(0.0f,-3.0f,4.0f))
                score_bar.translateLocal(Vector3f(0.0f,-3.0f,4.0f))
                score_p1.translateLocal(Vector3f(0.0f,-3.0f,4.0f))
                score_p2.translateLocal(Vector3f(0.0f,-3.0f,4.0f))

                viewActive = 1
                view1 = true
                view2 = false
            }

            if (view3 && view1 == false) {
                camera.rotateAroundPoint(0f, org.joml.Math.toRadians(90f),0.0f,Vector3f(0.0f))
                camera.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,-2.0f,-5.0f))

                text_score.rotateAroundPoint(0f, org.joml.Math.toRadians(90f),0.0f,Vector3f(0.0f))
                text_score.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                text_score.translateLocal(Vector3f(0.0f,-5.0f,2.0f))
                score_bar.rotateAroundPoint(0f, org.joml.Math.toRadians(90f),0.0f,Vector3f(0.0f))
                score_bar.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                score_bar.translateLocal(Vector3f(0.0f,-5.0f,2.0f))
                score_p1.rotateAroundPoint(0f, org.joml.Math.toRadians(90f),0.0f,Vector3f(0.0f))
                score_p1.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                score_p1.translateLocal(Vector3f(0.0f,-5.0f,2.0f))
                score_p2.rotateAroundPoint(0f, org.joml.Math.toRadians(90f),0.0f,Vector3f(0.0f))
                score_p2.rotateAroundPoint(org.joml.Math.toRadians(-85f),0.0f,0.0f,Vector3f(0.0f))
                score_p2.translateLocal(Vector3f(0.0f,-5.0f,2.0f))

                viewActive = 1
                view1 = true
                view3 = false
            }
        }

        //Seitenansicht
        if (window.getKeyState(GLFW.GLFW_KEY_2) && viewActive == 1 && !pause) {
            if (view2 == false) {
                camera.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,4.0f,3.0f))

                text_score.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                score_bar.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                score_p1.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                score_p2.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                text_score.translateLocal(Vector3f(0.0f,3.0f,-4.0f))
                score_bar.translateLocal(Vector3f(0.0f,3.0f,-4.0f))
                score_p1.translateLocal(Vector3f(0.0f,3.0f,-4.0f))
                score_p2.translateLocal(Vector3f(0.0f,3.0f,-4.0f))

                viewActive = 2
                view2 = true
                view1 = false
            }
        }

        //3rd-Person Ansicht (für Spieler 1)
        if (window.getKeyState(GLFW.GLFW_KEY_3) && viewActive == 1 && !pause) {
            if (view3 == false) {
                camera.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                camera.rotateAroundPoint(0f, org.joml.Math.toRadians(-90f),0.0f,Vector3f(0.0f))
                camera.translateLocal(Vector3f(0.0f,2.0f,5.0f))

                text_score.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                text_score.rotateAroundPoint(0f, org.joml.Math.toRadians(-90f),0.0f,Vector3f(0.0f))
                text_score.translateLocal(Vector3f(0.0f,5.0f,-2.0f))
                score_bar.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                score_bar.rotateAroundPoint(0f, org.joml.Math.toRadians(-90f),0.0f,Vector3f(0.0f))
                score_bar.translateLocal(Vector3f(0.0f,5.0f,-2.0f))
                score_p1.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                score_p1.rotateAroundPoint(0f, org.joml.Math.toRadians(-90f),0.0f,Vector3f(0.0f))
                score_p1.translateLocal(Vector3f(0.0f,5.0f,-2.0f))
                score_p2.rotateAroundPoint(org.joml.Math.toRadians(85f),0.0f,0.0f,Vector3f(0.0f))
                score_p2.rotateAroundPoint(0f, org.joml.Math.toRadians(-90f),0.0f,Vector3f(0.0f))
                score_p2.translateLocal(Vector3f(0.0f,5.0f,-2.0f))

                viewActive = 3
                view3 = true
                view1 = false
            }
        }
    }
}