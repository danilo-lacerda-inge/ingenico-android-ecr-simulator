package com.ingenico.ecrsimulator.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CommerceTest {
    @Test
    fun addingSameCustomizedProductMergesQuantities() {
        val product = products.first()
        val cart = addToCart(emptyList(), product, 2, "  oat milk ")
        val updated = addToCart(cart, product, 3, "oat milk")

        assertEquals(1, updated.size)
        assertEquals(5, updated.single().quantity)
        assertEquals("oat milk", updated.single().customization)
    }

    @Test
    fun editedCartQuantitiesUpdateAndRemoveLines() {
        val first = CartLine(products[0], 2, "")
        val second = CartLine(products[1], 1, "")

        val updated = updateCartQuantities(
            listOf(first, second),
            mapOf(first.key to 4, second.key to 0)
        )

        assertEquals(listOf(first.copy(quantity = 4)), updated)
    }

    @Test
    fun orderTaxRoundsHalfUpToCents() {
        assertEquals(OrderTotals(100, 6, 106), calculateOrderTotals(100))
        assertEquals(OrderTotals(250, 15, 265), calculateOrderTotals(250))
        assertEquals(OrderTotals(8, 0, 8), calculateOrderTotals(8))
        assertEquals(OrderTotals(25, 2, 27), calculateOrderTotals(25))
    }

    @Test
    fun parsesAndValidatesTransactionAmounts() {
        assertEquals(2450L, parseAmountToCents("24.50"))
        assertEquals(25L, parseAmountToCents(".25"))
        assertEquals(700L, parseAmountToCents("7."))
        assertNull(parseAmountToCents("1.234"))
        assertNull(parseAmountToCents("abc"))
        assertFalse(isValidTransactionAmount("0.00"))
        assertTrue(isValidTransactionAmount("0.01"))
    }

    @Test
    fun workflowStepsMatchSelectedWorkflow() {
        assertEquals("Points awarded", workflowSteps(Workflow.LOYALTY).last().title)
        assertEquals("Survey completed", workflowSteps(Workflow.SURVEY).last().title)
    }

    @Test
    fun iapWorkflowIdsMatchTheConnectorDemo() {
        assertEquals(
            listOf(
                "asiInputExample",
                "asiMessageExample",
                "asiTransactionExample",
                "testMenuCommand",
                "testProcess"
            ),
            availableIapWorkflows.map { it.id }
        )
    }
}
