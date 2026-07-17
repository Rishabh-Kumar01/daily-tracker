package dev.rishabh.dailytracker.core.designsystem.component.model

/**
 * An in-progress field value, before it becomes a log_values row.
 *
 * Mirrors the sparse value columns exactly (number / text / bool / json), so committing a
 * draft is a straight copy with no lossy mapping. One draft per field; the renderer owns
 * the draft and hands edits back through onChange.
 */
data class LogValueDraft(
    val fieldKey: String,
    val number: Double? = null,
    val text: String? = null,
    val bool: Boolean? = null,
    val json: String? = null,
) {
    fun withNumber(value: Double?) = copy(number = value)
    fun withText(value: String?) = copy(text = value)
    fun withBool(value: Boolean?) = copy(bool = value)
    fun withJson(value: String?) = copy(json = value)

    companion object {
        fun empty(fieldKey: String) = LogValueDraft(fieldKey)
    }
}
