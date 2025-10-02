package cz.deels.admin

import com.apollographql.apollo.api.Optional
import com.apollographql.cache.normalized.watch
import cz.deels.graphql.OrdersQuery
import cz.deels.graphql.type.OrderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Instant

data class Connection<T>(
    val nodes: List<T>,
    val cursor: Any?,
    val totalCount: Int,
)

data class OrdersFilter(
    val states: List<OrderState>,
    val from: Instant,
    val to: Instant,
)

data class Order(
    val id: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val totalAmount: Double,
    val currency: String,
    val status: OrderState,
    val store: Store,
    val items: List<Item>,
) {
    data class Store(
        val id: String,
        val name: String,
        val address: String,
    )

    data class Item(
        val id: String,
        val name: String,
        val price: Double,
        val type: Type,
    ) {
        enum class Type {
            BUNDLE, DISCOUNT, LOYALTY
        }
    }
}

fun Apollo.ordersFlow(filter: OrdersFilter): Flow<Connection<Order>?> = client
    .query(
        OrdersQuery(
            filteredStates = filter.states,
            after = filter.from,
            before = filter.to
        )
    )
    .watch()
    .map { response ->
        response.data?.orders?.let { data ->
            Connection(
                nodes = data.nodes.map(OrdersQuery.Node::toOrder),
                cursor = data.pageInfo.endCursor.takeIf { data.pageInfo.hasNextPage },
                totalCount = data.totalCount,
            )
        }
    }

suspend fun Apollo.nextOrdersPage(filter: OrdersFilter, cursor: Any) {
    client
        .query(
            OrdersQuery(
                cursor = Optional.present(cursor),
                filteredStates = filter.states,
                after = filter.from,
                before = filter.to
            )
        )
        .executeFromNetwork()
}

private fun OrdersQuery.Node.toOrder(): Order = with(orderFragment.orderInfoFragment) {
    Order(
        id = id,
        createdAt = created,
        expiresAt = expiration,
        totalAmount = totalSum,
        currency = currency.rawValue,
        status = state,
        store = with(orderFragment.store.storeFragment) {
            Order.Store(
                id = id,
                name = name,
                address = address,
            )
        },
        items = with(orderFragment.orderItemsFragment) {
            bundleItems.nodes.map {
                Order.Item(
                    id = it.bundleSnapshot.id,
                    name = it.bundleSnapshot.name,
                    price = it.paidProductsPriceSum,
                    type = Order.Item.Type.BUNDLE
                )
            } + discountItems.nodes.map {
                Order.Item(
                    id = it.discountSnapshot.id,
                    name = it.discountSnapshot.name,
                    price = it.productPrice,
                    type = Order.Item.Type.DISCOUNT
                )
            } + loyaltyItems.nodes.map {
                Order.Item(
                    id = it.loyaltySnapshot.id,
                    name = it.loyaltySnapshot.name,
                    price = 0.0,
                    type = Order.Item.Type.LOYALTY
                )
            }
        }
    )
}
