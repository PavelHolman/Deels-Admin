package cz.deels.admin

import arrow.core.Either
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.appsupport.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import org.publicvalue.multiplatform.oidc.types.remote.AccessTokenResponse

expect val flowFactory: CodeAuthFlowFactory

object Oidc {
    private const val DISCOVERY_URI = "https://auth.deels.cz/.well-known/openid-configuration"
    val client = OpenIdConnectClient(discoveryUri = DISCOVERY_URI) {
        clientId = "303076730095075332"
        scope = listOf(
            "openid",
            "profile",
            "phone",
            "email",
            "offline_access",
            "urn:zitadel:iam:user:metadata",
            "urn:zitadel:iam:org:project:id:312589975231201284:aud"
        ).joinToString(" ")
        codeChallengeMethod = CodeChallengeMethod.S256
        redirectUri = "cz.deels.app://callback"
        postLogoutRedirectUri = "cz.deels.app://callback"
    }

    private val tokenFlow = MutableStateFlow<AccessTokenResponse?>(null)
    val isLoggedInFlow = tokenFlow.map { it != null }

    fun getAccessToken(): String? {
        return tokenFlow.value?.access_token
    }

    suspend fun login() = Either.catch {
        tokenFlow.value = flowFactory.createAuthFlow(client).getAccessToken()
    }

    suspend fun logout() = Either.catch {
        flowFactory.createEndSessionFlow(client).endSession(tokenFlow.value?.id_token)
        tokenFlow.value = null
    }
}
