package anarchy.minesky.core.command;

import anarchy.minesky.core.impl.referral.ReferralManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public final class ReferralAdminHandler {

  public static final String PERMISSION = ReferralManager.ADMIN_PERMISSION;

    private final ReferralManager referral;

    public ReferralAdminHandler(ReferralManager referral) {
        this.referral = referral;
    }

    public boolean handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("Apenas jogadores.");
            return true;
        }
        if (args.length < 2) {
            return false;
        }
        String action = args[1].toLowerCase(Locale.ROOT);
        return switch (action) {
            case "inspect", "info" -> {
                if (args.length < 3) {
                    yield false;
                }
                referral.adminInspect(admin, args[2]);
                yield true;
            }
            case "block", "ban" -> {
                if (args.length < 3) {
                    yield false;
                }
                String reason = args.length >= 4 ? String.join(" ", java.util.Arrays.copyOfRange(args, 3, args.length))
                        : "admin";
                referral.adminBlock(admin, args[2], reason);
                yield true;
            }
            case "reprocess", "payout" -> {
                if (args.length < 3) {
                    yield false;
                }
                referral.adminReprocess(admin, args[2]);
                yield true;
            }
            case "reload" -> {
                referral.adminReload(admin);
                yield true;
            }
            default -> false;
        };
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return Stream.of("inspect", "block", "reprocess", "reload")
                    .filter(s -> s.startsWith(prefix))
                    .toList();
        }
        if (args.length == 3) {
            return new ArrayList<>();
        }
        return List.of();
    }
}