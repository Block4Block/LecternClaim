package hasjamon.lecternclaim.utils;

import com.comphenix.protocol.utility.MinecraftReflection;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import hasjamon.lecternclaim.LecternClaim;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.libs.org.eclipse.sisu.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.scheduler.BukkitTask;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;

public class utils {
    private static final LecternClaim plugin = LecternClaim.getInstance();
    public static final Map<String, Set<Player>> intruders = new HashMap<>();
    public static final Map<IronGolem, String> ironGolems = new HashMap<>();
    public static final Map<Player, Set<String>> playerClaimsIntruded = new HashMap<>();
    public static final Map<Player, Long> lastIntrusionMsgReceived = new HashMap<>();
    public static final Map<Player, BukkitTask> undisguiseTasks = new HashMap<>();
    public static final Map<Player, String> activeDisguises = new HashMap<>();
    public static int minSecBetweenAlerts;
    private static boolean masterBookChangeMsgSent = false;
    public static boolean isPaperServer = true;

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
        double lecternX = claimData.getDouble(cID + ".location.X", Double.MAX_VALUE);
        double lecternY = claimData.getDouble(cID + ".location.Y", Double.MAX_VALUE);
        double lecternZ = claimData.getDouble(cID + ".location.Z", Double.MAX_VALUE);

        if(lecternX == Double.MAX_VALUE || lecternY == Double.MAX_VALUE || lecternZ == Double.MAX_VALUE)
            return false;

