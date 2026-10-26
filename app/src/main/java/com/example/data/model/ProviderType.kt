package com.example.data.model

enum class ProviderType(
    val displayName: String,
    val defaultModel: String,
    val suggestedModels: List<String>,
    val defaultBaseUrl: String,
    val keyHint: String,
    val documentationUrl: String
) {
    GEMINI(
        displayName = "Google Gemini",
        defaultModel = "gemini-2.5-flash",
        suggestedModels = listOf(
            "gemini-2.5-flash",
            "gemini-2.0-flash",
            "gemini-1.5-flash",
            "gemini-1.5-pro"
        ),
        defaultBaseUrl = "https://generativelanguage.googleapis.com",
        keyHint = "AIzaSy...",
        documentationUrl = "aistudio.google.com/apikey"
    ),
    GROK(
        displayName = "xAI Grok",
        defaultModel = "grok-2",
        suggestedModels = listOf(
            "grok-2",
            "grok-2-mini",
            "grok-beta"
        ),
        defaultBaseUrl = "https://api.x.ai/v1",
        keyHint = "xai-...",
        documentationUrl = "console.x.ai"
    ),
    CLAUDE(
        displayName = "Anthropic Claude",
        defaultModel = "claude-3-5-haiku-20241022",
        suggestedModels = listOf(
            "claude-3-5-haiku-20241022",
            "claude-3-5-sonnet-20241022",
            "claude-3-opus-20240229"
        ),
        defaultBaseUrl = "https://api.anthropic.com",
        keyHint = "sk-ant-...",
        documentationUrl = "console.anthropic.com"
    ),
    OPENAI(
        displayName = "OpenAI",
        defaultModel = "gpt-4o-mini",
        suggestedModels = listOf(
            "gpt-4o-mini",
            "gpt-4o",
            "o3-mini",
            "gpt-3.5-turbo"
        ),
        defaultBaseUrl = "https://api.openai.com/v1",
        keyHint = "sk-proj-...",
        documentationUrl = "platform.openai.com/api-keys"
    ),
    OPENROUTER(
        displayName = "OpenRouter",
        defaultModel = "meta-llama/llama-3.3-70b-instruct:free",
        suggestedModels = listOf(
            "meta-llama/llama-3.3-70b-instruct:free",
            "google/gemini-2.0-flash-exp:free",
            "deepseek/deepseek-chat",
            "anthropic/claude-3.5-sonnet"
        ),
        defaultBaseUrl = "https://openrouter.ai/api/v1",
        keyHint = "sk-or-...",
        documentationUrl = "openrouter.ai/keys"
    ),
    HUGGINGFACE(
        displayName = "Hugging Face",
        defaultModel = "meta-llama/Llama-3.2-3B-Instruct",
        suggestedModels = listOf(
            "meta-llama/Llama-3.2-3B-Instruct",
            "Qwen/Qwen2.5-72B-Instruct",
            "mistralai/Mistral-7B-Instruct-v0.3"
        ),
        defaultBaseUrl = "https://router.huggingface.co/hf-inference/v1",
        keyHint = "hf_...",
        documentationUrl = "huggingface.co/settings/tokens"
    )
}
