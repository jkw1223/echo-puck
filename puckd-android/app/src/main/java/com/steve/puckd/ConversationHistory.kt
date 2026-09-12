package com.steve.puckd

import android.util.Log

/** Short-lived, bounded context shared by fresh Realtime sessions. */
object ConversationHistory {
    private const val TAG = "Conversation"
    private const val IDLE_TIMEOUT_MS = 5 * 60 * 1000L
    private const val MAX_TURNS = 8
    private const val MAX_CONTEXT_CHARS = 6000

    data class Snapshot(val id: String, val turnNumber: Int, val text: String, val turnCount: Int, val chars: Int)
    private data class Turn(val user: String, val assistant: String)

    private val turns = ArrayDeque<Turn>()
    private var conversationId = ""
    private var lastActivity = 0L

    @Synchronized
    fun begin(now: Long = System.currentTimeMillis()): Snapshot {
        if (lastActivity != 0L && now - lastActivity > IDLE_TIMEOUT_MS) {
            clearLocked("idle_timeout")
        }
        if (conversationId.isBlank()) conversationId = "conv-${now}"
        lastActivity = now
        val context = renderLocked()
        val snapshot = Snapshot(conversationId, turns.size + 1, context, turns.size, context.length)
        Log.i(TAG, "conversation_id=${snapshot.id} conversation_turn=${snapshot.turnNumber} conversation_history_turns=${snapshot.turnCount} conversation_context_chars=${snapshot.chars}")
        return snapshot
    }

    @Synchronized
    fun complete(user: String, assistant: String, now: Long = System.currentTimeMillis()) {
        val cleanUser = user.trim()
        val cleanAssistant = assistant.trim()
        if (cleanUser.isBlank() && cleanAssistant.isBlank()) return
        if (conversationId.isBlank()) conversationId = "conv-$now"
        turns.addLast(Turn(cleanUser.ifBlank { "[audio input]" }, cleanAssistant.ifBlank { "[no transcript]" }))
        while (turns.size > MAX_TURNS || renderLocked().length > MAX_CONTEXT_CHARS) turns.removeFirst()
        lastActivity = now
        Log.i(TAG, "conversation_id=$conversationId completed_turns=${turns.size} conversation_context_chars=${renderLocked().length}")
    }

    @Synchronized
    fun clearConversation() = clearLocked("explicit")

    @Synchronized
    fun idleTimeoutMs(): Long = IDLE_TIMEOUT_MS

    private fun renderLocked(): String = turns.joinToString("\n") { "User: ${it.user}\nSteve: ${it.assistant}" }

    private fun clearLocked(reason: String) {
        turns.clear()
        conversationId = ""
        lastActivity = 0L
        Log.i(TAG, "conversation_reset reason=$reason")
    }
}
