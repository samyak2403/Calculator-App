package com.samyak2403.calculator

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.samyak2403.bannerad.Control
import com.samyak2403.calculator.R
import com.samyak2403.calculator.databinding.ActivityMainBinding
import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {

    // View binding for accessing UI elements
    private lateinit var binding: ActivityMainBinding

    // Variables to track the state of the calculator
    private var lastNumeric = false
    private var stateError = false
    private var lastDot = false

    // Expression object for evaluating mathematical expressions
    private lateinit var expression: Expression

    // Variables for managing rewarded interstitial ads
    private var rewardedInterstitialAd: RewardedInterstitialAd? = null
    private val adHandler = Handler(Looper.getMainLooper())
    private val adInterval: Long = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the Control class and load the banner ad
        val control = Control(this)
        val adUnitId = getString(R.string.admob_banner_id) // Fetch the AdMob banner ID from resources
        control.loadBannerAd(R.id.bannerAdContainer, adUnitId)

        // Initialize MobileAds SDK
        MobileAds.initialize(this) {}

        // Load rewarded interstitial ad
        loadRewardedInterstitialAd()

        // Schedule the rewarded interstitial ad to show after 30 seconds
        adHandler.postDelayed({
            showRewardedInterstitialAd()
        }, 30000) // 30 seconds delay
    }

    private fun loadRewardedInterstitialAd() {
        val adRequest = AdRequest.Builder().build()
        RewardedInterstitialAd.load(this, getString(R.string.admob_rewarded_interstitial_id), adRequest, object : RewardedInterstitialAdLoadCallback() {

            override fun onAdLoaded(rewardedInterstitialAd: RewardedInterstitialAd) {
                this@MainActivity.rewardedInterstitialAd = rewardedInterstitialAd
                setAdCallbacks()
                Log.d("AdLoad", "Rewarded interstitial ad loaded.")
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdLoadError", "Ad failed to load: ${loadAdError.message}")
                rewardedInterstitialAd = null
            }
        })
    }

    private fun setAdCallbacks() {
        rewardedInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.v("AdEvent", "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.v("AdEvent", "Ad was dismissed.")
                loadRewardedInterstitialAd() // Preload next ad
                // Schedule the ad to show again after 24 hours
                adHandler.postDelayed({
                    showRewardedInterstitialAd()
                }, adInterval)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                Log.e("AdShowError", "Ad failed to show: ${adError.message}")
            }

            override fun onAdShowedFullScreenContent() {
                Log.v("AdEvent", "Ad showed full-screen content.")
                rewardedInterstitialAd = null
            }
        }
    }

    private fun showRewardedInterstitialAd() {
        rewardedInterstitialAd?.show(this) { rewardItem: RewardItem ->
            // Handle the reward
            val rewardAmount = rewardItem.amount
            val rewardType = rewardItem.type
            Log.v("AdReward", "User earned the reward: $rewardAmount $rewardType")
        } ?: run {
            Log.v("AdStatus", "Rewarded interstitial ad is not ready yet.")
        }
    }

    // Function to handle 'All Clear' button click
    fun onAllClearClick(view: View) {
        // Clear all text fields and reset the state
        binding.dataTv.text = ""
        binding.resultTv.text = ""
        stateError = false
        lastDot = false
        lastNumeric = false
        binding.resultTv.visibility = View.GONE
    }

    // Function to handle 'Equal' button click
    fun onEqualClick(view: View) {
        // Calculate and display the result
        onEqual()
        // Update the data text view with the result (dropping the '=' sign)
        binding.dataTv.text = binding.resultTv.text.toString().drop(1)
    }

    // Function to handle digit button clicks
    fun onDigitClick(view: View) {
        // If an error occurred, reset the text view
        if (stateError) {
            binding.dataTv.text = (view as Button).text
            stateError = false
        } else {
            // Append the clicked digit to the text view
            binding.dataTv.append((view as Button).text)
        }

        lastNumeric = true // A digit has been entered
        onEqual() // Calculate the intermediate result
    }

    // Function to handle operator button clicks
    fun onOperatorClick(view: View) {
        // Only append an operator if the last input was numeric and no error exists
        if (!stateError && lastNumeric) {
            binding.dataTv.append((view as Button).text)
            lastDot = false
            lastNumeric = false
            onEqual() // Calculate the intermediate result
        }
    }

    // Function to handle the 'Backspace' button click
    fun onBackClick(view: View) {
        val text = binding.dataTv.text.toString()
        // Remove the last character if the text is not empty
        if (text.isNotEmpty()) {
            binding.dataTv.text = text.dropLast(1)

            try {
                // Check the last character after deletion
                val lastChar = binding.dataTv.text.toString().lastOrNull()
                if (lastChar != null && lastChar.isDigit()) {
                    onEqual() // Calculate the intermediate result
                }
            } catch (e: Exception) {
                // Handle any exception if no character is left
                binding.resultTv.text = ""
                binding.resultTv.visibility = View.GONE
                Log.e("Last char error", e.toString())
            }
        }
    }

    // Function to handle the 'Clear' button click
    fun onClearClick(view: View) {
        // Clear the data text view
        binding.dataTv.text = ""
        lastNumeric = false
    }

    // Function to evaluate the expression and display the result
    private fun onEqual() {
        if (lastNumeric && !stateError) {
            var txt = binding.dataTv.text.toString()

            try {
                // Replace 'x' with '*' for multiplication
                txt = txt.replace("x", "*")

                // Build and evaluate the expression
                expression = ExpressionBuilder(txt).build()
                val result = expression.evaluate()

                // Display the result
                binding.resultTv.visibility = View.VISIBLE
                binding.resultTv.text = "" + result.toString()
            } catch (ex: ArithmeticException) {
                // Handle any arithmetic errors during evaluation
                Log.e("Evaluate error", ex.toString())
                binding.resultTv.text = "Error"
                stateError = true
                lastNumeric = false
            } catch (ex: Exception) {
                // Handle other exceptions
                Log.e("Unknown error", ex.toString())
                binding.resultTv.text = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove any pending callbacks to avoid memory leaks
        adHandler.removeCallbacksAndMessages(null)
    }
}
