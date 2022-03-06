package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.World;
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
        World.Environment environment = e.getEntity().getWorld().getEnvironment();
        String dimension = switch(environment){
            case NORMAL -> "overworld";
            case NETHER -> "nether";
            case THE_END -> "end";
            case CUSTOM -> "custom";
        };

        List<?> claimImmunity = plugin.getConfig().getList("claim-explosion-immunity." + dimension);

        if(claimImmunity != null && claimImmunity.contains(e.getEntityType().toString())){
            FileConfiguration claimData = plugin.cfg.getClaimData();

            e.blockList().removeIf(b -> claimData.contains(utils.getClaimID(b.getLocation())));
        }
    }
}
