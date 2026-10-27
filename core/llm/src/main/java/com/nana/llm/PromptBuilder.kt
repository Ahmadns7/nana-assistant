package com.nana.llm

import com.nana.common.SessionContext
import com.nana.common.VoiceIntent

/**
 * Builds structured prompts for the on-device LLM.
 */
object PromptBuilder {
    private const val FAST_PATH_PROMPT = """
Classify the user's intent into ONE category:
- MEDIA: play/pause/next/prev/volume/what's playing
- APP: open/close/switch app
- MESSAGE: WhatsApp/SMS - open/find/read/reply/send
- CALL: make call
- ALARM: set alarm
- TIMER: set timer
- TOGGLE: wifi/bluetooth/flashlight/dnd/brightness
- INFO: battery/storage/calendar/contacts
- SCREEN: read/summarize/copy/translate/ocr
- MACRO: trigger voice macro
- SETTINGS: get/set/reset setting
- UNKNOWN: anything else

User: {intent}
Category:""".trimIndent()

    private const val AGENT_PROMPT = """
You are NaNa, controlling an Android phone via accessibility.
Current screen: {screenSummary}
User intent: {intent}
Referenced entities: {entities}

Plan the next action. Respond with ONLY one line:
ACTION: <tool_name>(<params>) | RESPONSE: <text> | ASK: <question>

Available tools:
{tools}

Rules:
- One action per step
- Use screen context to find elements
- Verify after acting
- Say "STUCK" if unable to proceed
""".trimIndent()

    fun buildFastPathPrompt(intent: String): String = FAST_PATH_PROMPT.replace("{intent}", intent)

    fun buildAgentPrompt(
        intent: String,
        screenSummary: String,
        entities: String,
        tools: List<String>
    ): String = AGENT_PROMPT
        .replace("{intent}", intent)
        .replace("{screenSummary}", screenSummary)
        .replace("{entities}", entities)
        .replace("{tools}", tools.joinToString("
"))

    fun buildScreenQueryPrompt(question: String, screenText: String): String = """
Answer the question based ONLY on the screen content below.
If the answer isn't there, say "I don't see that on screen."

Screen content:
$screenText

Question: $question
Answer:""".trimIndent()

    fun buildWhatsAppPrompt(action: String, chatName: String?, lastMessages: List<String>): String {
        val messages = lastMessages.joinToString("
") { "  - $it" }
        return """
WhatsApp task: $action
Target chat: ${chatName ?: "unknown"}
Recent messages:
$messages

Plan the exact accessibility actions needed. Be specific about element text/content-desc.ACTION: ...""".trimIndent()
    }
}
