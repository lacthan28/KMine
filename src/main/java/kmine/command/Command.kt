package kmine.command

import kmine.event.TimingsHandler

/**
 * @param name String
 *
 */
abstract class Command(var name: String, description: String = "", usageMessage: String? = null, aliases: Array<String> = emptyArray()) {

    /** @param commandData array */
    protected var commandData: Array<String>? = null

    /** @param nextLabel String */
    private var nextLabel = ""

    /** @param label String */
    private var label = ""

    /** @param aliases String[] */
    private var aliases = emptyArray<String>()

    /**
     * @param activeAliases String[]
     */
    private var activeAliases = emptyArray<String>()

    /** @param commandMap CommandMap */
    private var commandMap: CommandMap? = null

    /** @param string */
    protected var description = ""

    /** @param usageMessage String */
    protected var usageMessage: String? = null

    /** @param permission String */
    private var permission: String? = null

    /** @param permissionMessage string */
    private var permissionMessage: String? = null

    /** @param TimingsHandler */
    public lateinit var timings: TimingsHandler

    /**
     * @param name String
     * @param description String
     * @param usageMessage String
     * @param aliases String[]
     */
    init {
        setLabel(name)
        setDescription(description)
        this.usageMessage = usageMessage ?: ("/$name")
        setAliases(aliases)
    }

    /**
     * @param sender CommandSender
     * @param commandLabel String
     * @param args String[]
     *
     * @return mixed
     */
    abstract fun execute(sender: CommandSender, commandLabel: String, args: Array<String>)

}