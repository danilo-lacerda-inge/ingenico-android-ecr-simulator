package com.ingenico.ecrsimulator.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.ingenico.ecrsimulator.shared.CartLine
import com.ingenico.ecrsimulator.shared.CartLineKey
import com.ingenico.ecrsimulator.shared.Product
import com.ingenico.ecrsimulator.shared.ProductCategory
import com.ingenico.ecrsimulator.shared.ProductSelection
import com.ingenico.ecrsimulator.shared.availableIapWorkflows
import com.ingenico.ecrsimulator.shared.IapWorkflow
import com.ingenico.ecrsimulator.shared.addToCart
import com.ingenico.ecrsimulator.shared.calculateItemTotalCents
import com.ingenico.ecrsimulator.shared.calculateOrderTotals
import com.ingenico.ecrsimulator.shared.products
import com.ingenico.ecrsimulator.shared.updateCartQuantities
import com.ingenico.ecrsimulator.iap.IapConnectorClient
import com.ingenico.ecrsimulator.iap.IapExecutor
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.NumberFormat
import java.util.Locale
import java.util.prefs.Preferences

private val background = androidx.compose.ui.graphics.Color(0xFF09265C)
private val deepSurface = androidx.compose.ui.graphics.Color(0xFF071D49)
private val surfaceRaised = androidx.compose.ui.graphics.Color(0xFF123D78)
private val cyan = androidx.compose.ui.graphics.Color(0xFF37B9D2)
private val mint = androidx.compose.ui.graphics.Color(0xFF57C99B)
private val white = androidx.compose.ui.graphics.Color.White
private val softWhite = androidx.compose.ui.graphics.Color(0xFFEAF0F5)
private val muted = androidx.compose.ui.graphics.Color(0xFFA9BED7)

private fun terminalSerialLabel(executor: IapExecutor): String =
    executor.serialNumber?.takeIf(String::isNotBlank)?.let { "$it · ${executor.status}" }
        ?: "Serial number unavailable · ${executor.status}"

private fun IapExecutor.isAvailable(): Boolean = status.equals("AVAILABLE", ignoreCase = true)

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Coffee House — ECR Simulator",
        state = rememberWindowState(size = androidx.compose.ui.unit.DpSize(1280.dp, 820.dp))
    ) {
        MaterialTheme {
            DesktopCashier()
        }
    }
}

