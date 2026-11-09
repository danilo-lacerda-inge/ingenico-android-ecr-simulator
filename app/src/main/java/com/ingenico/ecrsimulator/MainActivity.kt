package com.ingenico.ecrsimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import com.ingenico.ecrsimulator.ui.theme.ECRSimulatorTheme
import com.ingenico.ecrsimulator.ui.theme.Palette
import com.ingenico.ecrsimulator.iap.IapConnectorClient
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import com.ingenico.ecrsimulator.shared.CartLine
import com.ingenico.ecrsimulator.shared.CartLineKey
import com.ingenico.ecrsimulator.shared.Product
import com.ingenico.ecrsimulator.shared.ProductCategory
import com.ingenico.ecrsimulator.shared.ProductSelection
import com.ingenico.ecrsimulator.shared.addToCart
import com.ingenico.ecrsimulator.shared.calculateItemTotalCents
import com.ingenico.ecrsimulator.shared.calculateOrderTotals
import com.ingenico.ecrsimulator.shared.products
import com.ingenico.ecrsimulator.shared.updateCartQuantities
import com.ingenico.ecrsimulator.shared.availableIapWorkflows
import com.ingenico.ecrsimulator.shared.IapWorkflow
import com.ingenico.ecrsimulator.iap.IapExecutor
import kotlinx.coroutines.launch

private fun ProductCategory.tint(): Color = when (this) {
    ProductCategory.HOT_DRINKS -> Color(0xFFE9C8A5)
    ProductCategory.COLD_DRINKS -> Color(0xFFC5E6EE)
    ProductCategory.SALADS -> Color(0xFFCFE3BE)
    ProductCategory.SNACKS -> Color(0xFFF1D6A8)
    ProductCategory.SANDWICHES -> Color(0xFFEBC9A2)
    ProductCategory.DESSERTS -> Color(0xFFF0C8D3)
}

private fun Product.imageRes(): Int = when (imageKey) {
    "product_americano" -> R.drawable.product_americano
    "product_chai_latte" -> R.drawable.product_chai_latte
    "product_cappuccino" -> R.drawable.product_cappuccino
    "product_mocha" -> R.drawable.product_mocha
    "product_berry_smoothie" -> R.drawable.product_berry_smoothie
    "product_cold_brew" -> R.drawable.product_cold_brew
    "product_iced_matcha" -> R.drawable.product_iced_matcha
    "product_lemonade" -> R.drawable.product_lemonade
    "product_caesar_salad" -> R.drawable.product_caesar_salad
    "product_garden_salad" -> R.drawable.product_garden_salad
    "product_greek_salad" -> R.drawable.product_greek_salad
    "product_quinoa_bowl" -> R.drawable.product_quinoa_bowl
    "product_banana_bread" -> R.drawable.product_banana_bread
    "product_granola_bar" -> R.drawable.product_granola_bar
    "product_pretzel" -> R.drawable.product_pretzel
    "product_yogurt_cup" -> R.drawable.product_yogurt_cup
    "product_caprese_panini" -> R.drawable.product_caprese_panini
    "product_chicken_club" -> R.drawable.product_chicken_club
    "product_egg_salad" -> R.drawable.product_egg_salad
    "product_turkey_avocado" -> R.drawable.product_turkey_avocado
    "product_brownie" -> R.drawable.product_brownie
    "product_carrot_cake" -> R.drawable.product_carrot_cake
    "product_cheesecake" -> R.drawable.product_cheesecake
    "product_lemon_tart" -> R.drawable.product_lemon_tart
    else -> error("Unknown product image key: $imageKey")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ECRSimulatorTheme(dynamicColor = false) {
                CashierScreen()
            }
        }
    }
}

