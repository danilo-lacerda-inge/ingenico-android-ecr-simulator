package com.ingenico.ecrsimulator

import android.content.Intent
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import com.ingenico.ecrsimulator.ui.theme.ECRSimulatorTheme
import com.ingenico.ecrsimulator.ui.theme.Palette
import androidx.compose.ui.window.DialogProperties
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private enum class ProductCategory(val label: String, val tint: Color) {
    HOT_DRINKS("Hot drinks", Color(0xFFE9C8A5)),
    COLD_DRINKS("Cold drinks", Color(0xFFC5E6EE)),
    SALADS("Salads", Color(0xFFCFE3BE)),
    SNACKS("Snacks", Color(0xFFF1D6A8)),
    SANDWICHES("Sandwiches", Color(0xFFEBC9A2)),
    DESSERTS("Desserts", Color(0xFFF0C8D3))
}

private data class Product(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val price: BigDecimal,
    val description: String,
    @param:androidx.annotation.DrawableRes val imageRes: Int
)

private data class ProductSelection(val quantity: Int = 0, val customization: String = "")
private data class CartLine(val product: Product, val quantity: Int, val customization: String) {
    val subtotal: BigDecimal get() = product.price * quantity.toBigDecimal()
    val key: CartLineKey get() = CartLineKey(product.id, customization)
}
private data class CartLineKey(val productId: String, val customization: String)
private data class OrderTotals(val items: BigDecimal, val tax: BigDecimal, val total: BigDecimal)

private val products = listOf(
    Product("americano", "Americano", ProductCategory.HOT_DRINKS, BigDecimal("3.25"), "Espresso diluted with hot water for a smooth, rich cup.", R.drawable.product_americano),
    Product("chai-latte", "Chai Latte", ProductCategory.HOT_DRINKS, BigDecimal("4.50"), "Black tea blended with warming spices and steamed milk.", R.drawable.product_chai_latte),
    Product("cappuccino", "Cappuccino", ProductCategory.HOT_DRINKS, BigDecimal("4.25"), "Double espresso topped with steamed milk and airy foam.", R.drawable.product_cappuccino),
    Product("mocha", "Mocha", ProductCategory.HOT_DRINKS, BigDecimal("4.75"), "Espresso, chocolate, and steamed milk finished with foam.", R.drawable.product_mocha),
    Product("berry-smoothie", "Berry Smoothie", ProductCategory.COLD_DRINKS, BigDecimal("5.25"), "A chilled blend of strawberries, blueberries, banana, and yogurt.", R.drawable.product_berry_smoothie),
    Product("cold-brew", "Cold Brew", ProductCategory.COLD_DRINKS, BigDecimal("4.00"), "Slow-steeped coffee served over ice.", R.drawable.product_cold_brew),
    Product("iced-matcha", "Iced Matcha Latte", ProductCategory.COLD_DRINKS, BigDecimal("5.00"), "Ceremonial green tea whisked with milk and served over ice.", R.drawable.product_iced_matcha),
    Product("lemonade", "Lemonade", ProductCategory.COLD_DRINKS, BigDecimal("3.50"), "Fresh lemon juice with a touch of sweetness, served chilled.", R.drawable.product_lemonade),
    Product("caesar-salad", "Caesar Salad", ProductCategory.SALADS, BigDecimal("8.50"), "Romaine, parmesan, croutons, and classic Caesar dressing.", R.drawable.product_caesar_salad),
    Product("garden-salad", "Garden Salad", ProductCategory.SALADS, BigDecimal("7.75"), "Mixed greens with cucumber, tomato, carrot, and house dressing.", R.drawable.product_garden_salad),
    Product("greek-salad", "Greek Salad", ProductCategory.SALADS, BigDecimal("8.25"), "Cucumber, tomato, olives, red onion, and feta.", R.drawable.product_greek_salad),
    Product("quinoa-bowl", "Quinoa Bowl", ProductCategory.SALADS, BigDecimal("9.25"), "Quinoa, roasted vegetables, greens, and lemon tahini.", R.drawable.product_quinoa_bowl),
    Product("banana-bread", "Banana Bread", ProductCategory.SNACKS, BigDecimal("3.25"), "Moist banana loaf baked with ripe bananas and a hint of cinnamon.", R.drawable.product_banana_bread),
    Product("granola-bar", "Granola Bar", ProductCategory.SNACKS, BigDecimal("2.50"), "Oats, honey, and toasted seeds in a chewy snack bar.", R.drawable.product_granola_bar),
    Product("pretzel", "Soft Pretzel", ProductCategory.SNACKS, BigDecimal("3.00"), "Warm, oven-baked pretzel with a lightly salted crust.", R.drawable.product_pretzel),
    Product("yogurt-cup", "Yogurt & Fruit Cup", ProductCategory.SNACKS, BigDecimal("4.25"), "Creamy yogurt layered with seasonal fruit and crunchy granola.", R.drawable.product_yogurt_cup),
    Product("caprese-panini", "Caprese Panini", ProductCategory.SANDWICHES, BigDecimal("9.50"), "Mozzarella, tomato, and basil pressed on toasted sourdough.", R.drawable.product_caprese_panini),
    Product("chicken-club", "Chicken Club", ProductCategory.SANDWICHES, BigDecimal("10.50"), "Grilled chicken, bacon, lettuce, and tomato on multigrain bread.", R.drawable.product_chicken_club),
    Product("egg-salad", "Egg Salad Sandwich", ProductCategory.SANDWICHES, BigDecimal("8.75"), "House-made egg salad with crisp greens on soft wheat bread.", R.drawable.product_egg_salad),
    Product("turkey-avocado", "Turkey & Avocado", ProductCategory.SANDWICHES, BigDecimal("10.25"), "Roasted turkey, avocado, and greens with herb mayo.", R.drawable.product_turkey_avocado),
    Product("brownie", "Chocolate Brownie", ProductCategory.DESSERTS, BigDecimal("3.75"), "Fudgy chocolate brownie with a rich cocoa finish.", R.drawable.product_brownie),
    Product("carrot-cake", "Carrot Cake", ProductCategory.DESSERTS, BigDecimal("5.00"), "Spiced carrot cake layered with cream cheese frosting.", R.drawable.product_carrot_cake),
    Product("cheesecake", "New York Cheesecake", ProductCategory.DESSERTS, BigDecimal("5.25"), "Classic baked cheesecake on a buttery biscuit base.", R.drawable.product_cheesecake),
    Product("lemon-tart", "Lemon Tart", ProductCategory.DESSERTS, BigDecimal("4.75"), "Bright lemon curd in a crisp, buttery pastry shell.", R.drawable.product_lemon_tart)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ECRSimulatorTheme(dynamicColor = false) {
                CashierScreen(onOpenWorkflows = {
                    startActivity(Intent(this, WorkflowActivity::class.java))
                })
            }
        }
    }
}

