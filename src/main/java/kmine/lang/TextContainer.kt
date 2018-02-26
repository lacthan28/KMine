package kmine.lang

import kmine.Server

open class TextContainer(private var text: String) : Cloneable {

    fun setText(text: String) {
        this.text = text
    }

    fun getText(): String {
        return text
    }

    override fun toString(): String {
        return this.getText()
    }

    override fun clone(): Any {
        try {
            return super.clone() as TextContainer
        } catch (e: CloneNotSupportedException) {
            Server.instance.getLogger().logException(e)
        }
        return ""
    }
}