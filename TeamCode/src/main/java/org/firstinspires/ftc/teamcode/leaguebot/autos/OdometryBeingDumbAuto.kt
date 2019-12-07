package org.firstinspires.ftc.teamcode.leaguebot.autos

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.field.Pose
import org.firstinspires.ftc.teamcode.leaguebot.hardware.ScorerState
import org.firstinspires.ftc.teamcode.leaguebot.misc.LeagueBotAutoBase
import org.firstinspires.ftc.teamcode.opmodeLib.Alliance

@Autonomous(group = "c")
class OdometryBeingDumbAuto(alliance: Alliance) : LeagueBotAutoBase(alliance, Pose(0.0, 0.0, 0.0)){
    override fun onMainLoop() {
        if(secondsTillEnd < 2.0)
            ScorerState.triggerExtend()
    }
}