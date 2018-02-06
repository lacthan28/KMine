package kmine

import java.io.PrintWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

fun Double.round(precision: Int, roundingMode: RoundingMode): Double {
    val bd = BigDecimal(this)
    val rounded = bd.setScale(precision, roundingMode)
    return rounded.toDouble()
}

fun PrintWriter.writeln(string: String) {
    this.write(string)
    this.write("\n")
}

fun List<String>.pop(): String {
    val str = this[0]
    this.toMutableList().removeAt(0)
    return str
}

fun Date.createDateFromFormat(format: SimpleDateFormat, time: String = ""): Date {
    val date = if (time.isNotEmpty()) {
        DateFormat.getInstance().parse(time)
    } else this
    format.format(date)
    return date
}