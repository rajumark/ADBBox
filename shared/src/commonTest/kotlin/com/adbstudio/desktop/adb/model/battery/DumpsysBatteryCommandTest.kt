package com.adbstudio.desktop.adb.model.battery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DumpsysBatteryCommandTest {

    @Test
    fun `parse empty output returns empty entries`() {
        val cmd = DumpsysBatteryCommand(serial = "test")
        val result = cmd.parse("")
        assertTrue(result.entries.isEmpty())
        assertEquals("", result.raw.trim())
    }

    @Test
    fun `parse battery dump extracts key-value pairs`() {
        val output = """
            Current Battery Service state:
              AC powered: true
              USB powered: false
              status: 5
              health: 2
              present: true
              level: 85
              scale: 100
              voltage: 4200
              temperature: 300
              technology: Li-ion
        """.trimIndent()

        val result = DumpsysBatteryCommand(serial = "test").parse(output)
        assertEquals(10, result.entries.size)

        val entriesMap = result.entries.toMap()
        assertEquals("true", entriesMap["AC powered"])
        assertEquals("5", entriesMap["status"])
        assertEquals("85", entriesMap["level"])
        assertEquals("Li-ion", entriesMap["technology"])
    }

    @Test
    fun `parse deduplicates keys`() {
        val output = """
            Current Battery Service state:
              level: 85
              level: 90
        """.trimIndent()

        val result = DumpsysBatteryCommand(serial = "test").parse(output)
        val entriesMap = result.entries.toMap()
        assertEquals(1, entriesMap.size)
        assertEquals("85", entriesMap["level"])
    }

    @Test
    fun `parse lines without colon are skipped`() {
        val output = """
            Current Battery Service state:
              level: 85
              some line without colon
        """.trimIndent()

        val result = DumpsysBatteryCommand(serial = "test").parse(output)
        val entriesMap = result.entries.toMap()
        assertEquals(1, entriesMap.size)
        assertEquals("85", entriesMap["level"])
    }

    @Test
    fun `toCliArgs includes serial`() {
        val cmd = DumpsysBatteryCommand(serial = "test123")
        val args = cmd.toCliArgs()
        assertTrue(args.contains("-s"))
        assertTrue(args.contains("test123"))
        assertTrue(args.contains("dumpsys"))
        assertTrue(args.contains("battery"))
    }

    @Test
    fun `toCliArgs excludes serial when null`() {
        val cmd = DumpsysBatteryCommand(serial = null)
        val args = cmd.toCliArgs()
        assertTrue(!args.contains("-s"))
    }
}