@Composable
private fun CashierScreen(onOpenWorkflows: () -> Unit) {
    var selectedCategory by remember { mutableStateOf<ProductCategory?>(null) }
    val selections = remember { mutableStateMapOf<String, ProductSelection>() }
    val cart = remember { mutableStateListOf<CartLine>() }
    var infoProduct by remember { mutableStateOf<Product?>(null) }
    var editingCustomizationProduct by remember { mutableStateOf<Product?>(null) }
    var showOrder by remember { mutableStateOf(false) }
    var showCheckout by remember { mutableStateOf(false) }
    var checkoutComplete by remember { mutableStateOf(false) }
    var completedAmount by remember { mutableStateOf(BigDecimal.ZERO) }
    val editedCartQuantities = remember { mutableStateMapOf<CartLineKey, Int>() }
    val productListState = rememberLazyListState()

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
    val itemTotal = cart.fold(BigDecimal.ZERO) { sum, line -> sum + line.subtotal }
    val totals = calculateOrderTotals(itemTotal)
    val editedItemCount = cart.sumOf { editedCartQuantities[it.key] ?: it.quantity }
    val cartHasQuantityChanges = cart.any { line ->
        (editedCartQuantities[line.key] ?: line.quantity) != line.quantity
    }

    fun applyCartEdits() {
        val updatedCart = cart.mapNotNull { line ->
            val quantity = editedCartQuantities[line.key] ?: line.quantity
            if (quantity == 0) null else line.copy(quantity = quantity)
        }
        cart.clear()
        cart.addAll(updatedCart)
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
                TextButton(onClick = onOpenWorkflows) {
                    Text("Workflow demos", color = Palette.cyan, fontWeight = FontWeight.SemiBold)
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
                        onInfo = { infoProduct = product }
                    )
                }
            }

            Button(
                onClick = {
                    selections.forEach { (id, selection) ->
                        if (selection.quantity > 0) {
                            val product = products.first { it.id == id }
                            val existingIndex = cart.indexOfFirst {
                                it.product.id == id && it.customization == selection.customization.trim()
                            }
                            if (existingIndex >= 0) {
                                cart[existingIndex] = cart[existingIndex].copy(
                                    quantity = cart[existingIndex].quantity + selection.quantity
                                )
                            } else {
                                cart.add(CartLine(product, selection.quantity, selection.customization.trim()))
                            }
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
                    itemTotal = itemTotal,
                    onExpand = {
                        editedCartQuantities.clear()
                        cart.forEach { line -> editedCartQuantities[line.key] = line.quantity }
                        showOrder = true
                    },
                    onCheckout = {
                        editedCartQuantities.clear()
                        cart.forEach { line -> editedCartQuantities[line.key] = line.quantity }
                        showOrder = true
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
        val editedItemTotal = cart.fold(BigDecimal.ZERO) { sum, line ->
            sum + line.product.price * (editedCartQuantities[line.key] ?: line.quantity).toBigDecimal()
        }
        val editedTotals = calculateOrderTotals(editedItemTotal)
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
                                        "(${formatPrice(editedTotals.items)} + ${formatPrice(editedTotals.tax)} in taxes)",
                                        color = Palette.muted,
                                        fontSize = 12.sp
                                    )
                                }
                                Text(formatPrice(editedTotals.total), color = Palette.mint, fontSize = 21.sp, fontWeight = FontWeight.Bold)
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
                                        showCheckout = true
                                    },
                                    enabled = editedItemCount > 0,
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
                    Text("${product.category.label}  •  ${formatPrice(product.price)} each", color = Palette.muted)
                }
            },
            confirmButton = {
                TextButton(onClick = { infoProduct = null }) { Text("Done", color = Palette.cyan) }
            }
        )
    }

    if (showCheckout || checkoutComplete) {
        AlertDialog(
            onDismissRequest = {
                showCheckout = false
                checkoutComplete = false
            },
            containerColor = Palette.deepSurface,
            titleContentColor = Palette.white,
            textContentColor = Palette.softWhite,
            title = { Text(if (checkoutComplete) "Order complete" else "Confirm checkout") },
            text = {
                Text(
                    if (checkoutComplete) "Demo payment approved for ${formatPrice(completedAmount)}."
                    else "$cartCount ${if (cartCount == 1) "item" else "items"} · ${formatPrice(totals.total)}"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (checkoutComplete) {
                            checkoutComplete = false
                        } else {
                            completedAmount = totals.total
                            cart.clear()
                            showCheckout = false
                            checkoutComplete = true
                        }
                    }
                ) { Text(if (checkoutComplete) "Done" else "Pay", color = Palette.mint) }
            },
            dismissButton = if (!checkoutComplete) {
                { TextButton(onClick = { showCheckout = false }) { Text("Cancel", color = Palette.cyan) } }
            } else null
        )
    }
}

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
    onInfo: () -> Unit
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
                    .background(product.category.tint),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(product.imageRes),
                    contentDescription = "${product.name} photo",
                    modifier = Modifier.fillMaxSize(),
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
                    Text("${formatPrice(product.price)} / each", color = Palette.surfaceRaised, fontSize = 13.sp)
                }
                Surface(
                    onClick = onInfo,
                    modifier = Modifier.size(21.dp).semantics {
                        contentDescription = "Details about ${product.name}"
                    },
                    color = Palette.surfaceRaised,
                    shape = CircleShape
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("i", color = Palette.white, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
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
    itemTotal: BigDecimal,
    onExpand: () -> Unit,
    onCheckout: () -> Unit
) {
    val totals = calculateOrderTotals(itemTotal)
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
                        "(${formatPrice(totals.items)} + ${formatPrice(totals.tax)} in taxes)",
                        color = Palette.muted,
                        fontSize = 9.sp
                    )
                }
                Text(formatPrice(totals.total), color = Palette.white, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
            Text("${formatPrice(line.product.price)} each", color = Palette.muted, fontSize = 12.sp)
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
                formatPrice(line.product.price * quantity.toBigDecimal()),
                color = Palette.white,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun formatPrice(value: BigDecimal): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(value)

private fun calculateOrderTotals(itemTotal: BigDecimal): OrderTotals {
    val tax = itemTotal.multiply(BigDecimal("0.06")).setScale(2, RoundingMode.HALF_UP)
    return OrderTotals(itemTotal, tax, itemTotal + tax)
}
