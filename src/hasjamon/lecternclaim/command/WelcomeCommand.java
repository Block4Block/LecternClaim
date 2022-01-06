package hasjamon.lecternclaim.command;

import hasjamon.lecternclaim.utils.utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class WelcomeCommand implements CommandExecutor {
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(sender instanceof Player player) {
            utils.sendWelcomeMsg(player);

            return true;
        }

        return false;
    }
}
