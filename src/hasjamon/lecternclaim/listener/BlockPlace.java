package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class BlockPlace implements Listener {
    private final LecternClaim plugin;

    public BlockPlace(LecternClaim plugin){
        this.plugin = plugin;
    }

    // Prevent blocks from being placed in someone else's claim
    @EventHandler
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        if (p.getGameMode() == GameMode.CREATIVE) return;

        if(b.getType() == Material.LECTERN)
            return;

        // If the block was placed in a claimed chunk
        if (plugin.cfg.getClaimData().contains(utils.getChunkID(e.getBlockPlaced().getLocation()))) {
            String[] members = utils.getMembers(b.getLocation());

            if (!utils.isClaimBlock(b)){
                // If the player placing the block is a member: Don't prevent block placement
                if (members != null)
                    if(utils.isMemberOfClaim(members, p))
                        return;

                e.setCancelled(true);
                p.sendMessage(utils.chat("&cYou cannot place blocks in this claim"));
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e){
        Block b = e.getBlock();
        Player p = e.getPlayer();
        String chunkID = utils.getChunkID(b.getLocation());

        if(plugin.cfg.getClaimData().contains(chunkID)){
            String[] members = utils.getMembers(b.getLocation());

            if (members != null) {
                if(utils.isMemberOfClaim(members, p))
                    return;

                p.sendMessage(utils.chat("&cYou cannot empty buckets in this claim"));
                e.setCancelled(true);
            }
        }
    }
}

