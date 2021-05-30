package hasjamon.lecternclaim.utils;

import hasjamon.lecternclaim.LecternClaim;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import java.util.*;

public class utils {
    private static final LecternClaim plugin = LecternClaim.getInstance();

    public static String chat(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String getChunkID(Chunk chunk) {
        return chunk.getWorld().getEnvironment().name() + "|" + chunk.getX() + "," + chunk.getZ();
    }

    public static String getChunkID(int blockX, int blockY, World.Environment environment) {
        return environment.name() + "|" + (blockX >> 4) + "," + (blockY >> 4);
    }

    public static String[] getMembers(Chunk chunk) {
        String members = plugin.cfg.getClaimData().getString(getChunkID(chunk) + ".members");

        if (members != null)
            return members.split("\\n");
        else
            return null;
    }

    // Check if a block is a lectern with a claim book
    public static boolean isClaimBlock(Block b) {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        String cID = getChunkID(b.getChunk());

        if (claimData.get(cID + ".location.X").equals(b.getLocation().getX()))
            if (claimData.get(cID + ".location.Y").equals(b.getLocation().getY()))
                if (claimData.get(cID + ".location.Z").equals(b.getLocation().getZ()))
                    return true;
        return false;
    }

    public static boolean claimChunk(Block block, Player p, ItemStack book) {
        BookMeta bookmeta = (BookMeta) book.getItemMeta();

        if (bookmeta != null) {
            List<String> pages = bookmeta.getPages();
            List<String> members = new ArrayList<>();

            // Collect a list of members
            for (String page : pages) {
                // If it isn't a claim page, stop looking for members
                if (!page.substring(0, 5).equalsIgnoreCase("claim"))
                    break;

                String[] lines = page.split("\\n");

                for (int i = 1; i < lines.length; i++) {
                    String member = lines[i].trim();

                    // If the member name is valid
                    if(!member.contains(" ") && !member.isEmpty() && !members.contains(member))
                        members.add(member);
                }
            }

            // If it's a valid claim book
            if(members.size() > 0) {
                // If the lectern is next to bedrock: Cancel
                for (int x = -1; x <= 1; x++) {
                    for (int y = -1; y <= 1; y++) {
                        for (int z = -1; z <= 1; z++) {
                            if (block.getRelative(x, y, z).getType() == Material.BEDROCK) {
                                p.sendMessage(utils.chat("&cYou cannot place a claim next to bedrock"));
                                return false;
                            }
                        }
                    }
                }

                FileConfiguration claimData = plugin.cfg.getClaimData();
                Location blockLoc = block.getLocation();
                String chunkID = utils.getChunkID(block.getChunk());
                String membersString = String.join("\n", members);

                claimData.set(chunkID + ".location.X", blockLoc.getX());
                claimData.set(chunkID + ".location.Y", blockLoc.getY());
                claimData.set(chunkID + ".location.Z", blockLoc.getZ());
                claimData.set(chunkID + ".members", membersString);
                plugin.cfg.saveClaimData(); // Save members to claimdata.yml

                OfflinePlayer[] knownPlayers = Bukkit.getServer().getOfflinePlayers();

                // Inform the player of the claim and its members
                p.sendMessage(utils.chat("&aThis chunk has now been claimed!"));
                p.sendMessage(utils.chat("&aMembers who can access this chunk:"));
                for (String member : members)
                    if(Arrays.stream(knownPlayers).anyMatch(kp -> kp.getName() != null && kp.getName().equalsIgnoreCase(member)))
                        p.sendMessage(ChatColor.GRAY + " - " + member);
                    else
                        p.sendMessage(ChatColor.GRAY + " - " + member + ChatColor.RED + " (unknown player)");

                updateClaimCount();
            }else{
                p.sendMessage(utils.chat("&cHINT: Add \"claim\" at the top of the first page, followed by a list members, to claim this chunk!"));
            }
        }

        return true;
    }

    public static void unclaimChunk(Player p, Block block, Boolean wasExploded) {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        String chunkID = utils.getChunkID(block.getChunk());

        claimData.set(chunkID, null);
        plugin.cfg.saveClaimData();

        if (!wasExploded)
            p.sendMessage(utils.chat("&aYou have removed this claim!"));

        updateClaimCount();
    }

    // Update tablist with current number of claims for each player
    public static void updateClaimCount() {
        HashMap<String, Integer> membersNumClaims = countMemberClaims();

        for(Player p : Bukkit.getOnlinePlayers()) {
            Integer pClaims = membersNumClaims.get(p.getName().toLowerCase());

            if(pClaims == null)
                p.setPlayerListName(p.getName() + utils.chat(" - &c0"));
            else
                p.setPlayerListName(p.getName() + utils.chat(" - &c" + pClaims));
        }
    }

    // Returns a HashMap of player name (lowercase) and number of claims
    public static HashMap<String, Integer> countMemberClaims() {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        HashMap<String, Integer> count = new HashMap<>();

        for(String key : claimData.getKeys(false)){
            ConfigurationSection chunk = claimData.getConfigurationSection(key);

            if(chunk != null){
                String currentMembers = chunk.getString("members");
                if(currentMembers != null)
                    for (String cm : currentMembers.toLowerCase().split("\\n"))
                        count.merge(cm, 1, Integer::sum);
            }
        }

        return count;
    }
}
