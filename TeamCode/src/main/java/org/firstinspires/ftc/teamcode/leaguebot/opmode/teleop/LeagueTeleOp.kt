package org.firstinspires.ftc.teamcode.leaguebot.opmode.teleop

import com.qualcomm.robotcore.eventloop.opmode.*
import org.firstinspires.ftc.teamcode.leaguebot.*
import org.firstinspires.ftc.teamcode.leaguebot.opmode.*
import org.firstinspires.ftc.teamcode.leaguebot.opmode.hardware.*
import org.firstinspires.ftc.teamcode.movement.*
import org.firstinspires.ftc.teamcode.movement.DriveMovement.world_angle_unwrapped_raw
import org.firstinspires.ftc.teamcode.movement.DriveMovement.world_x_raw
import org.firstinspires.ftc.teamcode.movement.DriveMovement.world_y_raw
import org.firstinspires.ftc.teamcode.util.*

@TeleOp
class LeagueTeleOp : LeagueBotTeleOpBase() {
    override fun onMainLoop() {
        DriveMovement.gamepadControl(driver)

        LeagueBot.intake.state = when {
            gamepad1.right_bumper -> {
                ScorerState.triggerLoad()
                if (ScorerState.clearToIntake) MainIntake.State.IN else MainIntake.State.OUT
            }
            gamepad1.left_bumper  -> {
                MainIntake.State.OUT
            }
            else                  -> {
                MainIntake.State.STOP
            }
        }

        LeagueBot.lift.manualTemp = (-gamepad2.right_stick_y.toDouble() deadZone 0.05)

        //DriveMovement.moveFieldCentric(driver.leftStick.x, driver.leftStick.y, driver.rightStick.x)

        if (driver.b.currentState)
            DriveMovement.setPosition_raw(0.0, 0.0, 0.0)

        when {
            gamepad2.dpad_up    -> ScorerState.triggerLoad()
            gamepad2.dpad_right -> ScorerState.triggerGrab()
            gamepad2.dpad_down  -> ScorerState.triggerExtend()
            gamepad2.dpad_left  -> ScorerState.triggerRelease()
        }

        /*when {
            gamepad2.dpad_down  -> LeagueBot.grabber.state = Grabber.State.GRAB
            gamepad2.dpad_right -> LeagueBot.grabber.state = Grabber.State.LOAD
            gamepad2.dpad_up    -> LeagueBot.grabber.state = Grabber.State.RELEASE
        }

        when {
            gamepad2.right_trigger > 0.5 -> LeagueBot.extension.extend()
            gamepad2.left_trigger > 0.5  -> LeagueBot.extension.retract()
        }*/


        telemetry.addData("drive wheels y pos", LeagueBot.drive.y_drivePos)
        telemetry.addData("lf pos", LeagueBot.drive.leftFront.encoderTicks)
        telemetry.addData("lb pos", LeagueBot.drive.leftBack.encoderTicks)
        telemetry.addData("rf pos", LeagueBot.drive.rightFront.encoderTicks)
        telemetry.addData("rb pos", LeagueBot.drive.rightBack.encoderTicks)

        combinedPacket.put("ys", driver.leftStick.y)

        combinedPacket.put("y fps", Speedometer.yInchPerSec / 12.0)
        combinedPacket.put("x fps", Speedometer.xInchPerSec / 12.0)
        combinedPacket.put("leftInches", LeagueThreeWheelOdometry.leftInches)
        combinedPacket.put("rightInches", LeagueThreeWheelOdometry.rightInches)
        combinedPacket.put("auxInches", LeagueThreeWheelOdometry.auxInches)
        combinedPacket.put("y", world_y_raw)
        combinedPacket.put("x", world_x_raw)
        combinedPacket.put("deg", world_angle_unwrapped_raw.deg)
    }
}