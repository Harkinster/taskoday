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

    @Test
    fun `register parent request uses display name and birth date fields`() {
        val json = gson.toJson(
            RegisterParentRequestDto(
                displayName = "Matthieu",
                birthDate = "1990-09-21",
                email = "parent@example.test",
                password = "secret",
            ),
        )

        assertEquals(
            "{\"display_name\":\"Matthieu\",\"email\":\"parent@example.test\",\"password\":\"secret\",\"birth_date\":\"1990-09-21\"}",
            json,
        )
    }

    @Test
    fun `legacy me payload without identity fields remains compatible`() {
        val user = gson.fromJson(
            """
            {
              "id": 7,
              "email": "legacy@example.test",
              "role": "PARENT",
              "is_active": true,
              "family_ids": []
            }
            """.trimIndent(),
            MeResponseDto::class.java,
        ).toDomain()

        assertEquals("legacy", user.displayName)
        assertNull(user.birthDate)
        assertEquals(emptyList<Long>(), user.familyIds)
    }

    @Test
    fun `profile update request uses display name and birth date`() {
        val json = gson.toJson(UpdateProfileRequestDto("Matthieu", "1990-09-21"))

        assertEquals(
            "{\"display_name\":\"Matthieu\",\"birth_date\":\"1990-09-21\"}",
            json,
        )
    }
}
