package com.nopo.cheonnyeon.core.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/** 수동 주입용: `viewModel(factory = factoryOf { MapViewModel(container) })` */
inline fun <reified T : ViewModel> factoryOf(crossinline create: () -> T): ViewModelProvider.Factory =
    object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <V : ViewModel> create(modelClass: Class<V>): V = create() as V
    }
