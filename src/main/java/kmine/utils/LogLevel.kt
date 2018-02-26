package kmine.utils

enum class LogLevel : Comparable<LogLevel> {
    NONE,
    EMERGENCY,
    ALERT,
    CRITICAL,
    ERROR,
    WARNING,
    NOTICE,
    INFO,
    DEBUG;

    companion object {
        val DEFAULT_LEVEL = INFO
    }

    fun getLevel(): Int = ordinal
}