package cz.deels.admin.feature.orders.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

public data class Connection<T>(
    val items: List<T>,
    val totalCount: Int,
    val fetch: suspend () -> Unit,
    val fetchNextPage: (suspend () -> Unit)?,
    // TODO: Fetchers with state
)

public interface OrdersRepo {
    public fun ordersFlow(filter: OrdersFilter): Flow<Connection<Order>?>

    public data class OrdersFilter(
        val states: List<Order.Status>,
        val itemType: Order.Item.Type?,
        val from: LocalDate,
        val to: LocalDate,
        val query: String,
    )
}
