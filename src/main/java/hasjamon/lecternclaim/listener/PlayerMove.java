package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.GameMode;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerMove implements Listener {
    private final LecternClaim plugin;

    public PlayerMove(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e){
        Player p = e.getPlayer();

        if(p.getGameMode() == GameMode.SURVIVAL && e.getTo() != null) {
            String prevChunkID = utils.getChunkID(e.getFrom());
            String currentChunkID = utils.getChunkID(e.getTo());

            // If p has entered a new chunk
            if (!prevChunkID.equals(currentChunkID)) {
                String prevClaimID = utils.getClaimID(e.getFrom());
                String currentClaimID = utils.getClaimID(e.getTo());

                // If p has entered a new claim
                if (!prevClaimID.equals(currentClaimID)) {
                    // Remove p from the previous chunk's intruder list
                    utils.onIntruderLeaveClaim(p, prevClaimID);

                    if (utils.isIntruder(p, currentClaimID)) {
                        utils.onIntruderEnterClaim(p, currentClaimID);
                    }
                }

                // If p is currently an intruder
                if (utils.isIntruder(p, currentClaimID)) {
                    // Make all iron golems in chunk hostile to the intruder
                    if(plugin.getConfig().getBoolean("golems-guard-claims"))
                        for(IronGolem golem : utils.ironGolems.keySet())
                            if(currentChunkID.equals(utils.getChunkID(golem.getLocation())))
                                golem.damage(0, p);
                }
            }

            utils.lastPlayerMoves.put(p, System.nanoTime());
        }
    }
}
