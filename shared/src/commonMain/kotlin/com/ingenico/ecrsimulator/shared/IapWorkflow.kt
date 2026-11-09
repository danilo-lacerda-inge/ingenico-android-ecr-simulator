package com.ingenico.ecrsimulator.shared

data class IapWorkflow(val id: String, val description: String)

val availableIapWorkflows = listOf(
    IapWorkflow("asiInputExample", "ASI input example"),
    IapWorkflow("asiMessageExample", "ASI message example"),
    IapWorkflow("asiTransactionExample", "ASI transaction example"),
    IapWorkflow("testMenuCommand", "Test the Menu command"),
    IapWorkflow("testProcess", "Test Process")
)
