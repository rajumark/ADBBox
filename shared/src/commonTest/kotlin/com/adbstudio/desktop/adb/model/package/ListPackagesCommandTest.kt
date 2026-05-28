package com.adbstudio.desktop.adb.model.`package`

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListPackagesCommandTest {

    @Test
    fun `parse empty output`() {
        val cmd = ListPackagesCommand(serial = null)
        val result = cmd.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse single package`() {
        val cmd = ListPackagesCommand(serial = null)
        val result = cmd.parse("package:com.example.app")
        assertEquals(1, result.size)
        assertEquals("com.example.app", result[0])
    }

    @Test
    fun `parse multiple packages`() {
        val output = """
            package:com.android.chrome
            package:com.google.android.gm
            package:com.example.test
        """.trimIndent()
        val cmd = ListPackagesCommand(serial = null)
        val result = cmd.parse(output)
        assertEquals(3, result.size)
        assertEquals("com.android.chrome", result[0])
        assertEquals("com.google.android.gm", result[1])
        assertEquals("com.example.test", result[2])
    }

    @Test
    fun `parse non package lines are skipped`() {
        val output = """
            package:com.example.app
            some random output
            package:com.example.other
        """.trimIndent()
        val cmd = ListPackagesCommand(serial = null)
        val result = cmd.parse(output)
        assertEquals(2, result.size)
    }

    @Test
    fun `toCliArgs includes serial and filter`() {
        val cmd = ListPackagesCommand(serial = "test123", filter = "-3")
        val args = cmd.toCliArgs()
        assertTrue(args.contains("-s"))
        assertTrue(args.contains("test123"))
        assertTrue(args.contains("-3"))
    }

    @Test
    fun `toCliArgs excludes serial when null`() {
        val cmd = ListPackagesCommand(serial = null)
        val args = cmd.toCliArgs()
        assertTrue(!args.contains("-s"))
    }
}
