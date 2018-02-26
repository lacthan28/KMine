package kmine.plugin

import jdk.tools.jlink.plugin.PluginException
import kmine.permission.Permission
import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import java.util.ArrayList
import java.util.HashMap

class PluginDescription {
    private var name: String = ""
    private var main: String = ""
    private lateinit var api: List<String>
    private var depend: List<String> = ArrayList()
    private var softDepend: List<String> = ArrayList()
    private var loadBefore: List<String> = ArrayList()
    private var version: String = ""
    private var commands: Map<String, Any> = HashMap()
    private var description: String = ""
    private val authors = ArrayList<String>()
    private var website: String = ""
    private var prefix: String = ""
    private var order = PluginLoadOrder.POSTWORLD

    private var permissions: List<Permission> = ArrayList()

    constructor(yamlMap: Map<String, Any>) {
        this.loadMap(yamlMap)
    }

    constructor(yamlString: String) {
        val dumperOptions: DumperOptions = DumperOptions()
        dumperOptions.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
        val yaml: Yaml = Yaml(dumperOptions)
        this.loadMap(yaml.loadAs(yamlString, LinkedHashMap::class.java) as Map<String, Any>)
    }

    @Throws(PluginException::class)
    private fun loadMap(plugin: Map<String, Any>) {
        this.name = (plugin["name"] as String).replace("[^A-Za-z0-9 _.-]".toRegex(), "")
        if (this.name == "") {
            throw PluginException("Invalid PluginDescription name")
        }
        this.name = this.name.replace(" ", "_")
        this.version = plugin["version"].toString()
        this.main = plugin["main"] as String
        val api = plugin["api"]
        if (api is List<*>) {
            this.api = api as List<String>
        } else {
            val list = ArrayList<String>()
            list.add(api as String)
            this.api = list
        }
        if (this.main.startsWith("cn.nukkit.")) {
            throw PluginException("Invalid PluginDescription main, cannot start within the cn.nukkit. package")
        }

        if (plugin.containsKey("commands") && plugin["commands"] is Map<*, *>) {
            this.commands = plugin["commands"] as Map<String, Any>
        }

        if (plugin.containsKey("depend")) {
            this.depend = plugin["depend"] as List<String>
        }

        if (plugin.containsKey("softdepend")) {
            this.softDepend = plugin["softdepend"] as List<String>
        }

        if (plugin.containsKey("loadbefore")) {
            this.loadBefore = plugin["loadbefore"] as List<String>
        }

        if (plugin.containsKey("website")) {
            this.website = plugin["website"] as String
        }

        if (plugin.containsKey("description")) {
            this.description = plugin["description"] as String
        }

        if (plugin.containsKey("prefix")) {
            this.prefix = plugin["prefix"] as String
        }

        if (plugin.containsKey("load")) {
            val order = plugin["load"] as String
            try {
                this.order = PluginLoadOrder.valueOf(order)
            } catch (e: Exception) {
                throw PluginException("Invalid PluginDescription load")
            }

        }

        if (plugin.containsKey("author")) {
            this.authors.add(plugin["author"] as String)
        }

        if (plugin.containsKey("authors")) {
            this.authors.addAll(plugin["authors"] as Collection<String>)
        }

        if (plugin.containsKey("permissions")) {
            this.permissions = Permission.loadPermissions(plugin["permissions"] as Map<String, Any>)
        }
    }

    fun getFullName(): String = "${this.name} v${this.version}"

    fun getCompatibleAPIs(): List<String> = api

    fun getAuthors(): List<String> = authors

    fun getPrefix(): String = prefix

    fun getCommands(): Map<String, Any> = commands

    fun getDepend(): List<String> = depend

    fun getDescription(): String = description

    fun getLoadBefore(): List<String> = loadBefore

    fun getMain(): String = main

    fun getName(): String = name

    fun getOrder(): PluginLoadOrder = order

    fun getPermissions(): List<Permission> = permissions

    fun getSoftDepend(): List<String> = softDepend

    fun getVersion(): String = version

    fun getWebsite(): String = website
}