package com.ingenico.ecrsimulator.iap

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonParseException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.net.URI
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class IapExecutor(
    val executorId: String,
    val status: String,
    val serialNumber: String? = null,
    val name: String? = null
)

data class IapServiceExecution(
    val serviceExecutionId: String,
    val executorId: String,
    val workflowId: String
)

class IapConnectorClient {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()
    private val clientId = UUID.randomUUID().toString()
    private val pendingRequests = ConcurrentHashMap<String, CompletableDeferred<JsonObject>>()
    private val mutableEvents = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = mutableEvents.asSharedFlow()

    @Volatile private var socket: WebSocket? = null
    @Volatile private var connectedUrl: String? = null
    @Volatile private var activeExecutorId: String? = null
    @Volatile private var activeConversationId: String? = null

    val isConnected: Boolean get() = socket != null && connectedUrl != null
    val currentExecutorId: String? get() = activeExecutorId

    suspend fun connect(connectorUrl: String) {
        val normalizedUrl = normalizeConnectorUrl(connectorUrl)
        if (isConnected && connectedUrl == normalizedUrl) return
        disconnect()

        val sessionUrl = "$normalizedUrl/session"
        val opened = CompletableDeferred<Unit>()
        val request = Request.Builder()
            .url(sessionUrl)
            .header("X-Device-Id", clientId)
            .build()
        val newSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                if (socket === webSocket) opened.complete(Unit)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                val message = try {
                    JsonParser.parseString(text).takeIf { it.isJsonObject }?.asJsonObject
                } catch (_: JsonParseException) {
                    null
                }
                val requestId = message?.get("requestId")?.takeUnless { it.isJsonNull }?.asString
                if (message != null && requestId != null) {
                    pendingRequests.remove(requestId)?.complete(message)
                } else {
                    mutableEvents.tryEmit(text)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (socket === webSocket) {
                    socket = null
                    connectedUrl = null
                    activeExecutorId = null
                    activeConversationId = null
                }
                opened.completeExceptionally(t)
                failPendingRequests(t)
                mutableEvents.tryEmit("Connector WebSocket failed: ${t.message ?: "connection error"}")
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                clearConnection(webSocket)
                mutableEvents.tryEmit("Connector WebSocket closed ($code): $reason")
            }
        })
        socket = newSocket
        connectedUrl = normalizedUrl
        try {
            withTimeout(CONNECTION_TIMEOUT_MS) { opened.await() }
        } catch (exception: TimeoutCancellationException) {
            newSocket.cancel()
            clearConnection(newSocket)
            throw IllegalStateException("Timed out connecting to POSConnect at $normalizedUrl", exception)
        } catch (exception: Exception) {
            newSocket.cancel()
            clearConnection(newSocket)
            throw IllegalStateException("Could not connect to POSConnect at $normalizedUrl", exception)
        }
    }

    suspend fun listExecutors(): List<IapExecutor> {
        val response = request("GET_AVAILABLE_EXECUTORS_REQUEST")
        requireSuccess(response, "Could not retrieve available terminals")
        val executorArray = response.getAsJsonArray("executors")
            ?: return emptyList()
        return executorArray.mapNotNull { item ->
            when {
                item.isJsonPrimitive -> IapExecutor(item.asString, "UNKNOWN")
                item.isJsonObject -> item.asJsonObject.takeIf { it.has("executorId") }?.let { executor ->
                    IapExecutor(
                        executorId = executor.get("executorId").asString,
                        status = executor.stringOrNull("status") ?: "UNKNOWN",
                        serialNumber = executor.stringOrNull("serialNumber"),
                        name = executor.stringOrNull("name")
                    )
                }
                else -> null
            }
        }
    }

    suspend fun ensureConversation(executorId: String): String {
        require(executorId.isNotBlank()) { "Select a terminal before starting checkout" }
        val existingConversation = activeConversationId
        if (existingConversation != null && activeExecutorId == executorId) {
            return existingConversation
        }
        if (existingConversation != null) {
            request(
                "RELEASE_CONVERSATION_REQUEST",
                "conversationId" to existingConversation
            )
        }
        val response = request(
            "CREATE_CONVERSATION_REQUEST",
            "executorId" to executorId
        )
        requireSuccess(response, "Could not connect to terminal $executorId")
        val conversationId = response.stringOrNull("conversationId")
            ?: error("POSConnect did not return a conversation ID")
        activeExecutorId = executorId
        activeConversationId = conversationId
        return conversationId
    }

    suspend fun startWorkflow(
        executorId: String,
        workflowId: String,
        amount: String
    ): IapServiceExecution {
        require(workflowId.isNotBlank()) { "Select a workflow before checkout" }
        require(amount.isNotBlank()) { "Transaction amount cannot be empty" }
        val conversationId = ensureConversation(executorId)
        val parameters = JsonObject().apply {
            addProperty("workflowId", workflowId)
            addProperty("amount", amount)
        }
        val response = request(
            "START_SERVICE_EXECUTION_REQUEST",
            "conversationId" to conversationId,
            "serviceId" to workflowId,
            "serviceParameters" to parameters
        )
        requireSuccess(response, "Workflow could not be started")
        val executionId = response.stringOrNull("serviceExecutionId")
            ?: error("POSConnect did not return a service execution ID")
        return IapServiceExecution(executionId, executorId, workflowId)
    }

    suspend fun disconnect() {
        val oldSocket = socket
        socket = null
        connectedUrl = null
        activeExecutorId = null
        activeConversationId = null
        oldSocket?.close(1000, "ECR disconnected")
        failPendingRequests(IllegalStateException("Disconnected from POSConnect"))
    }

    private suspend fun request(
        messageType: String,
        vararg fields: Pair<String, Any>
    ): JsonObject {
        val currentSocket = socket ?: error("Connect to POSConnect first")
        val requestId = UUID.randomUUID().toString()
        val payload = JsonObject().apply {
            addProperty("messageIdType", messageType)
            addProperty("requestId", requestId)
            fields.forEach { (key, value) ->
                when (value) {
                    is String -> addProperty(key, value)
                    is JsonObject -> add(key, value)
                    else -> error("Unsupported POSConnect request field: $key")
                }
            }
        }
        val response = CompletableDeferred<JsonObject>()
        pendingRequests[requestId] = response
        if (!currentSocket.send(payload.toString())) {
            pendingRequests.remove(requestId)
            error("Could not send $messageType to POSConnect")
        }
        return try {
            withTimeout(REQUEST_TIMEOUT_MS) { response.await() }
        } catch (exception: TimeoutCancellationException) {
            error("POSConnect timed out while processing $messageType")
        } finally {
            pendingRequests.remove(requestId)
        }
    }

    private fun requireSuccess(response: JsonObject, failureMessage: String) {
        if (response.stringOrNull("status") != "success") {
            val reason = response.stringOrNull("reason") ?: failureMessage
            error("$failureMessage: $reason")
        }
    }

    private fun clearConnection(webSocket: WebSocket) {
        if (socket === webSocket) {
            socket = null
            connectedUrl = null
            activeExecutorId = null
            activeConversationId = null
            failPendingRequests(IllegalStateException("POSConnect connection closed"))
        }
    }

    private fun failPendingRequests(exception: Throwable) {
        pendingRequests.values.forEach { it.completeExceptionally(exception) }
        pendingRequests.clear()
    }

    private fun normalizeConnectorUrl(value: String): String {
        val base = value.trim().trimEnd('/')
        val uri = try {
            URI(base)
        } catch (exception: Exception) {
            throw IllegalArgumentException("Invalid POSConnect URL", exception)
        }
        require(uri.scheme == "ws" || uri.scheme == "wss") {
            "POSConnect URL must start with ws:// or wss://"
        }
        require(uri.host != null && uri.query == null && uri.fragment == null) {
            "Enter the POSConnect base URL without a query or fragment"
        }
        return base
    }

    private fun JsonObject.stringOrNull(key: String): String? =
        get(key)?.takeUnless { it.isJsonNull }?.asString

    companion object {
        const val DEFAULT_CONNECTOR_URL = "wss://api.dev.icp.ingenico.com/pos-connect"
        private const val CONNECTION_TIMEOUT_MS = 15_000L
        private const val REQUEST_TIMEOUT_MS = 30_000L
    }
}
