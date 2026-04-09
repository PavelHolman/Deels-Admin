package cz.deels.admin.app

import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.getOrNull
import com.apollographql.cache.normalized.watch
import cz.deels.admin.app.graphql.OrdersQuery
import cz.deels.admin.core.graphql.schema.Apollo
import cz.deels.admin.core.graphql.schema.Cursor
import cz.deels.admin.core.graphql.type.OrderState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant

data class Connection<T>(
    val nodes: List<T>,
    val cursor: Cursor?,
    val totalCount: Int,
)

enum class OrderStatus {
    CREATION_DUMMY,
    NEW,
    PAYMENT_IN_PROCESS,
    PAYMENT_AUTHORIZED,
    READY_FOR_FREE_PICK_UP,
    REJECTED_BY_STAFF,
    REJECTED_MINOR,
    PICKED_UP,
    FREE__PICKED_UP,
    EXPIRED,
    FREE__EXPIRED,
    PAYMENT_DELIVERED,
    EXPIRED__PAYMENT_DELIVERED,
    PAYMENT_PROCESSED,
    EXPIRED__PAYMENT_PROCESSED,
    STORE_SHARE_SENT,
    FORCED_REFUND_IN_PROGRESS,
    REFUND_IN_PROGRESS,
    FORCED_REFUNDED,
    REFUNDED,
    UNKNOWN;

    companion object {
        val knownEntries: List<OrderStatus>
            get() = entries - UNKNOWN
    }
}

private fun OrderState.toOrderStatus(): OrderStatus = when (this) {
    OrderState.CREATION_DUMMY -> OrderStatus.CREATION_DUMMY
    OrderState.NEW -> OrderStatus.NEW
    OrderState.PAYMENT_IN_PROCESS -> OrderStatus.PAYMENT_IN_PROCESS
    OrderState.PAYMENT_AUTHORIZED -> OrderStatus.PAYMENT_AUTHORIZED
    OrderState.READY_FOR_FREE_PICK_UP -> OrderStatus.READY_FOR_FREE_PICK_UP
    OrderState.REJECTED_BY_STAFF -> OrderStatus.REJECTED_BY_STAFF
    OrderState.REJECTED_MINOR -> OrderStatus.REJECTED_MINOR
    OrderState.PICKED_UP -> OrderStatus.PICKED_UP
    OrderState.FREE__PICKED_UP -> OrderStatus.FREE__PICKED_UP
    OrderState.EXPIRED -> OrderStatus.EXPIRED
    OrderState.FREE__EXPIRED -> OrderStatus.FREE__EXPIRED
    OrderState.PAYMENT_DELIVERED -> OrderStatus.PAYMENT_DELIVERED
    OrderState.EXPIRED__PAYMENT_DELIVERED -> OrderStatus.EXPIRED__PAYMENT_DELIVERED
    OrderState.PAYMENT_PROCESSED -> OrderStatus.PAYMENT_PROCESSED
    OrderState.EXPIRED__PAYMENT_PROCESSED -> OrderStatus.EXPIRED__PAYMENT_PROCESSED
    OrderState.STORE_SHARE_SENT -> OrderStatus.STORE_SHARE_SENT
    OrderState.FORCED_REFUND_IN_PROGRESS -> OrderStatus.FORCED_REFUND_IN_PROGRESS
    OrderState.REFUND_IN_PROGRESS -> OrderStatus.REFUND_IN_PROGRESS
    OrderState.FORCED_REFUNDED -> OrderStatus.FORCED_REFUNDED
    OrderState.REFUNDED -> OrderStatus.REFUNDED
    OrderState.UNKNOWN__ -> OrderStatus.UNKNOWN
}

