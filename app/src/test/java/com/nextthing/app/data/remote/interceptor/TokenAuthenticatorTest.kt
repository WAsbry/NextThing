package com.nextthing.app.data.remote.interceptor

import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AuthApi
import com.nextthing.app.data.remote.dto.ApiResult
import com.nextthing.app.data.remote.dto.AuthResponse
import com.nextthing.app.data.remote.dto.RefreshRequest
import com.nextthing.app.data.remote.dto.UserDto
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response as RetrofitResponse

class TokenAuthenticatorTest {

    private lateinit var tokenManager: TokenManager
    private lateinit var refreshAuthApi: AuthApi
    private lateinit var authenticator: TokenAuthenticator

    @Before
    fun setUp() {
        tokenManager = mock()
        refreshAuthApi = mock()
        authenticator = TokenAuthenticator(tokenManager, refreshAuthApi)
    }

    @Test
    fun `refresh success code zero saves tokens and retries request`() = runBlocking {
        whenever(tokenManager.getAccessTokenOnce()).thenReturn("expired-access")
        whenever(tokenManager.refreshToken).thenReturn(flowOf("valid-refresh"))
        whenever(refreshAuthApi.refresh(RefreshRequest("valid-refresh"))).thenReturn(
            ApiResult(
                code = 0,
                message = "success",
                data = authResponse("new-access", "new-refresh")
            )
        )

        val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

        assertEquals("Bearer new-access", retry?.header("Authorization"))
        verify(tokenManager).saveTokens("new-access", "new-refresh")
        Unit
    }

    @Test
    fun `changed access token retries without refreshing again`() = runBlocking {
        whenever(tokenManager.getAccessTokenOnce()).thenReturn("already-refreshed")

        val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

        assertEquals("Bearer already-refreshed", retry?.header("Authorization"))
        verify(refreshAuthApi, never()).refresh(any())
        Unit
    }

    @Test
    fun `nonzero refresh result does not retry`() = runBlocking {
        whenever(tokenManager.getAccessTokenOnce()).thenReturn("expired-access")
        whenever(tokenManager.refreshToken).thenReturn(flowOf("invalid-refresh"))
        whenever(refreshAuthApi.refresh(RefreshRequest("invalid-refresh"))).thenReturn(
            ApiResult(code = 401, message = "expired", data = null)
        )

        val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

        assertNull(retry)
        verify(tokenManager).clear()
        verify(tokenManager, never()).saveTokens(any(), any())
        Unit
    }

    @Test
    fun `missing refresh token clears unusable local session`() = runBlocking {
        whenever(tokenManager.getAccessTokenOnce()).thenReturn("expired-access")
        whenever(tokenManager.refreshToken).thenReturn(flowOf(null))

        val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

        assertNull(retry)
        verify(tokenManager).clear()
        verify(refreshAuthApi, never()).refresh(any())
        Unit
    }

    @Test
    fun `temporary refresh server failure preserves session for later retry`() = runBlocking {
        whenever(tokenManager.getAccessTokenOnce()).thenReturn("expired-access")
        whenever(tokenManager.refreshToken).thenReturn(flowOf("valid-refresh"))
        whenever(refreshAuthApi.refresh(RefreshRequest("valid-refresh")))
            .thenThrow(httpException(503))

        val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

        assertNull(retry)
        verify(tokenManager, never()).clear()
        verify(tokenManager, never()).saveTokens(any(), any())
        Unit
    }

    @Test
    fun `refresh credential rejection clears unusable local session`() = runBlocking {
        whenever(tokenManager.getAccessTokenOnce()).thenReturn("expired-access")
        whenever(tokenManager.refreshToken).thenReturn(flowOf("invalid-refresh"))
        whenever(refreshAuthApi.refresh(RefreshRequest("invalid-refresh")))
            .thenThrow(httpException(400))

        val retry = authenticator.authenticate(null, unauthorizedResponse("expired-access"))

        assertNull(retry)
        verify(tokenManager).clear()
        verify(tokenManager, never()).saveTokens(any(), any())
        Unit
    }

    @Test
    fun `auth endpoints never trigger token refresh`() = runBlocking {
        val response = unauthorizedResponse(
            accessToken = "expired-access",
            path = "/api/auth/login"
        )

        assertNull(authenticator.authenticate(null, response))
        verify(tokenManager, never()).getAccessTokenOnce()
        verify(refreshAuthApi, never()).refresh(any())
        Unit
    }

    private fun unauthorizedResponse(
        accessToken: String,
        path: String = "/sync/full"
    ): Response {
        val request = Request.Builder()
            .url("https://example.com$path")
            .header("Authorization", "Bearer $accessToken")
            .build()
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(401)
            .message("Unauthorized")
            .build()
    }

    private fun authResponse(accessToken: String, refreshToken: String): AuthResponse {
        return AuthResponse(
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = 7_200_000,
            user = UserDto(
                id = 1,
                username = "smoke-user",
                email = "smoke@example.com",
                nickname = null,
                avatarUrl = null
            )
        )
    }

    private fun httpException(code: Int): HttpException {
        return HttpException(
            RetrofitResponse.error<ApiResult<AuthResponse>>(
                code,
                "{}".toResponseBody("application/json".toMediaType())
            )
        )
    }
}
