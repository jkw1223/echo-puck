package com.steve.puckd

/** Transport-independent turn contract. Implementations own network/protocol details. */
interface TurnTransport {
    val busy: Boolean
    val recording: Boolean
    fun start()
    fun finish()
    fun cancel()
    fun connectionChanged(token: String)
    fun close()
}