@Composable
private fun DesktopCashier() {
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    val selections = remember { mutableStateMapOf<String, ProductSelection>() }
    val cart = remember { mutableStateListOf<CartLine>() }
    val editedQuantities = remember { mutableStateMapOf<CartLineKey, Int>() }
    var infoProduct by remember { mutableStateOf<Product?>(null) }
    var showOrder by remember { mutableStateOf(false) }
    var showConfiguration by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var checkoutComplete by remember { mutableStateOf(false) }
    var checkoutAmountCents by remember { mutableStateOf(0L) }
    var checkoutMessage by remember { mutableStateOf("") }
    var checkoutError by remember { mutableStateOf<String?>(null) }
    var checkoutInProgress by remember { mutableStateOf(false) }
    val preferences = remember { Preferences.userRoot().node("com/ingenico/ecrsimulator") }
    var connectorUrl by remember {
        mutableStateOf(preferences.get("posconnect_url", IapConnectorClient.DEFAULT_CONNECTOR_URL))
    }
    var configUrlDraft by remember { mutableStateOf(connectorUrl) }
    var executors by remember { mutableStateOf(emptyList<IapExecutor>()) }
    var selectedExecutorId by remember { mutableStateOf("") }
    var selectedWorkflow by remember {
        mutableStateOf(availableIapWorkflows.first { it.id == "asiTransactionExample" })
    }
    var isRefreshingTerminals by remember { mutableStateOf(false) }
    var isPreparingCheckout by remember { mutableStateOf(false) }
    var terminalRefreshError by remember { mutableStateOf<String?>(null) }
    val connectorClient = remember { IapConnectorClient() }
    val scope = rememberCoroutineScope()
    val terminalRefreshMutex = remember { Mutex() }

    val visibleProducts = products
        .filter { selectedCategory == null || it.category == selectedCategory }
        .sortedWith(compareBy<Product> { it.category.ordinal }.thenBy { it.name.lowercase(Locale.ROOT) })
    val selectedCount = selections.values.sumOf { it.quantity }
    val cartCount = cart.sumOf { it.quantity }
    val availableTerminals = executors.filter(IapExecutor::isAvailable)
    val itemTotalCents = calculateItemTotalCents(cart)
    val totals = calculateOrderTotals(itemTotalCents)
    val editedCart = updateCartQuantities(cart, editedQuantities)
    val editedItemCount = editedCart.sumOf { it.quantity }
    val editedTotals = calculateOrderTotals(calculateItemTotalCents(editedCart))
    val hasCartEdits = cart.any { (editedQuantities[it.key] ?: it.quantity) != it.quantity }

    fun applyEdits() {
        cart.replaceWith(editedCart)
    }

    suspend fun refreshTerminals(): Boolean = terminalRefreshMutex.withLock {
        isRefreshingTerminals = true
        terminalRefreshError = null
        try {
            connectorClient.connect(connectorUrl)
            executors = connectorClient.listExecutors()
            if (executors.none { it.executorId == selectedExecutorId && it.isAvailable() }) {
                selectedExecutorId = ""
            }
            true
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            executors = emptyList()
            selectedExecutorId = ""
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

    fun startCheckout() {
        if (selectedExecutorId.isBlank()) {
            checkoutError = "Select an available terminal before starting checkout."
            return
        }

        checkoutError = null
        checkoutInProgress = true
        scope.launch {
            try {
                connectorClient.connect(connectorUrl)
                val amount = "${checkoutAmountCents / 100}.${(checkoutAmountCents % 100).toString().padStart(2, '0')}"
                val execution = connectorClient.startWorkflow(
                    executorId = selectedExecutorId,
                    workflowId = selectedWorkflow.id,
                    amount = amount
                )
                val selected = executors.firstOrNull { it.executorId == execution.executorId }
                checkoutMessage = "${selectedWorkflow.description} started on terminal ${selected?.serialNumber?.takeIf(String::isNotBlank) ?: "selected terminal"} for ${formatPrice(checkoutAmountCents)}. Check the terminal for the transaction result."
                checkoutError = null
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
        showOrder,
        showConfiguration,
        showCheckout,
        checkoutComplete,
        infoProduct,
        isPreparingCheckout,
        checkoutInProgress
    ) {
        if (!showOrder && !showConfiguration && !showCheckout && !checkoutComplete &&
            infoProduct == null && !isPreparingCheckout && !checkoutInProgress
        ) {
            while (true) {
                delay(5 * 60 * 1000L)
                refreshTerminals()
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = background
    ) {
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
                    Text("Coffee House", color = white, fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
                    Text("Choose items for this order", color = muted, fontSize = 13.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Terminals: ${availableTerminals.size}", color = cyan, fontWeight = FontWeight.SemiBold)
                    TextButton(onClick = {
                        configUrlDraft = connectorUrl
                        showConfiguration = true
                    }) {
                        Text("⚙", color = cyan, fontSize = 21.sp)
                    }
                }
            }

            CategorySelector(selectedCategory) { selectedCategory = it }

            LazyRow(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(visibleProducts, key = Product::id) { product ->
                    val selection = selections[product.id] ?: ProductSelection()
                    ProductCard(
                        product = product,
                        selection = selection,
                        onImageClick = { infoProduct = product },
                        onQuantityChange = { quantity ->
                            selections[product.id] = selection.copy(quantity = quantity.coerceAtLeast(0))
                        },
                        onCustomizationChange = { customization ->
                            selections[product.id] = selection.copy(customization = customization)
                        }
                    )
                }
            }

            Button(
                onClick = {
                    selections.toMap().forEach { (id, selection) ->
                        if (selection.quantity > 0) {
                            val product = products.first { it.id == id }
                            cart.replaceWith(
                                addToCart(cart, product, selection.quantity, selection.customization)
                            )
                            selections[id] = ProductSelection()
                        }
                    }
                },
                enabled = selectedCount > 0,
                modifier = Modifier.align(Alignment.CenterHorizontally).width(220.dp).height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cyan,
                    contentColor = deepSurface,
                    disabledContainerColor = surfaceRaised,
                    disabledContentColor = muted
                )
            ) {
                Text("Add to cart", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }

            OrderSummary(
                itemCount = cartCount,
                totals = totals,
                onExpand = {
                    editedQuantities.clear()
                    cart.forEach { editedQuantities[it.key] = it.quantity }
                    showOrder = true
                },
                onCheckout = {
                    openCheckout(totals.totalCents)
                }
            )
        }
    }

    if (showOrder) {
        Dialog(
            onDismissRequest = { showOrder = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                color = deepSurface,
                contentColor = white,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, cyan.copy(alpha = 0.35f))
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
                                color = muted,
                                fontSize = 13.sp
                            )
                        }
                        TextButton(onClick = { showOrder = false }) {
                            Text("Close", color = cyan)
                        }
                    }

                    if (cart.isEmpty()) {
                        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text("Your cart is empty.", color = muted)
                        }
                    } else {
                        LazyRow(
                            modifier = Modifier.weight(1f).fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(cart, key = { "${it.key.productId}|${it.key.customization}" }) { line ->
                                CartDetailCard(
                                    line = line,
                                    quantity = editedQuantities[line.key] ?: line.quantity,
                                    onQuantityChange = { quantity ->
                                        editedQuantities[line.key] = quantity.coerceAtLeast(0)
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
                            Text(
                                "TOTAL  (${formatPrice(editedTotals.itemsCents)} + ${formatPrice(editedTotals.taxCents)} in taxes)",
                                color = muted,
                                fontSize = 12.sp
                            )
                            Text(
                                formatPrice(editedTotals.totalCents),
                                color = mint,
                                fontSize = 21.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = {
                                    applyEdits()
                                    showOrder = false
                                },
                                enabled = hasCartEdits,
                                colors = ButtonDefaults.buttonColors(containerColor = surfaceRaised)
                            ) {
                                Text("Update items")
                            }
                            Button(
                                onClick = {
                                    applyEdits()
                                    showOrder = false
                                    openCheckout(editedTotals.totalCents)
                                },
                                enabled = editedItemCount > 0 && !checkoutInProgress && !isPreparingCheckout,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = mint,
                                    contentColor = deepSurface
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

    if (checkoutComplete) {
        AlertDialog(
            onDismissRequest = { checkoutComplete = false },
            containerColor = deepSurface,
            titleContentColor = white,
            textContentColor = softWhite,
            title = { Text("Transaction sent") },
            text = { Text(checkoutMessage) },
            confirmButton = {
                TextButton(onClick = { checkoutComplete = false }) {
                    Text("Done", color = mint)
                }
            }
        )
    }

    infoProduct?.let { product ->
        AlertDialog(
            onDismissRequest = { infoProduct = null },
            containerColor = deepSurface,
            titleContentColor = white,
            textContentColor = softWhite,
            title = { Text(product.name, fontWeight = FontWeight.SemiBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(product.description)
                    Text("${product.category.label}  •  ${formatPrice(product.priceCents)} each", color = muted)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoProduct = null }) { Text("Done", color = cyan) }
            }
        )
    }

    if (showConfiguration) {
        AlertDialog(
            onDismissRequest = { showConfiguration = false },
            containerColor = deepSurface,
            titleContentColor = white,
            textContentColor = softWhite,
            title = { Text("POSConnect configuration") },
            text = {
                OutlinedTextField(
                        value = configUrlDraft,
                        onValueChange = { configUrlDraft = it },
                        label = { Text("WebSocket URL") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = cyan,
                            unfocusedBorderColor = surfaceRaised,
                            focusedTextColor = white,
                            unfocusedTextColor = white
                        )
                )
            },
            confirmButton = {
                TextButton(
                        onClick = {
                            val newUrl = configUrlDraft.trim()
                            if (newUrl.isNotBlank()) {
                                connectorUrl = newUrl
                                preferences.put("posconnect_url", newUrl)
                                executors = emptyList()
                                selectedExecutorId = ""
                                showConfiguration = false
                                scope.launch { refreshTerminals() }
                            }
                        },
                        enabled = configUrlDraft.isNotBlank()
                ) { Text("Save", color = mint) }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = { configUrlDraft = IapConnectorClient.DEFAULT_CONNECTOR_URL }) {
                        Text("Reset to default", color = cyan)
                    }
                    TextButton(onClick = { showConfiguration = false }) {
                        Text("Cancel", color = cyan)
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
                    color = deepSurface,
                    contentColor = white,
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, cyan.copy(alpha = 0.35f))
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        Text(
                            "Checkout · ${formatPrice(checkoutAmountCents)}",
                            color = white,
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
                                        preferences.put("selected_executor_id", selectedExecutorId)
                                        preferences.put("selected_terminal_serial", executor.serialNumber.orEmpty())
                                    },
                                    onWorkflowSelected = { workflow ->
                                        selectedWorkflow = workflow
                                        preferences.put("selected_workflow_id", workflow.id)
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
                                    preferences.put("selected_executor_id", selectedExecutorId)
                                    preferences.put("selected_terminal_serial", executor.serialNumber.orEmpty())
                                },
                                onWorkflowSelected = { workflow ->
                                    selectedWorkflow = workflow
                                    preferences.put("selected_workflow_id", workflow.id)
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
                            ) { Text("Cancel", color = cyan) }
                            TextButton(
                                onClick = { startCheckout() },
                                enabled = selectedExecutorId.isNotBlank() && !checkoutInProgress &&
                                    !isRefreshingTerminals
                            ) {
                                Text(
                                    if (checkoutInProgress) "Starting…" else "Start checkout",
                                    color = mint
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

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
        CheckoutStepTitle(1, "Select terminal", cyan)
        Box {
            TextButton(
                onClick = { executorMenuExpanded = true },
                enabled = availableTerminals.isNotEmpty() && !isRefreshingTerminals
            ) {
                Text(
                    availableTerminals.firstOrNull { it.executorId == selectedExecutorId }
                        ?.let(::terminalSerialLabel) ?: "Select an available terminal",
                    color = cyan,
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
        CheckoutStepTitle(2, "Select workflow", mint)
        Box {
            TextButton(onClick = { workflowMenuExpanded = true }) {
                Text(
                    "${selectedWorkflow.description} (${selectedWorkflow.id})",
                    color = mint,
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
        terminalRefreshError?.let {
            Text(it, color = androidx.compose.ui.graphics.Color(0xFFFF8A80))
        }
        checkoutError?.let {
            Text(it, color = androidx.compose.ui.graphics.Color(0xFFFF8A80))
        }
    }
}

@Composable
private fun CheckoutStepTitle(number: Int, title: String, accent: androidx.compose.ui.graphics.Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(modifier = Modifier.size(28.dp), color = accent, shape = CircleShape) {
            Box(contentAlignment = Alignment.Center) {
                Text(number.toString(), color = deepSurface, fontWeight = FontWeight.Bold)
            }
        }
        Text(title, color = white, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun WorkflowPreview(modifier: Modifier, workflow: IapWorkflow) {
    Surface(
        modifier = modifier,
        color = surfaceRaised,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, cyan.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Workflow image", color = muted, fontSize = 14.sp)
            Spacer(Modifier.height(20.dp))
            Text(
                workflow.description,
                color = white,
                fontSize = 30.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(workflow.id, color = cyan, fontSize = 14.sp, textAlign = TextAlign.Center)
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
        (listOf(null) + ProductCategory.entries).forEach { category ->
            val selected = selectedCategory == category
            Surface(
                onClick = { onSelect(category) },
                color = if (selected) mint else surfaceRaised,
                shape = RoundedCornerShape(50),
                border = if (selected) null else BorderStroke(1.dp, cyan.copy(alpha = 0.35f))
            ) {
                Text(
                    text = category?.label ?: "All items",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    color = if (selected) deepSurface else softWhite,
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
    onImageClick: () -> Unit,
    onQuantityChange: (Int) -> Unit,
    onCustomizationChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.width(252.dp).fillMaxHeight(),
        colors = CardDefaults.cardColors(containerColor = softWhite),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (selection.quantity > 0) 2.dp else 1.dp,
            if (selection.quantity > 0) mint else surfaceRaised.copy(alpha = 0.35f)
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
                    painter = painterResource(product.imageKey.replace("product_", "product_") + ".png"),
                    contentDescription = "Show details about ${product.name}",
                    modifier = Modifier.fillMaxSize().clickable(onClick = onImageClick),
                    contentScale = ContentScale.Crop
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        product.name,
                        color = deepSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text("${formatPrice(product.priceCents)} / each", color = surfaceRaised, fontSize = 13.sp)
                }
            }
            OutlinedTextField(
                value = selection.customization,
                onValueChange = onCustomizationChange,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                singleLine = true,
                placeholder = { Text("Customization", fontSize = 12.sp) },
                textStyle = MaterialTheme.typography.bodySmall,
                shape = RoundedCornerShape(9.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = surfaceRaised,
                    unfocusedBorderColor = surfaceRaised.copy(alpha = 0.4f),
                    cursorColor = surfaceRaised,
                    focusedTextColor = deepSurface,
                    unfocusedTextColor = deepSurface
                )
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                QuantityButton("−", enabled = selection.quantity > 0) {
                    onQuantityChange(selection.quantity - 1)
                }
                Text(
                    selection.quantity.toString(),
                    modifier = Modifier.width(48.dp),
                    color = deepSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                QuantityButton("+", enabled = true) {
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
        modifier = Modifier.size(42.dp),
        color = if (enabled) surfaceRaised else surfaceRaised.copy(alpha = 0.45f),
        shape = CircleShape
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                label,
                modifier = Modifier.align(Alignment.Center).padding(bottom = 4.dp),
                color = white,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun OrderSummary(
    itemCount: Int,
    totals: com.ingenico.ecrsimulator.shared.OrderTotals,
    onExpand: () -> Unit,
    onCheckout: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(deepSurface, RoundedCornerShape(16.dp))
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Surface(
            onClick = onExpand,
            color = androidx.compose.ui.graphics.Color.Transparent,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    "Order summary  ·  $itemCount ${if (itemCount == 1) "item" else "items"}",
                    color = white,
                    fontWeight = FontWeight.SemiBold
                )
                Text("Tap to view item details", color = muted, fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "(${formatPrice(totals.itemsCents)} + ${formatPrice(totals.taxCents)} in taxes)",
                    color = muted,
                    fontSize = 10.sp
                )
                Text(formatPrice(totals.totalCents), color = white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = onCheckout,
                enabled = itemCount > 0,
                modifier = Modifier.width(190.dp).height(58.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = mint,
                    contentColor = deepSurface,
                    disabledContainerColor = surfaceRaised,
                    disabledContentColor = muted
                )
            ) {
                Text("Checkout", fontSize = 17.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CartDetailCard(
    line: CartLine,
    quantity: Int,
    onQuantityChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.width(380.dp).background(surfaceRaised, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(line.product.name, color = white, fontWeight = FontWeight.SemiBold)
            Text("${formatPrice(line.product.priceCents)} each", color = muted, fontSize = 12.sp)
            if (line.customization.isNotBlank()) {
                Text(line.customization, color = softWhite, fontSize = 12.sp)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            QuantityButton("−", enabled = quantity > 0) { onQuantityChange(quantity - 1) }
            Text(quantity.toString(), color = white, fontWeight = FontWeight.Bold)
            QuantityButton("+", enabled = true) { onQuantityChange(quantity + 1) }
            Spacer(Modifier.width(4.dp))
            Text(formatPrice(line.product.priceCents * quantity), color = white, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun ProductCategory.tint(): androidx.compose.ui.graphics.Color = when (this) {
    ProductCategory.HOT_DRINKS -> androidx.compose.ui.graphics.Color(0xFFE9C8A5)
    ProductCategory.COLD_DRINKS -> androidx.compose.ui.graphics.Color(0xFFC5E6EE)
    ProductCategory.SALADS -> androidx.compose.ui.graphics.Color(0xFFCFE3BE)
    ProductCategory.SNACKS -> androidx.compose.ui.graphics.Color(0xFFF1D6A8)
    ProductCategory.SANDWICHES -> androidx.compose.ui.graphics.Color(0xFFEBC9A2)
    ProductCategory.DESSERTS -> androidx.compose.ui.graphics.Color(0xFFF0C8D3)
}

private fun formatPrice(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(java.math.BigDecimal.valueOf(cents, 2))

private fun MutableList<CartLine>.replaceWith(lines: List<CartLine>) {
    clear()
    addAll(lines)
}
