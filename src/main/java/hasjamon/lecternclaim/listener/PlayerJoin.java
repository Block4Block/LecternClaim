package hasjamon.lecternclaim.listener;

import com.mojang.authlib.properties.Property;
import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class PlayerJoin implements Listener {
    private final LecternClaim plugin;

    public PlayerJoin(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();

        if(!p.hasPlayedBefore()) {
            utils.knownPlayers.add(p.getName().toLowerCase());
            utils.sendWelcomeMsg(p);
        }else {
            utils.populatePlayerClaimsIntruded(p);
        }

        utils.updateClaimCount();

        String claimID = utils.getClaimID(p.getLocation());
        if(utils.isIntruder(p, claimID))
            utils.onIntruderEnterClaim(p, claimID);


        String pName = p.getName().toLowerCase();
        FileConfiguration offlineClaimNotifications = plugin.cfg.getOfflineClaimNotifications();
        ConfigurationSection claimsLost = offlineClaimNotifications.getConfigurationSection(pName + ".chunks");
        ConfigurationSection masterBooksRemovedFrom = offlineClaimNotifications.getConfigurationSection(pName + ".masterbooks");

        if(claimsLost != null){
            Set<String> claimIDs = claimsLost.getKeys(false);
            int i = 0;

            for(String cID : claimIDs){
                if(++i >= 10 && claimIDs.size() > 10){
                    p.sendMessage(ChatColor.RED + "... and " + (claimIDs.size() - 9) + " other claims");
                    break;
                }

                String xyz = claimsLost.getString(cID);
                String worldName = utils.getWorldName(World.Environment.valueOf(claimID.split("\\|")[0]));
                p.sendMessage(ChatColor.RED + "You have lost a claim! Location: " + xyz + " in " + worldName);
            }
        }

        if(masterBooksRemovedFrom != null){
            for(String mbID : masterBooksRemovedFrom.getKeys(false))
                if(masterBooksRemovedFrom.getBoolean(mbID))
                    p.sendMessage(ChatColor.RED + "Your name has been removed from Master Book #" + mbID + " and all related claims!");
        }

        offlineClaimNotifications.set(pName, null);
        plugin.cfg.saveOfflineClaimNotifications();

        Collection<Property> textures = utils.getTextures(p);

        if(textures != null){
            Property prop = textures.iterator().next();
            List<String> copy = new ArrayList<>();

            copy.add(prop.getName());
            copy.add(prop.getValue());
            if(prop.hasSignature())
                copy.add(prop.getSignature());

            plugin.cfg.getPlayerTextures().set(p.getUniqueId().toString(), copy);
            plugin.cfg.savePlayerTextures();
        }
    }
}
