package com.example
import org.junit.Test
class TestTime {
    @Test
    fun testParse() {
        val t = "2026-08-13T14:11:03.123456+00"
        try {
            println("Result: " + java.time.Instant.parse(t))
        } catch(e: Exception) {
            println("Error: " + e.message)
        }
    }
}
