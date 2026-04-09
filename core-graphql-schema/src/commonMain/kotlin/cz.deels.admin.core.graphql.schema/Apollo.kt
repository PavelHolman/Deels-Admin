package cz.deels.admin.core.graphql.schema

import com.apollographql.apollo.ApolloClient
import com.apollographql.apollo.api.Adapter
import com.apollographql.apollo.api.ApolloRequest
import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.CustomScalarAdapters
import com.apollographql.apollo.api.Operation
import com.apollographql.apollo.api.http.HttpRequest
import com.apollographql.apollo.api.json.JsonNumber
import com.apollographql.apollo.api.json.JsonReader
import com.apollographql.apollo.api.json.JsonWriter
import com.apollographql.apollo.exception.CacheMissException
import com.apollographql.apollo.interceptor.ApolloInterceptor
import com.apollographql.apollo.interceptor.ApolloInterceptorChain
import com.apollographql.apollo.network.http.HttpInterceptor
import com.apollographql.apollo.network.http.HttpInterceptorChain
import com.apollographql.apollo.network.http.LoggingInterceptor
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.fetchPolicy
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import cz.deels.admin.core.graphql.cache.Cache.cache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import kotlin.uuid.Uuid

public data class Cursor(val value: String)

public class Apollo(tokenProvider: TokenProvider) {
    public fun interface TokenProvider {
        public suspend fun getAccessToken(): String?
    }

    public val client: ApolloClient = ApolloClient.Builder()
        .serverUrl("https://deels.cz/graphql")
        .cache(MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024))
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .addHttpInterceptor(AuthInterceptor(tokenProvider))
        .addHttpInterceptor(LoggingInterceptor())
        .ignoreCacheMisses()
        .build()

    private fun ApolloClient.Builder.ignoreCacheMisses() = apply {
        addInterceptor(object : ApolloInterceptor {
            override fun <D : Operation.Data> intercept(
                request: ApolloRequest<D>,
                chain: ApolloInterceptorChain
            ): Flow<ApolloResponse<D>> {
                return chain.proceed(request).filter { it.exception !is CacheMissException }
            }
        })
    }

    private class AuthInterceptor(
        private val tokenProvider: TokenProvider,
    ) : HttpInterceptor {
        override suspend fun intercept(
            request: HttpRequest,
            chain: HttpInterceptorChain
        ) = request.newBuilder().run {
            tokenProvider.getAccessToken()?.let { token ->
                addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(build())
        }
    }

    public object UuidAdapter : Adapter<Uuid> {
        override fun fromJson(
            reader: JsonReader,
            customScalarAdapters: CustomScalarAdapters
        ): Uuid {
            return Uuid.parse(reader.nextString()!!)
        }

        override fun toJson(
            writer: JsonWriter,
            customScalarAdapters: CustomScalarAdapters,
            value: Uuid,
        ) {
            writer.value(value.toString())
        }
    }

    public object BigDecimalAdapter : Adapter<Double> {
        override fun fromJson(
            reader: JsonReader,
            customScalarAdapters: CustomScalarAdapters
        ): Double {
            return reader.nextString()!!.toDouble()
        }

        override fun toJson(
            writer: JsonWriter,
            customScalarAdapters: CustomScalarAdapters,
            value: Double,
        ) {
            writer.value(JsonNumber(value.toString()))
        }
    }

    public object InstantAdapter : Adapter<Instant> {
        override fun fromJson(
            reader: JsonReader,
            customScalarAdapters: CustomScalarAdapters
        ): Instant {
            return InstantSerializer.parse(reader.nextString()!!)
        }

        override fun toJson(
            writer: JsonWriter,
            customScalarAdapters: CustomScalarAdapters,
            value: Instant
        ) {
            writer.value(value.toLocalDateTime(TimeZone.UTC).toString())
        }
    }

    public object CursorAdapter : Adapter<Cursor> {
        override fun fromJson(
            reader: JsonReader,
            customScalarAdapters: CustomScalarAdapters,
        ): Cursor {
            return Cursor(reader.nextString()!!)
        }

        override fun toJson(
            writer: JsonWriter,
            customScalarAdapters: CustomScalarAdapters,
            value: Cursor,
        ) {
            writer.value(value.value)
        }
    }

    // Borrowed from Deeler
    private object InstantSerializer {
        fun parse(value: String): Instant = try {
            Instant.parse(value)
        } catch (e1: Exception) {
            try {
                LocalDateTime.parse(value).toInstant(TimeZone.UTC)
            } catch (e2: Exception) {
                throw IllegalArgumentException("Cannot parse datetime string: $value", e2).also {
                    it.addSuppressed(e1)
                }
            }
        }
    }
}
