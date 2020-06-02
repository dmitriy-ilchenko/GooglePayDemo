package com.example.googlepaydemo

import android.app.Activity
import android.content.Intent
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wallet.*
import io.reactivex.Single
import io.reactivex.SingleEmitter
import org.json.JSONArray

class GooglePay(private val activity: Activity) {

    companion object {
        private const val PAYMENTS_ENVIRONMENT = WalletConstants.ENVIRONMENT_TEST
        private const val API_VERSION = 2
        private const val API_VERSION_MINOR = 0
        private const val MERCHANT_NAME = "Example Merchant"
        private val ALLOWED_AUTH_METHODS = JSONArray(listOf("PAN_ONLY", "CRYPTOGRAM_3DS"))
        private val ALLOWED_CARD_NETWORKS = JSONArray(listOf("AMEX", "DISCOVER", "JCB", "MASTERCARD", "VISA"))
        private const val CURRENCY_CODE = "USD"
        private const val GATEWAY = "example"
        private const val GATEWAY_MERCHANT_ID = "exampleGatewayMerchantId"
    }

    private val walletOptions = Wallet.WalletOptions.Builder()
        .setEnvironment(PAYMENTS_ENVIRONMENT)
        .build()

    private val paymentsClient = Wallet.getPaymentsClient(activity, walletOptions)

    fun isReadyToPay(): Single<Boolean> {
        return Single.create(::isReadyToPay)
    }

    fun pay(price: String, activityRequestCode: Int) {
        val request = PaymentDataRequest.fromJson(createPaymentDataRequest(price))
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
        val request = IsReadyToPayRequest.fromJson(createIsReadyToPayRequest())
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

    private fun createIsReadyToPayRequest(): String {
        return "{\n" +
                "  \"apiVersion\": ${API_VERSION},\n" +
                "  \"apiVersionMinor\": ${API_VERSION_MINOR},\n" +
                "  \"allowedPaymentMethods\": [\n" +
                "    {\n" +
                "      \"type\": \"CARD\",\n" +
                "      \"parameters\": {\n" +
                "        \"allowedAuthMethods\": $ALLOWED_AUTH_METHODS,\n" +
                "        \"allowedCardNetworks\": $ALLOWED_CARD_NETWORKS\n" +
                "      }\n" +
                "    }\n" +
                "  ]\n" +
                "}"
    }

    private fun createPaymentDataRequest(price: String): String {
        return "{\n" +
                "  \"apiVersion\": ${API_VERSION},\n" +
                "  \"apiVersionMinor\": ${API_VERSION_MINOR},\n" +
                "  \"merchantInfo\": {\n" +
                "    \"merchantName\": \"$MERCHANT_NAME\"\n" +
                "  },\n" +
                "  \"allowedPaymentMethods\": [\n" +
                "    {\n" +
                "      \"type\": \"CARD\",\n" +
                "      \"parameters\": {\n" +
                "        \"allowedAuthMethods\": $ALLOWED_AUTH_METHODS,\n" +
                "        \"allowedCardNetworks\": $ALLOWED_CARD_NETWORKS\n" +
                "      },\n" +
                "      \"tokenizationSpecification\": {\n" +
                "        \"type\": \"PAYMENT_GATEWAY\",\n" +
                "        \"parameters\": {\n" +
                "          \"gateway\": \"$GATEWAY\",\n" +
                "          \"gatewayMerchantId\": \"$GATEWAY_MERCHANT_ID\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  ],\n" +
                "  \"transactionInfo\": {\n" +
                "    \"totalPriceStatus\": \"FINAL\",\n" +
                "    \"totalPrice\": \"$price\",\n" +
                "    \"currencyCode\": \"$CURRENCY_CODE\"\n" +
                "  }\n" +
                "}"
    }
}