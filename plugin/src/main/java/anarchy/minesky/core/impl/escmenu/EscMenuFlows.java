package anarchy.minesky.core.impl.escmenu;

import net.kyori.adventure.text.Component;
import anarchy.minesky.core.api.text.DialogComponentUtils;

import java.util.List;

public record EscMenuFlows(
        int selectorPageSize,
        int selectorButtonWidth,
        String tpaCommandTo,
        String tpaCommandHere,
        double payMin,
        double payMax,
        List<PayPreset> payPresets,
        String payInputKey,
        String payFallbackCommand) {

    public record PayPreset(double amount, Component label) {}

    public static EscMenuFlows defaults() {
        return new EscMenuFlows(
                24,
                160,
                "tpa",
                "tpaqui",
                0.01,
                1_000_000,
                List.of(
                        preset(10, "&a$10"),
                        preset(100, "&a$100"),
                        preset(1000, "&a$1.000"),
                        preset(10000, "&a$10.000")),
                "valor_pix",
                "pay");
    }

    private static PayPreset preset(double amount, String label) {
        return new PayPreset(amount, DialogComponentUtils.parse(label));
    }
}