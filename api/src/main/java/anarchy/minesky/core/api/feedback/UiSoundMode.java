package anarchy.minesky.core.api.feedback;

/**
 * Som de interface reproduzido junto com uma mensagem de feedback.
 */
public enum UiSoundMode {
    /** Não reproduz som. */
    NONE,
    /** {@link FeedbackPriority#HIGH} → erro; demais → sucesso. */
    AUTO,
    /** block.note_block.didgeridoo (configurável). */
    ERROR,
    /** block.note_block.chime (configurável). */
    SUCCESS,
    /** block.note_block.bell — mensagens de tell recebidas (configurável). */
    TELL
}
