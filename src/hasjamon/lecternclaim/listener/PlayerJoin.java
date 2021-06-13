package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;

public class PlayerJoin implements Listener {
    private final LecternClaim plugin;

    public PlayerJoin(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        List<String> welcomeMessages = plugin.getConfig().getStringList("welcome-messages");

        if(!p.hasPlayedBefore())
            for(String msg : welcomeMessages)
                p.sendMessage(utils.chat(msg));
        else
            utils.populatePlayerClaimsIntruded(p);

        utils.updateClaimCount();

        String chunkID = utils.getChunkID(p.getLocation());
        if(utils.isIntruder(p, chunkID))
            utils.onIntruderEnterClaim(p, chunkID);
    }
}
