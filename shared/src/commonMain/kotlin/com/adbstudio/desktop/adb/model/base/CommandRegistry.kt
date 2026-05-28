package com.adbstudio.desktop.adb.model.base

/**
 * Central registry for all ADB command metadata (§3, §14).
 *
 * - Populated at startup with [CommandMetadata] for every registered command.
 * - Powers command palette, docs, warnings, filtering, AI tooling.
 * - Thread-safe: registrations happen once at init, reads are concurrent.
 */
class CommandRegistry {
    private val _commands = mutableMapOf<String, CommandMetadata>()

    /** All registered command metadata, keyed by command id. */
    val commands: Map<String, CommandMetadata> get() = _commands

    /** Register a single command's metadata. */
    fun register(metadata: CommandMetadata) {
        _commands[metadata.id] = metadata
    }

    /** Register multiple command metadata entries. */
    fun registerAll(entries: List<CommandMetadata>) {
        entries.forEach { register(it) }
    }

    /** Look up metadata by command id. */
    fun get(id: String): CommandMetadata? = _commands[id]

    /** All commands in a given category. */
    fun byCategory(category: CommandCategory): List<CommandMetadata> =
        _commands.values.filter { it.category == category }

    /** All dangerous commands. */
    fun dangerousCommands(): List<CommandMetadata> =
        _commands.values.filter { it.dangerous }

    /** All commands requiring root. */
    fun rootCommands(): List<CommandMetadata> =
        _commands.values.filter { it.requiresRoot }

    /** Search commands by query string (matches id, name, description). */
    fun search(query: String): List<CommandMetadata> {
        val q = query.lowercase()
        return _commands.values.filter {
            it.id.lowercase().contains(q) ||
                    it.displayName.lowercase().contains(q) ||
                    it.description.lowercase().contains(q)
        }
    }

    /** Commands compatible with a given API level. */
    fun forApiLevel(apiLevel: Int): List<CommandMetadata> =
        _commands.values.filter { it.minApi == null || it.minApi <= apiLevel }
}
