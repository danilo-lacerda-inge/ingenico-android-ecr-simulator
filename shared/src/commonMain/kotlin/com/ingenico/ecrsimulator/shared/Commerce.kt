package com.ingenico.ecrsimulator.shared

enum class ProductCategory(val label: String) {
    HOT_DRINKS("Hot drinks"),
    COLD_DRINKS("Cold drinks"),
    SALADS("Salads"),
    SNACKS("Snacks"),
    SANDWICHES("Sandwiches"),
    DESSERTS("Desserts")
}

data class Product(
    val id: String,
    val name: String,
    val category: ProductCategory,
    val priceCents: Long,
    val description: String,
    val imageKey: String
)

data class ProductSelection(val quantity: Int = 0, val customization: String = "")

data class CartLine(
    val product: Product,
    val quantity: Int,
    val customization: String
) {
    val subtotalCents: Long get() = product.priceCents * quantity
    val key: CartLineKey get() = CartLineKey(product.id, customization)
}

data class CartLineKey(val productId: String, val customization: String)

data class OrderTotals(val itemsCents: Long, val taxCents: Long, val totalCents: Long)

val products = listOf(
    Product("americano", "Americano", ProductCategory.HOT_DRINKS, 325, "Espresso diluted with hot water for a smooth, rich cup.", "product_americano"),
    Product("chai-latte", "Chai Latte", ProductCategory.HOT_DRINKS, 450, "Black tea blended with warming spices and steamed milk.", "product_chai_latte"),
    Product("cappuccino", "Cappuccino", ProductCategory.HOT_DRINKS, 425, "Double espresso topped with steamed milk and airy foam.", "product_cappuccino"),
    Product("mocha", "Mocha", ProductCategory.HOT_DRINKS, 475, "Espresso, chocolate, and steamed milk finished with foam.", "product_mocha"),
    Product("berry-smoothie", "Berry Smoothie", ProductCategory.COLD_DRINKS, 525, "A chilled blend of strawberries, blueberries, banana, and yogurt.", "product_berry_smoothie"),
    Product("cold-brew", "Cold Brew", ProductCategory.COLD_DRINKS, 400, "Slow-steeped coffee served over ice.", "product_cold_brew"),
    Product("iced-matcha", "Iced Matcha Latte", ProductCategory.COLD_DRINKS, 500, "Ceremonial green tea whisked with milk and served over ice.", "product_iced_matcha"),
    Product("lemonade", "Lemonade", ProductCategory.COLD_DRINKS, 350, "Fresh lemon juice with a touch of sweetness, served chilled.", "product_lemonade"),
    Product("caesar-salad", "Caesar Salad", ProductCategory.SALADS, 850, "Romaine, parmesan, croutons, and classic Caesar dressing.", "product_caesar_salad"),
    Product("garden-salad", "Garden Salad", ProductCategory.SALADS, 775, "Mixed greens with cucumber, tomato, carrot, and house dressing.", "product_garden_salad"),
    Product("greek-salad", "Greek Salad", ProductCategory.SALADS, 825, "Cucumber, tomato, olives, red onion, and feta.", "product_greek_salad"),
    Product("quinoa-bowl", "Quinoa Bowl", ProductCategory.SALADS, 925, "Quinoa, roasted vegetables, greens, and lemon tahini.", "product_quinoa_bowl"),
    Product("banana-bread", "Banana Bread", ProductCategory.SNACKS, 325, "Moist banana loaf baked with ripe bananas and a hint of cinnamon.", "product_banana_bread"),
    Product("granola-bar", "Granola Bar", ProductCategory.SNACKS, 250, "Oats, honey, and toasted seeds in a chewy snack bar.", "product_granola_bar"),
    Product("pretzel", "Soft Pretzel", ProductCategory.SNACKS, 300, "Warm, oven-baked pretzel with a lightly salted crust.", "product_pretzel"),
    Product("yogurt-cup", "Yogurt & Fruit Cup", ProductCategory.SNACKS, 425, "Creamy yogurt layered with seasonal fruit and crunchy granola.", "product_yogurt_cup"),
    Product("caprese-panini", "Caprese Panini", ProductCategory.SANDWICHES, 950, "Mozzarella, tomato, and basil pressed on toasted sourdough.", "product_caprese_panini"),
    Product("chicken-club", "Chicken Club", ProductCategory.SANDWICHES, 1050, "Grilled chicken, bacon, lettuce, and tomato on multigrain bread.", "product_chicken_club"),
    Product("egg-salad", "Egg Salad Sandwich", ProductCategory.SANDWICHES, 875, "House-made egg salad with crisp greens on soft wheat bread.", "product_egg_salad"),
    Product("turkey-avocado", "Turkey & Avocado", ProductCategory.SANDWICHES, 1025, "Roasted turkey, avocado, and greens with herb mayo.", "product_turkey_avocado"),
    Product("brownie", "Chocolate Brownie", ProductCategory.DESSERTS, 375, "Fudgy chocolate brownie with a rich cocoa finish.", "product_brownie"),
    Product("carrot-cake", "Carrot Cake", ProductCategory.DESSERTS, 500, "Spiced carrot cake layered with cream cheese frosting.", "product_carrot_cake"),
    Product("cheesecake", "New York Cheesecake", ProductCategory.DESSERTS, 525, "Classic baked cheesecake on a buttery biscuit base.", "product_cheesecake"),
    Product("lemon-tart", "Lemon Tart", ProductCategory.DESSERTS, 475, "Bright lemon curd in a crisp, buttery pastry shell.", "product_lemon_tart")
)

