package cz.deels.admin.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import arrow.core.Either
import cz.deels.graphql.type.OrderState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.format
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.char
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
@Preview
fun App() {
    MaterialTheme {
        val isLoggedIn by Oidc.isLoggedInFlow.collectAsState(initial = false)

        if (!isLoggedIn) {
            LoggedOutScreen(onLoginClick = Oidc::login)
        } else {
            LoggedInScreen(onLogoutClick = Oidc::logout)
        }
    }
}

@Composable
fun LoggedOutScreen(onLoginClick: suspend () -> Either<*, *>) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color = MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape = RoundedCornerShape(size = 12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.padding(all = 16.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(all = 24.dp)
            ) {
                Text(
                    text = "Deels Admin",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(height = 16.dp))
                Text(
                    text = "Please log in to view and manage orders",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(height = 24.dp))
                AsyncButton(onClick = onLoginClick) {
                    Text("Log in")
                }
            }
        }
    }
}

@Composable
fun LoggedInScreen(onLogoutClick: suspend () -> Either<*, *>) {
    val focusManager = LocalFocusManager.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                while (true) {
                    awaitPointerEventScope {
                        val event = awaitPointerEvent()
                        if (event.changes.any { it.changedToUp() }) {
                            focusManager.clearFocus(force = true)
                        }
                    }
                }
            }
            .background(color = MaterialTheme.colorScheme.primaryContainer)
            .padding(top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding())
            .padding(horizontal = 12.dp)
    ) {
        OrdersSection()

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(all = 16.dp)
        ) {
            AsyncButton(onClick = onLogoutClick, enabled = true) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = null
                    )
                    Spacer(Modifier.width(width = 4.dp))
                    Text(text = "Log out", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
fun OrdersSection() {
    var filter by remember {
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val to = today.plus(value = 1, unit = DateTimeUnit.DAY)
        val from = to.minus(value = 1, unit = DateTimeUnit.MONTH)

        mutableStateOf(
            OrdersFilter(
                states = OrderState.knownEntries,
                from = from,
                to = to,
                itemType = null,
                query = "",
            )
        )
    }
    val ordersConnection by Apollo.ordersFlow(filter = filter).collectAsState(initial = null)

    val lazyListState = rememberLazyListState()
    val filterVisible by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 && lazyListState.firstVisibleItemScrollOffset < 10
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = filterVisible,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut(),
        ) {
            OrdersFilterBar(
                filter = filter,
                onFilterChange = { filter = it }
            )
        }

        LazyColumn(
            state = lazyListState,
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
                                        modifier = Modifier.size(size = 20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(width = 8.dp))
                                    Text(
                                        text = "Orders",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .background(
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                            shape = RoundedCornerShape(12.dp)
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
                                    .background(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                    )
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
        // Palette 1: dummy
        OrderState.CREATION_DUMMY -> Color(0xFF9C92A3)            // muted purple-grey, hue ~280

        // Palette 2: active / in-progress / pickups → greener/bluer
        OrderState.NEW -> Color(0xFF1976D2)                       // deep blue, hue ~210
        OrderState.PAYMENT_IN_PROCESS -> Color(0xFF2E7D32)        // green, hue ~120
        OrderState.PAYMENT_AUTHORIZED -> Color(0xFF388E3C)        // green, hue ~120
        OrderState.READY_FOR_FREE_PICK_UP -> Color(0xFF66BB6A)    // light green, hue ~130
        OrderState.FREE__PICKED_UP -> Color(0xFF81C784)           // lighter green, hue ~130
        OrderState.PICKED_UP -> Color(0xFF4CAF50)                 // green, hue ~120
        OrderState.PAYMENT_PROCESSED -> Color(0xFF43A047)         // green, hue ~120
        OrderState.PAYMENT_DELIVERED -> Color(0xFF64B5F6)         // soft blue, hue ~200
        OrderState.STORE_SHARE_SENT -> Color(0xFF0288D1)          // cyan-blue, hue ~195

        // Palette 3: rejected
        OrderState.REJECTED_BY_STAFF -> Color(0xFFF44336)         // red, hue ~0
        OrderState.REJECTED_MINOR -> Color(0xFFD32F2F)            // darker red, hue ~0

        // Palette 4: expired / historical
        OrderState.EXPIRED -> Color(0xFF8F7FA5)                   // darker purple-grey, hue ~260
        OrderState.FREE__EXPIRED -> Color(0xFF8F7FA5)             // darker purple-grey, hue ~260
        OrderState.EXPIRED__PAYMENT_DELIVERED -> Color(0xFF82769D) // even darker purple-grey, hue ~265
        OrderState.EXPIRED__PAYMENT_PROCESSED -> Color(0xFF716B94) // muted purple-grey, hue ~270

        // Palette 5: refunds → more red
        OrderState.FORCED_REFUND_IN_PROGRESS -> Color(0xFFD32F2F) // dark red, hue ~0
        OrderState.REFUND_IN_PROGRESS -> Color(0xFFE53935)        // bright red, hue ~0
        OrderState.FORCED_REFUNDED -> Color(0xFFC62828)           // deeper red, hue ~0
        OrderState.REFUNDED -> Color(0xFFB71C1C)                  // darkest red, hue ~0

        // Palette 6: unknown / neutral
        OrderState.UNKNOWN__ -> Color.Gray                         // neutral grey, no hue
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

fun Instant.formatted(): String =
    toLocalDateTime(TimeZone.currentSystemDefault()).format(
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
    )

fun LocalDate.formatted(): String = format(
    LocalDate.Format {
        day(Padding.NONE)
        char('.')
        monthNumber(Padding.NONE)
        char('.')
        year()
    }
)

@Composable
fun OrderItem(order: Order) {
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var showCopyPopup by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .combinedClickable(
                    onClick = { /* do nothing */ },
                    onLongClick = { showCopyPopup = true }
                ),
            shape = RoundedCornerShape(size = 8.dp),
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
                                    color = Color(color = 0xFFD1C4E9),
                                    shape = RoundedCornerShape(12.dp),
                                )
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Pickup",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(color = 0xFF512DA8),
                            )
                        }

                        Spacer(modifier = Modifier.width(width = 8.dp))

                        Text(
                            text = "${order.items.size} item${if (order.items.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.weight(weight = 1f))

                    val statusColor = order.status.color
                    Box(
                        modifier = Modifier
                            .background(
                                color = statusColor.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(4.dp)
                            )
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

                Spacer(modifier = Modifier.height(height = 4.dp))

                Text(
                    text = "${order.store.name} (${order.store.address})",
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(height = 4.dp))

                Text(
                    text = "${order.createdAt.formatted()} → ${order.expiresAt?.formatted() ?: "No expiration"}",
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
                                modifier = Modifier.weight(weight = 1f)
                            ) {
                                Icon(
                                    imageVector = item.type.icon,
                                    contentDescription = null,
                                    tint = item.type.color,
                                    modifier = Modifier.size(size = 16.dp)
                                )
                                Spacer(modifier = Modifier.width(width = 4.dp))
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
                        shape = RoundedCornerShape(size = 8.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                        modifier = Modifier.padding(all = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(all = 16.dp)
                                .clickable(enabled = false) {}, // prevent dismissal when clicking inside
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Copy Order ID?",
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(height = 4.dp))
                            Text(
                                text = order.id,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(height = 12.dp))
                            Button(onClick = {
                                clipboardManager.setText(AnnotatedString(text = order.id))
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                showCopyPopup = false
                            }) {
                                Text(text = "Copy")
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        SearchBar(
            query = filter.query,
            onQueryChange = { onFilterChange(filter.copy(query = it)) }
        )

        Spacer(Modifier.height(height = 8.dp))

        StatesFilter(
            selectedStates = filter.states,
            onChange = { onFilterChange(filter.copy(states = it)) }
        )

        Spacer(Modifier.height(height = 8.dp))

        ItemTypeFilter(
            selectedType = filter.itemType,
            onChange = { onFilterChange(filter.copy(itemType = it)) }
        )

        Spacer(Modifier.height(height = 8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(space = 12.dp)) {
            DatePickerField(label = "From", selectedDate = filter.from) {
                onFilterChange(filter.copy(from = it))
            }
            DatePickerField(label = "To", selectedDate = filter.to) {
                onFilterChange(filter.copy(to = it))
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(size = 24.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.width(width = 8.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier
                    .weight(weight = 1f)
                    .padding(vertical = 4.dp),
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = "Search by store, customer, or campaign",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    innerTextField()
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = { focusManager.clearFocus() }
                )
            )

            if (query.isNotEmpty()) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear",
                    modifier = Modifier
                        .clickable {
                            onQueryChange("")
                            focusManager.clearFocus()
                        }
                        .padding(4.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatesFilter(
    selectedStates: List<OrderState>,
    onChange: (List<OrderState>) -> Unit
) {
    data class HCL(val h: Float, val c: Float, val l: Float)

    fun Color.toHCL(): HCL {
        val r = red
        val g = green
        val b = blue

        fun pivot(c: Float) = if (c > 0.04045f) ((c + 0.055f) / 1.055f).pow(2.4f) else c / 12.92f
        val R = pivot(r)
        val G = pivot(g)
        val B = pivot(b)

        val X = R * 0.4124f + G * 0.3576f + B * 0.1805f
        val Y = R * 0.2126f + G * 0.7152f + B * 0.0722f
        val Z = R * 0.0193f + G * 0.1192f + B * 0.9505f

        fun f(t: Float) = if (t > 0.008856f) t.pow(1f / 3f) else (7.787f * t) + (16f / 116f)
        val fx = f(X / 0.95047f)
        val fy = f(Y / 1.00000f)
        val fz = f(Z / 1.08883f)

        val L = 116f * fy - 16f
        val a = 500f * (fx - fy)
        val bLab = 200f * (fy - fz)

        val C = sqrt(a * a + bLab * bLab)
        var H = atan2(bLab, a) * (180f / PI.toFloat())
        if (H < 0) H += 360f

        return HCL(H, C, L)
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        modifier = Modifier
            .horizontalScroll(state = rememberScrollState())
            .fillMaxWidth()
    ) {
        OrderState.knownEntries.sortedWith(
            /* This is for sure one of ways to sort order states o.o */
            compareByDescending { it.color.toHCL().h }
        ).forEach { state ->
            FilterChip(
                text = state.name,
                color = state.color,
                selected = state in selectedStates,
                onClick = {
                    val newSelection = if (state in selectedStates) {
                        selectedStates - state
                    } else {
                        selectedStates + state
                    }
                    onChange(newSelection)
                }
            )
        }
    }
}

@Composable
fun FilterChip(
    text: String,
    color: Color = MaterialTheme.colorScheme.primary,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(
        modifier = Modifier
            .clickable(enabled = enabled) { onClick() }
            .background(
                color = if (selected) color.copy(alpha = 0.2f) else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ItemTypeFilter(
    selectedType: Order.Item.Type?,
    onChange: (Order.Item.Type?) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Order.Item.Type.entries.forEach { type ->
            FilterChipWithIcon(
                text = type.name,
                icon = type.icon,
                color = type.color,
                selected = type == selectedType
            ) {
                onChange(type.takeIf { it != selectedType })
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
                shape = RoundedCornerShape(size = 16.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) color else Color.Gray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(width = 4.dp))
        Text(
            text = text,
            color = if (selected) color else MaterialTheme.colorScheme.onSurface,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    OutlinedButton(
        onClick = { showPicker = true },
        shape = RoundedCornerShape(size = 12.dp),
        modifier = Modifier.height(height = 48.dp)
    ) {
        Icon(
            imageVector = Icons.Default.LocalOffer,
            contentDescription = null
        )
        Spacer(Modifier.width(width = 8.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall)
            Text(
                text = selectedDate.formatted(),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    if (showPicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDayIn(TimeZone.UTC)
                .toEpochMilliseconds()
        )

        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val timestamp = Instant.fromEpochMilliseconds(epochMilliseconds = millis)
                        onDateSelected(timestamp.toLocalDateTime(TimeZone.currentSystemDefault()).date)
                    }
                    showPicker = false
                    focusManager.clearFocus()
                }) {
                    Text(text = "OK")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showPicker = false
                }) {
                    Text(text = "Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
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
