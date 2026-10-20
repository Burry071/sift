package com.sift.pipeline

// The engine is injected so the app layer owns model lifecycle
// (LiteRT-LM on device; scripted fakes in tests).
interface LlmEngine {
    suspend fun generate(prompt: String, maxNewTokens: Int, temperature: Double): String
}
