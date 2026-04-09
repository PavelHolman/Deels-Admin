package cz.deels.admin.feature.orders.domain

import kotlin.time.Instant
import kotlin.uuid.Uuid

public data class Order(
    val id: Uuid,
    val createdAt: Instant,
    val expiresAt: Instant?,
    val totalAmount: Double,
    val currency: String,
    val status: Status,
    val store: Store,
    val items: List<Item>,
) {
    public enum class Status {
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

        public companion object {
            public val knownEntries: List<Status>
                get() = entries - UNKNOWN
        }
    }

    public data class Store(
        val id: Uuid,
        val name: String,
        val address: String,
    )

    public data class Item(
        val id: Uuid,
        val name: String,
        val price: Double,
        val type: Type,
    ) {
        public enum class Type {
            Bundle, Discount, Loyalty
        }
    }
}
