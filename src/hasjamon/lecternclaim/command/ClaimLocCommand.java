package hasjamon.lecternclaim.command;

import hasjamon.lecternclaim.LecternClaim;
import hasjamon.lecternclaim.utils.utils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class ClaimLocCommand implements CommandExecutor {
    private final LecternClaim plugin;

    public ClaimLocCommand(LecternClaim plugin) {
        this.plugin = plugin;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if(sender instanceof Player){
            Player p = (Player) sender;
            String chunkID = utils.getChunkID(p.getLocation());
            FileConfiguration claimData = plugin.cfg.getClaimData();

            if(claimData.contains(chunkID)){
                double x = claimData.getDouble(chunkID + ".location.X");
                double y = claimData.getDouble(chunkID + ".location.Y");
                double z = claimData.getDouble(chunkID + ".location.Z");

                p.sendMessage("Lectern is located at " + x + ", " + y + ", " + z);
            }else{
                p.sendMessage("This chunk isn't claimed.");
            }
            return true;
        }
        return false;
    }
}