        return lecternX == b.getLocation().getX() && lecternY == b.getLocation().getY() && lecternZ == b.getLocation().getZ();
    }

    public static boolean claimChunk(Block block, BookMeta meta, Consumer<String> sendMessage) {
        if (meta != null) {
            List<String> members = findMembersInBook(meta);

            // If it's a valid claim book
            if(members.size() > 0) {
                // If the lectern is next to bedrock: Cancel
                if(isNextToBedrock(block)){
                    sendMessage.accept(chat("&cYou cannot place a claim next to bedrock"));
                    return false;
                }

                setChunkClaim(block, members, sendMessage, null);
                updateClaimCount();

            }else{
                sendMessage.accept(chat("&cHINT: Add \"claim\" at the top of the first page, followed by a list members, to claim this chunk!"));
            }
        }

        return true;
    }

    public static void claimChunkBulk(Set<Block> blocks, BookMeta meta, String masterBookID) {
        if (meta != null) {
            List<String> members = findMembersInBook(meta);

            // If it's a valid claim book
            if(members.size() > 0) {
                for (Block block : blocks) {
                    // If the lectern is next to bedrock: Cancel
                    if (isNextToBedrock(block))
                        continue;

                    setChunkClaim(block, members, masterBookID);
                }

                updateClaimCount();
            }
        }
    }

    private static void setChunkClaim(Block block, List<String> members, String masterBookID){
        setChunkClaim(block, members, null, masterBookID);
    }

    private static void setChunkClaim(Block block, List<String> members, @Nullable Consumer<String> sendMessage, String masterBookID) {
        FileConfiguration claimData = plugin.cfg.getClaimData();
        Location blockLoc = block.getLocation();
        String chunkID = getChunkID(blockLoc);
        String membersString = String.join("\n", members);

        claimData.set(chunkID + ".location.X", blockLoc.getX());
        claimData.set(chunkID + ".location.Y", blockLoc.getY());
        claimData.set(chunkID + ".location.Z", blockLoc.getZ());
        claimData.set(chunkID + ".members", membersString);
        plugin.cfg.saveClaimData();

        onChunkClaim(chunkID, members, sendMessage, masterBookID);
    }

    public static void onChunkClaim(String chunkID, List<String> members, @Nullable Consumer<String> sendMessage, String masterBookID){
        if(sendMessage == null)
            sendMessage = (msg) -> {};
        OfflinePlayer[] knownPlayers = Bukkit.getOfflinePlayers();
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        // Inform the player of the claim and its members
        sendMessage.accept(chat("&eThis chunk has now been claimed!"));
        sendMessage.accept(chat("&aMembers who can access this chunk:"));
        for (String member : members) {
            Optional<OfflinePlayer> offlinePlayer = Arrays.stream(knownPlayers).filter(kp -> kp.getName() != null && kp.getName().equalsIgnoreCase(member)).findFirst();

            if (offlinePlayer.isPresent()) {
                sendMessage.accept(ChatColor.GRAY + " - " + member);

                boolean isOffline = onlinePlayers.stream().noneMatch(op -> op.getName().equalsIgnoreCase(member));

                if(isOffline){
                    String name = offlinePlayer.get().getName();
                    FileConfiguration offlineClaimNotifications = plugin.cfg.getOfflineClaimNotifications();

                    if(masterBookID != null)
                        offlineClaimNotifications.set(name + ".masterbooks." + masterBookID, false);
                    else
                        offlineClaimNotifications.set(name + ".chunks." + chunkID, null);
                    plugin.cfg.saveOfflineClaimNotifications();
                }
            } else {
                sendMessage.accept(ChatColor.GRAY + " - " + member + ChatColor.RED + " (unknown player)");
            }
        }

        for (Player player : onlinePlayers)
            if (chunkID.equals(getChunkID(player.getLocation())))
                if (isIntruder(player, chunkID))
                    onIntruderEnterClaim(player, chunkID);
    }

    public static void onChunkUnclaim(String chunkID, String[] members, Location lecternLoc, String masterBookID){
        String xyz = lecternLoc.getBlockX() +", "+ lecternLoc.getBlockY() +", "+ lecternLoc.getBlockZ();

        onChunkUnclaim(chunkID, members, xyz, masterBookID);
    }

    public static void onChunkUnclaim(String chunkID, String[] members, String lecternXYZ, String masterBookID){
        OfflinePlayer[] knownPlayers = Bukkit.getOfflinePlayers();
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();

        if(members != null) {
            for (String member : members) {
                Optional<OfflinePlayer> offlinePlayer = Arrays.stream(knownPlayers).filter(kp -> kp.getName() != null && kp.getName().equalsIgnoreCase(member)).findFirst();

                if (offlinePlayer.isPresent()) {
                    boolean isOffline = true;

                    // Notify online members that they have lost the claim
                    for (Player player : onlinePlayers) {
                        if (player.getName().equalsIgnoreCase(member)) {
                            isOffline = false;
                            if(masterBookID != null) {
                                if(!masterBookChangeMsgSent) {
                                    String msg = "Your name has been removed from Master Book #" + masterBookID + " and all related claims!";
                                    player.sendMessage(ChatColor.RED + msg);
                                    masterBookChangeMsgSent = true;
                                }
                            }else {
                                player.sendMessage(ChatColor.RED + "You have lost a claim! Location: " + lecternXYZ);
                            }
                            break;
                        }
                    }

                    if (isOffline) {
                        String name = offlinePlayer.get().getName();
                        FileConfiguration offlineClaimNotifications = plugin.cfg.getOfflineClaimNotifications();

                        if(masterBookID != null) {
                            offlineClaimNotifications.set(name + ".masterbooks." + masterBookID, true);
                        }else {
                            offlineClaimNotifications.set(name + ".chunks." + chunkID, lecternXYZ);
                        }
                        plugin.cfg.saveOfflineClaimNotifications();
                    }
                }
            }
        }

        if(intruders.containsKey(chunkID))
            for(Player intruder : intruders.get(chunkID))
                onIntruderLeaveClaim(intruder, chunkID);
    }

    public static List<String> findMembersInBook(BookMeta meta) {
        List<String> pages = meta.getPages();

        return findMembersInBook(pages);
    }

    public static List<String> findMembersInBook(List<String> pages){
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
        Location blockLoc = block.getLocation();
        String chunkID = getChunkID(blockLoc);
        String[] members = getMembers(chunkID);

        claimData.set(chunkID, null);
        plugin.cfg.saveClaimData();

        if (causedByPlayer)
            sendMessage.accept(ChatColor.RED + "You have removed this claim!");

        onChunkUnclaim(chunkID, members, blockLoc, null);
        updateClaimCount();

        plugin.cfg.getClaimTakeovers().set(chunkID, null);
        plugin.cfg.saveClaimTakeovers();
    }

    public static void unclaimChunkBulk(Set<Block> blocks, String masterBookID, BookMeta meta) {
        FileConfiguration claimData = plugin.cfg.getClaimData();

        for(Block b : blocks) {
            Location bLoc = b.getLocation();
            String chunkID = getChunkID(bLoc);
            String[] membersBefore = getMembers(chunkID);
            List<String> membersAfter = findMembersInBook(meta);
            String[] membersRemoved = null;

            if(membersBefore != null)
                membersRemoved = Arrays.stream(membersBefore).filter(mb -> !membersAfter.contains(mb)).toArray(String[]::new);

            claimData.set(chunkID, null);

            onChunkUnclaim(chunkID, membersRemoved, bLoc, masterBookID);
        }
        plugin.cfg.saveClaimData();

        masterBookChangeMsgSent = false;
        updateClaimCount();
    }

    // Update tablist with current number of claims for each player
    public static void updateClaimCount() {
        HashMap<String, Integer> membersNumClaims = countMemberClaims();

        for(Player p : Bukkit.getOnlinePlayers()) {
            Integer pClaims = membersNumClaims.get(p.getName().toLowerCase());

            if(pClaims == null)
                p.setPlayerListName(p.getName() + chat(" - &c0"));
            else
                p.setPlayerListName(p.getName() + chat(" - &c" + pClaims));
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
        if(plugin.getConfig().getBoolean("golems-guard-claims"))
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

                    if(now - lastIntrusionMsgReceived.getOrDefault(p, 0L) >= minSecBetweenAlerts * 1e9){
                        p.sendMessage(ChatColor.RED + "An intruder has entered your claim at "+x+", "+y+", "+z);
                        lastIntrusionMsgReceived.put(p, now);
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
        String[] members = getMembers(chunkID);

        // If the chunk isn't claimed or p is a member
        if (members == null || isMemberOfClaim(members, p))
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
                if(intruders.containsKey(currentChunkID))
                    for(Player intruder : intruders.get(currentChunkID))
                        golem.damage(0, intruder);
            }
        }
    }

    public static void populatePlayerClaimsIntruded(Player p){
        // Go through all intruded claims
        for(String chunkID : intruders.keySet()){
            String[] members = getMembers(chunkID);

            if(members != null) {
                if(isMemberOfClaim(members, p, false)) {
                    if (!playerClaimsIntruded.containsKey(p))
                        playerClaimsIntruded.put(p, new HashSet<>());

                    // Add the chunk as one of p's intruded claims
                    playerClaimsIntruded.get(p).add(chunkID);
                }
            }
        }
    }

    public static boolean isMemberOfClaim(String[] members, Player p) {
        return isMemberOfClaim(members, p, true);
    }

    public static boolean isMemberOfClaim(String[] members, Player p, boolean allowDisguise) {
        for (String member : members)
            if (member.equalsIgnoreCase(p.getName()) || (member.equalsIgnoreCase(activeDisguises.get(p)) && allowDisguise))
                return true;

        return false;
    }

    public static void disguisePlayer(Player disguiser, OfflinePlayer disguisee) {
        Collection<Property> textures = getCachedTextures(disguisee);
        disguisePlayer(disguiser, textures);
    }

    public static void disguisePlayer(Player disguiser, Collection<Property> textures) {
        setTextures(disguiser, textures);
        updateTexturesForOthers(disguiser);
        updateTexturesForSelf(disguiser);
    }

    public static Collection<Property> getTextures(OfflinePlayer p){
        try{
            Method getProfile = MinecraftReflection.getCraftPlayerClass().getDeclaredMethod("getProfile");
            GameProfile gp = (GameProfile) getProfile.invoke(p);

            return gp.getProperties().get("textures");
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Collection<Property> getCachedTextures(OfflinePlayer p){
        List<String> strs = plugin.cfg.getPlayerTextures().getStringList(p.getUniqueId().toString());
        Collection<Property> textures = new ArrayList<>();

        if(strs.size() == 3)
            textures.add(new Property(strs.get(0), strs.get(1), strs.get(2)));
        else
            textures.add(new Property(strs.get(0), strs.get(1)));

        return textures;
    }

    public static void setTextures(Player p, Collection<Property> textures){
        try{
            Method getProfile = MinecraftReflection.getCraftPlayerClass().getDeclaredMethod("getProfile");
            GameProfile gp = (GameProfile) getProfile.invoke(p);

            gp.getProperties().removeAll("textures");
            gp.getProperties().putAll("textures", textures);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public static void updateTexturesForOthers(Player disguiser) {
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.hidePlayer(plugin, disguiser);
            p.showPlayer(plugin, disguiser);
        }
    }

    public static void updateTexturesForSelf(Player disguiser) {
        Entity vehicle = disguiser.getVehicle();

        if (vehicle != null) {
            vehicle.removePassenger(disguiser);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                vehicle.addPassenger(disguiser);
            }, 1);
        }

        try {
            Method refreshPlayerMethod = MinecraftReflection.getCraftPlayerClass().getDeclaredMethod("refreshPlayer");

            refreshPlayerMethod.setAccessible(true);
            refreshPlayerMethod.invoke(disguiser);
        } catch (InvocationTargetException | IllegalAccessException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            isPaperServer = false;
        }
    }

    public static void restorePlayerSkin(Player p) {
        disguisePlayer(p, getCachedTextures(p));
    }

    public static void onLoseDisguise(Player disguiser) {
        if (activeDisguises.containsKey(disguiser)) {
            activeDisguises.remove(disguiser);
            disguiser.sendMessage("Your disguise has expired!");

            if (undisguiseTasks.containsKey(disguiser)) {
                undisguiseTasks.get(disguiser).cancel();
                undisguiseTasks.remove(disguiser);
            }else {
                String chunkID = getChunkID(disguiser.getLocation());
                if (isIntruder(disguiser, chunkID))
                    onIntruderEnterClaim(disguiser, chunkID);
            }
        }
    }

    public static void replaceInClaimPages(List<String> pages, String search, String replace) {
        for(int i = 0; i < pages.size(); i++){
            String page = pages.get(i);

            if (!utils.isClaimPage(page))
                break;

            String[] membersArray = page.split("\\n");

            for (int j = 1; j < membersArray.length; j++)
                if (membersArray[j].equalsIgnoreCase(search))
                    membersArray[j] = replace;

            pages.set(i, String.join("\n", membersArray));
        }
    }
}
