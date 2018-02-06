package kmine.command

interface CommandMap {
    /**
     * @param fallbackPrefix String
     * @param commands Command[]
     */
    fun registerAll(fallbackPrefix: String, commands: Array<Command>)

    /**
     * @param fallbackPrefix String
     * @param command Command
     * @param label String?
     *
     * @return Boolean
     */
    fun register(fallbackPrefix: String, command: Command, label: String? = null): Boolean

    /**
     * @param CommandSender $sender
     * @param string        $cmdLine
     *
     * @return bool
     */
    public function dispatch(CommandSender $sender, string $cmdLine) : bool;

    /**
     * @return void
     */
    public function clearCommands();

    /**
     * @param string $name
     *
     * @return Command|null
     */
    public function getCommand(string $name)
}