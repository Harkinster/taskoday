package com.example.taskoday.features.shop

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class ChestOpeningPolicyTest {
    @Test
    fun `insufficient balance disables opening and shows balance over cost`() {
        val state =
            chestActionState(
                crystalsBalance = 1,
                crystalCost = 3,
                hasRemoteCatalog = true,
                isSubmitting = false,
            )

        assertFalse(state.enabled)
        assertEquals("Cristaux insuffisants", state.label)
        assertEquals("1 / 3 Cristaux", state.balanceLabel)
    }

    @Test
    fun `submitting disables opening to prevent a second request`() {
        val state =
            chestActionState(
                crystalsBalance = 10,
                crystalCost = 3,
                hasRemoteCatalog = true,
                isSubmitting = true,
            )

        assertFalse(state.enabled)
        assertEquals("Ouverture...", state.label)
    }

    @Test
    fun `sufficient balance enables opening`() {
        val state =
            chestActionState(
                crystalsBalance = 3,
                crystalCost = 3,
                hasRemoteCatalog = true,
                isSubmitting = false,
            )

        assertTrue(state.enabled)
        assertEquals("Ouvrir", state.label)
        assertEquals("3 / 3 Cristaux", state.balanceLabel)
    }

    @Test
    fun `backend insufficient crystal response becomes a precise message`() {
        val body =
            """{"success":false,"error":{"code":"BAD_REQUEST","message":"Cristaux insuffisants: balance=1, required=3."}}"""
                .toResponseBody("application/json".toMediaType())
        val error = HttpException(Response.error<String>(400, body))

        assertEquals(
            "Il te manque 2 Cristaux pour ouvrir ce coffre.",
            error.toChestOpenUserMessage(),
        )
    }
}
