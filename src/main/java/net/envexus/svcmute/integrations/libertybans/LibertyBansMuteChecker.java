package net.envexus.svcmute.integrations.libertybans;

import net.envexus.svcmute.integrations.MuteChecker;
import org.bukkit.entity.Player;
import space.arim.libertybans.api.LibertyBans;
import space.arim.libertybans.api.PunishmentType;
import space.arim.libertybans.api.punish.Punishment;
import space.arim.omnibus.util.concurrent.ReactionStage;

import java.net.InetAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class LibertyBansMuteChecker implements MuteChecker {
    private final LibertyBans libertyBans;

    public LibertyBansMuteChecker(LibertyBans libertyBans) {
        this.libertyBans = libertyBans;
    }

    @Override
    public boolean isPlayerMuted(Player player) {
        if (!player.isOnline()) {
            return true;
        }

        InetAddress address = Objects.requireNonNull(player.getAddress()).getAddress();
        ReactionStage<Optional<Punishment>> punishmentsFuture = libertyBans.getSelector().selectionByApplicabilityBuilder(player.getUniqueId(), address).type(PunishmentType.MUTE).selectActiveOnly().build().getFirstSpecificPunishment();

        try {
            Optional<Punishment> punishment = punishmentsFuture.toCompletableFuture().get();

            if (punishment.isPresent()) {
                return true;
            }
        } catch (Exception e) {
            return false;
        }

        return false;
    }
}
