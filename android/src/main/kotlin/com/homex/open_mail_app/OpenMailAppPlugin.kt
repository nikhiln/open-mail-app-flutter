package com.homex.open_mail_app

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.annotation.NonNull
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class OpenMailAppPlugin : FlutterPlugin, MethodCallHandler {
    private lateinit var channel: MethodChannel
    private lateinit var applicationContext: Context

    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "open_mail_app")
        channel.setMethodCallHandler(this)
        applicationContext = flutterPluginBinding.applicationContext
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onMethodCall(@NonNull call: MethodCall, @NonNull result: Result) {
        when (call.method) {
            "openMailApp" -> result.success(emailAppIntent(call.argument("nativePickerTitle") ?: ""))
            "openSpecificMailApp" -> result.success(specificEmailAppIntent(call.argument("name") ?: ""))
            "composeNewEmailInMailApp" -> result.success(composeNewEmailAppIntent(call.argument("nativePickerTitle") ?: "", call.argument("emailContent") ?: ""))
            "composeNewEmailInSpecificMailApp" -> result.success(composeNewEmailInSpecificEmailAppIntent(call.argument("name") ?: "", call.argument("emailContent") ?: ""))
            "getMainApps" -> result.success(Gson().toJson(getInstalledMailApps()))
            else -> result.notImplemented()
        }
    }

    private fun emailAppIntent(@NonNull chooserTitle: String): Boolean {
        val emailIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
        val packageManager = applicationContext.packageManager
        val emailApps = packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)

        if (emailApps.isNotEmpty()) {
            val firstApp = emailApps.first().activityInfo.packageName
            val firstAppIntent = packageManager.getLaunchIntentForPackage(firstApp)

            val chooserIntent = Intent.createChooser(firstAppIntent, chooserTitle)
            applicationContext.startActivity(chooserIntent)
            return true
        }
        return false
    }

    private fun specificEmailAppIntent(name: String): Boolean {
        val emailIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
        val packageManager = applicationContext.packageManager
        val emailApps = packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val selectedApp = emailApps.firstOrNull { it.loadLabel(packageManager).toString() == name }
            ?: return false

        val intent = packageManager.getLaunchIntentForPackage(selectedApp.activityInfo.packageName) ?: return false
        applicationContext.startActivity(intent)
        return true
    }

    private fun composeNewEmailAppIntent(@NonNull chooserTitle: String, @NonNull contentJson: String): Boolean {
        val emailContent = Gson().fromJson(contentJson, EmailContent::class.java)
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, emailContent.to.toTypedArray())
            putExtra(Intent.EXTRA_CC, emailContent.cc.toTypedArray())
            putExtra(Intent.EXTRA_BCC, emailContent.bcc.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, emailContent.subject)
            putExtra(Intent.EXTRA_TEXT, emailContent.body)
        }

        val packageManager = applicationContext.packageManager
        val emailApps = packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)

        if (emailApps.isNotEmpty()) {
            val chooserIntent = Intent.createChooser(emailIntent, chooserTitle)
            applicationContext.startActivity(chooserIntent)
            return true
        }
        return false
    }

    private fun composeNewEmailInSpecificEmailAppIntent(@NonNull name: String, @NonNull contentJson: String): Boolean {
        val emailContent = Gson().fromJson(contentJson, EmailContent::class.java)
        val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:")).apply {
            putExtra(Intent.EXTRA_EMAIL, emailContent.to.toTypedArray())
            putExtra(Intent.EXTRA_CC, emailContent.cc.toTypedArray())
            putExtra(Intent.EXTRA_BCC, emailContent.bcc.toTypedArray())
            putExtra(Intent.EXTRA_SUBJECT, emailContent.subject)
            putExtra(Intent.EXTRA_TEXT, emailContent.body)
        }

        val packageManager = applicationContext.packageManager
        val emailApps = packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY)

        val selectedApp = emailApps.firstOrNull { it.loadLabel(packageManager).toString() == name }
            ?: return false

        emailIntent.setPackage(selectedApp.activityInfo.packageName)
        applicationContext.startActivity(emailIntent)
        return true
    }

    private fun getInstalledMailApps(): List<App> {
        val emailIntent = Intent(Intent.ACTION_VIEW, Uri.parse("mailto:"))
        val packageManager = applicationContext.packageManager
        return packageManager.queryIntentActivities(emailIntent, PackageManager.MATCH_DEFAULT_ONLY).map {
            App(it.loadLabel(packageManager).toString())
        }
    }
}

data class App(@SerializedName("name") val name: String)

data class EmailContent(
    @SerializedName("to") val to: List<String>,
    @SerializedName("cc") val cc: List<String>,
    @SerializedName("bcc") val bcc: List<String>,
    @SerializedName("subject") val subject: String,
    @SerializedName("body") val body: String
)
