package com.nopo.cheonnyeon.platform.map

import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.naver.maps.map.MapView
import com.naver.maps.map.NaverMap

/**
 * 네이버 지도 공식 MapView를 Compose에 붙인다 (아키텍처 5.6, ADR-02).
 * 공식 문서 요구대로 생명주기 8개(onCreate·onStart·onResume·onPause·onStop·onDestroy·onSaveInstanceState·onLowMemory)를 모두 전달한다.
 * onCreate를 빠뜨리면 지도가 빈 화면이 된다.
 */
@Composable
fun NaverMapView(
    modifier: Modifier = Modifier,
    onMapReady: (NaverMap) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    // 회전·프로세스 재생성 후 지도 상태(카메라 위치 등) 복원
    val saved = rememberSaveable { Bundle() }
    val mapView = remember {
        MapView(context).apply {
            onCreate(saved)
            getMapAsync(onMapReady)
        }
    }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> {
                    mapView.onSaveInstanceState(saved)
                    mapView.onStop()
                }
                else -> Unit
            }
        }
        // 공식 문서가 요구하는 onLowMemory 전달 (시스템이 메모리 부족을 알릴 때)
        val memory = object : ComponentCallbacks {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit
            @Deprecated("Deprecated in Java")
            override fun onLowMemory() = mapView.onLowMemory()
        }
        // addObserver는 이미 지난 ON_START·ON_RESUME을 다시 보내 주므로 따로 호출하지 않는다
        lifecycleOwner.lifecycle.addObserver(observer)
        context.registerComponentCallbacks(memory)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            context.unregisterComponentCallbacks(memory)
            mapView.onSaveInstanceState(saved)
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) mapView.onPause()
            if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) mapView.onStop()
            mapView.onDestroy()
        }
    }
    AndroidView(factory = { mapView }, modifier = modifier)
}
