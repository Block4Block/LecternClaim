package hasjamon.lecternclaim.utils;

import hasjamon.lecternclaim.LecternClaim;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;

import java.util.*;
import java.util.function.Consumer;

public class utils {
    private static final LecternClaim plugin = LecternClaim.getInstance();
    public static final Map<String, Set<Player>> intruders = new HashMap<>();
    public static final Map<IronGolem, String> ironGolems = new HashMap<>();
    public static final Map<Player, Set<String>> playerClaimsIntruded = new HashMap<>();
    public static final Map<Player, Long> lastIntrusionMessageReceived = new HashMap<>();
    public static int minSecBetweenAlerts;

    public static String chat(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String getChunkID(int blockX, int blockZ, World.Environment environment) {
        return environment.name() + "|" + (blockX >> 4) + "," + (blockZ >> 4);
    }

    public static String getChunkID(Location loc) {
        return getChunkID(loc.getBlockX(), loc.getBlockZ(), loc.getWorld().getEnvironment());
    }

    public static String[] getMembers(Location loc) {
        return getMembers(getChunkID(loc));
    }

    public static String[] getMembers(String chunkID) {
        String members = plugin.cfg.getClaimData().getString(chunkID + ".members");

        if (members != null)
            return members.split("\\n");
        else
            return null;
    }

    // Check if a block is a lectern with a claim book
    public static boolean isClaimBlock(Block b) {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        String cID = getChunkID(b.getLocation());

        if (claimData.get(cID + ".location.X").equals(b.getLocation().getX()))
            if (claimData.get(cID + ".location.Y").equals(b.getLocation().getY()))
                if (claimData.get(cID + ".location.Z").equals(b.getLocation().getZ()))
                    return true;
        return false;
    }

    public static boolean claimChunk(Block block, BookMeta meta, Consumer<String> sendMessage) {
        if (meta != null) {
            List<String> members = findMembersInBook(meta);

            // If it's a valid claim book
            if(members.size() > 0) {
                // If the lectern is next to bedrock: Cancel
                if(isNextToBedrock(block)){
                    sendMessage.accept(utils.chat("&cYou cannot place a claim next to bedrock"));
                    return false;
                }

                String membersString = String.join("\n", members);

                setChunkClaim(block, membersString);
                plugin.cfg.saveClaimData(); // Save members to claimdata.yml

                OfflinePlayer[] knownPlayers = Bukkit.getServer().getOfflinePlayers();

                // Inform the player of the claim and its members
                sendMessage.accept(utils.chat("&eThis chunk has now been claimed!"));
                sendMessage.accept(utils.chat("&aMembers who can access this chunk:"));
                for (String member : members)
                    if(Arrays.stream(knownPlayers).anyMatch(kp -> kp.getName() != null && kp.getName().equalsIgnoreCase(member)))
                        sendMessage.accept(ChatColor.GRAY + " - " + member);
                    else
                        sendMessage.accept(ChatColor.GRAY + " - " + member + ChatColor.RED + " (unknown player)");

                updateClaimCount();

            }else{
                sendMessage.accept(utils.chat("&cHINT: Add \"claim\" at the top of the first page, followed by a list members, to claim this chunk!"));
            }
        }

        return true;
    }

    public static boolean claimChunkBulk(Set<Block> blocks, BookMeta meta) {
        if (meta != null) {
            List<String> members = findMembersInBook(meta);

            // If it's a valid claim book
            if(members.size() > 0) {
                for (Block block : blocks) {
                    // If the lectern is next to bedrock: Cancel
                    if (isNextToBedrock(block))
                        continue;

                    String membersString = String.join("\n", members);

                    setChunkClaim(block, membersString);
                }

                plugin.cfg.saveClaimData();
                updateClaimCount();
            }
        }

        return true;
    }

    private static void setChunkClaim(Block block, String membersString) {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        Location blockLoc = block.getLocation();
        String chunkID = utils.getChunkID(blockLoc);

        claimData.set(chunkID + ".location.X", blockLoc.getX());
        claimData.set(chunkID + ".location.Y", blockLoc.getY());
        claimData.set(chunkID + ".location.Z", blockLoc.getZ());
        claimData.set(chunkID + ".members", membersString);

        for (Player player : Bukkit.getOnlinePlayers())
            if (chunkID.equals(getChunkID(player.getLocation())))
                if (isIntruder(player, chunkID))
                    onIntruderEnterClaim(player, chunkID);
    }

    private static List<String> findMembersInBook(BookMeta meta) {
        List<String> pages = meta.getPages();
        List<String> members = new ArrayList<>();

        for (String page : pages) {
            // If it isn't a claim page, stop looking for members
            if (!isClaimPage(page))
                break;

            String[] lines = page.split("\\n");

            for (int i = 1; i < lines.length; i++) {
                String member = lines[i].trim();

                // If the member name is valid
                if(!member.contains(" ") && !member.isEmpty() && !members.contains(member))
                    members.add(member);
            }
        }

        return members;
    }

    private static boolean isNextToBedrock(Block block) {
        for (int x = -1; x <= 1; x++)
            for (int y = -1; y <= 1; y++)
                for (int z = -1; z <= 1; z++)
                    if (block.getRelative(x, y, z).getType() == Material.BEDROCK)
                        return true;
        return false;
    }

    public static boolean isClaimPage(String page) {
        return page.length() >= 5 && page.substring(0, 5).equalsIgnoreCase("claim");
    }

    public static void unclaimChunk(Block block, boolean causedByPlayer, Consumer<String> sendMessage) {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        String chunkID = utils.getChunkID(block.getLocation());

        claimData.set(chunkID, null);
        plugin.cfg.saveClaimData();

        if (causedByPlayer)
            sendMessage.accept(ChatColor.RED + "You have removed this claim!");

        updateClaimCount();

        if(intruders.containsKey(chunkID))
            for(Player intruder : intruders.get(chunkID))
                onIntruderLeaveClaim(intruder, chunkID);
    }

    public static void unclaimChunkBulk(Set<Block> blocks) {
        FileConfiguration claimData = plugin.cfg.getClaimData();

        for(Block b : blocks) {
            String chunkID = utils.getChunkID(b.getLocation());
            claimData.set(chunkID, null);

            if(intruders.containsKey(chunkID))
                for(Player intruder : intruders.get(chunkID))
                    onIntruderLeaveClaim(intruder, chunkID);
        }
        plugin.cfg.saveClaimData();

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

    public static void onIntruderEnterClaim(Player intruder, String chunkID) {
        if(intruder.getGameMode() != GameMode.SURVIVAL)
            return;

        if(!intruders.containsKey(chunkID))
            intruders.put(chunkID, new HashSet<>());

        intruders.get(chunkID).add(intruder);

        // Make all iron golems in chunk hostile to the intruder
        if(plugin.getConfig().getBoolean("golems-guard-claims", true))
            for(IronGolem golem : ironGolems.keySet())
                if(chunkID.equals(getChunkID(golem.getLocation())))
                    golem.damage(0, intruder);

        String[] members = getMembers(chunkID);

        if(members != null) {
            for (String m : members) {
                Player p = Bukkit.getPlayerExact(m);

                if (p != null) {
                    FileConfiguration claimData = plugin.cfg.getClaimData();
                    long now = System.nanoTime();
                    double x = claimData.getDouble(chunkID + ".location.X");
                    double y = claimData.getDouble(chunkID + ".location.Y");
                    double z = claimData.getDouble(chunkID + ".location.Z");

                    if (!playerClaimsIntruded.containsKey(p))
                        playerClaimsIntruded.put(p, new HashSet<>());
                    playerClaimsIntruded.get(p).add(chunkID);

                    if(now - lastIntrusionMessageReceived.getOrDefault(p, 0L) >= minSecBetweenAlerts * 1e9){
                        p.sendMessage(ChatColor.RED + "An intruder has entered your claim at "+x+", "+y+", "+z);
                        lastIntrusionMessageReceived.put(p, now);
                    }
                }
            }
        }
    }

    public static void onIntruderLeaveClaim(Player intruder, String chunkID) {
        if(intruders.containsKey(chunkID))
            intruders.get(chunkID).remove(intruder);
    }

    public static boolean isIntruder(Player p, String chunkID){
        String[] members = utils.getMembers(chunkID);

        // If the chunk isn't claimed; else if p is a member
        if (members == null)
            return false;
        else
            for (String member : members)
                if (member.equalsIgnoreCase(p.getName()))
                    return false;

        return true;
    }

    public static void updateGolemHostility(){
        for(Map.Entry<IronGolem, String> entry : ironGolems.entrySet()){
            IronGolem golem = entry.getKey();
            String currentChunkID = getChunkID(golem.getLocation());
            String prevChunkID = entry.getValue();

            if(!currentChunkID.equals(prevChunkID)){
                entry.setValue(currentChunkID);

                // Make it hostile to all intruders in chunk
                if(utils.intruders.containsKey(currentChunkID))
                    for(Player intruder : utils.intruders.get(currentChunkID))
                        golem.damage(0, intruder);
            }
        }
    }

    public static void populatePlayerClaimsIntruded(Player p){
        // Go through all intruded claims
        for(String chunkID : intruders.keySet()){
            String[] members = getMembers(chunkID);

            if(members != null) {
                for (String m : members) {
                    // If p is a member
                    if (m.equalsIgnoreCase(p.getName())) {
                        if (!playerClaimsIntruded.containsKey(p))
                            playerClaimsIntruded.put(p, new HashSet<>());

                        // Add the chunk as one of p's intruded claims
                        playerClaimsIntruded.get(p).add(chunkID);
                        break;
                    }
                }
            }
        }
    }
}
