package com.expensetracker.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback
import com.expensetracker.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdManager @Inject constructor(
  private val application: Application
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

  private var appOpenAd: AppOpenAd? = null
  private var interstitialAd: InterstitialAd? = null
  private var rewardedAd: RewardedAd? = null
  private var rewardedInterstitialAd: RewardedInterstitialAd? = null
  private var currentActivity: Activity? = null
  private var lastAppOpenTime = 0L
  private val isShowingAd = AtomicBoolean(false)
  private var actionCount = 0

  fun initialize() {
    if (!BuildConfig.ENABLE_ADS) return
    MobileAds.initialize(application)
    application.registerActivityLifecycleCallbacks(this)
    ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    loadInterstitial()
    loadRewarded()
    loadRewardedInterstitial()
  }

  override fun onStart(owner: LifecycleOwner) {
    if (System.currentTimeMillis() - lastAppOpenTime > 4 * 60 * 60 * 1000) {
      showAppOpenIfAvailable()
    }
  }

  fun onSignificantAction() {
    actionCount++
    if (actionCount % 8 == 0) showInterstitialIfAvailable()
  }

  fun showRewarded(activity: Activity, onReward: () -> Unit) {
    val ad = rewardedAd
    if (ad != null) {
      ad.show(activity) { onReward(); loadRewarded() }
    } else {
      rewardedInterstitialAd?.show(activity) { onReward(); loadRewardedInterstitial() }
        ?: onReward()
    }
  }

  private fun showAppOpenIfAvailable() {
    val activity = currentActivity ?: return
    val ad = appOpenAd ?: return
    if (isShowingAd.get()) return
    isShowingAd.set(true)
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        appOpenAd = null
        isShowingAd.set(false)
        lastAppOpenTime = System.currentTimeMillis()
        loadAppOpen()
      }
      override fun onAdFailedToShowFullScreenContent(error: AdError) {
        isShowingAd.set(false)
      }
    }
    ad.show(activity)
  }

  private fun showInterstitialIfAvailable() {
    val activity = currentActivity ?: return
    val ad = interstitialAd ?: return
    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
      override fun onAdDismissedFullScreenContent() {
        interstitialAd = null
        loadInterstitial()
      }
    }
    ad.show(activity)
  }

  private fun loadAppOpen() {
    AppOpenAd.load(application, BuildConfig.ADMOB_APP_OPEN_ID, AdRequest.Builder().build(),
      object : AppOpenAd.AppOpenAdLoadCallback() {
        override fun onAdLoaded(ad: AppOpenAd) { appOpenAd = ad }
        override fun onAdFailedToLoad(error: LoadAdError) { appOpenAd = null }
      })
  }

  private fun loadInterstitial() {
    InterstitialAd.load(application, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
      object : InterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: InterstitialAd) { interstitialAd = ad }
        override fun onAdFailedToLoad(error: LoadAdError) { interstitialAd = null }
      })
  }

  private fun loadRewarded() {
    RewardedAd.load(application, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
      object : RewardedAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedAd) { rewardedAd = ad }
        override fun onAdFailedToLoad(error: LoadAdError) { rewardedAd = null }
      })
  }

  private fun loadRewardedInterstitial() {
    RewardedInterstitialAd.load(application, BuildConfig.ADMOB_REWARDED_INTERSTITIAL_ID,
      AdRequest.Builder().build(), object : RewardedInterstitialAdLoadCallback() {
        override fun onAdLoaded(ad: RewardedInterstitialAd) { rewardedInterstitialAd = ad }
        override fun onAdFailedToLoad(error: LoadAdError) { rewardedInterstitialAd = null }
      })
  }

  override fun onActivityResumed(activity: Activity) { currentActivity = activity }
  override fun onActivityPaused(activity: Activity) { if (currentActivity == activity) currentActivity = null }
  override fun onActivityCreated(a: Activity, b: Bundle?) {}
  override fun onActivityStarted(a: Activity) {}
  override fun onActivityStopped(a: Activity) {}
  override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
  override fun onActivityDestroyed(a: Activity) {}
}