fun addToCart(
    cart: List<CartLine>,
    product: Product,
    quantity: Int,
    customization: String
): List<CartLine> {
    if (quantity <= 0) return cart
    val normalizedCustomization = customization.trim()
    val existingIndex = cart.indexOfFirst {
        it.product.id == product.id && it.customization == normalizedCustomization
    }
    return if (existingIndex < 0) {
        cart + CartLine(product, quantity, normalizedCustomization)
    } else {
        cart.toMutableList().also { lines ->
            val existing = lines[existingIndex]
            lines[existingIndex] = existing.copy(quantity = existing.quantity + quantity)
        }
    }
}

fun updateCartQuantities(
    cart: List<CartLine>,
    editedQuantities: Map<CartLineKey, Int>
): List<CartLine> = cart.mapNotNull { line ->
    val quantity = (editedQuantities[line.key] ?: line.quantity).coerceAtLeast(0)
    if (quantity == 0) null else line.copy(quantity = quantity)
}

fun calculateItemTotalCents(cart: List<CartLine>): Long =
    cart.sumOf { it.subtotalCents }

fun calculateOrderTotals(itemTotalCents: Long): OrderTotals {
    require(itemTotalCents >= 0) { "Item total cannot be negative" }
    val taxCents = (itemTotalCents * 6 + 50) / 100
    return OrderTotals(itemTotalCents, taxCents, itemTotalCents + taxCents)
}

enum class Workflow(val title: String, val subtitle: String, val tag: String) {
    LOYALTY(
        title = "Sale With Loyalty",
        subtitle = "Take payment, then award points to a returning customer.",
        tag = "LOYALTY"
    ),
    SURVEY(
        title = "Sale With Survey",
        subtitle = "Complete a sale and route the customer to a quick survey.",
        tag = "CUSTOMER EXPERIENCE"
    )
}

data class WorkflowEvent(val title: String, val detail: String)

fun parseAmountToCents(value: String): Long? {
    if (value.isEmpty() || value.any { it !in '0'..'9' && it != '.' }) return null
    val decimalIndex = value.indexOf('.')
    if (decimalIndex != value.lastIndexOf('.')) return null

    val wholePart = if (decimalIndex < 0) value else value.substring(0, decimalIndex)
    val fractionPart = if (decimalIndex < 0) "" else value.substring(decimalIndex + 1)
    if (wholePart.isEmpty() && fractionPart.isEmpty() || fractionPart.length > 2) return null

    val wholeCents = (wholePart.ifEmpty { "0" }.toLongOrNull() ?: return null)
    if (wholeCents > (Long.MAX_VALUE - 99) / 100) return null
    val fractionalCents = fractionPart.padEnd(2, '0').toLongOrNull() ?: 0L
    return wholeCents * 100 + fractionalCents
}

fun isValidTransactionAmount(value: String): Boolean =
    parseAmountToCents(value)?.let { it > 0 } == true

fun workflowSteps(workflow: Workflow): List<WorkflowEvent> = when (workflow) {
    Workflow.LOYALTY -> listOf(
        WorkflowEvent("Sale completed", "Transaction approved by local demo"),
        WorkflowEvent("Loyalty gateway", "Returning customer path selected"),
        WorkflowEvent("Points awarded", "25 points added to the demo account")
    )
    Workflow.SURVEY -> listOf(
        WorkflowEvent("Sale completed", "Transaction approved by local demo"),
        WorkflowEvent("Survey gateway", "Customer accepted the survey"),
        WorkflowEvent("Survey completed", "Customer rating: 5 / 5")
    )
}
