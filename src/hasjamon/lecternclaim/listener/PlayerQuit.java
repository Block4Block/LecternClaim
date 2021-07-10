package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.utils.utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuit implements Listener {
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e){
        Player p = e.getPlayer();
        String chunkID = utils.getChunkID(p.getLocation());

        utils.onIntruderLeaveClaim(p, chunkID);

        // Stop keeping track of the player's intruded claims
        utils.playerClaimsIntruded.remove(p);

        utils.onLoseDisguise(p);
    }
}