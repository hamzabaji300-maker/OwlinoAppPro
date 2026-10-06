package com.example
import org.junit.Test
class TestTime3 {
    @Test
    fun testParse() {
        val t = "14:11"
        try {
            println("Result: " + java.time.Instant.parse(t))
        } catch(e: Exception) {
            println("Error: " + e.message)
        }
    }
}
