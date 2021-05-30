package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
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

        if(b.getType() == Material.LECTERN)
            return;

        // If the block was placed in a claimed chunk
        if (plugin.cfg.getClaimData().contains(utils.getChunkID(e.getBlockPlaced().getChunk()))) {
            String[] members = utils.getMembers(b.getChunk());

            if (!utils.isClaimBlock(b)){
                // If the player placing the block is a member: Don't prevent block placement
                if (members != null)
                    for (String member : members)
                        if (member.equalsIgnoreCase(p.getName()))
                            return;

                e.setCancelled(true);
                p.sendMessage(utils.chat("&cYou cannot place blocks in this claim"));
            }
        }
    }

    @EventHandler
    public void onEmpty(PlayerBucketEmptyEvent e){
        Block b = e.getBlock();
        Player p = e.getPlayer();
        Material bucket = e.getBucket();

        if(plugin.cfg.getClaimData().contains(utils.getChunkID(b.getChunk()))){
            String[] members = utils.getMembers(b.getChunk());
            if (members != null) {
                for (String member : members)
                    if (member.equalsIgnoreCase(p.getName()))
                        if (bucket.toString().contains("LAVA") || bucket.toString().contains("WATER"))
                            return;

                p.sendMessage(utils.chat("&cYou cannot place Lava/Water inside this claim"));
                e.setCancelled(true);
            }
        }
    }
}

