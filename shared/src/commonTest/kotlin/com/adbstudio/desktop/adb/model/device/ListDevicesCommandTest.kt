package com.adbstudio.desktop.adb.model.device

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ListDevicesCommandTest {

    @Test
    fun `parse empty output`() {
        val cmd = ListDevicesCommand()
        val result = cmd.parse("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse header only`() {
        val cmd = ListDevicesCommand()
        val result = cmd.parse("List of devices attached")
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse single device`() {
        val cmd = ListDevicesCommand()
        val result = cmd.parse("List of devices attached\nemulator-5554\tdevice")
        assertEquals(1, result.size)
        assertEquals("emulator-5554", result[0].serial)
        assertEquals("device", result[0].state)
    }

    @Test
    fun `parse multiple devices`() {
        val cmd = ListDevicesCommand()
        val output = """
            List of devices attached
            emulator-5554	device
            R58M1234ABC	unauthorized
            192.168.1.10:5555	device
        """.trimIndent()
        val result = cmd.parse(output)
        assertEquals(3, result.size)
        assertEquals("R58M1234ABC", result[1].serial)
        assertEquals("unauthorized", result[1].state)
        assertEquals("192.168.1.10:5555", result[2].serial)
    }

    @Test
    fun `parse offline device`() {
        val cmd = ListDevicesCommand()
        val output = "List of devices attached\n1234567890abcdef	offline"
        val result = cmd.parse(output)
        assertEquals(1, result.size)
        assertEquals("offline", result[0].state)
    }

    @Test
    fun `parse no device line`() {
        val cmd = ListDevicesCommand()
        val result = cmd.parse("List of devices attached\n\n")
        assertTrue(result.isEmpty())
    }
}
