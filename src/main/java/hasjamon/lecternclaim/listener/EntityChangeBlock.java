package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;

import java.util.List;

public class EntityChangeBlock implements Listener {
    private final LecternClaim plugin;

    public EntityChangeBlock(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onEntityChangeBlock(EntityChangeBlockEvent e){
        if(e.getEntityType() == EntityType.ENDERMAN){
            Block b = e.getBlock();
            List<String> endermanBlacklist = plugin.getConfig().getStringList("enderman-place-in-claim-blacklist");

            if (endermanBlacklist.contains(e.getTo().toString())) {
                FileConfiguration claimData = plugin.cfg.getClaimData();
                String claimID = utils.getClaimID(b.getLocation());

                if(claimData.contains(claimID)) {
                    e.setCancelled(true);
                }
            }
        }
    }
}
