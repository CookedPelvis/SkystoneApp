package org.firstinspires.ftc.teamcode.leaguebot.opmode.hardware

import com.acmerobotics.dashboard.config.*
import com.qualcomm.robotcore.util.*
import org.firstinspires.ftc.teamcode.bulkLib.*
import org.firstinspires.ftc.teamcode.lib.Globals.mode
import org.firstinspires.ftc.teamcode.lib.hardware.*
import org.firstinspires.ftc.teamcode.util.*
import kotlin.math.*

/**
 * distance is in inches
 * rail length of 200mm = 7.874"
 * stroke length of 120 mm = 4.724"
 * spool diameter is 1.35"
 * */

/**
 * up a =
 * down a =
 *
 * up b =
 * down b =
 *
 * constantG =
 * slideG =
 * friction =
 */

@Config
class SuperSonicLift {
    companion object {
        @JvmField
        var kP: Double = 0.1
        @JvmField
        var kI = 0.4
        @JvmField
        var kD: Double = 0.01
        @JvmField
        var speedStartInegrating = 0.4
        @JvmField
        var integralCap = 1.0

        private var hasBeenCalibrated = false
        private var resetSpoolRadians = 0.0
        private const val SPOOL_RADIUS = 1.4 / 2.0

        @JvmField
        var fudgeFactor = 1.0
    }

    init {
        if (mode.isAutonomous)
            hasBeenCalibrated = false
    }

    var ultraManual: Double = 0.0
        set(value) {
            desiredControlState = ControlStates.ULTRA_MANUAL
            field = value
        }

    var heightTarget = 0.0
        set(value) {
            if (value != field || desiredControlState != ControlStates.HEIGHT)
                resetIntegral()
            desiredControlState = ControlStates.HEIGHT
            field = value

            if (value <= 0.0)
                lower()
        }

    var errorSum = 0.0

    fun resetIntegral() {
        errorSum = 0.0
    }

    fun lower() {
        desiredControlState = ControlStates.LOWER
    }

    private var desiredControlState = ControlStates.LOWER

    enum class ControlStates {
        HEIGHT,
        LOWER,
        ULTRA_MANUAL
    }

    var lastRawHeight = Double.NaN
    var lastTime = Double.NaN

    fun update() {
        val currRawHeight = rawHeight
        val currTime = Clock.seconds
        val dt = if (lastTime.isNaN()) 0.0 else (currTime - lastTime)
        val speed = if (lastRawHeight.isNaN()) 0.0 else ((rawHeight - lastRawHeight) / dt)
        lastRawHeight = currRawHeight
        lastTime = currTime

        var power = 0.0

        var controlState = desiredControlState

        checkCalibration()

        if (!hasBeenCalibrated)
            controlState = ControlStates.LOWER

        val heightLeft = heightTarget - height

        when (controlState) {
            ControlStates.LOWER        -> {
                power = if (height > 10.0) -1.0 else -0.25
            }
            ControlStates.HEIGHT       -> {
                power += heightLeft * kP
                power -= speed * kD

                if (power.absoluteValue > speedStartInegrating) {
                    resetIntegral()
                } else {
                    errorSum += (heightLeft * dt)
                    if (kI != 0.0) {
                        val maxRange = integralCap / kI
                        val minRange = -integralCap / kI
                        errorSum = Range.clip(errorSum, minRange, maxRange)
                    } else {
                        resetIntegral()
                    }
                }

                power += errorSum * kI

                mode.combinedPacket.put("liftError", heightLeft)
            }

            ControlStates.ULTRA_MANUAL -> {
                power += ultraManual
            }
        }


        if (bottomPressed && power < 0.0)
            power = 0.0

        left.power = power
        right.power = power
    }

    fun checkCalibration() {
        if (bottomPressed) {
            hasBeenCalibrated = true
            resetSpoolRadians = rawRadians
        }
    }

    val rawRadians get() = -encoder.radians
    val rawHeight get() = rawRadians * SPOOL_RADIUS * fudgeFactor
    val radians get() = (rawRadians - resetSpoolRadians)
    val height get() = radians * SPOOL_RADIUS * fudgeFactor

    val left = RevHubMotor("leftLift", Go_5_2::class).openLoopControl.float
    val right = RevHubMotor("rightLift", Go_5_2::class).reverse.openLoopControl.float
    val encoder = Encoder(LeagueBot.lynx1, 1, MotorEncoder.G3_7)
    val bottomPressed get() = !LeagueBot.lynx1.cachedInput.getDigitalInput(1)
}