package cz.deels.admin

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
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
import kotlinx.datetime.toInstant
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
    var filter by remember {
        mutableStateOf(
            OrdersFilter(
                states = OrderState.knownEntries,
                from = LocalDateTime(2000, 1, 1, 0, 0).toInstant(TimeZone.UTC),
                to = LocalDateTime(3000, 1, 1, 0, 0).toInstant(TimeZone.UTC),
                itemTypes = Order.Item.Type.entries,
                query = "",
            )
        )
    }
    val ordersConnection by Apollo.ordersFlow(filter = filter).collectAsState(initial = null)

    Column(modifier = Modifier.fillMaxSize()) {
        OrdersFilterBar(
            filter = filter,
            onFilterChange = { filter = it },
        )

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
                                Apollo.nextOrdersPage(filter = filter, cursor = cursor)
                            }
                            CircularProgressIndicator()
                        }
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
        Order.Item.Type.Bundle -> Color(0xFF03A9F4)
        Order.Item.Type.Discount -> Color(0xFF4CAF50)
        Order.Item.Type.Loyalty -> Color(0xFFFFC107)
    }

val Order.Item.Type.icon: ImageVector
    get() = when (this) {
        Order.Item.Type.Bundle -> Icons.Default.CardGiftcard
        Order.Item.Type.Discount -> Icons.Default.LocalOffer
        Order.Item.Type.Loyalty -> Icons.Default.Star
    }

@Composable
fun OrderItem(order: Order) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showCopyPopup by remember { mutableStateOf(false) }

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

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = { /* do nothing */ },
                    onLongClick = { showCopyPopup = true }
                ),
            shape = RoundedCornerShape(8.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header
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

        if (showCopyPopup) {
            Popup(
                alignment = Alignment.Center,
                onDismissRequest = { showCopyPopup = false }
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Transparent)
                        .clickable { showCopyPopup = false }
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        elevation = CardDefaults.cardElevation(8.dp),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .clickable(enabled = false) {}, // prevent dismissal when clicking inside
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Copy Order ID?",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = order.id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(onClick = {
                                clipboardManager.setText(AnnotatedString(order.id))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showCopyPopup = false
                            }) {
                                Text("Copy")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersFilterBar(
    filter: OrdersFilter,
    onFilterChange: (OrdersFilter) -> Unit,
) {
    var expandedStates by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {

        // --- Query ---
        OutlinedTextField(
            value = filter.query,
            onValueChange = { onFilterChange(filter.copy(query = it)) },
            label = { Text("Search (store, address, customer)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        StatesFilter(
            selectedStates = filter.states,
            onChange = { onFilterChange(filter.copy(states = it)) }
        )

        Spacer(Modifier.height(8.dp))

        ItemTypeFilter(
            selectedTypes = filter.itemTypes,
            onChange = { onFilterChange(filter.copy(itemTypes = it)) }
        )

        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("From:", fontWeight = FontWeight.Bold)
            DatePickerButton(filter.from) { onFilterChange(filter.copy(from = it)) }

            Text("To:", fontWeight = FontWeight.Bold)
            DatePickerButton(filter.to) { onFilterChange(filter.copy(to = it)) }
        }
    }
}

@Composable
fun StatesFilter(
    selectedStates: List<OrderState>,
    onChange: (List<OrderState>) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val remainingStates = OrderState.knownEntries - selectedStates

    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.horizontalScroll(rememberScrollState())
    ) {
        // Selected states as chips
        selectedStates.forEach { state ->
            FilterChip(text = state.name, color = state.color, selected = true) {
                onChange(selectedStates - state)
            }
        }

        // Add State button
        Box(
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text("+", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        // Dropdown menu with remaining states
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            remainingStates.forEach { state ->
                DropdownMenuItem(
                    text = { Text(state.name) },
                    onClick = {
                        onChange(selectedStates + state)
                    }
                )
            }
        }
    }
}

@Composable
fun ItemTypeFilter(
    selectedTypes: List<Order.Item.Type>,
    onChange: (List<Order.Item.Type>) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Order.Item.Type.entries.forEach { type ->
            FilterChipWithIcon(
                text = type.name,
                icon = type.icon,
                color = type.color,
                selected = type in selectedTypes
            ) {
                val newTypes = if (type in selectedTypes) selectedTypes - type
                else selectedTypes + type
                onChange(newTypes)
            }
        }
    }
}

@Composable
fun FilterChipWithIcon(
    text: String,
    icon: ImageVector,
    color: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color = if (selected) color.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) color else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            color = if (selected) color else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FilterChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .background(
                color = if (selected) color.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (selected) color else MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}


@Composable
fun DatePickerButton(date: Instant, onDateSelected: (Instant) -> Unit) {
    // Implement your favorite date picker here.
    Button(onClick = { /* show date picker, call onDateSelected */ }) {
        Text(
            date.toLocalDateTime(TimeZone.currentSystemDefault()).format(
                LocalDateTime.Format {
                    day(Padding.NONE); char('.'); monthNumber(Padding.NONE); char(
                    '.'
                ); year()
                }
            ))
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
