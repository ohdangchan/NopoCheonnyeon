package com.nopo.cheonnyeon

import android.app.Application
import android.content.Context
import android.os.StrictMode
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import okhttp3.OkHttpClient
import com.nopo.cheonnyeon.app.AppContainer

class NopoApp : Application(), SingletonImageLoader.Factory {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectDiskReads().detectDiskWrites().detectNetwork().penaltyLog().build())
        }
        container = AppContainer(this)
    }

    /** 공식 사진 서버(sbiz24)는 Accept 헤더가 없는 요청에 500을 돌려줘서, 모든 요청에 Accept를 붙인다 */
    override fun newImageLoader(context: Context): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val req = chain.request().newBuilder().header("Accept", "image/*,*/*").build()
                val res = chain.proceed(req)
                if (BuildConfig.DEBUG && !res.isSuccessful) {
                    android.util.Log.e("Nopo", "사진 서버 응답 ${res.code} 주소=${req.url} 본문=${res.peekBody(300).string()}")
                }
                res
            }
            .build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    }
}
