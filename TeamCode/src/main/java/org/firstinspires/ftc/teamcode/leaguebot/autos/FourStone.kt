package org.firstinspires.ftc.teamcode.leaguebot.autos

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.util.Range
import org.firstinspires.ftc.teamcode.field.Field
import org.firstinspires.ftc.teamcode.field.Point
import org.firstinspires.ftc.teamcode.field.Pose
import org.firstinspires.ftc.teamcode.leaguebot.hardware.AutoClaw
import org.firstinspires.ftc.teamcode.leaguebot.hardware.Robot.autoClaw
import org.firstinspires.ftc.teamcode.leaguebot.hardware.Robot.foundationGrabber
import org.firstinspires.ftc.teamcode.leaguebot.misc.LeagueBotAutoBase
import org.firstinspires.ftc.teamcode.movement.PurePursuit
import org.firstinspires.ftc.teamcode.movement.PurePursuitPath
import org.firstinspires.ftc.teamcode.movement.SimpleMotion.goToPosition_mirror
import org.firstinspires.ftc.teamcode.movement.SimpleMotion.pointAngle_mirror
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DriveMovement.moveFieldCentric_mirror
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DriveMovement.movement_turn
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DriveMovement.movement_y
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DriveMovement.stopDrive
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_deg_mirror
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_y_mirror
import org.firstinspires.ftc.teamcode.movement.toRadians
import org.firstinspires.ftc.teamcode.opmodeLib.Alliance
import org.firstinspires.ftc.teamcode.opmodeLib.RunData.ALLIANCE
import org.firstinspires.ftc.teamcode.vision.SkystoneDetector
import org.firstinspires.ftc.teamcode.vision.SkystoneRandomization
import kotlin.math.absoluteValue

private val startPoint = Point(Field.EAST_WALL - 8.625, Field.SOUTH_WALL + 38.25)

@Config
abstract class FourStone(alliance: Alliance) : LeagueBotAutoBase(alliance, Pose(startPoint.x, startPoint.y, (-90.0).toRadians)) {
    companion object {
        @JvmField
        var grabX = 33.5
        @JvmField
        var followDistance = 35.0

        @JvmField
        var crossX = 40.0

        @JvmField
        var farY = 56.0

        @JvmField
        var nearY = 46.0

        @JvmField
        var placeX = 26.75

        @JvmField
        var toFoundationX = 44.0
    }

    private val stoneYs = arrayOf(-59.75, -51.75, -43.75, -35.75, -27.75, -19.75)
    private val nearStones = arrayOf(2, 5, 4, 3/*, 1*/)
    private val midStones = arrayOf(1, 4, 5, 3/*, 2*/)
    private val farStones = arrayOf(0, 3, 5, 4/*, 2*/)
    private val stones by lazy {

        when (SkystoneDetector.place) {
            SkystoneRandomization.NEAR -> nearStones
            SkystoneRandomization.MID -> midStones
            SkystoneRandomization.FAR -> farStones
        }
    }

    val stone get() = stones[cycle]
    private var cycle = 0

    private val grabY get() = stoneYs[stone]

    enum class progStages {

        goBack,
        goToStone,

        grab,

        cross,

        eject,

        rotate,
        backUp,
        rotateFoundation,
        slam,
        park,

        stopDoNothing
    }

    override fun onStart() {
        super.onStart()
        nextStage(progStages.goToStone.ordinal)
    }

    override fun onMainLoop() {
        val currentStage = progStages.values()[stage]

        telemetry.addData("stage", currentStage)

        stopDrive()

        when (currentStage) {
            progStages.goBack -> {
                if(!isTimedOut(0.25))
                    stopDrive()

                val path = PurePursuitPath(followDistance)
                path.add(Point(crossX, 72.0))
                path.toY(grabY)
                PurePursuit.followCurve(path, 180.0)

                pointAngle_mirror(0.0)

                if (world_y_mirror < 5.0)
                    nextStage()
            }

            progStages.goToStone -> {
                autoClaw.state = AutoClaw.State.PRE_GRAB
                val error = goToPosition_mirror(grabX, grabY, 0.0)
                if (error.point.y.absoluteValue < 2.0 && error.point.x.absoluteValue < 1.24 && world_deg_mirror < 4.0 && world_deg_mirror > -3.0)
                    nextStage()
            }

            progStages.grab -> {
                autoClaw.state = AutoClaw.State.GRABBING
                val error = goToPosition_mirror(grabX, grabY, 0.0)
                if (isTimedOut(0.35))
                    nextStage()
            }

            progStages.cross -> {
                autoClaw.state = AutoClaw.State.STOW_STONE
                val curve = PurePursuitPath(followDistance)
                curve.add(Point(toFoundationX, grabY))
                curve.toY(24.0)
                curve.add(Point(placeX, if (cycle % 2 == 0) farY else nearY))

                val doneWithCurve = PurePursuit.followCurve(curve)

                pointAngle_mirror(0.0)

                if (stone < 5 && cycle < 2 && !isTimedOut(0.25))
                    stopDrive()

                if (doneWithCurve) {
                    autoClaw.state = if (cycle < 1) AutoClaw.State.EJECT else if (cycle < 2) AutoClaw.State.PRE_GRAB else AutoClaw.State.EJECT
                    nextStage()
                }
            }

            progStages.eject -> {

                if (isTimedOut(if (cycle < 2) 0.07 else 0.2))
                    autoClaw.state = AutoClaw.State.PRE_GRAB

                if (isTimedOut(0.3)) {
                    cycle++
                    autoClaw.state = AutoClaw.State.TELEOP
                    if (cycle >= stones.size) {
                        foundationGrabber.prepForGrab()
                        nextStage()
                    } else {
                        nextStage(progStages.goBack.ordinal)
                    }
                }
            }

            progStages.rotate -> {
                if (isTimedOut(0.2)) {
                    val error = goToPosition_mirror(28.0, 49.5, 90.0)
                    if (error.deg.absoluteValue < 6.0 && error.x.absoluteValue < 3.0)
                        nextStage()
                }
            }

            progStages.backUp -> {
                val error = goToPosition_mirror(23.0, 49.5, 90.0)
                if (error.x.absoluteValue < 3.0)
                    nextStage()
            }

            progStages.rotateFoundation -> {
                foundationGrabber.grab()

                val error = pointAngle_mirror(180.0)

                val maxSpeed = 1.0
                movement_turn = Range.clip(movement_turn, -maxSpeed, maxSpeed)

                movement_y = movement_turn * ALLIANCE.sign * 1.6

                if (!isTimedOut(0.25))
                    stopDrive()

                if (error.absoluteValue < 5.0)
                    nextStage()
            }

            progStages.slam -> {
                moveFieldCentric_mirror(0.0, 0.5, 0.0)
                pointAngle_mirror(180.0)

                if (isTimedOut(1.0) || secondsTillEnd < 1.5) {
                    nextStage()
                    foundationGrabber.release()
                }
            }

            progStages.park -> {
                val error = goToPosition_mirror(31.0, 4.5, 180.0)

                if (!isTimedOut(0.25))
                    stopDrive()

                if (error.point.hypot < 3.0)
                    nextStage()
            }

            progStages.stopDoNothing -> {
                if (isTimedOut(1.0))
                    requestOpModeStop()
            }
        }
    }
}

@Autonomous
class FourStone_Red : FourStone(Alliance.RED)

@Autonomous
class FourStone_BLUE : FourStone(Alliance.BLUE)