private fun OrderStatus.toOrderState(): OrderState = when (this) {
    OrderStatus.CREATION_DUMMY -> OrderState.CREATION_DUMMY
    OrderStatus.NEW -> OrderState.NEW
    OrderStatus.PAYMENT_IN_PROCESS -> OrderState.PAYMENT_IN_PROCESS
    OrderStatus.PAYMENT_AUTHORIZED -> OrderState.PAYMENT_AUTHORIZED
    OrderStatus.READY_FOR_FREE_PICK_UP -> OrderState.READY_FOR_FREE_PICK_UP
    OrderStatus.REJECTED_BY_STAFF -> OrderState.REJECTED_BY_STAFF
    OrderStatus.REJECTED_MINOR -> OrderState.REJECTED_MINOR
    OrderStatus.PICKED_UP -> OrderState.PICKED_UP
    OrderStatus.FREE__PICKED_UP -> OrderState.FREE__PICKED_UP
    OrderStatus.EXPIRED -> OrderState.EXPIRED
    OrderStatus.FREE__EXPIRED -> OrderState.FREE__EXPIRED
    OrderStatus.PAYMENT_DELIVERED -> OrderState.PAYMENT_DELIVERED
    OrderStatus.EXPIRED__PAYMENT_DELIVERED -> OrderState.EXPIRED__PAYMENT_DELIVERED
    OrderStatus.PAYMENT_PROCESSED -> OrderState.PAYMENT_PROCESSED
    OrderStatus.EXPIRED__PAYMENT_PROCESSED -> OrderState.EXPIRED__PAYMENT_PROCESSED
    OrderStatus.STORE_SHARE_SENT -> OrderState.STORE_SHARE_SENT
    OrderStatus.FORCED_REFUND_IN_PROGRESS -> OrderState.FORCED_REFUND_IN_PROGRESS
    OrderStatus.REFUND_IN_PROGRESS -> OrderState.REFUND_IN_PROGRESS
    OrderStatus.FORCED_REFUNDED -> OrderState.FORCED_REFUNDED
    OrderStatus.REFUNDED -> OrderState.REFUNDED
    OrderStatus.UNKNOWN -> OrderState.UNKNOWN__
}

data class OrdersFilter(
    val states: List<OrderStatus>,
    val itemType: Order.Item.Type?,
    val from: LocalDate,
    val to: LocalDate,
    val query: String,
)

data class Order(
    val id: String,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val totalAmount: Double,
    val currency: String,
    val status: OrderStatus,
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
            Bundle, Discount, Loyalty
        }
    }
}

fun Apollo.ordersFlow(filter: OrdersFilter): Flow<Connection<Order>?> = client.query(
    OrdersQuery(
        filteredStates = filter.states.map { it.toOrderState() },
        after = filter.from.atStartOfDayIn(TimeZone.currentSystemDefault()),
        before = filter.to.atStartOfDayIn(TimeZone.currentSystemDefault()),
        includeBundles = Order.Item.Type.Bundle == filter.itemType,
        includeDiscounts = Order.Item.Type.Discount == filter.itemType,
        includeLoyalties = Order.Item.Type.Loyalty == filter.itemType,
        includeAllInverse = filter.itemType != null,
    )
).watch().map { response ->
    response.data?.orders?.getOrNull()?.let { data ->
        Connection(
            nodes = data.nodes.map(OrdersQuery.Node::toOrder),
            cursor = data.pageInfo.endCursor.getOrNull().takeIf { data.pageInfo.hasNextPage },
            totalCount = data.totalCount,
        )
    }
}

suspend fun Apollo.nextOrdersPage(filter: OrdersFilter, cursor: Cursor) {
    client.query(
        OrdersQuery(
            cursor = Optional.present(cursor),
            filteredStates = filter.states.map { it.toOrderState() },
            after = filter.from.atStartOfDayIn(TimeZone.currentSystemDefault()),
            before = filter.to.atStartOfDayIn(TimeZone.currentSystemDefault()),
            includeBundles = Order.Item.Type.Bundle == filter.itemType,
            includeDiscounts = Order.Item.Type.Discount == filter.itemType,
            includeLoyalties = Order.Item.Type.Loyalty == filter.itemType,
            includeAllInverse = filter.itemType != null,
        )
    ).toFlow().last()
}

private fun OrdersQuery.Node.toOrder(): Order = with(orderFragment.orderInfoFragment) {
    Order(
        id = id,
        createdAt = created,
        expiresAt = expiration.getOrNull(),
        totalAmount = totalSum,
        currency = currency.rawValue,
        status = state.toOrderStatus(),
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
                    type = Order.Item.Type.Bundle
                )
            } + discountItems.nodes.map {
                Order.Item(
                    id = it.discountSnapshot.id,
                    name = it.discountSnapshot.name,
                    price = it.productPrice,
                    type = Order.Item.Type.Discount
                )
            } + loyaltyItems.nodes.map {
                Order.Item(
                    id = it.loyaltySnapshot.id,
                    name = it.loyaltySnapshot.name,
                    price = 0.0,
                    type = Order.Item.Type.Loyalty
                )
            }
        }
    )
}
