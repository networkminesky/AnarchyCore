package anarchy.minesky.core.impl.kits;

public final class KitUiSlots {

    public static final int INVENTORY_SIZE = 54;
    public static final int CONTENT_SLOTS = KitDefinition.CONTENT_SLOT_COUNT;

    public static final int COOLDOWN_MINUS_6H = 45;
    public static final int COOLDOWN_MINUS_1H = 46;
    public static final int COOLDOWN_MINUS_1M = 47;
    public static final int COOLDOWN_INFO = 49;
    public static final int COOLDOWN_PLUS_1M = 50;
    public static final int COOLDOWN_PLUS_1H = 51;
    public static final int COOLDOWN_PLUS_6H = 52;

    /** Centro da última linha — resgatar / aguardar cooldown (preview). */
    public static final int ACTION_PRIMARY = 49;

    /** Salvar conteúdo (admin), à direita da linha de controle. */
    public static final int ACTION_SAVE = 53;

    private static final int[] ADMIN_CONTROL_SLOTS = {
            COOLDOWN_MINUS_6H,
            COOLDOWN_MINUS_1H,
            COOLDOWN_MINUS_1M,
            COOLDOWN_INFO,
            COOLDOWN_PLUS_1M,
            COOLDOWN_PLUS_1H,
            COOLDOWN_PLUS_6H,
            ACTION_SAVE,
    };

    public static boolean isAdminControlSlot(int slot) {
        for (int control : ADMIN_CONTROL_SLOTS) {
            if (control == slot) {
                return true;
            }
        }
        return false;
    }

    private KitUiSlots() {}
}
