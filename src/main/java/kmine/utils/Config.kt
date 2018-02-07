package kmine.utils

import com.google.gson.Gson
import kmine.jsonDecode
import kmine.jsonDecodeToArray
import kmine.pop
import java.io.File

class Config(file: String, type: Int = Config.DETECT, default: List<String> = emptyList(), var correct: Boolean = false) {
    companion object {
        const val DETECT = -1
        const val PROPERTIES = 0
        const val CNF = Config.PROPERTIES
        const val JSON = 1
        const val YAML = 2
        const val SERIALIZED = 4
        const val ENUM = 5
        const val ENUMERATION = Config.ENUM

        var formats = mapOf(
                "properties" to Config.PROPERTIES,
                "cnf" to Config.CNF,
                "conf" to Config.CNF,
                "config" to Config.CNF,
                "json" to Config.JSON,
                "js" to Config.JSON,
                "yml" to Config.YAML,
                "yaml" to Config.YAML,
                //"export" to Config.EXPORT,
                //"xport" to Config.EXPORT,
                "sl" to Config.SERIALIZED,
                "serialize" to Config.SERIALIZED,
                "txt" to Config.ENUM,
                "list" to Config.ENUM,
                "enum" to Config.ENUM
        )

        fun fixYAMLIndexes(str: String): String = str.replace("#^([ ]*)([a-zA-Z_]{1}[ ]*)\\:$#m", "$1\"$2\":")
    }

    private var config: List<String> = emptyList()
    private var nestedCache: List<String> = emptyList()

    private var file: String = ""
    private var type = Config.DETECT
//    private var jsonOptions:Int = JSON_PRETTY_PRINT | JSON_BIGINT_AS_STRING

    var changed = false

    init {
        load(file, type, default)
    }

    fun reload() {
        this.config = emptyList()
        this.nestedCache = emptyList()
        this.correct = false
        load(file, type)
    }

    fun load(file: String, type: Int = Config.DETECT, default: List<String> = emptyList()): Boolean {
        this.correct = true
        this.file = file

        this.type = type
        if (this.type == Config.DETECT) {
            val extension = File(file).name.split(".")
            val extensionStr = extension.pop().trim().toLowerCase()
            if (Config.formats[extensionStr] != null) {
                this.type = Config.formats[extensionStr]!!
            } else {
                this.correct = false
            }
        }

        val oFile = File(this.file);

        if (!oFile.exists()){
            this.config = default
            save()
        } else{
            if (correct){
                var content = oFile.readText()
                when(this.type){
                    Config.PROPERTIES or Config.CNF -> parseProperties(this.file)
                    Config.JSON -> config = content.jsonDecodeToArray()
                    Config.YAML -> {
                        content = fixYAMLIndexes(content)
                        TODO()
                    }
                }
            }
        }
    }

}