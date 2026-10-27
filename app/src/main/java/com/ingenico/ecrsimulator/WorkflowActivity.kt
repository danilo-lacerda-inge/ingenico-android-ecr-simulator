package com.ingenico.ecrsimulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ingenico.ecrsimulator.ui.theme.ECRSimulatorTheme
import com.ingenico.ecrsimulator.ui.theme.Palette
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

private enum class Workflow(val title: String, val subtitle: String, val tag: String) {
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

private enum class RunState { READY, RUNNING, COMPLETE }

private data class RunEvent(val title: String, val detail: String)

class WorkflowActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ECRSimulatorTheme(dynamicColor = false) {
                EcrSimulatorScreen()
            }
        }
    }
}

@Composable
private fun EcrSimulatorScreen() {
    var selectedWorkflow by remember { mutableStateOf(Workflow.LOYALTY) }
    var amount by remember { mutableStateOf("24.50") }
    var runState by remember { mutableStateOf(RunState.READY) }
    var showDiagram by remember { mutableStateOf(false) }
    var events by remember { mutableStateOf(emptyList<RunEvent>()) }
    val scope = rememberCoroutineScope()

    val amountValue = amount.toBigDecimalOrNull()
    val canStart = amountValue != null && amountValue > java.math.BigDecimal.ZERO &&
        runState != RunState.RUNNING

    fun startDemo() {
        if (!canStart) return
        runState = RunState.RUNNING
        events = listOf(RunEvent("Workflow started", "Preparing the demo transaction"))
        scope.launch {
            val steps = when (selectedWorkflow) {
                Workflow.LOYALTY -> listOf(
                    RunEvent("Amount received", "Input: ${formatAmount(amount)}"),
                    RunEvent("Sale completed", "Transaction approved by local demo"),
                    RunEvent("Loyalty gateway", "Returning customer path selected"),
                    RunEvent("Points awarded", "25 points added to the demo account")
                )
                Workflow.SURVEY -> listOf(
                    RunEvent("Amount received", "Input: ${formatAmount(amount)}"),
                    RunEvent("Sale completed", "Transaction approved by local demo"),
                    RunEvent("Survey gateway", "Customer accepted the survey"),
                    RunEvent("Survey completed", "Customer rating: 5 / 5")
                )
            }
            steps.forEach { event ->
                delay(1100)
                events = events + event
            }
            delay(650)
            runState = RunState.COMPLETE
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        color = Palette.background
    ) {
        BoxWithConstraints {
            val wideLayout = maxWidth >= 900.dp
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = if (wideLayout) 32.dp else 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Header()
                ConnectionNotice()

                if (wideLayout) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier.weight(1.2f),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            WorkflowPicker(
                                selected = selectedWorkflow,
                                onSelect = {
                                    if (runState != RunState.RUNNING) {
                                        selectedWorkflow = it
                                        runState = RunState.READY
                                        events = emptyList()
                                    }
                                },
                                onDiagram = { showDiagram = true },
                                enabled = runState != RunState.RUNNING
                            )
                            AmountInput(
                                amount = amount,
                                onAmountChange = { amount = it },
                                enabled = runState != RunState.RUNNING
                            )
                        }
                        Column(
                            modifier = Modifier.weight(0.9f),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            RunMonitor(runState, events, amount, selectedWorkflow)
                            StartButton(canStart, runState, ::startDemo) {
                                runState = RunState.READY
                                events = emptyList()
                            }
                        }
                    }
                } else {
                    WorkflowPicker(
                        selected = selectedWorkflow,
                        onSelect = {
                            if (runState != RunState.RUNNING) {
                                selectedWorkflow = it
                                runState = RunState.READY
                                events = emptyList()
                            }
                        },
                        onDiagram = { showDiagram = true },
                        enabled = runState != RunState.RUNNING
                    )
                    AmountInput(
                        amount = amount,
                        onAmountChange = { amount = it },
                        enabled = runState != RunState.RUNNING
                    )
                    RunMonitor(runState, events, amount, selectedWorkflow)
                    StartButton(canStart, runState, ::startDemo) {
                        runState = RunState.READY
                        events = emptyList()
                    }
                }

                Text(
                    text = "DEMO KIT  •  CASHIER / INITIATOR",
                    color = Palette.muted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                )
            }
        }
    }

    if (showDiagram) {
        WorkflowDiagramDialog(workflow = selectedWorkflow, onDismiss = { showDiagram = false })
    }
}

