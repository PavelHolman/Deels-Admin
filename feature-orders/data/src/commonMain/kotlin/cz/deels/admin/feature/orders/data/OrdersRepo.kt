package cz.deels.admin.feature.orders.data

import com.apollographql.apollo.api.ApolloResponse
import com.apollographql.apollo.api.Optional
import com.apollographql.apollo.api.getOrNull
import com.apollographql.cache.normalized.watch
import cz.deels.admin.core.graphql.schema.Apollo
import cz.deels.admin.core.graphql.schema.Cursor
import cz.deels.admin.core.graphql.type.OrderState
import cz.deels.admin.feature.orders.data.graphql.OrdersQuery
import cz.deels.admin.feature.orders.domain.Connection
import cz.deels.admin.feature.orders.domain.Order
import cz.deels.admin.feature.orders.domain.OrdersRepo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

// TODO: Replace with DI annotations
public fun injectOrdersRepo(apollo: Apollo): OrdersRepo = OrdersRepo(apollo)

internal class OrdersRepo(
    private val apollo: Apollo
) : OrdersRepo {
    override fun ordersFlow(filter: OrdersRepo.OrdersFilter): Flow<Connection<Order>?> {
        val fetcher = Fetcher(filter)
        return fetcher.watch().map { response ->
            response.data?.orders?.getOrNull()?.run {
                Connection(
                    items = nodes.map { it.toOrder() },
                    totalCount = totalCount,
                    fetch = fetcher.fetch(),
                    fetchNextPage = fetcher.fetch(pageInfo),
                )
            }
        }
    }

    private inner class Fetcher(
        private val filter: OrdersRepo.OrdersFilter,
    ) {
        private fun query(cursor: Cursor? = null) = apollo.client.query(
            OrdersQuery(
                cursor = Optional.presentIfNotNull(cursor),
                filteredStates = filter.states.map { it.toOrderState() },
                after = filter.from.atStartOfDayIn(TimeZone.currentSystemDefault()),
                before = filter.to.atStartOfDayIn(TimeZone.currentSystemDefault()),
                includeBundles = Order.Item.Type.Bundle == filter.itemType,
                includeDiscounts = Order.Item.Type.Discount == filter.itemType,
                includeLoyalties = Order.Item.Type.Loyalty == filter.itemType,
                includeAllInverse = filter.itemType != null,
            )
        )

        private typealias Callback = suspend () -> Unit

        fun watch(): Flow<ApolloResponse<OrdersQuery.Data>> = query().watch()
        fun fetch(): Callback = { query().toFlow().last() }
        fun fetch(pageInfo: OrdersQuery.PageInfo): (Callback)? = if (pageInfo.hasNextPage) suspend {
            query(pageInfo.endCursor.getOrNull()).toFlow().last()
        } else null
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


    private fun OrderState.toOrderStatus(): Order.Status = when (this) {
        OrderState.CREATION_DUMMY -> Order.Status.CREATION_DUMMY
        OrderState.NEW -> Order.Status.NEW
        OrderState.PAYMENT_IN_PROCESS -> Order.Status.PAYMENT_IN_PROCESS
        OrderState.PAYMENT_AUTHORIZED -> Order.Status.PAYMENT_AUTHORIZED
        OrderState.READY_FOR_FREE_PICK_UP -> Order.Status.READY_FOR_FREE_PICK_UP
        OrderState.REJECTED_BY_STAFF -> Order.Status.REJECTED_BY_STAFF
        OrderState.REJECTED_MINOR -> Order.Status.REJECTED_MINOR
        OrderState.PICKED_UP -> Order.Status.PICKED_UP
        OrderState.FREE__PICKED_UP -> Order.Status.FREE__PICKED_UP
        OrderState.EXPIRED -> Order.Status.EXPIRED
        OrderState.FREE__EXPIRED -> Order.Status.FREE__EXPIRED
        OrderState.PAYMENT_DELIVERED -> Order.Status.PAYMENT_DELIVERED
        OrderState.EXPIRED__PAYMENT_DELIVERED -> Order.Status.EXPIRED__PAYMENT_DELIVERED
        OrderState.PAYMENT_PROCESSED -> Order.Status.PAYMENT_PROCESSED
        OrderState.EXPIRED__PAYMENT_PROCESSED -> Order.Status.EXPIRED__PAYMENT_PROCESSED
        OrderState.STORE_SHARE_SENT -> Order.Status.STORE_SHARE_SENT
        OrderState.FORCED_REFUND_IN_PROGRESS -> Order.Status.FORCED_REFUND_IN_PROGRESS
        OrderState.REFUND_IN_PROGRESS -> Order.Status.REFUND_IN_PROGRESS
        OrderState.FORCED_REFUNDED -> Order.Status.FORCED_REFUNDED
        OrderState.REFUNDED -> Order.Status.REFUNDED
        OrderState.UNKNOWN__ -> Order.Status.UNKNOWN
    }

    private fun Order.Status.toOrderState(): OrderState = when (this) {
        Order.Status.CREATION_DUMMY -> OrderState.CREATION_DUMMY
        Order.Status.NEW -> OrderState.NEW
        Order.Status.PAYMENT_IN_PROCESS -> OrderState.PAYMENT_IN_PROCESS
        Order.Status.PAYMENT_AUTHORIZED -> OrderState.PAYMENT_AUTHORIZED
        Order.Status.READY_FOR_FREE_PICK_UP -> OrderState.READY_FOR_FREE_PICK_UP
        Order.Status.REJECTED_BY_STAFF -> OrderState.REJECTED_BY_STAFF
        Order.Status.REJECTED_MINOR -> OrderState.REJECTED_MINOR
        Order.Status.PICKED_UP -> OrderState.PICKED_UP
        Order.Status.FREE__PICKED_UP -> OrderState.FREE__PICKED_UP
        Order.Status.EXPIRED -> OrderState.EXPIRED
        Order.Status.FREE__EXPIRED -> OrderState.FREE__EXPIRED
        Order.Status.PAYMENT_DELIVERED -> OrderState.PAYMENT_DELIVERED
        Order.Status.EXPIRED__PAYMENT_DELIVERED -> OrderState.EXPIRED__PAYMENT_DELIVERED
        Order.Status.PAYMENT_PROCESSED -> OrderState.PAYMENT_PROCESSED
        Order.Status.EXPIRED__PAYMENT_PROCESSED -> OrderState.EXPIRED__PAYMENT_PROCESSED
        Order.Status.STORE_SHARE_SENT -> OrderState.STORE_SHARE_SENT
        Order.Status.FORCED_REFUND_IN_PROGRESS -> OrderState.FORCED_REFUND_IN_PROGRESS
        Order.Status.REFUND_IN_PROGRESS -> OrderState.REFUND_IN_PROGRESS
        Order.Status.FORCED_REFUNDED -> OrderState.FORCED_REFUNDED
        Order.Status.REFUNDED -> OrderState.REFUNDED
        Order.Status.UNKNOWN -> OrderState.UNKNOWN__
    }
}
