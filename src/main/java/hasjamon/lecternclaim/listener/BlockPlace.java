package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;

public class BlockPlace implements Listener {
    private final LecternClaim plugin;

    public BlockPlace(LecternClaim plugin){
        this.plugin = plugin;
    }

    // Prevent blocks from being placed in someone else's claim
    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlace(BlockPlaceEvent e) {
        Block b = e.getBlock();
        Player p = e.getPlayer();

        if (p.getGameMode() == GameMode.CREATIVE) return;

        if(b.getType() == Material.LECTERN)
            return;

        // If the block was placed in a claimed chunk
        if (plugin.cfg.getClaimData().contains(utils.getClaimID(e.getBlockPlaced().getLocation()))) {
            String[] members = utils.getMembers(b.getLocation());

            // If the block isn't the lectern claiming the chunk and the player isn't placing an easily breakable crop
            if (!utils.isClaimBlock(b) && !isEasilyBreakableCrop(b.getType())){
                // If the player placing the block isn't a member: Prevent block placement
                if (members == null || !utils.isMemberOfClaim(members, p)) {
                    e.setCancelled(true);
                    p.sendMessage(utils.chat("&cYou cannot place blocks in this claim"));
                }
            }
        }
    }

    @EventHandler
    public void onBucketEmpty(PlayerBucketEmptyEvent e){
        Block b = e.getBlock();
        Player p = e.getPlayer();
        String claimID = utils.getClaimID(b.getLocation());

        if(plugin.cfg.getClaimData().contains(claimID)){
            String[] members = utils.getMembers(b.getLocation());

            if (members != null) {
                if(!utils.isMemberOfClaim(members, p)) {
                    e.setCancelled(true);
                    p.sendMessage(utils.chat("&cYou cannot empty buckets in this claim"));
                }
            }
        }
    }

    private boolean isEasilyBreakableCrop(Material blockType) {
        switch (blockType){
            case WHEAT, POTATOES, CARROTS, BEETROOTS, SUGAR_CANE, COCOA, NETHER_WART -> {
                return true;
            }
            default -> {
                return false;
            }
        }
    }
}

