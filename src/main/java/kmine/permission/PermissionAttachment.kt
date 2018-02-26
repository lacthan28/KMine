package kmine.permission

import kmine.plugin.Plugin
import kmine.utils.PluginException
import java.util.HashMap

class PermissionAttachment(private var plugin: Plugin, private var permissible: Permissible) {
    private lateinit var removalCallback: PermissionRemovedExecutor

    private val permissions: Map<String, Boolean> = HashMap()

    init {
        if (!plugin.isEnabled()) throw PluginException("Plugin ${plugin.getDescription().getName()} is disabled")
    }

    fun getPlugin() = plugin

    fun setRemovalCallback(executor: PermissionRemovedExecutor) {
        this.removalCallback = executor
    }

    fun getRemovalCallback(): PermissionRemovedExecutor = removalCallback

    fun getPermissions(): Map<String, Boolean> = permissions

    fun clearPermissions() {
        this.permissions.toMutableMap().clear()
        this.permissible.recalculatePermissions()
    }

    fun setPermissions(permissions: Map<String, Boolean>) {
        for ((key, value) in permissions) {
            this.permissions.toMutableMap()[key] = value
        }
        this.permissible.recalculatePermissions()
    }

    fun unsetPermissions(permissions: List<String>) {
        for (node in permissions) {
            this.permissions.toMutableMap().remove(node)
        }
        this.permissible.recalculatePermissions()
    }

    fun setPermission(permission: Permission, value: Boolean) {
        this.setPermission(permission.name, value)
    }

    private fun setPermission(name: String, value: Boolean) {
        if (this.permissions.containsKey(name)) {
            if (this.permissions[name] == value) {
                return
            }
            this.permissions.toMutableMap().remove(name)
        }
        this.permissions.toMutableMap()[name] = value
        this.permissible.recalculatePermissions()
    }

    fun unsetPermission(permission: Permission) {
        this.unsetPermission(permission.name)
    }

    private fun unsetPermission(name: String) {
        if (this.permissions.containsKey(name)) {
            this.permissions.toMutableMap().remove(name)
            this.permissible.recalculatePermissions()
        }
    }

    fun remove() {
        this.permissible.removeAttachment(this)
    }
}