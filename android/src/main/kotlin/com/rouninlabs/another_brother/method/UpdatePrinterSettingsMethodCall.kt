package com.rouninlabs.another_brother.method

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.brother.ptouch.sdk.Printer
import com.brother.ptouch.sdk.PrinterInfo
import com.brother.ptouch.sdk.PrinterStatus
import com.rouninlabs.another_brother.BrotherManager
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import kotlinx.coroutines.*

/**
 * Command for updating the printer settings a Brother printer.
 * This support both one-time as well as the standard openCommunication/print/closeCommunication
 * approach.
 */
class UpdatePrinterSettingsMethodCall(val flutterAssets: FlutterPlugin.FlutterAssets, val context: Context, val call: MethodCall, val result: MethodChannel.Result) {
    companion object {
        const val METHOD_NAME = "updatePrinterSettings"
    }

    fun execute() {

        GlobalScope.launch(Dispatchers.IO) {

            val dartPrintInfo: HashMap<String, Any> = call.argument<HashMap<String, Any>>("printInfo")!!
            val printerId: String = call.argument<String>("printerId")!!
            val dartSettings:Map<Map<String, Any>, String> = call.argument("settings")!!

            // Decoded Printer Info
            val printInfo = printerInfofromMap(context = context, flutterAssets = flutterAssets, map = dartPrintInfo)

            // A print request is considered one-time if there was no printer tracked with this ID.
            // this will open a new connection and close it when done.
            // If it is not one-time it means someone must have already opened a connection using
            // the startCommunication() API. When endCommunication() is called that printer will be removed.
            // Create Printer
            val trackedPrinter = BrotherManager.getPrinter(printerId = printerId)
            val isOneTime:Boolean = trackedPrinter == null;
            val printer = trackedPrinter?: Printer()

            // Prepare local connection.
            val error = setupConnectionManagers(context = context, printer = printer, printInfo = printInfo)
            if (error != PrinterInfo.ErrorCode.ERROR_NONE) {
                // There was an error notify
                withContext(Dispatchers.Main) {
                    // Set result Printer status.
                    result.success(PrinterStatus().apply {
                        errorCode = error
                    }.toMap())
                }
                return@launch
            }

            // Set Printer Info
            printer.printerInfo = printInfo

            // Start communication with defensive error handling for Bluetooth/WiFi connection issues
            if (isOneTime) {
                try {
                    val started: Boolean = printer.startCommunication()
                    if (!started) {
                        Log.w("UpdatePrinterSettings", "Failed to start communication with printer")
                        withContext(Dispatchers.Main) {
                            result.success(PrinterStatus().apply {
                                errorCode = PrinterInfo.ErrorCode.ERROR_COMMUNICATION_ERROR
                            }.toMap())
                        }
                        return@launch
                    }
                } catch (e: NullPointerException) {
                    Log.e("UpdatePrinterSettings", "NPE in startCommunication - Bluetooth/WiFi connection failed", e)
                    withContext(Dispatchers.Main) {
                        result.success(PrinterStatus().apply {
                            errorCode = PrinterInfo.ErrorCode.ERROR_COMMUNICATION_ERROR
                        }.toMap())
                    }
                    return@launch
                } catch (e: Exception) {
                    Log.e("UpdatePrinterSettings", "Error in startCommunication", e)
                    withContext(Dispatchers.Main) {
                        result.success(PrinterStatus().apply {
                            errorCode = PrinterInfo.ErrorCode.ERROR_BROTHER_PRINTER_NOT_FOUND
                        }.toMap())
                    }
                    return@launch
                }
            }

            // Send Settings - only if startCommunication succeeded
            val settings: Map<PrinterInfo.PrinterSettingItem, String> = dartSettings.entries.associate { (key, value) -> printerSettingItemFromMap(key) to value }
            val printResult = printer.updatePrinterSettings(settings)

            // End Communication
            if (isOneTime) {
                val connectionClosed: Boolean = printer.endCommunication()
            }

            // Encode PrinterStatus
            val dartPrintStatus = printResult.toMap()
           withContext(Dispatchers.Main) {
               // Set result Printer status.
               result.success(dartPrintStatus)
           }
        }

    }
}