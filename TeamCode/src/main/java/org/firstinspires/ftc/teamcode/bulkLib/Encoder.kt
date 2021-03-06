package org.firstinspires.ftc.teamcode.bulkLib

import com.qualcomm.hardware.lynx.*
import org.firstinspires.ftc.teamcode.field.Geometry.TAU

class Encoder(private val module: LynxModule, private val portNumber: Int, private val ticks_per_revolution: Double) {
    val ticks: Int
        get() = module.cachedInput.getEncoder(portNumber)
    val rotations: Double
        get() = ticks.toDouble() / ticks_per_revolution
    val radians: Double
        get() = rotations * TAU
}

object S4T {
    const val CPR_1000 = 4000.0
}

object MotorEncoder {
    const val G3_7 = 103.6
    const val G5_2 = 145.6
}