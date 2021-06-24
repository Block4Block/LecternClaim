package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import java.util.List;

public class EntityExplode implements Listener {
    private final LecternClaim plugin;

    public EntityExplode(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onTNTExplode(EntityExplodeEvent e){
        List<?> claimImmunity = plugin.getConfig().getList("claim-explosion-immunity");

        if(claimImmunity != null && claimImmunity.contains(e.getEntityType().toString())){
            FileConfiguration claimData = plugin.cfg.getClaimData();

            e.blockList().removeIf(b -> claimData.contains(utils.getChunkID(b.getLocation())));
        }
    }
}
