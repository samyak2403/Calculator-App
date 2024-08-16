package com.samyak2403.calculator

import com.google.android.gms.ads.interstitial.InterstitialAd



import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback

import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

class Helper(private val activity: Activity) {

    private var mInterstitialAd: InterstitialAd? = null
    private var countClicks = 0

    init {
        prepareInterstitialAd()
    }

    private fun prepareInterstitialAd() {
        MobileAds.initialize(activity) {
            loadAd()
        }
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(activity, activity.getString(R.string.admob_interstitial_id), adRequest, object : InterstitialAdLoadCallback() {

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                mInterstitialAd = interstitialAd
                setAdCallbacks()
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                Log.e("AdLoadError", "Ad failed to load: ${loadAdError.message}")
                mInterstitialAd = null
            }
        })
    }

    private fun setAdCallbacks() {
        mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdClicked() {
                Log.v("AdEvent", "Ad was clicked.")
            }

            override fun onAdDismissedFullScreenContent() {
                Log.v("AdEvent", "Ad was dismissed.")
                loadAd() // Preload next ad
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.e("AdShowError", "Ad failed to show: ${adError.message}")
            }

            override fun onAdShowedFullScreenContent() {
                Log.v("AdEvent", "Ad showed full-screen content.")
                mInterstitialAd = null
            }
        }
    }

    fun showInterstitialAd() {
        if (mInterstitialAd != null) {
            mInterstitialAd?.show(activity)
        } else {
            Log.v("AdStatus", "Interstitial ad is not ready yet.")
        }
    }

    fun showCounterInterstitialAd(check: Int) {
        countClicks++
        if (countClicks >= check) {
            showInterstitialAd()
            countClicks = 0
        }
    }
}
