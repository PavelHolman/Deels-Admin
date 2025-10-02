package cz.deels.admin

import com.apollographql.apollo.ApolloCall
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
import com.apollographql.cache.normalized.CacheManager
import com.apollographql.cache.normalized.FetchPolicy
import com.apollographql.cache.normalized.api.ConnectionMetadataGenerator
import com.apollographql.cache.normalized.api.ConnectionRecordMerger
import com.apollographql.cache.normalized.cacheManager
import com.apollographql.cache.normalized.fetchPolicy
import com.apollographql.cache.normalized.memory.MemoryCacheFactory
import com.apollographql.cache.normalized.refetchPolicy
import cz.deels.graphql.pagination.Pagination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.last
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

object Apollo {
    val client: ApolloClient = ApolloClient.Builder()
        .serverUrl("https://deels.cz/graphql")
        .cacheManager(
            CacheManager(
                normalizedCacheFactory = MemoryCacheFactory(maxSizeBytes = 10 * 1024 * 1024),
                metadataGenerator = ConnectionMetadataGenerator(Pagination.connectionTypes),
                recordMerger = ConnectionRecordMerger,
            )
        )
        .fetchPolicy(FetchPolicy.CacheAndNetwork)
        .addHttpInterceptor(AuthInterceptor())
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

    private class AuthInterceptor() : HttpInterceptor {
        override suspend fun intercept(
            request: HttpRequest,
            chain: HttpInterceptorChain
        ) = request.newBuilder().run {
            Oidc.getAccessToken()?.let { token ->
                addHeader("Authorization", "Bearer $token")
            }
            chain.proceed(build())
        }
    }

    object BigDecimalAdapter : Adapter<Double> {
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

    object InstantAdapter : Adapter<Instant> {
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

internal suspend fun <D> ApolloCall<D>.executeFromNetwork(): ApolloResponse<D> where D : Operation.Data {
    return fetchPolicy(FetchPolicy.NetworkOnly)
        .refetchPolicy(FetchPolicy.NetworkOnly)
        .toFlow()
        .last()
}
