package org.firstinspires.ftc.teamcode.movement

import org.firstinspires.ftc.teamcode.field.*
import org.firstinspires.ftc.teamcode.movement.SimpleMotion.goToPosition_raw
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DriveMovement.movement_x
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DriveMovement.movement_y
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_angle_raw
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_deg_raw
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_pose_raw
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_x_raw
import org.firstinspires.ftc.teamcode.movement.basicDriveFunctions.DrivePosition.world_y_raw
import org.firstinspires.ftc.teamcode.opmodeLib.Globals.mode
import org.firstinspires.ftc.teamcode.util.*
import kotlin.math.*

class PurePursuitPath(var followDistance: Double) {
    val curvePoints = ArrayList<CurvePoint>()

    val firstFollowDistance = followDistance

    fun resetArgs() {
        followDistance = firstFollowDistance
    }

    fun add(point: Point) {
        curvePoints.add(CurvePoint(point, followDistance))
    }

    var finalAngle = Double.NaN
}

object PurePursuit {
    fun angleBetween_deg(point: Point, otherPoint: Point): Double {
        return atan2(otherPoint.x - point.x, otherPoint.y - point.y).toDegrees
    }

    fun angleWrap_deg(_angle: Double): Double {
        var angle = _angle
        while (angle < -180.0)
            angle += 360.0
        while (angle > 180.0)
            angle -= 360.0
        return angle
    }

    fun goToFollowPoint(targetPoint: Point, robotLocation: Point, followAngle: Double, allowSkipping:Boolean = false) {
        goToPosition_raw(targetPoint.x, targetPoint.y, angleBetween_deg(robotLocation, targetPoint) + followAngle)
        val movementAbs = (movement_y + movement_x).absoluteValue
        /*if(movementAbs != 0.0){
            movement_y /= movementAbs
            movement_x /= movementAbs
        }*/
    }

    var lastIndex = 0
    var finishingMove = false

    fun reset() {
        lastIndex = 0
        finishingMove = false
        followMe = null
    }

    fun followCurve(path: PurePursuitPath, followAngle: Double = 0.0) {
        val allPoints = path.curvePoints
        val finalAngle = path.finalAngle

        val robotLocation = world_pose_raw

        val followMe = getFollowPoint(allPoints, robotLocation, allPoints[0].followDistance, followAngle)

        goToFollowPoint(followMe, robotLocation.point, followAngle)

        val finalPoint = allPoints.last()

        if (finalAngle.notNaN() && finalPoint.point.distanceTo(robotLocation.point) < finalPoint.followDistance)
            finishingMove = true

        if (finishingMove)
            goToPosition_raw(finalPoint.point.x, finalPoint.point.y, finalAngle)
    }

    var followMe: Point? = null
    fun getFollowPoint(pathPoints: ArrayList<CurvePoint>, robotLocation: Pose, followDistance: Double, followAngle: Double): Point {
        if (followMe == null)
            followMe = pathPoints[0].point

        for (i in 0 until pathPoints.size - 1) {
            val startLine = pathPoints[i]
            val endLine = pathPoints[i + 1]

            val intersections = lineCircleIntersection(robotLocation.point, followDistance, startLine.point.copy(), endLine.point.copy())

            var closestAngle = 1000.0

            mode.telemetry.addData("intersections", intersections.size)
            mode.telemetry.addData("x", world_x_raw)
            mode.telemetry.addData("y", world_y_raw)
            mode.telemetry.addData("deg", world_angle_raw.deg)

            for (intersection in intersections) {
                val angle = angleWrap_deg((angleBetween_deg(robotLocation.point, intersection) - world_deg_raw + followAngle)).absoluteValue

                if (angle < closestAngle) {
                    closestAngle = angle
                    followMe = intersection
                }
            }
        }

        return followMe!!
    }

    fun lineCircleIntersection(center: Point, radius: Double, lineP1: Point, lineP2: Point): ArrayList<Point> {
        if ((lineP1.y - lineP2.y).absoluteValue < 0.003)
            lineP1.y = lineP2.y + 0.003
        if ((lineP1.x - lineP2.x).absoluteValue < 0.003)
            lineP1.x = lineP2.x + 0.003

        val m1 = (lineP2.y - lineP1.y) / (lineP2.x - lineP1.x)

        val quadA = 1.0 + Math.pow(m1, 2.0)

        val x1 = lineP1.x - center.x
        val y1 = lineP1.y - center.y

        val quadB = (2.0 * m1 * y1) - (2.0 * Math.pow(m1, 2.0) * x1)

        val quadC = (Math.pow(m1, 2.0) * Math.pow(x1, 2.0)) - (2.0 * y1 * m1 * x1) + Math.pow(y1, 2.0) - Math.pow(radius, 2.0)

        val points = ArrayList<Point>()

        var xRoot1 = (-quadB + sqrt(Math.pow(quadB, 2.0) - (4.0 * quadA * quadC))) / (2.0 * quadA)

        var yRoot1 = m1 * (xRoot1 - x1) + y1

        xRoot1 += center.x
        yRoot1 += center.y

        val minX = if (lineP1.x < lineP2.x) lineP1.x else lineP2.x
        val maxX = if (lineP1.x > lineP2.x) lineP1.x else lineP2.x

        if (xRoot1.notNaN() && yRoot1.notNaN())
            if (xRoot1 > minX && xRoot1 < maxX)
                points.add(Point(xRoot1, yRoot1))

        var xRoot2 = (-quadB - sqrt(Math.pow(quadB, 2.0) - (4.0 * quadA * quadC))) / (2.0 * quadA)

        var yRoot2 = m1 * (xRoot2 - x1) + y1

        xRoot2 += center.x
        yRoot2 += center.y

        if (xRoot2.notNaN() && yRoot2.notNaN())
            if (xRoot2 > minX && xRoot2 < maxX)
                points.add(Point(xRoot2, yRoot2))

        return points
    }
}

data class CurvePoint(var point: Point, var followDistance: Double)