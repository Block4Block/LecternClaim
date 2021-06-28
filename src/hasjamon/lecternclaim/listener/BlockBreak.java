package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;

public class BlockBreak implements Listener {
    private final LecternClaim plugin;
    private long andesiteLatestBreak = 0;

    public BlockBreak(LecternClaim plugin){
        this.plugin = plugin;
    }

    // This Class is for the block break event (This runs every time a player breaks a block)
    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        FileConfiguration cfg = plugin.getConfig();

        // Lecterns can always be broken, even if they're in a claim
        if (b.getType() == Material.LECTERN) return;
        if (p.getGameMode() == GameMode.CREATIVE) return;

        // If the block is in a claimed chunk
        if (plugin.cfg.getClaimData().contains(utils.getChunkID(b.getLocation()))) {
            if (!utils.isClaimBlock(b)) {
                String[] members = utils.getMembers(b.getLocation());

                if (members != null) {
                    boolean isMember = false;

                    for (String member : members) {
                        if (member.equalsIgnoreCase(p.getName())) {
                            isMember = true;
                            break;
                        }
                    }

                    // If the chunk is claimed, the player isn't a member, and 'can-break-in-others-claims' isn't on
                    if (!isMember && !cfg.getBoolean("can-break-in-others-claims")) {
                        // Cancel BlockBreakEvent, i.e., prevent block from breaking
                        e.setCancelled(true);
                        p.sendMessage(utils.chat("&cYou cannot break blocks in this claim"));
                        return;
                    }
                }
            }
        }

        if(plugin.getConfig().getBoolean("andesite-splash-on")) {
            if (b.getType() == Material.ANDESITE) {
                // Add splash if it's been at least 0.1 second since the last time andesite was broken (to avoid chain reaction)
                if(System.nanoTime() - andesiteLatestBreak > 1E8) {
                    andesiteLatestBreak = System.nanoTime();
                    for (int x = -1; x <= 1; x++)
                        for (int y = -1; y <= 1; y++)
                            for (int z = -1; z <= 1; z++)
                                if (b.getRelative(x, y, z).getType() == Material.ANDESITE)
                                    if(plugin.getConfig().getBoolean("andesite-splash-reduce-durability"))
                                        p.breakBlock(b.getRelative(x, y, z));
                                    else
                                        b.getRelative(x, y, z).breakNaturally(p.getInventory().getItemInMainHand());
                }
            }
        }
    }

    @EventHandler
    public void onBucketFill(PlayerBucketFillEvent e) {
        Player p = e.getPlayer();
        Block b = e.getBlock();
        String chunkID = utils.getChunkID(b.getLocation());

        // Allow milking
        if(b.getType() == Material.AIR)
            return;

        // Disallow filling buckets with anything other than milk
        if (plugin.cfg.getClaimData().contains(chunkID)) {
            String[] members = utils.getMembers(b.getLocation());

            if (members != null) {
                for (String member : members)
                    if (member.equalsIgnoreCase(p.getName()))
                        return;

                p.sendMessage(utils.chat("&cYou cannot fill buckets in this claim"));
                e.setCancelled(true);
            }
        }
    }
}
