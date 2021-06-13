package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.utils.utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeath implements Listener {
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e){
        Player p = e.getEntity();
        String chunkID = utils.getChunkID(p.getLocation());

        utils.onIntruderLeaveClaim(p, chunkID);
    }
}
