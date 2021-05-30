package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

public class LecternBreak implements Listener {
    private final LecternClaim plugin;

    public LecternBreak(LecternClaim plugin){
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBreak(BlockBreakEvent e){
        if(!e.isCancelled()) {
            Block b = e.getBlock();
            String chunkID = utils.getChunkID(b.getChunk());

            // If the block is in a claimed chunk
            if (plugin.cfg.getClaimData().contains(chunkID)) {
                Player p = e.getPlayer();

                if (b.getType() == Material.LECTERN)
                    if (utils.isClaimBlock(b))
                        utils.unclaimChunk(p, b, false);
            }
        }
    }

    // If a lectern is destroyed in an explosion: Inform the members of the claim
    @EventHandler
    public void onExplosion(EntityExplodeEvent e) {
        for (Block block : e.blockList()) {
            if (block.getType().equals(Material.LECTERN) && utils.isClaimBlock(block)) {
                utils.unclaimChunk(null, block, true);
                String[] members = utils.getMembers(block.getChunk());
                for (Player p : Bukkit.getOnlinePlayers())
                    if (members != null)
                        for (String member : members)
                            if (member.equalsIgnoreCase(p.getName()))
                                p.sendMessage(utils.chat("&cYour claim has been destroyed!"));
            }
        }
    }
}
