package com.nextthing.app.data.remote.interceptor

import com.nextthing.app.data.preferences.TokenManager
import com.nextthing.app.data.remote.api.AuthApi
import com.nextthing.app.data.remote.dto.RefreshRequest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("auth-refresh") private val refreshAuthApi: AuthApi
) : Authenticator {

    private val refreshLock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.startsWith("/api/auth/")) return null
        if (responseCount(response) >= MAX_ATTEMPTS) return null

        val failedToken = response.request.header("Authorization")
            ?.removePrefix(BEARER_PREFIX)

        return synchronized(refreshLock) {
            runBlocking {
                val currentToken = tokenManager.getAccessTokenOnce()
                if (!currentToken.isNullOrBlank() && currentToken != failedToken) {
                    return@runBlocking retryWithToken(response, currentToken)
                }

                val refreshToken = tokenManager.refreshToken.first()
                    ?.takeIf { it.isNotBlank() }
                    ?: run {
                        Timber.w("Cannot refresh access token because refresh token is missing")
                        tokenManager.clear()
                        return@runBlocking null
                    }

                val result = try {
                    refreshAuthApi.refresh(RefreshRequest(refreshToken))
                } catch (error: HttpException) {
                    if (error.code() in DEFINITIVE_AUTH_HTTP_CODES) {
                        Timber.w("Refresh token rejected with HTTP ${error.code()}; clearing session")
                        tokenManager.clear()
                    } else {
                        Timber.w(error, "Refresh request failed with HTTP ${error.code()}")
                    }
                    return@runBlocking null
                } catch (error: Exception) {
                    Timber.w(error, "Refresh request failed; preserving session for a later retry")
                    return@runBlocking null
                }

                val auth = result.takeIf { it.code == 0 }?.data
                if (auth == null) {
                    if (result.code in DEFINITIVE_AUTH_RESULT_CODES) {
                        Timber.w("Refresh token rejected with business code ${result.code}; clearing session")
                        tokenManager.clear()
                    } else {
                        Timber.w("Refresh response was unusable with business code ${result.code}")
                    }
                    return@runBlocking null
                }

                tokenManager.saveTokens(auth.accessToken, auth.refreshToken)
                retryWithToken(response, auth.accessToken)
            }
        }
    }

    private fun retryWithToken(response: Response, token: String): Request {
        return response.request.newBuilder()
            .header("Authorization", "$BEARER_PREFIX$token")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val BEARER_PREFIX = "Bearer "
        const val MAX_ATTEMPTS = 2
        val DEFINITIVE_AUTH_HTTP_CODES = setOf(400, 401)
        val DEFINITIVE_AUTH_RESULT_CODES = setOf(400, 401, 1005, 1006, 1007)
    }
}
