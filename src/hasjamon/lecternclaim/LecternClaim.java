package hasjamon.lecternclaim;

import hasjamon.lecternclaim.command.*;
import hasjamon.lecternclaim.listener.*;
import hasjamon.lecternclaim.files.ConfigManager;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Collections;
import java.util.List;

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
        registerEvents(); // Registers all the listeners
        setCommandExecutors(); // Registers all the commands
        setupHints(); // Prepares hints and starts broadcasting them
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

        if(dieCmd != null) dieCmd.setExecutor(new DieCommand());
        if(hintsCmd != null) hintsCmd.setExecutor(new HintsCommand(this));
        if(claimContestCmd != null) claimContestCmd.setExecutor(new ClaimContestCommand(this));
        if(welcomeCmd != null) welcomeCmd.setExecutor(new WelcomeCommand(this));
    }

    private void registerEvents() {
        pluginManager.registerEvents(new BlockBreak(this), this);
        pluginManager.registerEvents(new BookPlaceTake(this), this);
        pluginManager.registerEvents(new LecternBreak(this), this);
        pluginManager.registerEvents(new EditBook(), this);
        pluginManager.registerEvents(new BlockPlace(this), this);
        pluginManager.registerEvents(new LavaCasting(), this);
        pluginManager.registerEvents(new PlayerJoin(this), this);
    }

    public static LecternClaim getInstance(){
        return instance;
    }
}
