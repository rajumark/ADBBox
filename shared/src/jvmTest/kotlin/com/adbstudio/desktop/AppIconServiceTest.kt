package com.adbstudio.desktop

import com.adbstudio.desktop.device.AppIconService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AppIconServiceTest {

    @Test
    fun `parse extractor output with valid JSON`() {
        val output = """{"packageInfos":[{"packageName":"com.example.app","label":"My App","iconCachePath":"/data/local/tmp/adbstudio/icons/com.example.app.123.png","apkSize":123456,"enabled":true,"system":false,"versionName":"1.0"},{"packageName":"com.example.other","label":"Other","iconCachePath":"","apkSize":78901,"enabled":false,"system":true,"versionName":"2.0"}]}"""

        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test")
        val result = service.parseExtractorOutput(output)

        assertEquals(2, result.size)

        with(result[0]) {
            assertEquals("com.example.app", packageName)
            assertEquals("My App", label)
            assertEquals("/data/local/tmp/adbstudio/icons/com.example.app.123.png", iconCachePath)
            assertEquals(123456L, apkSize)
        }

        with(result[1]) {
            assertEquals("com.example.other", packageName)
            assertEquals("Other", label)
            assertEquals("", iconCachePath)
            assertEquals(78901L, apkSize)
        }
    }

    @Test
    fun `parse extractor output with empty array`() {
        val output = """{"packageInfos":[]}"""
        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test2")
        val result = service.parseExtractorOutput(output)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse extractor output with special characters in label`() {
        val output = """{"packageInfos":[{"packageName":"com.example.app","label":"My Cool App!","iconCachePath":"/path/icon.png","apkSize":100}]}"""
        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test3")
        val result = service.parseExtractorOutput(output)
        assertEquals(1, result.size)
        assertEquals("My Cool App!", result[0].label)
    }

    @Test
    fun `parse extractor output with minimal fields`() {
        val output = """{"packageInfos":[{"packageName":"com.example.app"}]}"""
        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test4")
        val result = service.parseExtractorOutput(output)
        assertEquals(1, result.size)
        assertEquals("com.example.app", result[0].packageName)
        assertEquals("com.example.app", result[0].label)
        assertEquals("", result[0].iconCachePath)
        assertEquals(0, result[0].apkSize)
    }

    @Test
    fun `parse single entry`() {
        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test5")
        val json = """{"packageName":"com.test.app","label":"Test App","iconCachePath":"/cache/test.png","apkSize":5000}"""
        val result = service.parseEntry(json)
        assertNotNull(result)
        assertEquals("com.test.app", result.packageName)
        assertEquals("Test App", result.label)
        assertEquals("/cache/test.png", result.iconCachePath)
        assertEquals(5000L, result.apkSize)
    }

    @Test
    fun `extract json string`() {
        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test6")
        assertEquals("Hello", service.extractString("""{"name":"Hello"}""", "name"))
        assertEquals("test.value", service.extractString("""{"k":"test.value"}""", "k"))
        assertEquals(null, service.extractString("""{"other":"val"}""", "missing"))
    }

    @Test
    fun `extract json long`() {
        val service = AppIconService("/fake/adb", "/tmp/adbstudio-test7")
        assertEquals(12345L, service.extractLong("""{"size":12345}""", "size"))
        assertEquals(0L, service.extractLong("""{"size":0}""", "size"))
        assertEquals(null, service.extractLong("""{"other":"val"}""", "missing"))
    }

    @Test
    fun `icon extractor dex resource exists`() {
        val resource = AppIconService::class.java.classLoader
            ?.getResource("adbstudio-server.dex")
        assertNotNull(resource) { "adbstudio-server.dex resource not found" }
        val bytes = resource.openStream().readBytes()
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun `icon extractor java source exists`() {
        val sourceFile = java.io.File("../server/src/main/java/com/adbstudio/IconExtractor.java")
        assertTrue(sourceFile.exists(), "IconExtractor.java not found")
        val content = sourceFile.readText()
        assertTrue(content.contains("class IconExtractor"), "Missing class declaration")
        assertTrue(content.contains("getPackageInfo"), "Missing getPackageInfo")
        assertTrue(content.contains("drawableToBitmap"), "Missing drawableToBitmap")
    }

    @Test
    fun `build script exists`() {
        val buildScript = java.io.File("../server/build.sh")
        assertTrue(buildScript.exists(), "server/build.sh not found")
        assertTrue(buildScript.canExecute(), "build.sh not executable")
    }
}
