package org.firstinspires.ftc.teamcode.leaguebot.opmode.autos

import org.firstinspires.ftc.teamcode.field.*

object IntakeStateMachine {
    var currentStone = Stone(0)

    fun start() {
        currentStone = Quarry.popStone()
    }

    fun update(): Boolean {

        return false
    }
}