@Composable
private fun Header() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = "Workflow demos",
                color = Palette.white,
                fontSize = 26.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Choose a simulated payment flow",
                color = Palette.muted,
                fontSize = 14.sp
            )
        }
        Surface(
            color = Palette.surfaceRaised,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, Palette.teal.copy(alpha = 0.55f))
        ) {
            Text(
                text = "CX9000",
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 9.dp),
                color = Palette.softWhite,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.7.sp
            )
        }
    }
}

@Composable
private fun ConnectionNotice() {
    Card(
        colors = CardDefaults.cardColors(containerColor = Palette.deepSurface),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, Palette.cyan.copy(alpha = 0.28f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(10.dp).background(Palette.cyan, CircleShape)
            )
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("Local demonstration mode", color = Palette.white, fontWeight = FontWeight.Medium)
                Text(
                    "PosConnect and RX executor are not connected. Runs below are simulated on this device.",
                    color = Palette.muted,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }
    }
}

@Composable
private fun WorkflowPicker(
    selected: Workflow,
    onSelect: (Workflow) -> Unit,
    onDiagram: () -> Unit,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionHeading("Choose a workflow", "Select a repeatable demo")
        Workflow.entries.forEach { workflow ->
            val isSelected = workflow == selected
            Card(
                onClick = { onSelect(workflow) },
                enabled = enabled,
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) Palette.surfaceRaised else Palette.deepSurface
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) Palette.cyan else Palette.surfaceRaised
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = workflow.title,
                            color = Palette.white,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (isSelected) {
                            Text("SELECTED", color = Palette.mint, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text(
                        workflow.subtitle,
                        color = Palette.muted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(color = Palette.deepSurface, shape = RoundedCornerShape(50)) {
                            Text(
                                workflow.tag,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                color = Palette.softWhite,
                                fontSize = 9.sp,
                                letterSpacing = 0.6.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        TextButton(onClick = onDiagram, enabled = enabled) {
                            Text("View BPMN", color = Palette.cyan, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AmountInput(amount: String, onAmountChange: (String) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeading("Transaction amount", "Enter an amount to pass to the workflow")
        OutlinedTextField(
            value = amount,
            onValueChange = { input ->
                if (input.matches(Regex("^\\d{0,6}(\\.\\d{0,2})?$"))) onAmountChange(input)
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = enabled,
            singleLine = true,
            leadingIcon = { Text("$", color = Palette.muted, fontSize = 20.sp) },
            placeholder = { Text("0.00", color = Palette.muted) },
            textStyle = MaterialTheme.typography.headlineSmall.copy(
                color = Palette.white,
                fontWeight = FontWeight.SemiBold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
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
        Text("USD  •  Demo currency", color = Palette.muted, fontSize = 11.sp)
    }
}

@Composable
private fun RunMonitor(
    state: RunState,
    events: List<RunEvent>,
    amount: String,
    workflow: Workflow
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Palette.deepSurface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Palette.surfaceRaised)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Workflow monitor", color = Palette.white, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(workflow.title, color = Palette.muted, fontSize = 12.sp)
                }
                StateBadge(state)
            }
            if (events.isEmpty()) {
                Text(
                    "Execution updates will appear here after you start the workflow.",
                    color = Palette.muted,
                    fontSize = 13.sp,
                    lineHeight = 19.sp
                )
            } else {
                events.forEachIndexed { index, event ->
                    EventRow(
                        event = event,
                        isLatest = index == events.lastIndex,
                        isComplete = state == RunState.COMPLETE
                    )
                }
            }
            if (state == RunState.COMPLETE) {
                Surface(
                    color = Palette.surfaceRaised,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text("DEMO RESULT", color = Palette.cyan, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Status", color = Palette.muted, fontSize = 13.sp)
                            Text("Approved", color = Palette.mint, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount", color = Palette.muted, fontSize = 13.sp)
                            Text(formatAmount(amount), color = Palette.white, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Result", color = Palette.muted, fontSize = 13.sp)
                            Text(
                                if (workflow == Workflow.LOYALTY) "25 points awarded" else "Rating 5 / 5",
                                color = Palette.white,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventRow(event: RunEvent, isLatest: Boolean, isComplete: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(11.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp)
                .size(9.dp)
                .background(
                    if (isLatest && !isComplete) Palette.cyan else Palette.mint,
                    CircleShape
                )
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(event.title, color = Palette.white, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(event.detail, color = Palette.muted, fontSize = 11.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun StateBadge(state: RunState) {
    val (label, color) = when (state) {
        RunState.READY -> "READY" to Palette.cyan
        RunState.RUNNING -> "RUNNING" to Palette.mint
        RunState.COMPLETE -> "COMPLETE" to Palette.mint
    }
    Surface(color = color.copy(alpha = 0.13f), shape = RoundedCornerShape(50)) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp
        )
    }
}

@Composable
private fun StartButton(
    canStart: Boolean,
    state: RunState,
    onStart: () -> Unit,
    onReset: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = onStart,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Palette.mint,
                contentColor = Palette.deepSurface,
                disabledContainerColor = Palette.surfaceRaised,
                disabledContentColor = Palette.muted
            )
        ) {
            Text(
                if (state == RunState.RUNNING) "Running workflow…" else "Run workflow",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (state == RunState.COMPLETE) {
            TextButton(onClick = onReset, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Reset demo", color = Palette.cyan)
            }
        } else if (!canStart && state != RunState.RUNNING) {
            Text(
                "Enter a valid amount greater than zero.",
                color = Palette.muted,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(title, color = Palette.white, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
        Text(subtitle, color = Palette.muted, fontSize = 12.sp)
    }
}

@Composable
private fun WorkflowDiagramDialog(workflow: Workflow, onDismiss: () -> Unit) {
    val isLoyalty = workflow == Workflow.LOYALTY
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Palette.deepSurface,
        titleContentColor = Palette.white,
        textContentColor = Palette.softWhite,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("BPMN workflow", fontWeight = FontWeight.SemiBold)
                Text(workflow.title, color = Palette.muted, fontSize = 13.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BpmnNode("Start", "START EVENT", Palette.cyan)
                FlowConnector()
                BpmnNode("Input amount", "USER TASK", Palette.surfaceRaised)
                FlowConnector()
                BpmnNode("Process sale", "SERVICE TASK", Palette.surfaceRaised)
                FlowConnector()
                GatewayDiamond()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    BranchNode(
                        label = if (isLoyalty) "Loyalty member" else "Accepts survey",
                        task = if (isLoyalty) "Award points" else "Show survey",
                        positive = true
                    )
                    BranchNode(
                        label = if (isLoyalty) "Guest sale" else "Declines",
                        task = if (isLoyalty) "Skip loyalty" else "Skip survey",
                        positive = false
                    )
                }
                FlowConnector()
                BpmnNode("Return result", "END EVENT", Palette.mint)
                Text(
                    "Exclusive gateway selects one path; the result is returned to the cashier.",
                    color = Palette.muted,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = Palette.cyan) }
        }
    )
}

@Composable
private fun BpmnNode(title: String, type: String, accent: Color) {
    Surface(
        modifier = Modifier.fillMaxWidth(0.88f),
        color = Palette.surfaceRaised,
        shape = if (type.contains("EVENT")) CircleShape else RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.7f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(type, color = accent, fontSize = 8.sp, letterSpacing = 0.8.sp, fontWeight = FontWeight.Bold)
            Text(title, color = Palette.white, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun FlowConnector() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
        Box(Modifier.width(1.dp).height(8.dp).background(Palette.muted))
        Text("↓", color = Palette.muted, fontSize = 12.sp, lineHeight = 12.sp)
    }
}

@Composable
private fun GatewayDiamond() {
    Box(
        modifier = Modifier.size(42.dp).rotate(45f)
            .background(Palette.surfaceRaised, RoundedCornerShape(5.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text("X", modifier = Modifier.rotate(-45f), color = Palette.cyan, fontWeight = FontWeight.Bold)
    }
    Text("EXCLUSIVE GATEWAY", color = Palette.cyan, fontSize = 8.sp, letterSpacing = 0.6.sp)
}

@Composable
private fun BranchNode(label: String, task: String, positive: Boolean) {
    Column(
        modifier = Modifier.width(118.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, color = Palette.muted, fontSize = 9.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        Text("↓", color = Palette.muted, fontSize = 12.sp, lineHeight = 12.sp)
        Surface(
            color = if (positive) Palette.mint.copy(alpha = 0.16f) else Palette.surfaceRaised,
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, if (positive) Palette.mint.copy(alpha = 0.55f) else Palette.surfaceRaised)
        ) {
            Text(
                task,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
                color = Palette.white,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatAmount(value: String): String {
    val amount = value.toBigDecimalOrNull() ?: return "$0.00"
    return "$" + String.format(Locale.US, "%.2f", amount)
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, widthDp = 1024)
@Composable
private fun EcrSimulatorPreview() {
    ECRSimulatorTheme(dynamicColor = false) {
        EcrSimulatorScreen()
    }
}
