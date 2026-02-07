package dev.veyno.vHomes;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SetHomeCommand implements CommandExecutor, TabCompleter {


    private final VHomes plugin;

    public SetHomeCommand(VHomes plugin){
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        if(!(commandSender instanceof Player p)) return false;
        if(args.length<1){
            commandSender.sendMessage(MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("messages.no-name-specified", "<red>Invalid Arguments")));
            return true;
        }
        String name = String.join(" ", args);
        plugin.getHomeManager().attemptCreateHome(p, name);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String @NotNull [] args) {
        return List.of("<name>");
    }
}
