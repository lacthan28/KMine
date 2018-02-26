package kmine.permission

import kmine.Server
import java.util.ArrayList
import java.util.HashMap

class Permission constructor(val name: String, description: String? = null, defualtValue: String? = null, children: Map<String, Boolean> = HashMap()) {

    var description: String? = null

    private var children: Map<String, Boolean> = HashMap()

    private var defaultValue: String? = null

    var default: String?
        get() = defaultValue
        set(value) {
            if (value != this.defaultValue) {
                this.defaultValue = value
                this.recalculatePermissibles()
            }
        }

    private val permissibles: Set<Permissible>
        get() = Server.instance.getPluginManager().getPermissionSubscriptions(this.name)

    init {
        this.description = description ?: ""
        this.defaultValue = defualtValue ?: DEFAULT_PERMISSION
        this.children = children

        this.recalculatePermissibles()
    }

    private fun getChildren(): MutableMap<String, Boolean> {
        return children.toMutableMap()
    }

    private fun recalculatePermissibles() {
        val perms = this.permissibles

        Server.instance.getPluginManager().recalculatePermissionDefaults(this)

        for (p in perms) {
            p.recalculatePermissions()
        }
    }

    private fun addParent(permission: Permission, value: Boolean) {
        this.getChildren()[this.name] = value
        permission.recalculatePermissibles()
    }

    fun addParent(name: String, value: Boolean): Permission {
        var perm = Server.instance.getPluginManager().getPermission(name)
        if (perm == null) {
            perm = Permission(name)
            Server.instance.getPluginManager().addPermission(perm)
        }

        this.addParent(perm, value)

        return perm
    }

    companion object {

        private const val DEFAULT_OP = "op"
        private const val DEFAULT_NOT_OP = "notop"
        private const val DEFAULT_TRUE = "true"
        private const val DEFAULT_FALSE = "false"

        const val DEFAULT_PERMISSION = DEFAULT_OP

        private fun getByName(value: String): String {
            return when (value.toLowerCase()) {
                "op", "isop", "operator", "isoperator", "admin", "isadmin" -> DEFAULT_OP

                "!op", "notop", "!operator", "notoperator", "!admin", "notadmin" -> DEFAULT_NOT_OP

                "true" -> DEFAULT_TRUE

                else -> DEFAULT_FALSE
            }
        }

        fun loadPermissions(data: Map<String, Any>?, defaultValue: String = DEFAULT_OP): List<Permission> {
            val result = ArrayList<Permission>()
            if (data != null) {
                for ((key1, value) in data) {
                    val entry = value as Map<String, Any>
                    result.add(loadPermission(key1, entry, defaultValue, result))
                }
            }
            return result
        }

        private fun loadPermission(name: String, data: Map<String, Any>, defaultValue: String = DEFAULT_OP, output: MutableList<Permission> = ArrayList()): Permission {
            var defaultValue = defaultValue
            var desc: String? = null
            val children = HashMap<String, Boolean>()
            if (data.containsKey("default")) {
                val value = Permission.getByName(data["default"].toString())
                defaultValue = value
            }

            if (data.containsKey("children")) {
                if (data["children"] is Map<*, *>) {
                    for ((key, v) in data["children"] as Map<String, Any>) {
                        if (v is Map<*, *>) {
                            val permission = loadPermission(key, v as Map<String, Any>, defaultValue, output)
                            output.add(permission)
                        }
                        children[key] = true
                    }
                } else {
                    throw IllegalStateException("'children' key is of wrong type")
                }
            }

            if (data.containsKey("description")) {
                desc = data["description"] as String
            }

            return Permission(name, desc, defaultValue, children)
        }
    }

}