package com.example.googlepaydemo

import android.app.Activity
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import io.reactivex.Single
import io.reactivex.SingleEmitter
import org.json.JSONArray
import org.json.JSONObject

class GooglePay(private val activity: Activity) {

    companion object {
        private const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST

        private val SUPPORTED_METHODS = listOf("PAN_ONLY", "CRYPTOGRAM_3DS")
        private val SUPPORTED_NETWORKS = listOf("AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA")

        private const val COUNTRY_CODE = "US"
        private const val CURRENCY_CODE = "USD"
        private val SHIPPING_SUPPORTED_COUNTRIES = listOf("US", "GB", "RU")

        private val PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS = mapOf(
            "gateway" to "example",
            "gatewayMerchantId" to "exampleGatewayMerchantId"
        )

        private const val MERCHANT_NAME = "Example Merchant"
    }

    private val walletOptions = Wallet.WalletOptions.Builder()
        .setEnvironment(PAYMENTS_ENVIRONMENT)
        .build()

    private val paymentsClient = Wallet.getPaymentsClient(activity, walletOptions)

    fun isReadyToPay(): Single<Boolean> {
        return Single.create(::isReadyToPay)
    }

    fun pay(price: String, activityRequestCode: Int) {
        val request = PaymentDataRequest.fromJson(createPaymentDataRequest(price).toString())
        AutoResolveHelper.resolveTask(
            paymentsClient.loadPaymentData(request),
            activity,
            activityRequestCode
        )
    }

    fun getPaymentData(intent: Intent): PaymentData? {
        return PaymentData.getFromIntent(intent)
    }

    fun getErrorCode(intent: Intent): Int? {
        return AutoResolveHelper.getStatusFromIntent(intent)?.statusCode
    }

    private fun isReadyToPay(emitter: SingleEmitter<Boolean>) {
        val request = IsReadyToPayRequest.fromJson(createIsReadyToPayRequest().toString())
        val task = paymentsClient.isReadyToPay(request)
        task.addOnCompleteListener { completedTask ->
            try {
                val result = completedTask.getResult(ApiException::class.java)
                emitter.onSuccess(result ?: false)
            } catch (ex: ApiException) {
                emitter.onError(ex)
            }
        }
    }

    private fun createIsReadyToPayRequest(): JSONObject {
        return createBaseRequest().apply {
            put("allowedPaymentMethods", JSONArray().put(createBaseCardPaymentMethod()))
        }
    }

    private fun createPaymentDataRequest(price: String): JSONObject {
        return createBaseRequest().apply {
            put("allowedPaymentMethods", JSONArray().put(createCardPaymentMethod()))
            put("transactionInfo", createTransactionInfo(price))
            put("merchantInfo", createMerchantInfo())
            put("shippingAddressRequired", true)

            val shippingAddressParameters = JSONObject().apply {
                put("phoneNumberRequired", false)
                put("allowedCountryCodes", JSONArray(SHIPPING_SUPPORTED_COUNTRIES))
            }
            put("shippingAddressParameters", shippingAddressParameters)
        }
    }

    private fun createCardPaymentMethod(): JSONObject {
        return createBaseCardPaymentMethod().apply {
            put("tokenizationSpecification", createGatewayTokenizationSpecification())
        }
    }

    private fun createGatewayTokenizationSpecification(): JSONObject {
        return JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject(PAYMENT_GATEWAY_TOKENIZATION_PARAMETERS))
        }
    }

    private fun createTransactionInfo(price: String): JSONObject {
        return JSONObject().apply {
            put("totalPrice", price)
            put("totalPriceStatus", "FINAL")
            put("countryCode", COUNTRY_CODE)
            put("currencyCode", CURRENCY_CODE)
        }
    }

    private fun createMerchantInfo(): JSONObject {
        return JSONObject().put("merchantName", MERCHANT_NAME)
    }

    private fun createBaseCardPaymentMethod(): JSONObject {
        val parameters = JSONObject().apply {
            put("allowedAuthMethods", JSONArray(SUPPORTED_METHODS))
            put("allowedCardNetworks", JSONArray(SUPPORTED_NETWORKS))
            put("billingAddressRequired", true)
            put("billingAddressParameters", JSONObject().apply {
                put("format", "FULL")
            })
        }
        return JSONObject().apply {
            put("type", "CARD")
            put("parameters", parameters)
        }
    }

    private fun createBaseRequest(): JSONObject {
        return JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }
    }
}