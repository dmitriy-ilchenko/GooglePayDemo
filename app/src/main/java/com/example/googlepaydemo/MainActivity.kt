package com.example.googlepaydemo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import com.google.android.gms.wallet.AutoResolveHelper
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val GOOGLE_PAY_ACTIVITY_REQUEST_CODE = 1
    }

    private val disposable = CompositeDisposable()
    private val googlePay by lazy { GooglePay(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        handlePriceChanged()
        handlePayButtonClicked()
        checkIfGooglePayReady()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != GOOGLE_PAY_ACTIVITY_REQUEST_CODE || data == null) {
            return
        }

        when (resultCode) {
            RESULT_OK -> {
                val response = googlePay.getPaymentData(data)?.toJson().toString()
                showPaymentSuccess(response)
            }
            AutoResolveHelper.RESULT_ERROR -> {
                val error = googlePay.getErrorCode(data).toString()
                showErrorDialog(error)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        disposable.clear()
    }

    private fun handlePriceChanged() {
        edit_text_price.addTextChangedListener {
            togglePayButton()
        }
    }

    private fun handlePayButtonClicked() {
        button_pay.setOnClickListener {
            pay()
        }
    }

    private fun togglePayButton() {
        val price = edit_text_price.text.toString().toDoubleOrNull()
        button_pay.isEnabled = price != null
    }

    private fun checkIfGooglePayReady() {
        googlePay.isReadyToPay()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(::showGooglePayIsReady, ::showError)
            .addTo(disposable)
    }

    private fun pay() {
        val price = edit_text_price.text.toString()
        googlePay.pay(price, GOOGLE_PAY_ACTIVITY_REQUEST_CODE)
    }

    private fun showGooglePayIsReady(isReady: Boolean) {
        group_main_content.isVisible = true
        progress_bar_loading.isVisible = false
        if (isReady.not()) {
            showGooglePayIsNotReadyError()
        }
    }

    private fun showGooglePayIsNotReadyError() {
       showErrorDialog(getString(R.string.activity_main_error_google_pay_is_not_ready_error_text))
    }

    private fun showPaymentSuccess(message: String) {
        showSuccessDialog(getString(R.string.activity_main_success_payment, message))
    }

    private fun showError(error: Throwable) {
        showErrorDialog(error.message.orEmpty())
    }

    private fun showErrorDialog(message: String) {
        showDialog(getString(R.string.activity_main_error_dialog_title), message)
    }

    private fun showSuccessDialog(message: String) {
        showDialog(getString(R.string.activity_main_success_dialog_title), message)
    }

    private fun showDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(R.string.activity_main_error_dialog_positive_button_text, null)
            .create()
            .show()
    }
}
