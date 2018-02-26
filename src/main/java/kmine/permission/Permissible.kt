package kmine.permission

import kmine.plugin.Plugin

interface Permissible : ServerOperator {
    abstract fun isPermissionSet(name: String): Boolean

    abstract fun isPermissionSet(permission: Permission): Boolean

    abstract fun hasPermission(name: String): Boolean

    abstract fun hasPermission(permission: Permission): Boolean

    abstract fun addAttachment(plugin: Plugin): PermissionAttachment

    abstract fun addAttachment(plugin: Plugin, name: String): PermissionAttachment

    abstract fun addAttachment(plugin: Plugin, name: String, value: Boolean?): PermissionAttachment

    abstract fun removeAttachment(attachment: PermissionAttachment)

    abstract fun recalculatePermissions()

    abstract fun getEffectivePermissions(): Map<String, PermissionAttachmentInfo>
}