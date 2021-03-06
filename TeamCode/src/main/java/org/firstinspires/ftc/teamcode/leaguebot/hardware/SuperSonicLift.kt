package org.firstinspires.ftc.teamcode.leaguebot.hardware

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.util.Range
import org.firstinspires.ftc.teamcode.bulkLib.*
import org.firstinspires.ftc.teamcode.leaguebot.teleop.LeagueTeleOp
import org.firstinspires.ftc.teamcode.opmodeLib.Globals.mode
import org.firstinspires.ftc.teamcode.util.Clock
import kotlin.math.absoluteValue

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

        var kP_low: Double = 0.15
        @JvmField
        var kI_low = 1.0
        @JvmField
        var kD_low: Double = 0.01

        @JvmField
        var speedStartIntegrating = 1.0
        @JvmField
        var integralCap = 1.0

        @JvmField
        var targetCap = 37.0

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
            var newValue = value
            if (newValue > targetCap)
                newValue = targetCap

            if (desiredControlState != ControlStates.HEIGHT || newValue < field)
                resetIntegral()
            desiredControlState = ControlStates.HEIGHT
            field = newValue

            if (newValue <= 0.0)
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
        val low = heightTarget < 2.0
        val kP = if (low) kP_low else kP
        val kI = if (low) kI_low else kI
        val kD = if (low) kD_low else kD

        val currRawHeight = rawHeight
        val currTime = Clock.seconds
        val dt = if (lastTime.isNaN()) 0.0 else (currTime - lastTime)
        val speed = if (lastRawHeight.isNaN()) 0.0 else ((rawHeight - lastRawHeight) / dt)
        lastRawHeight = currRawHeight
        lastTime = currTime

        var power = 0.0

        var controlState = desiredControlState

        if (!hasBeenCalibrated)
            controlState = ControlStates.LOWER

        val heightLeft = heightTarget - height

        when (controlState) {
            ControlStates.LOWER -> {
                checkCalibration()
                power = -0.3
            }

            ControlStates.HEIGHT -> {
                power += heightLeft * kP
                power -= speed * kD

                if (power.absoluteValue > speedStartIntegrating) {
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
            forceCalibration()
        }
    }

    fun forceCalibration() {
        resetSpoolRadians = rawRadians
    }

    fun triggerIntake() {
        heightTarget = LeagueTeleOp.intakeHeight
    }

    val rawRadians get() = -encoder.radians
    val rawHeight get() = rawRadians * SPOOL_RADIUS * fudgeFactor
    val radians get() = (rawRadians - resetSpoolRadians)
    val height get() = radians * SPOOL_RADIUS * fudgeFactor

    val left = RevHubMotor("leftLift", Go_5_2::class).openLoopControl.float
    val right = RevHubMotor("rightLift", Go_5_2::class).reverse.openLoopControl.float
    val encoder = Encoder(Robot.lynx1, 1, MotorEncoder.G5_2)
    val bottomPressed get() = !Robot.lynx1.cachedInput.getDigitalInput(1) || !(Robot.lynx1.cachedInput.getDigitalInput(3))
}