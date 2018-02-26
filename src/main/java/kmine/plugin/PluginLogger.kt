package kmine.plugin

import kmine.Server
import kmine.utils.LogLevel
import kmine.utils.Logger

class PluginLogger: Logger {
    private var pluginName: String = ""

    fun constructor(context: Plugin) {
        val prefix = context.getDescription().getPrefix()
        this.pluginName = if (prefix.isNotEmpty()) "[$prefix] " else "[" + context.getDescription().getName() + "] "
    }

    override fun emergency(message: String) {
        this.log(LogLevel.EMERGENCY, message)
    }

    override fun alert(message: String) {
        this.log(LogLevel.ALERT, message)
    }

    override fun critical(message: String) {
        this.log(LogLevel.CRITICAL, message)
    }

    override fun error(message: String) {
        this.log(LogLevel.ERROR, message)
    }

    override fun warning(message: String) {
        this.log(LogLevel.WARNING, message)
    }

    override fun notice(message: String) {
        this.log(LogLevel.NOTICE, message)
    }

    override fun info(message: String) {
        this.log(LogLevel.INFO, message)
    }

    override fun debug(message: String) {
        this.log(LogLevel.DEBUG, message)
    }

    override fun log(level: LogLevel, message: String) {
        Server.instance.getLogger().log(level, this.pluginName + message)
    }

    override fun emergency(message: String, t: Throwable) {
        this.log(LogLevel.EMERGENCY, message, t)
    }

    override fun alert(message: String, t: Throwable) {
        this.log(LogLevel.ALERT, message, t)
    }

    override fun critical(message: String, t: Throwable) {
        this.log(LogLevel.CRITICAL, message, t)
    }

    override fun error(message: String, t: Throwable) {
        this.log(LogLevel.ERROR, message, t)
    }

    override fun warning(message: String, t: Throwable) {
        this.log(LogLevel.WARNING, message, t)
    }

    override fun notice(message: String, t: Throwable) {
        this.log(LogLevel.NOTICE, message, t)
    }

    override fun info(message: String, t: Throwable) {
        this.log(LogLevel.INFO, message, t)
    }

    override fun debug(message: String, t: Throwable) {
        this.log(LogLevel.DEBUG, message, t)
    }

    override fun log(level: LogLevel, message: String, t: Throwable) {
        Server.instance.getLogger().log(level, this.pluginName + message, t)
    }
}