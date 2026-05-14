package com.m2h.m2haichatbot.data.api

import kotlinx.serialization.Serializable

@Serializable
data class ChatChunk(
    val choices: List<ChunkChoice>
)

@Serializable
data class ChunkChoice(
    val delta: ChunkDelta
)

@Serializable
data class ChunkDelta(
    val content: String? = null
)
