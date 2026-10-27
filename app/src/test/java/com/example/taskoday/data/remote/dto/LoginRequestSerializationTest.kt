package com.example.taskoday.data.remote.dto

import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginRequestSerializationTest {
    @Test
    fun `login payload serializes long email and password symbols unchanged`() {
        val emailFixture = "a".repeat(29) + "@example.co"
        val passwordFixture = "Test !@# $%^&*()_+-=[]{}:;,.?"
        assertEquals(40, emailFixture.length)

        val json = JsonParser.parseString(
            GsonBuilder().create().toJson(LoginRequestDto(emailFixture, passwordFixture)),
        ).asJsonObject

        assertEquals(setOf("email", "password"), json.keySet())
        assertEquals(emailFixture, json.get("email").asString)
        assertEquals(passwordFixture, json.get("password").asString)
        assertTrue(json.get("email").asString.contains('@'))
    }
}
