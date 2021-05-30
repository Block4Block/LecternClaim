package hasjamon.lecternclaim.listener;

import hasjamon.lecternclaim.utils.utils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

public class EditBook implements Listener {
    @EventHandler
    public void onInteract(PlayerInteractEvent e){
        Player p = e.getPlayer();

        // If player right clicks air
        if(e.getAction().equals(Action.RIGHT_CLICK_AIR)){
            ItemStack item = e.getItem();

            // If player is holding a book and quill
            if(item != null && item.getType() == Material.WRITABLE_BOOK){

                // If the player isn't a member of any claims
                if(!utils.countMemberClaims().containsKey(p.getName().toLowerCase())) {
                    // Send the player instruction on what to do with the book
                    p.sendMessage(utils.chat("&cNOTE: &7To make a claim book, type &a\"claim\" &7at the top of the book!"));
                    p.sendMessage(utils.chat("&aThen write a player's ign on each line to add a member!"));
                    p.spigot().sendMessage(new ComponentBuilder(utils.chat("&aCLICK HERE to see an example"))
                            .event(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://hasjamon.github.io/b4block/lists.html" ))
                            .create());
                }
            }
        }
    }
}
