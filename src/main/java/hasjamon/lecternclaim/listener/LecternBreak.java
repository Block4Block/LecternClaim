package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class LecternBreak implements Listener {
    private final LecternClaim plugin;

    public LecternBreak(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e){
        Block b = e.getBlock();

        if (b.getType() == Material.LECTERN) {
            String chunkID = utils.getChunkID(b.getLocation());

            // If the block is in a claimed chunk
            if (plugin.cfg.getClaimData().contains(chunkID) && utils.isClaimBlock(b)) {
                Player p = e.getPlayer();

                utils.unclaimChunk(b, true, p::sendMessage);
            }
        }
    }

    // If a claim lectern is destroyed in an explosion, unclaim the chunk
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onExplosion(EntityExplodeEvent e) {
        for (Block block : e.blockList())
            if (block.getType() == Material.LECTERN && utils.isClaimBlock(block))
                utils.unclaimChunk(block, false, (msg) -> {});
    }

    // If a claim lectern is destroyed in a fire, unclaim the chunk
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent e) {
        Block block = e.getBlock();

        if (block.getType() == Material.LECTERN && utils.isClaimBlock(block))
            utils.unclaimChunk(block, false, (msg) -> {});
    }
}
