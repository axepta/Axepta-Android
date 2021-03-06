package com.axepta.android.sdk.example

import android.content.DialogInterface
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.axepta.android.sdk.*
import com.common.android.utils.extensions.MathExtensions
import com.common.android.utils.extensions.ViewExtensions
import com.axepta.android.sdk.example.adapters.ArticleAdapter
import com.axepta.android.sdk.example.adapters.PaymentMethodAdapter
import com.axepta.android.sdk.example.databinding.FragmentLandingPageBinding
import com.dtx12.android_animations_actions.actions.Actions
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import java.util.*

/**
 * Created by jan.rabe on 26/09/16.
 */
class LandingPage : Fragment() {

    private var _binding: FragmentLandingPageBinding? = null
    private val binding
        get() = _binding!!

    private var disposable: Disposable? = null
    private var paymentDisposable: Disposable? = null

    private val articles = mutableListOf(
        Article().setName("Buildmaster 2013").setColor("Color: Black").setPrice("20").setImage(R.drawable.black),
        Article().setName("Golden Screws 42").setColor("Color: Gold").setPrice("40").setImage(R.drawable.gold),
        Article().setName("Silver Screws T3").setColor("Color: Silver").setPrice("10").setImage(R.drawable.silver)
    )

    private val basket = ArrayList<Article>()
    private val basketItemsText by lazy { getString(R.string.basket_post_text) }
    private val basketText: String
        get() = "${basket.size} $basketItemsText"

    private var axepta: Axepta? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentLandingPageBinding.inflate(layoutInflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initAxepta()
        initArticles()

        initPaymentMethods()

        ViewExtensions.setOneTimeGlobalLayoutListener(view) {
            Actions.play(Actions.sizeTo(view.width.toFloat(), MathExtensions.dpToPx(0).toFloat(), 0f), binding.basketView)
        }

        binding.payOptions.setOnClickListener {
            if (basket.isNotEmpty()) {
                Actions.play(Actions.sizeTo(requireView().width.toFloat(), MathExtensions.dpToPx(300).toFloat(), 0.5f), binding.basketView)
            }
        }
    }

    override fun onDestroy() {
        _binding = null
        super.onDestroy()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        if (basket.isNotEmpty()) {
            Actions.play(Actions.sizeTo(MathExtensions.dpToPx(newConfig.screenWidthDp).toFloat(), binding.basketView.height.toFloat(), 0.1f), binding.basketView)
        }
    }

    private fun initAxepta() {
        axepta = Axepta.with(activity)
    }

    private fun initArticles() {
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.recyclerView.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(binding.recyclerView.context, LinearLayoutManager.VERTICAL)
        binding.recyclerView.addItemDecoration(dividerItemDecoration)

        val articleAdapter = ArticleAdapter(this::onItemClicked)
        binding.recyclerView.adapter = articleAdapter

        // add articles to recyclerView
        val items = mutableListOf<Article>()
        articles.filterNotNull().forEach { article -> items.add(article) }
        articleAdapter.updateList(items)
    }

    private fun initPaymentMethods() {
        val linearLayoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        binding.paymentMethodsList.layoutManager = linearLayoutManager

        val dividerItemDecoration = DividerItemDecoration(binding.paymentMethodsList.context, LinearLayoutManager.VERTICAL)
        binding.paymentMethodsList.addItemDecoration(dividerItemDecoration)

        val paymentMethodAdapter = PaymentMethodAdapter(this::payWithPaymentOption)
        binding.paymentMethodsList.adapter = paymentMethodAdapter

        //load payment methods
        paymentDisposable = axepta!!.requestPaymentMethods().subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { paymentMethods: List<PaymentMethod?>, throwable: Throwable? ->
                if (throwable != null) {
                    Log.e(TAG, "PaymentMethods error: ", throwable)
                    return@subscribe
                }

                // add paymentMethods to recyclerView
                val items = mutableListOf<PaymentMethod>()
                paymentMethods.filterNotNull().forEach { paymentMethod ->
                    items.add(paymentMethod)
                }
                paymentMethodAdapter?.updateList(items)
            }
    }

