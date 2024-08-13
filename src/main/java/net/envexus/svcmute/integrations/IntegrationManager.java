package net.envexus.svcmute.integrations;

import net.envexus.svcmute.integrations.advancedbans.AdvancedBansMuteChecker;
import net.envexus.svcmute.integrations.essentials.EssentialsMuteChecker;
import net.envexus.svcmute.integrations.libertybans.LibertyBansMuteChecker;
import net.envexus.svcmute.integrations.litebans.LiteBansMuteChecker;
import net.envexus.svcmute.integrations.svcmute.SQLiteMuteChecker;
import net.envexus.svcmute.util.SQLiteHelper;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import space.arim.libertybans.api.LibertyBans;
import space.arim.omnibus.Omnibus;
import space.arim.omnibus.OmnibusProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IntegrationManager {
    private final List<MuteChecker> muteCheckers = new ArrayList<>();
    private final List<MutedPlayer> mutedPlayers = new ArrayList<>(); // List to track muted players
    private final SQLiteHelper sqliteHelper;
    private final Logger logger;

    public IntegrationManager(SQLiteHelper sqliteHelper, Logger logger) {
        this.sqliteHelper = sqliteHelper;
        this.logger = logger;
        registerPlugins();
    }

    /**
     * Register all supported mute-management plugins.
     */
    private void registerPlugins() {
        Plugin liteBansPlugin = Bukkit.getPluginManager().getPlugin("LiteBans");
        boolean isLiteBansEnabled = liteBansPlugin != null && liteBansPlugin.isEnabled();

        Plugin advancedBansPlugin = Bukkit.getPluginManager().getPlugin("AdvancedBan");
        boolean isAdvancedBanEnabled = advancedBansPlugin != null && advancedBansPlugin.isEnabled();

        Plugin libertyBansPlugin = Bukkit.getPluginManager().getPlugin("LibertyBans");
        Omnibus libertyBansOmnibus = OmnibusProvider.getOmnibus();
        LibertyBans libertyBansInstance = libertyBansOmnibus.getRegistry().getProvider(LibertyBans.class).orElse(null);
        boolean isLibertyBansEnabled = libertyBansPlugin != null && libertyBansPlugin.isEnabled() && libertyBansInstance != null;

        if (!isLiteBansEnabled && !isAdvancedBanEnabled && !isLibertyBansEnabled) {
            Plugin essentialsPlugin = Bukkit.getPluginManager().getPlugin("Essentials");
            if (essentialsPlugin != null && essentialsPlugin.isEnabled()) {
                muteCheckers.add(new EssentialsMuteChecker(essentialsPlugin));
                logger.log(Level.INFO, "EssentialsMuteChecker loaded successfully.");
            }
        }

        if (isLiteBansEnabled) {
            muteCheckers.add(new LiteBansMuteChecker());
            logger.log(Level.INFO, "LiteBansMuteChecker loaded successfully.");
        }

        if (isAdvancedBanEnabled) {
            muteCheckers.add(new AdvancedBansMuteChecker(advancedBansPlugin));
            logger.log(Level.INFO, "AdvancedBansMuteChecker loaded successfully.");
        }

        if (isLibertyBansEnabled) {
            muteCheckers.add(new LibertyBansMuteChecker(libertyBansInstance));
            logger.log(Level.INFO, "LibertyBansMuteChecker loaded successfully.");
        }

        muteCheckers.add(new SQLiteMuteChecker(sqliteHelper));
    }

    /**
     * Check if the player is muted.
     *
     * @param player the player to check
     * @return true if the player is muted, false otherwise
     */
    public boolean isPlayerMuted(Player player) {
        UUID playerUUID = player.getUniqueId();
        // Check the list of manually muted players
        for (MutedPlayer mutedPlayer : mutedPlayers) {
            if (mutedPlayer.getPlayerUUID().equals(playerUUID) && mutedPlayer.getUnmuteTime() > System.currentTimeMillis()) {
                return true;
            }
        }
        // Also check through all registered mute checkers
        for (MuteChecker checker : muteCheckers) {
            if (checker.isPlayerMuted(player)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a player to the list of muted players.
     *
     * @param playerUUID the UUID of the player
     * @param unmuteTime the time when the player should be unmuted
     */
    public void addMutedPlayer(UUID playerUUID, long unmuteTime) {
        mutedPlayers.add(new MutedPlayer(playerUUID, unmuteTime));
    }

    /**
     * Remove a player from the list of muted players.
     *
     * @param playerUUID the UUID of the player
     */
    public void removeMutedPlayer(UUID playerUUID) {
        mutedPlayers.removeIf(mutedPlayer -> mutedPlayer.getPlayerUUID().equals(playerUUID));
    }

    // Inner class to represent a muted player
    private static class MutedPlayer {
        private final UUID playerUUID;
        private final long unmuteTime;

        public MutedPlayer(UUID playerUUID, long unmuteTime) {
            this.playerUUID = playerUUID;
            this.unmuteTime = unmuteTime;
        }

        public UUID getPlayerUUID() {
            return playerUUID;
        }

        public long getUnmuteTime() {
            return unmuteTime;
        }
    }
}
