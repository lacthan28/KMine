package kmine.plugin

import kmine.Server
import kmine.command.CommandExecutor
import kmine.utils.Config
import java.io.File
import java.io.InputStream

interface Plugin: CommandExecutor {
    fun onLoad()

    fun onEnable()

    fun isEnabled():Boolean

    fun onDisable()

    fun isDisabled():Boolean

    fun getDataFolder(): File

    fun getDescription(): PluginDescription

    fun getResource(filename: String): InputStream

    fun saveResource(filename: String): Boolean

    fun saveResource(filename: String, replace: Boolean): Boolean

    fun saveResource(filename: String, outputName: String, replace: Boolean): Boolean

    fun getConfig(): Config

    fun saveConfig()

    fun saveDefaultConfig()

    fun reloadConfig()

    fun getServer(): Server

    fun getName(): String

    fun getLogger(): PluginLogger

    fun getPluginLoader(): PluginLoader
}