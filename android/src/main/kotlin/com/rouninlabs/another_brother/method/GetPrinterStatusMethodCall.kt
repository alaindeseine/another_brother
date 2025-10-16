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
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Command for getting the print status of a Brother printer.
 * This support both one-time as well as the standard openCommunication/print/closeCommunication
 * approach.
 */
class GetPrinterStatusMethodCall(val flutterAssets: FlutterPlugin.FlutterAssets, val context: Context, val call: MethodCall, val result: MethodChannel.Result) {
    companion object {
        const val METHOD_NAME = "getPrinterStatus"
    }

    private val hasResponded = AtomicBoolean(false)

    fun execute() {
        // Use a job that can be cancelled to prevent channel crashes
        val job = GlobalScope.launch(Dispatchers.IO) {
            try {
                val dartPrintInfo: HashMap<String, Any> = call.argument<HashMap<String, Any>>("printInfo")!!
                val printerId: String = call.argument<String>("printerId")!!

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
                    if (isActive && hasResponded.compareAndSet(false, true)) {
                        withContext(Dispatchers.Main) {
                            result.success(PrinterStatus().apply {
                                errorCode = error
                            }.toMap())
                        }
                    }
                    return@launch
                }

                // Set Printer Info
                printer.printerInfo = printInfo

                // Start communication
                if (isOneTime) {
                    val started: Boolean = printer.startCommunication()
                }

                // Check if still active before Brother SDK call
                if (!isActive) return@launch

                // Get printer status (this is the blocking call)
                val printResult = printer.printerStatus

                // Check if still active before ending communication
                if (!isActive) return@launch

                // End Communication
                if (isOneTime) {
                    val connectionClosed: Boolean = printer.endCommunication()
                }

                // Check if still active before responding
                if (isActive && hasResponded.compareAndSet(false, true)) {
                    val dartPrintStatus = printResult.toMap()
                    withContext(Dispatchers.Main) {
                        result.success(dartPrintStatus)
                    }
                }

            } catch (e: Exception) {
                // Handle any exceptions and only respond if still active
                if (isActive && hasResponded.compareAndSet(false, true)) {
                    withContext(Dispatchers.Main) {
                        result.success(PrinterStatus().apply {
                            errorCode = PrinterInfo.ErrorCode.ERROR_COMMUNICATION_ERROR
                        }.toMap())
                    }
                }
            }
        }

        // Send timeout response if job takes too long
        GlobalScope.launch {
            delay(1500L) // 1.5 second timeout
            if (job.isActive && hasResponded.compareAndSet(false, true)) {
                job.cancel()
                withContext(Dispatchers.Main) {
                    result.success(PrinterStatus().apply {
                        errorCode = PrinterInfo.ErrorCode.ERROR_COMMUNICATION_ERROR
                    }.toMap())
                }
            }
        }

    }
}