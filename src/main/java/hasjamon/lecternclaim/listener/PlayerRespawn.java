package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.utils.utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class PlayerRespawn implements Listener {
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e){
        Player p = e.getPlayer();
        String chunkID = utils.getChunkID(e.getRespawnLocation());

        if(utils.isIntruder(p, chunkID))
            utils.onIntruderEnterClaim(p, chunkID);
    }
}
