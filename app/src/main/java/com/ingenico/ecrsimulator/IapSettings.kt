package com.ingenico.ecrsimulator

import android.content.Context
import com.ingenico.ecrsimulator.iap.IapConnectorClient

data class IapSettings(
    val connectorUrl: String,
    val executorId: String,
    val workflowId: String,
    val terminalSerialNumber: String = ""
)

object IapSettingsStore {
    private const val PREFERENCES = "iap_connector"
    private const val CONNECTOR_URL = "connector_url"
    private const val EXECUTOR_ID = "executor_id"
    private const val WORKFLOW_ID = "workflow_id"
    private const val TERMINAL_SERIAL_NUMBER = "terminal_serial_number"

    fun load(context: Context): IapSettings {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        return IapSettings(
            connectorUrl = preferences.getString(
                CONNECTOR_URL,
                IapConnectorClient.DEFAULT_CONNECTOR_URL
            ) ?: IapConnectorClient.DEFAULT_CONNECTOR_URL,
            executorId = preferences.getString(EXECUTOR_ID, "").orEmpty(),
            workflowId = preferences.getString(WORKFLOW_ID, "").orEmpty(),
            terminalSerialNumber = preferences.getString(TERMINAL_SERIAL_NUMBER, "").orEmpty()
        )
    }

    fun save(context: Context, settings: IapSettings) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(CONNECTOR_URL, settings.connectorUrl)
            .putString(EXECUTOR_ID, settings.executorId)
            .putString(WORKFLOW_ID, settings.workflowId)
            .putString(TERMINAL_SERIAL_NUMBER, settings.terminalSerialNumber)
            .apply()
    }
}

object IapConnector {
    val client = IapConnectorClient()
}