@Composable
private fun CashierScreen() {
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    val selections = remember { mutableStateMapOf<String, ProductSelection>() }
    val cart = remember { mutableStateListOf<CartLine>() }
    var infoProduct by remember { mutableStateOf<Product?>(null) }
    var editingCustomizationProduct by remember { mutableStateOf<Product?>(null) }
    var showOrder by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var checkoutComplete by remember { mutableStateOf(false) }
    var checkoutInProgress by remember { mutableStateOf(false) }
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var checkoutMessage by remember { mutableStateOf("") }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val initialSettings = remember { IapSettingsStore.load(context) }
    var connectorUrl by remember { mutableStateOf(initialSettings.connectorUrl) }
    var configUrlDraft by remember { mutableStateOf(initialSettings.connectorUrl) }
    var selectedExecutorId by remember { mutableStateOf(initialSettings.executorId) }
    var selectedWorkflow by remember {
        mutableStateOf(
            availableIapWorkflows.firstOrNull { it.id == initialSettings.workflowId }
                ?: availableIapWorkflows.first { it.id == "asiTransactionExample" }
        )
    }
    var executors by remember { mutableStateOf(emptyList<IapExecutor>()) }
    var showConfiguration by remember { mutableStateOf(false) }
    var checkoutAmountCents by remember { mutableStateOf(0L) }
    var isRefreshingTerminals by remember { mutableStateOf(false) }
    var isPreparingCheckout by remember { mutableStateOf(false) }
    var terminalRefreshError by remember { mutableStateOf<String?>(null) }
    var isActivityResumed by remember { mutableStateOf(false) }
    val terminalRefreshMutex = remember { Mutex() }
    val editedCartQuantities = remember { mutableStateMapOf<CartLineKey, Int>() }
    val productListState = rememberLazyListState()

    DisposableEffect(context) {
        val activity = context as? ComponentActivity
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isActivityResumed = true
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> isActivityResumed = false
                else -> Unit
            }
        }
        activity?.lifecycle?.addObserver(observer)
        isActivityResumed = activity?.lifecycle?.currentState?.isAtLeast(Lifecycle.State.RESUMED) == true
        onDispose { activity?.lifecycle?.removeObserver(observer) }
    }

    suspend fun refreshTerminals(): Boolean = terminalRefreshMutex.withLock {
        isRefreshingTerminals = true
        terminalRefreshError = null
        try {
            IapConnector.client.connect(connectorUrl)
            executors = IapConnector.client.listExecutors()
            if (executors.none { it.executorId == selectedExecutorId && it.isAvailable() }) {
                selectedExecutorId = ""
            }
            val selectedSerial = executors.firstOrNull { it.executorId == selectedExecutorId }
                ?.serialNumber.orEmpty()
            IapSettingsStore.save(
                context,
                IapSettings(connectorUrl, selectedExecutorId, selectedWorkflow.id, selectedSerial)
            )
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            executors = emptyList()
            selectedExecutorId = ""
            IapSettingsStore.save(
                context,
                IapSettings(connectorUrl, "", selectedWorkflow.id)
            )
            terminalRefreshError = exception.message ?: "Could not refresh POSConnect terminals."
            false
        } finally {
            isRefreshingTerminals = false
        }
    }

    fun openCheckout(amountCents: Long) {
        if (isPreparingCheckout || checkoutInProgress) return
        checkoutAmountCents = amountCents
        checkoutError = null
        isPreparingCheckout = true
        scope.launch {
            refreshTerminals()
            isPreparingCheckout = false
            showCheckout = true
        }
    }

    LaunchedEffect(selectedCategory) {
        if (selectedCategory == null) {
            productListState.scrollToItem(0)
        }
    }

    val visibleProducts = products
        .filter { selectedCategory == null || it.category == selectedCategory }
        .sortedWith(compareBy<Product> { it.category.ordinal }.thenBy { it.name.lowercase(Locale.ROOT) })
    val selectedCount = selections.values.sumOf { it.quantity }
    val cartCount = cart.sumOf { it.quantity }
    val availableTerminals = executors.filter(IapExecutor::isAvailable)
    val itemTotalCents = calculateItemTotalCents(cart)
    val totals = calculateOrderTotals(itemTotalCents)
    val editedItemCount = cart.sumOf { editedCartQuantities[it.key] ?: it.quantity }
    val cartHasQuantityChanges = cart.any { line ->
        (editedCartQuantities[line.key] ?: line.quantity) != line.quantity
    }

    fun applyCartEdits() {
        val updatedCart = updateCartQuantities(cart, editedCartQuantities)
        cart.clear()
        cart.addAll(updatedCart)
    }

    fun startCheckout() {
        if (selectedExecutorId.isBlank()) {
            checkoutError = "Select an available terminal before starting checkout."
            return
        }
        checkoutError = null
        checkoutInProgress = true
        scope.launch {
            try {
                IapConnector.client.connect(connectorUrl)
                val amount = "${checkoutAmountCents / 100}.${(checkoutAmountCents % 100).toString().padStart(2, '0')}"
                val execution = IapConnector.client.startWorkflow(
                    executorId = selectedExecutorId,
                    workflowId = selectedWorkflow.id,
                    amount = amount
                )
                val selected = executors.firstOrNull { it.executorId == execution.executorId }
                checkoutMessage = "${selectedWorkflow.description} started on terminal ${selected?.serialNumber?.takeIf(String::isNotBlank) ?: "selected terminal"} for ${formatPrice(checkoutAmountCents)}. Check the terminal for the transaction result."
                IapSettingsStore.save(
                    context,
                    IapSettings(connectorUrl, selectedExecutorId, selectedWorkflow.id, selected?.serialNumber.orEmpty())
                )
                cart.clear()
                showCheckout = false
                checkoutComplete = true
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                checkoutError = exception.message ?: "Could not start checkout on the POSConnect terminal."
            } finally {
                checkoutInProgress = false
            }
        }
    }

    LaunchedEffect(Unit) { refreshTerminals() }

    LaunchedEffect(
        isActivityResumed,
        showCheckout,
        checkoutComplete,
        showConfiguration,
        showOrder,
        isPreparingCheckout,
        checkoutInProgress
    ) {
        if (isActivityResumed && !showCheckout && !checkoutComplete && !showConfiguration &&
            !showOrder && !isPreparingCheckout && !checkoutInProgress
        ) {
            while (true) {
                delay(5 * 60 * 1000L)
                refreshTerminals()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        color = Palette.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Coffee House", color = Palette.white, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text("Choose items for this order", color = Palette.muted, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Terminals: ${availableTerminals.size}",
                        color = Palette.cyan,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.width(6.dp))
                    Surface(
                        onClick = {
                            configUrlDraft = connectorUrl
                            showConfiguration = true
                        },
                        modifier = Modifier.size(44.dp).semantics {
                            contentDescription = "Configure POSConnect URL"
                        },
                        color = Color.Transparent,
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚙", color = Palette.cyan, fontSize = 23.sp)
                        }
                    }
                }
            }

            CategorySelector(selectedCategory = selectedCategory, onSelect = { selectedCategory = it })

            LazyRow(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                state = productListState,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(visibleProducts, key = { it.id }) { product ->
                    val selection = selections[product.id] ?: ProductSelection()
                    ProductCard(
                        product = product,
                        selection = selection,
                        onQuantityChange = { quantity ->
                            selections[product.id] = selection.copy(quantity = quantity.coerceAtLeast(0))
                        },
                        onEditCustomization = { editingCustomizationProduct = product },
                        onImageClick = { infoProduct = product }
                    )
                }
            }

            Button(
                onClick = {
                    selections.forEach { (id, selection) ->
                        if (selection.quantity > 0) {
                            val product = products.first { it.id == id }
                            val updatedCart = addToCart(
                                cart,
                                product,
                                selection.quantity,
                                selection.customization
                            )
                            cart.clear()
                            cart.addAll(updatedCart)
                            selections[id] = ProductSelection()
                        }
                    }
                },
                enabled = selectedCount > 0,
                modifier = Modifier.align(Alignment.CenterHorizontally).width(220.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.cyan,
                    contentColor = Palette.deepSurface,
                    disabledContainerColor = Palette.surfaceRaised,
                    disabledContentColor = Palette.muted
                )
            ) {
                Text("Add to cart", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

                OrderSummary(
                    itemCount = cartCount,
                    itemTotalCents = itemTotalCents,
                    onExpand = {
                        editedCartQuantities.clear()
                        cart.forEach { line -> editedCartQuantities[line.key] = line.quantity }
                        showOrder = true
                    },
                    onCheckout = {
                        openCheckout(totals.totalCents)
                    }
                )
            }
            editingCustomizationProduct?.let { product ->
                CustomizationEditor(
                    productName = product.name,
                    initialValue = selections[product.id]?.customization.orEmpty(),
                    onDismiss = { editingCustomizationProduct = null },
                    onSave = { customization ->
                        val current = selections[product.id] ?: ProductSelection()
                        selections[product.id] = current.copy(customization = customization)
                        editingCustomizationProduct = null
                    }
                )
            }
        }
    }

    if (showOrder) {
        val editedCart = updateCartQuantities(cart, editedCartQuantities)
        val editedItemTotalCents = calculateItemTotalCents(editedCart)
        val editedTotals = calculateOrderTotals(editedItemTotalCents)
        Dialog(
            onDismissRequest = { showOrder = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Palette.deepSurface,
                    contentColor = Palette.white,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Palette.cyan.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("Order summary", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "$editedItemCount ${if (editedItemCount == 1) "item" else "items"}",
                                    color = Palette.muted,
                                    fontSize = 13.sp
                                )
                            }
                            Surface(
                                onClick = { showOrder = false },
                                modifier = Modifier.size(40.dp).semantics {
                                    contentDescription = "Close order summary"
                                },
                                color = Palette.cyan,
                                shape = CircleShape
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("×", color = Palette.white, fontSize = 26.sp)
                                }
                            }
                        }

                        if (cart.isEmpty()) {
                            Box(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Your cart is empty.", color = Palette.muted)
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f).fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(cart, key = { "${it.key.productId}|${it.key.customization}" }) { line ->
                                    CartDetailRow(
                                        line = line,
                                        quantity = editedCartQuantities[line.key] ?: line.quantity,
                                        onQuantityChange = { quantity ->
                                            editedCartQuantities[line.key] = quantity.coerceAtLeast(0)
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text("TOTAL", color = Palette.softWhite, fontWeight = FontWeight.Bold)
                                    Text(
                                        "(${formatPrice(editedTotals.itemsCents)} + ${formatPrice(editedTotals.taxCents)} in taxes)",
                                        color = Palette.muted,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(formatPrice(editedTotals.totalCents), color = Palette.mint, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(
                                    onClick = {
                                        applyCartEdits()
                                        showOrder = false
                                    },
                                    enabled = cartHasQuantityChanges,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Palette.surfaceRaised,
                                        contentColor = Palette.white,
                                        disabledContainerColor = Palette.surfaceRaised.copy(alpha = 0.5f),
                                        disabledContentColor = Palette.muted
                                    )
                                ) {
                                    Text("Update items", fontWeight = FontWeight.SemiBold)
                                }
                                Button(
                                    onClick = {
                                        applyCartEdits()
                                        showOrder = false
                                        openCheckout(editedTotals.totalCents)
                                    },
                                    enabled = editedItemCount > 0 && !isPreparingCheckout,
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Palette.mint,
                                        contentColor = Palette.deepSurface,
                                        disabledContainerColor = Palette.surfaceRaised,
                                        disabledContentColor = Palette.muted
                                    )
                                ) {
                                    Text("Checkout", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    infoProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { infoProduct = null },
            containerColor = Palette.deepSurface,
            titleContentColor = Palette.white,
            textContentColor = Palette.softWhite,
            title = { Text(product.name, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(product.description, lineHeight = 21.sp)
                    Text("${product.category.label}  •  ${formatPrice(product.priceCents)} each", color = Palette.muted)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoProduct = null }) { Text("Done", color = Palette.cyan) }
            }
        )
    }

    if (showConfiguration) {
        AlertDialog(
            onDismissRequest = { showConfiguration = false },
            containerColor = Palette.deepSurface,
            titleContentColor = Palette.white,
            textContentColor = Palette.softWhite,
            title = { Text("POSConnect configuration") },
            text = {
                OutlinedTextField(
                    value = configUrlDraft,
                    onValueChange = { configUrlDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    label = { Text("WebSocket URL") },
                    placeholder = { Text("wss://api.dev.icp.ingenico.com/pos-connect") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Palette.cyan,
                        unfocusedBorderColor = Palette.surfaceRaised,
                        cursorColor = Palette.mint,
                        focusedTextColor = Palette.white,
                        unfocusedTextColor = Palette.white,
                        focusedContainerColor = Palette.deepSurface,
                        unfocusedContainerColor = Palette.deepSurface
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val newUrl = configUrlDraft.trim()
                        if (newUrl.isNotBlank()) {
                            connectorUrl = newUrl
                            selectedExecutorId = ""
                            executors = emptyList()
                            IapSettingsStore.save(
                                context,
                                IapSettings(newUrl, "", selectedWorkflow.id)
                            )
                            showConfiguration = false
                            scope.launch { refreshTerminals() }
                        }
                    },
                    enabled = configUrlDraft.isNotBlank()
                ) { Text("Save", color = Palette.mint) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { configUrlDraft = IapConnectorClient.DEFAULT_CONNECTOR_URL }) {
                        Text("Reset to default", color = Palette.cyan)
                    }
                    TextButton(onClick = { showConfiguration = false }) {
                        Text("Cancel", color = Palette.cyan)
                    }
                }
            }
        )
    }

    if (showCheckout) {
        Dialog(
            onDismissRequest = { if (!checkoutInProgress) showCheckout = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                val wideLayout = maxWidth >= 760.dp
                Surface(
                    modifier = Modifier
                        .width(if (maxWidth >= 940.dp) 900.dp else maxWidth * 0.96f)
                        .heightIn(max = maxHeight * 0.96f),
                    color = Palette.deepSurface,
                    contentColor = Palette.white,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Palette.cyan.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            "Checkout · ${formatPrice(checkoutAmountCents)}",
                            color = Palette.white,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (wideLayout) {
                            Row(
                                modifier = Modifier.fillMaxWidth().height(440.dp),
                                horizontalArrangement = Arrangement.spacedBy(24.dp)
                            ) {
                                CheckoutSelectors(
                                    modifier = Modifier.width(330.dp).fillMaxHeight(),
                                    availableTerminals = availableTerminals,
                                    selectedExecutorId = selectedExecutorId,
                                    selectedWorkflow = selectedWorkflow,
                                    isRefreshingTerminals = isRefreshingTerminals,
                                    terminalRefreshError = terminalRefreshError,
                                    checkoutError = checkoutError,
                                    onTerminalSelected = { executor ->
                                        selectedExecutorId = executor.executorId
                                        IapSettingsStore.save(
                                            context,
                                            IapSettings(
                                                connectorUrl,
                                                selectedExecutorId,
                                                selectedWorkflow.id,
                                                executor.serialNumber.orEmpty()
                                            )
                                        )
                                    },
                                    onWorkflowSelected = { workflow ->
                                        selectedWorkflow = workflow
                                        IapSettingsStore.save(
                                            context,
                                            IapSettings(
                                                connectorUrl,
                                                selectedExecutorId,
                                                workflow.id,
                                                availableTerminals.firstOrNull {
                                                    it.executorId == selectedExecutorId
                                                }?.serialNumber.orEmpty()
                                            )
                                        )
                                    }
                                )
                                WorkflowPreview(
                                    modifier = Modifier.weight(1f).fillMaxHeight(),
                                    workflow = selectedWorkflow
                                )
                            }
                        } else {
                            CheckoutSelectors(
                                modifier = Modifier.fillMaxWidth(),
                                availableTerminals = availableTerminals,
                                selectedExecutorId = selectedExecutorId,
                                selectedWorkflow = selectedWorkflow,
                                isRefreshingTerminals = isRefreshingTerminals,
                                terminalRefreshError = terminalRefreshError,
                                checkoutError = checkoutError,
                                onTerminalSelected = { executor ->
                                    selectedExecutorId = executor.executorId
                                    IapSettingsStore.save(
                                        context,
                                        IapSettings(
                                            connectorUrl,
                                            selectedExecutorId,
                                            selectedWorkflow.id,
                                            executor.serialNumber.orEmpty()
                                        )
                                    )
                                },
                                onWorkflowSelected = { workflow ->
                                    selectedWorkflow = workflow
                                    IapSettingsStore.save(
                                        context,
                                        IapSettings(
                                            connectorUrl,
                                            selectedExecutorId,
                                            workflow.id,
                                            availableTerminals.firstOrNull {
                                                it.executorId == selectedExecutorId
                                            }?.serialNumber.orEmpty()
                                        )
                                    )
                                }
                            )
                            WorkflowPreview(
                                modifier = Modifier.fillMaxWidth().height(280.dp),
                                workflow = selectedWorkflow
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showCheckout = false },
                                enabled = !checkoutInProgress
                            ) { Text("Cancel", color = Palette.cyan) }
                            TextButton(
                                onClick = { startCheckout() },
                                enabled = selectedExecutorId.isNotBlank() && !checkoutInProgress &&
                                    !isRefreshingTerminals
                            ) {
                                Text(
                                    if (checkoutInProgress) "Starting…" else "Start checkout",
                                    color = Palette.mint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (checkoutComplete) {
        AlertDialog(
            onDismissRequest = { checkoutComplete = false },
            containerColor = Palette.deepSurface,
            titleContentColor = Palette.white,
            textContentColor = Palette.softWhite,
            title = { Text("Transaction sent") },
            text = { Text(checkoutMessage) },
            confirmButton = {
                TextButton(onClick = { checkoutComplete = false }) {
                    Text("Done", color = Palette.mint)
                }
            }
        )
    }
}

private fun terminalSerialLabel(executor: IapExecutor): String =
    executor.serialNumber?.takeIf(String::isNotBlank)?.let { "$it · ${executor.status}" }
        ?: "Serial number unavailable · ${executor.status}"

@Composable
private fun CheckoutSelectors(
    modifier: Modifier,
    availableTerminals: List<IapExecutor>,
    selectedExecutorId: String,
    selectedWorkflow: IapWorkflow,
    isRefreshingTerminals: Boolean,
    terminalRefreshError: String?,
    checkoutError: String?,
    onTerminalSelected: (IapExecutor) -> Unit,
    onWorkflowSelected: (IapWorkflow) -> Unit
) {
    var executorMenuExpanded by remember { mutableStateOf(false) }
    var workflowMenuExpanded by remember { mutableStateOf(false) }

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CheckoutStepTitle(1, "Select terminal", Palette.cyan)
        Box {
            TextButton(
                onClick = { executorMenuExpanded = true },
                enabled = availableTerminals.isNotEmpty() && !isRefreshingTerminals
            ) {
                Text(
                    availableTerminals.firstOrNull { it.executorId == selectedExecutorId }
                        ?.let(::terminalSerialLabel) ?: "Select an available terminal",
                    color = Palette.cyan,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = executorMenuExpanded,
                onDismissRequest = { executorMenuExpanded = false }
            ) {
                availableTerminals.forEach { executor ->
                    DropdownMenuItem(
                        text = { Text(terminalSerialLabel(executor)) },
                        onClick = {
                            onTerminalSelected(executor)
                            executorMenuExpanded = false
                        }
                    )
                }
            }
        }
        CheckoutStepTitle(2, "Select workflow", Palette.mint)
        Box {
            TextButton(onClick = { workflowMenuExpanded = true }) {
                Text(
                    "${selectedWorkflow.description} (${selectedWorkflow.id})",
                    color = Palette.mint,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            DropdownMenu(
                expanded = workflowMenuExpanded,
                onDismissRequest = { workflowMenuExpanded = false }
            ) {
                availableIapWorkflows.forEach { workflow ->
                    DropdownMenuItem(
                        text = { Text("${workflow.description} (${workflow.id})") },
                        onClick = {
                            onWorkflowSelected(workflow)
                            workflowMenuExpanded = false
                        }
                    )
                }
            }
        }
        terminalRefreshError?.let { Text(it, color = Color(0xFFFF8A80)) }
        checkoutError?.let { Text(it, color = Color(0xFFFF8A80)) }
    }
}

@Composable
private fun CheckoutStepTitle(number: Int, title: String, accent: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(modifier = Modifier.size(28.dp), color = accent, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), color = Palette.deepSurface, fontWeight = FontWeight.Bold)
            }
        }
        Text(title, color = Palette.white, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WorkflowPreview(modifier: Modifier, workflow: IapWorkflow) {
    Surface(
        modifier = modifier,
        color = Palette.surfaceRaised,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.cyan.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Workflow image", color = Palette.muted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                workflow.description,
                color = Palette.white,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(workflow.id, color = Palette.cyan, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
    }
}

private fun IapExecutor.isAvailable(): Boolean = status.equals("AVAILABLE", ignoreCase = true)

@Composable
private fun CustomizationEditor(
    productName: String,
    initialValue: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var customization by remember(productName) { mutableStateOf(initialValue) }

    LaunchedEffect(productName) {
        withFrameNanos { }
        focusRequester.requestFocus()
        delay(100)
        keyboardController?.show()
    }

    Box(
        modifier = Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.58f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Palette.deepSurface,
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.dp, Palette.cyan.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Customize $productName",
                    color = Palette.white,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = customization,
                        onValueChange = { customization = it },
                        modifier = Modifier.weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { if (it.isFocused) keyboardController?.show() },
                        placeholder = { Text("Add a note for this item", color = Palette.muted) },
                        minLines = 2,
                        maxLines = 4,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Palette.cyan,
                            unfocusedBorderColor = Palette.surfaceRaised,
                            cursorColor = Palette.mint,
                            focusedTextColor = Palette.white,
                            unfocusedTextColor = Palette.white,
                            focusedContainerColor = Palette.deepSurface,
                            unfocusedContainerColor = Palette.deepSurface
                        )
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CustomizationActionButton(
                            symbol = "×",
                            description = "Cancel customization",
                            color = Color(0xFFC94040),
                            onClick = onDismiss
                        )
                        CustomizationActionButton(
                            symbol = "✓",
                            description = "Save customization",
                            color = Color(0xFF2F9E59),
                            onClick = { onSave(customization) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomizationActionButton(
    symbol: String,
    description: String,
    color: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.size(44.dp).semantics { contentDescription = description },
        color = color,
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun CategorySelector(
    selectedCategory: ProductCategory?,
    onSelect: (ProductCategory?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val categories = listOf(null) + ProductCategory.entries
        categories.forEach { category ->
            val selected = selectedCategory == category
            val label = category?.label ?: "All items"
            Surface(
                onClick = { onSelect(category) },
                color = if (selected) Palette.mint else Palette.surfaceRaised,
                shape = RoundedCornerShape(50),
                border = if (selected) null else BorderStroke(1.dp, Palette.cyan.copy(alpha = 0.35f))
            ) {
                Text(
                    text = label,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (selected) Palette.deepSurface else Palette.softWhite,
                    fontSize = 13.sp,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun ProductCard(
    product: Product,
    selection: ProductSelection,
    onQuantityChange: (Int) -> Unit,
    onEditCustomization: () -> Unit,
    onImageClick: () -> Unit
) {
    Card(
        modifier = Modifier.width(252.dp).fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = Palette.softWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (selection.quantity > 0) 2.dp else 1.dp,
            if (selection.quantity > 0) Palette.mint else Palette.surfaceRaised.copy(alpha = 0.35f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(12.dp))
                    .background(product.category.tint()),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(product.imageRes()),
                    contentDescription = "Show details about ${product.name}",
                    modifier = Modifier.fillMaxSize().clickable(onClick = onImageClick),
                    contentScale = ContentScale.Crop
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        color = Palette.deepSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${formatPrice(product.priceCents)} / each", color = Palette.surfaceRaised, fontSize = 13.sp)
                }
            }
            Surface(
                onClick = onEditCustomization,
                modifier = Modifier.fillMaxWidth().height(54.dp)
                    .semantics { contentDescription = "Customize ${product.name}" },
                color = Palette.softWhite,
                shape = RoundedCornerShape(9.dp),
                border = BorderStroke(1.dp, Palette.surfaceRaised.copy(alpha = 0.4f))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Text(
                        selection.customization.ifBlank { "Customization" },
                        color = if (selection.customization.isBlank()) Palette.surfaceRaised else Palette.deepSurface,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuantityButton(label = "−", enabled = selection.quantity > 0) {
                    onQuantityChange(selection.quantity - 1)
                }
                Text(
                    selection.quantity.toString(),
                    modifier = Modifier.width(48.dp),
                    color = Palette.deepSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                QuantityButton(label = "+", enabled = true) {
                    onQuantityChange(selection.quantity + 1)
                }
            }
        }
    }
}

@Composable
private fun QuantityButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(42.dp).semantics {
            contentDescription = if (label == "+") "Increase quantity" else "Decrease quantity"
        },
        color = if (enabled) Palette.surfaceRaised else Palette.surfaceRaised.copy(alpha = 0.45f),
        shape = CircleShape
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = Palette.white, fontSize = 22.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun OrderSummary(
    itemCount: Int,
    itemTotalCents: Long,
    onExpand: () -> Unit,
    onCheckout: () -> Unit
) {
    val totals = calculateOrderTotals(itemTotalCents)
    Row(
        modifier = Modifier.fillMaxWidth().background(Palette.deepSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            onClick = onExpand,
            color = Color.Transparent,
            shape = RoundedCornerShape(10.dp)
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Order summary  ·  $itemCount ${if (itemCount == 1) "item" else "items"}", color = Palette.white, fontWeight = FontWeight.SemiBold)
                    Text("Tap to view item details", color = Palette.muted, fontSize = 12.sp)
                }
                Canvas(
                    modifier = Modifier.size(24.dp).semantics {
                        contentDescription = "Open order summary"
                    }
                ) {
                    val stroke = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                    val left = 3.dp.toPx()
                    val top = 7.dp.toPx()
                    val right = 16.dp.toPx()
                    val bottom = 21.dp.toPx()
                    drawLine(Palette.cyan, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, bottom), stroke.width)
                    drawLine(Palette.cyan, androidx.compose.ui.geometry.Offset(left, bottom), androidx.compose.ui.geometry.Offset(right, bottom), stroke.width)
                    drawLine(Palette.cyan, androidx.compose.ui.geometry.Offset(right, bottom), androidx.compose.ui.geometry.Offset(right, 15.dp.toPx()), stroke.width)
                    drawLine(Palette.cyan, androidx.compose.ui.geometry.Offset(10.dp.toPx(), 14.dp.toPx()), androidx.compose.ui.geometry.Offset(21.dp.toPx(), 3.dp.toPx()), stroke.width)
                    drawLine(Palette.cyan, androidx.compose.ui.geometry.Offset(14.dp.toPx(), 3.dp.toPx()), androidx.compose.ui.geometry.Offset(21.dp.toPx(), 3.dp.toPx()), stroke.width)
                    drawLine(Palette.cyan, androidx.compose.ui.geometry.Offset(21.dp.toPx(), 3.dp.toPx()), androidx.compose.ui.geometry.Offset(21.dp.toPx(), 10.dp.toPx()), stroke.width)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("TOTAL", color = Palette.muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                    Text(
                        "(${formatPrice(totals.itemsCents)} + ${formatPrice(totals.taxCents)} in taxes)",
                        color = Palette.muted,
                        fontSize = 9.sp
                    )
                }
                Text(formatPrice(totals.totalCents), color = Palette.white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCheckout,
                enabled = itemCount > 0,
                modifier = Modifier.width(190.dp).height(58.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Palette.mint,
                    contentColor = Palette.deepSurface,
                    disabledContainerColor = Palette.surfaceRaised,
                    disabledContentColor = Palette.muted
                )
            ) {
                Text("Checkout", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartDetailRow(
    line: CartLine,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Palette.surfaceRaised, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(line.product.name, color = Palette.white, fontWeight = FontWeight.SemiBold)
            Text("${formatPrice(line.product.priceCents)} each", color = Palette.muted, fontSize = 12.sp)
            if (line.customization.isNotBlank()) {
                Text(line.customization, color = Palette.softWhite, fontSize = 12.sp)
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuantityButton(label = "−", enabled = quantity > 0) {
                onQuantityChange(quantity - 1)
            }
            Text(
                quantity.toString(),
                modifier = Modifier.width(32.dp),
                color = Palette.white,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            QuantityButton(label = "+", enabled = true) {
                onQuantityChange(quantity + 1)
            }
            Spacer(Modifier.width(4.dp))
            Text(
                formatPrice(line.product.priceCents * quantity),
                color = Palette.white,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatPrice(valueCents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(BigDecimal.valueOf(valueCents, 2))
