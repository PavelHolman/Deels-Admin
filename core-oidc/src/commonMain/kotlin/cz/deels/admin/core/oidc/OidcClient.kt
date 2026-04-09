package cz.deels.admin.core.oidc

import arrow.core.Either
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import org.publicvalue.multiplatform.oidc.OpenIdConnectClient
import org.publicvalue.multiplatform.oidc.flows.CodeAuthFlowFactory
import org.publicvalue.multiplatform.oidc.types.CodeChallengeMethod
import org.publicvalue.multiplatform.oidc.types.remote.AccessTokenResponse

internal expect val flowFactory: CodeAuthFlowFactory

public class OidcClient {
    private companion object {
        private const val DISCOVERY_URI = "https://auth.deels.cz/.well-known/openid-configuration"
    }

    public val client: OpenIdConnectClient = OpenIdConnectClient(discoveryUri = DISCOVERY_URI) {
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
    public val isLoggedInFlow: Flow<Boolean> = tokenFlow.map { it != null }

    public fun getAccessToken(): String? = tokenFlow.value?.access_token

    public suspend fun login(): Either<Throwable, Unit> = Either.catch {
        tokenFlow.value = flowFactory.createAuthFlow(client).getAccessToken()
    }

    public suspend fun logout(): Either<Throwable, Unit> = Either.catch {
        flowFactory.createEndSessionFlow(client).endSession(tokenFlow.value?.id_token)
        tokenFlow.value = null
    }
}
