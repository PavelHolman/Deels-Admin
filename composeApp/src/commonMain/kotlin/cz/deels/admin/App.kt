package cz.deels.admin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import arrow.core.Either
import cz.deels.graphql.type.OrderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
@Preview
fun App() {
    MaterialTheme {
        val isLoggedIn by Oidc.isLoggedInFlow.collectAsState(initial = false)

        if (!isLoggedIn) {
            LoggedOutScreen { Oidc.login() }
        } else {
            LoggedInScreen { Oidc.logout() }
        }
    }
}

@Composable
fun LoggedOutScreen(onLoginClick: suspend () -> Either<*, *>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Deels Admin",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Please log in to view and manage orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(24.dp))
                AsyncButton(onClick = onLoginClick) {
                    Text("Log in")
                }
            }
        }
    }
}

@Composable
fun LoggedInScreen(onLogoutClick: suspend () -> Either<*, *>) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncButton(
                onClick = onLogoutClick,
                content = { Text("Log out") },
                enabled = true,
                modifier = Modifier.size(width = 100.dp, height = 36.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        OrdersSection()
    }
}


@Composable
fun OrdersSection() {
    val ordersConnection by Apollo.ordersFlow().collectAsState(initial = null)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (val c = ordersConnection) {
            null -> item { CircularProgressIndicator() }
            c -> {
                val orders = c.nodes
                val cursor = c.cursor

                stickyHeader {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = 2.dp,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.LocalOffer,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Orders",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        RoundedCornerShape(12.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "${orders.size}/${c.totalCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                        )
                    }
                }

                items(items = orders, key = { order -> order.id }) { order ->
                    OrderItem(order)
                }
                if (cursor != null) {
                    item {
                        LaunchedEffect(cursor) {
                            Apollo.nextOrdersPage(cursor = cursor)
                        }
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

val OrderState.color: Color
    get() = when (this) {
        OrderState.CREATION_DUMMY -> Color.Gray
        OrderState.NEW -> Color(0xFF2196F3)
        OrderState.PAYMENT_IN_PROCESS -> Color(0xFFFFA000)
        OrderState.PAYMENT_AUTHORIZED -> Color(0xFF4CAF50)
        OrderState.READY_FOR_FREE_PICK_UP -> Color(0xFF8BC34A)
        OrderState.REJECTED_BY_STAFF -> Color(0xFFF44336)
        OrderState.REJECTED_MINOR -> Color(0xFFE53935)
        OrderState.PICKED_UP -> Color(0xFF4CAF50)
        OrderState.FREE__PICKED_UP -> Color(0xFF43A047)
        OrderState.EXPIRED -> Color(0xFFBDBDBD)
        OrderState.FREE__EXPIRED -> Color(0xFF9E9E9E)
        OrderState.PAYMENT_DELIVERED -> Color(0xFF4CAF50)
        OrderState.EXPIRED__PAYMENT_DELIVERED -> Color(0xFF757575)
        OrderState.PAYMENT_PROCESSED -> Color(0xFF4CAF50)
        OrderState.EXPIRED__PAYMENT_PROCESSED -> Color(0xFF9E9E9E)
        OrderState.STORE_SHARE_SENT -> Color(0xFF03A9F4)
        OrderState.FORCED_REFUND_IN_PROGRESS -> Color(0xFFFF9800)
        OrderState.REFUND_IN_PROGRESS -> Color(0xFFFFC107)
        OrderState.FORCED_REFUNDED -> Color(0xFFD32F2F)
        OrderState.REFUNDED -> Color(0xFFF44336)
        OrderState.UNKNOWN__ -> Color.Gray
    }

val Order.Item.Type.color: Color
    get() = when (this) {
        Order.Item.Type.BUNDLE -> Color(0xFF03A9F4)
        Order.Item.Type.DISCOUNT -> Color(0xFF4CAF50)
        Order.Item.Type.LOYALTY -> Color(0xFFFFC107)
    }

val Order.Item.Type.icon: ImageVector
    get() = when (this) {
        Order.Item.Type.BUNDLE -> Icons.Default.CardGiftcard
        Order.Item.Type.DISCOUNT -> Icons.Default.LocalOffer
        Order.Item.Type.LOYALTY -> Icons.Default.Star
    }

@Composable
fun OrderItem(order: Order) {
    val clipboardManager = LocalClipboardManager.current

    fun Instant?.fmt(): String = this?.toLocalDateTime(TimeZone.currentSystemDefault())?.format(
        LocalDateTime.Format {
            day(Padding.NONE)
            char('.')
            monthNumber(Padding.NONE)
            char('.')
            year()
            char(' ')
            hour(Padding.NONE)
            char(':')
            minute(Padding.ZERO)
        }
    ) ?: "No expiration"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .combinedClickable(
                onClick = { /* do nothing */ },
                onLongClick = {
                    clipboardManager.setText(AnnotatedString(order.id))
                }
            ),
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = Color(0xFFD1C4E9),
                                shape = RoundedCornerShape(12.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Pickup",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF512DA8),
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${order.items.size} item${if (order.items.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.weight(1f))

                val statusColor = order.status.color
                Box(
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = order.status.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${order.store.name} (${order.store.address})",
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "${order.createdAt.fmt()} → ${order.expiresAt.fmt()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Column {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = item.type.icon,
                                contentDescription = null,
                                tint = item.type.color,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.name.ifEmpty { "Unnamed" },
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Price
                        Text(
                            text = "${item.price} ${order.currency}",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${order.totalAmount} ${order.currency}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


enum class AsyncButtonState {
    Idle, Loading, Success, Failure
}

@Composable
fun AsyncButton(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    resetDelay: Duration = 2.seconds,
    onClick: suspend () -> Either<*, *>,
    loadingContent: @Composable () -> Unit = {
        CircularProgressIndicator(
            modifier = Modifier.size(size = 20.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            strokeWidth = 2.dp
        )
    },
    successContent: @Composable () -> Unit = { Text(text = "✅ Done") },
    failureContent: @Composable () -> Unit = { Text(text = "❌ Retry") },
    content: @Composable () -> Unit,
) {
    var state by remember { mutableStateOf(AsyncButtonState.Idle) }

    LaunchedEffect(state) {
        if (state == AsyncButtonState.Success || state == AsyncButtonState.Failure) {
            delay(duration = resetDelay)
            state = AsyncButtonState.Idle
        }
    }

    val backgroundColor by animateColorAsState(
        targetValue = when (state) {
            AsyncButtonState.Idle -> MaterialTheme.colorScheme.primary
            AsyncButtonState.Loading -> MaterialTheme.colorScheme.secondary
            AsyncButtonState.Success -> Color(color = 0xFF4CAF50)
            AsyncButtonState.Failure -> Color(color = 0xFFF44336)
        }, animationSpec = tween(500)
    )

    val scale by animateFloatAsState(
        targetValue = when (state) {
            AsyncButtonState.Loading -> 0.95f
            AsyncButtonState.Success -> 1.05f
            AsyncButtonState.Failure -> 1.05f
            else -> 1f
        }, animationSpec = spring(stiffness = Spring.StiffnessMedium)
    )

    Button(
        onClick = {
            coroutineScope.launch {
                state = AsyncButtonState.Loading
                state = onClick().fold(
                    ifLeft = { AsyncButtonState.Failure },
                    ifRight = { AsyncButtonState.Success })
            }
        },
        enabled = enabled && state == AsyncButtonState.Idle,
        modifier = modifier.graphicsLayer(scaleX = scale, scaleY = scale),
        colors = ButtonDefaults.buttonColors(containerColor = backgroundColor)
    ) {
        Crossfade(targetState = state, label = "AsyncButtonState") { target ->
            when (target) {
                AsyncButtonState.Idle -> content()
                AsyncButtonState.Loading -> loadingContent()
                AsyncButtonState.Success -> successContent()
                AsyncButtonState.Failure -> failureContent()
            }
        }
    }
}