    private fun onItemClicked(article: Article) {
        basket.add(article)

        if (basket.size > 0) {
            Actions.play(Actions.sizeTo(requireView().width.toFloat(), MathExtensions.dpToPx(50).toFloat(), 0.5f), binding.basketView)
            binding.basketText.text = basketText
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()

        axepta?.clean()

        if (disposable != null && disposable?.isDisposed == false) {
            disposable?.dispose()
        }

        if (paymentDisposable != null && paymentDisposable?.isDisposed == false) {
            paymentDisposable?.dispose()
        }
    }

    private fun payWithPaymentOption(method: PaymentMethod, paymentType: String) {
        setPayment(method.payment, paymentType)

        //set payment method that has the right payment data, and checkout
        disposable = axepta
            ?.withPaymentMethod(method)
            ?.setWebViewListener { Log.i(TAG, "[WebViewClient] onPageFinishedLoading") }
            ?.checkout()
            ?.subscribeOn(Schedulers.io())
            ?.observeOn(AndroidSchedulers.mainThread())
            ?.subscribe({ axeptaPaymentResponse: AxeptaPaymentResponse ->
                Log.v(TAG, "Payment received")
                Log.v(TAG, axeptaPaymentResponse.toString())
            }) { throwable: Throwable ->
                when (throwable) {
                    is AxeptaError -> {
                        Log.e(TAG, "Axepta Error $throwable")
                        showErrorAlert(throwable)
                    }
                    is InternalError -> {
                        Log.e(TAG, "Internal Error boolean isPaymentCanceled(): " + throwable.isPaymentCanceled)
                    }
                    else -> {
                        Log.e(TAG, "Payment error: ", throwable)
                    }
                }
            }
    }

    /**
     * Fill payment data with key, value
     * @param payment
     * @return
     */
    private fun setPayment(payment: Payment, paymentType: String): Payment {
        var price = 0
        for (article in basket) {
            price += article.price?.toInt() ?: 0
        }
        payment.setParamWithKey("TransID", "****")
        payment.setParamWithKey("Amount", (price * 100).toString())
        payment.setParamWithKey("Currency", "EUR") //for Wechat payment only CNY is supported
        payment.setParamWithKey("URLSuccess", "****")
        payment.setParamWithKey("URLNotify", "****")
        payment.setParamWithKey("URLFailure", "****")
        payment.setParamWithKey("RefNr", "****")
        payment.setParamWithKey("OrderDesc", "****")
        payment.setParamWithKey("AddrCity", "****")
        payment.setParamWithKey("FirstName", "****")
        payment.setParamWithKey("LastName", "****")
        payment.setParamWithKey("AddrZip", "****")
        payment.setParamWithKey("AddrStreet", "****")
        payment.setParamWithKey("AddrState", "****")
        payment.setParamWithKey("AddrCountryCode", "****")
        payment.setParamWithKey("Phone", "****")
        payment.setParamWithKey("LandingPage", "****")
        payment.setParamWithKey("eMail", "****")
        payment.setParamWithKey("ShopID", "1")
        payment.setParamWithKey("Subject", "****")
        payment.setParamWithKey("Language", "en")
        payment.setParamWithKey("Channel", "APP")
        payment.setParamWithKey("MerchantID", "****")
        return payment
    }

    private fun showErrorAlert(error: AxeptaError) {
        val code = error.code
        val severity = error.severity
        val category = error.category
        val details = error.details
        val alertDialog = AlertDialog.Builder(requireContext()).create()
        alertDialog.setTitle("Error")
        alertDialog.setMessage(
            """
                Code: $code

                Severity: $severity

                Category: $category

                Abbreviation: $details


                """.trimIndent())
        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK") {
            dialog: DialogInterface, _: Int -> dialog.dismiss()
        }
        alertDialog.show()
    }

    companion object {
        val TAG = LandingPage::class.java.simpleName
    }
}