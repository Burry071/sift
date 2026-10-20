package com.sift.engine

import com.sift.pipeline.LlmEngine
import kotlinx.coroutines.flow.MutableStateFlow

// App-scope state: model lifecycle status, the engine provider the worker
// consumes, and the filing flag the catalog's SIFTING pulse row listens to.
object SiftEngine {

    enum class ModelStatus { MISSING, LOADING, READY }

    val modelStatus = MutableStateFlow(ModelStatus.MISSING)
    val filing = MutableStateFlow(false)

    @Volatile
    var engineProvider: () -> LlmEngine? = { null }
}
