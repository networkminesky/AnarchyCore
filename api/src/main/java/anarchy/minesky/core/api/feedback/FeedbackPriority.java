package anarchy.minesky.core.api.feedback;

/**
 * Prioridade de uma mensagem de feedback (action bar).
 * <ul>
 *   <li>{@link #LOW} — entra no fim da fila.</li>
 *   <li>{@link #NORMAL} — aguarda a mensagem atual terminar; entra na fila em ordem.</li>
 *   <li>{@link #HIGH} — interrompe a exibição atual e substitui pela nova imediatamente.</li>
 * </ul>
 */
public enum FeedbackPriority {
    LOW,
    NORMAL,
    HIGH
}
