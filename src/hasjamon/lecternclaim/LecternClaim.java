package hasjamon.lecternclaim;

import hasjamon.lecternclaim.command.*;
import hasjamon.lecternclaim.files.ConfigManager;
import hasjamon.lecternclaim.listener.*;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class LecternClaim extends JavaPlugin{
    public PluginManager pluginManager = getServer().getPluginManager();
    public ConfigManager cfg;
    private static LecternClaim instance;
    private List<?> hints;
    private int nextHint = 0;

    @Override
    public void onEnable() {
        instance = this; // Creates instance of the plugin
        cfg = new ConfigManager(); // Initializes config
        populateKnownPlayers();
        registerEvents(); // Registers all the listeners
        setCommandExecutors(); // Registers all the commands
        setupHints(); // Prepares hints and starts broadcasting them
        if(this.getConfig().getBoolean("golems-guard-claims"))
            getServer().getScheduler().scheduleSyncRepeatingTask(this, utils::updateGolemHostility, 0, 20);
        utils.minSecBetweenAlerts = this.getConfig().getInt("seconds-between-intruder-alerts");
        if(this.getConfig().getBoolean("enable-claim-maps"))
            addMapRenderers();
    }

    private void setupHints() {
        hints = getConfig().getList("hints");
        long interval = getConfig().getLong("seconds-between-hints");
        boolean hintsEnabled = getConfig().getBoolean("hints-enabled");

        // Shuffle hints, then show a hint every 10 minutes (20 ticks/second * 600 seconds)
        if(hints != null && hintsEnabled){
            Collections.shuffle(hints);
            getServer().getScheduler().scheduleSyncRepeatingTask(this, this::showHint, 0, 20 * interval);
        }
    }

    private void showHint() {
        if(++nextHint >= hints.size())
            nextHint = 0;

        String hint = (String) hints.get(nextHint);
        FileConfiguration hintSettings = this.cfg.getHintSettings();

        for(Player p : Bukkit.getOnlinePlayers()) {
            String pUUID = p.getUniqueId().toString();
            String pSettings = hintSettings.getString(pUUID);

            if (pSettings == null || pSettings.equals("on"))
                p.sendMessage(utils.chat(hint));
        }
    }

    private void setCommandExecutors() {
        PluginCommand dieCmd = this.getCommand("die");
        PluginCommand hintsCmd = this.getCommand("hints");
        PluginCommand claimContestCmd = this.getCommand("claimcontest");
        PluginCommand welcomeCmd = this.getCommand("welcome");
        PluginCommand claimLocCmd = this.getCommand("claimloc");
        PluginCommand claimFixCmd = this.getCommand("claimfix");

        if(dieCmd != null) dieCmd.setExecutor(new DieCommand());
        if(hintsCmd != null) hintsCmd.setExecutor(new HintsCommand(this));
        if(claimContestCmd != null) claimContestCmd.setExecutor(new ClaimContestCommand(this));
        if(welcomeCmd != null) welcomeCmd.setExecutor(new WelcomeCommand(this));
        if(claimLocCmd != null) claimLocCmd.setExecutor(new ClaimLocCommand(this));
        if(claimFixCmd != null) claimFixCmd.setExecutor(new ClaimFixCommand(this));
    }

    private void registerEvents() {
        pluginManager.registerEvents(new BlockBreak(this), this);
        pluginManager.registerEvents(new BookPlaceTake(this), this);
        pluginManager.registerEvents(new LecternBreak(this), this);
        pluginManager.registerEvents(new BookEdit(this), this);
        pluginManager.registerEvents(new BlockPlace(this), this);
        if(this.getConfig().getBoolean("balance-lavacasting"))
            pluginManager.registerEvents(new LavaCasting(), this);
        pluginManager.registerEvents(new PlayerJoin(this), this);
        pluginManager.registerEvents(new PlayerMove(), this);
        pluginManager.registerEvents(new PlayerQuit(), this);
        pluginManager.registerEvents(new PlayerDeath(this), this);
        pluginManager.registerEvents(new PlayerRespawn(), this);
        pluginManager.registerEvents(new ChunkLoad(), this);
        if(this.getConfig().getBoolean("disable-freecam-interactions"))
            pluginManager.registerEvents(new FreecamInteract(), this);
        if(this.getConfig().getBoolean("enable-lava-immunity"))
            pluginManager.registerEvents(new PlayerLavaDamage(this), this);
        pluginManager.registerEvents(new EntityExplode(this), this);
        if(this.getConfig().getBoolean("enable-disguises"))
            pluginManager.registerEvents(new EquipPlayerHead(this), this);
        if(this.getConfig().getBoolean("enable-claim-maps"))
            pluginManager.registerEvents(new LecternRightClick(this), this);
        pluginManager.registerEvents(new MapCraft(), this);
    }

    private void addMapRenderers() {
        FileConfiguration claimMaps = cfg.getClaimMaps();
        Set<String> mapIDs = claimMaps.getKeys(false);

        for(String id : mapIDs){
            MapView view = Bukkit.getMap(Integer.parseInt(id));
            String uuid = claimMaps.getString(id);

            if(view != null && uuid != null && view.getRenderers().size() == 1) {
                OfflinePlayer mapCreator = Bukkit.getOfflinePlayer(UUID.fromString(uuid));
                view.addRenderer(utils.createClaimRenderer(mapCreator));
                view.addRenderer(utils.createIntruderRenderer(mapCreator));
            }else {
                claimMaps.set(id, null);
            }
        }

        cfg.saveClaimMaps();
    }

    private void populateKnownPlayers() {
        for(OfflinePlayer p : Bukkit.getOfflinePlayers())
            if(p != null && p.getName() != null)
                utils.knownPlayers.add(p.getName().toLowerCase());
    }

    public static LecternClaim getInstance(){
        return instance;
    }
}
