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
        String claimID = utils.getClaimID(e.getRespawnLocation());

        if(utils.isIntruder(p, claimID))
            utils.onIntruderEnterClaim(p, claimID);
    }
}
