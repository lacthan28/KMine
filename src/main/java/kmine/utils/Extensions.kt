package kmine.utils

import com.google.gson.Gson
import kmine.lang.TextContainer
import java.io.PrintWriter
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.soap.Text
import kotlin.Cloneable

fun Double.round(precision: Int, roundingMode: RoundingMode): Double {
    val bd = BigDecimal(this)
    val rounded = bd.setScale(precision, roundingMode)
    return rounded.toDouble()
}

fun PrintWriter.writeln(string: String) {
    this.write(string)
    this.write("\n")
}

fun <T> List<T>.pop(): T {
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

fun String.jsonDecodeToArray(): List<String> {
    return Gson().fromJson(this, Array<String>::class.java).toList()
}