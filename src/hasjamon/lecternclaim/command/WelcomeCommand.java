package hasjamon.lecternclaim.command;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import java.util.List;

public class WelcomeCommand implements CommandExecutor {
    private final LecternClaim plugin;

    public WelcomeCommand(LecternClaim plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        List<String> welcomeMessages = plugin.getConfig().getStringList("welcome-messages");

        for(String msg : welcomeMessages)
            sender.sendMessage(utils.chat(msg));

        return true;
    }
}
