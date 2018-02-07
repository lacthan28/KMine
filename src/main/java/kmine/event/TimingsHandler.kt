package kmine.event

import kmine.Server
import kmine.writeln
import java.io.File
import java.io.PrintWriter
import kotlin.math.round

/**
 * @param name String
 * @param parent TimingsHandler
 */
class TimingsHandler(var name: String, var parent: TimingsHandler? = null) {

    companion object {
        private var HANDLERS = emptyArray<TimingsHandler>()

        fun printTimings(file: String) {
            File(file).printWriter().use { printWriter ->
                {
                    printWriter.writeln("Minecraft")

                    HANDLERS.forEach {
                        val time = it.totalTime
                        val count = it.count
                        if (count == 0) return@forEach

                        val avg = time / count

                        printWriter.writeln("    ${it.name} Time: ${round((time * 1000000000).toDouble())} Count: $count Avg: ${round(avg * 1000000000)} Violations: ${it.violations}")
                    }

                    printWriter.writeln("# Version ${}")
                    printWriter.writeln("# ${}")

                    var entities = 0
                    var livingEntities = 0
                    //Server.instance.levels.forEach{}
                }
            }
        }
    }

    private var count: Int = 0
    private var curCount: Int = 0
    private var start: Int = 0
    private var timingDepth: Int = 0
    private var totalTime: Int = 0
    private var curTickTotal: Int = 0
    private var violations: Int = 0

    init {
        TimingsHandler.HANDLERS[this.hashCode()] = this
    }

}