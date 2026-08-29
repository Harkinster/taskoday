package com.example.taskoday.data.remote.dto

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AuthDtosTest {
    private val gson = Gson()

    @Test
    fun `login and register token response reads refresh contract`() {
        val dto =
            gson.fromJson(
                """
                {
                  "access_token": "access-value",
                  "token_type": "bearer",
                  "expires_in": 7200,
                  "role": "PARENT",
                  "refresh_token": "refresh-value",
                  "refresh_expires_in": 2592000
                }
                """.trimIndent(),
                TokenResponseDto::class.java,
            )

        assertEquals("access-value", dto.accessToken)
        assertEquals("refresh-value", dto.refreshToken)
        assertEquals(2_592_000, dto.refreshExpiresIn)
        assertEquals("access-value", dto.toDomain().accessToken)
    }

    @Test
    fun `legacy token response without refresh remains compatible`() {
        val dto =
            gson.fromJson(
                """
                {
                  "access_token": "legacy-access",
                  "token_type": "bearer",
                  "expires_in": 7200,
                  "role": "CHILD"
                }
                """.trimIndent(),
                TokenResponseDto::class.java,
            )

        assertEquals("legacy-access", dto.accessToken)
        assertNull(dto.refreshToken)
        assertNull(dto.refreshExpiresIn)
    }

    @Test
    fun `refresh request uses backend field name`() {
        val json = gson.toJson(RefreshTokenRequestDto(refreshToken = "refresh-value"))

        assertEquals("{\"refresh_token\":\"refresh-value\"}", json)
    }
}
