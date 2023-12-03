package me.itzjustsamu.playerskills.command;

import com.google.common.collect.ImmutableList;

import java.util.*;

import me.itzjustsamu.playerskills.menu.SkillsSettings;
import me.itzjustsamu.playerskills.skill.Skill;
import me.itzjustsamu.playerskills.Permissions;
import me.itzjustsamu.playerskills.PlayerSkills;
import me.itzjustsamu.playerskills.player.SPlayer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SkillsAdminCommand extends Command implements TabCompleter {

    private final PlayerSkills plugin;

    public SkillsAdminCommand(PlayerSkills plugin) {
        super("skillsadmin", "Admin control for PlayerSkills", "/skillsadmin", List.of("sa", "skillsadmin", "skilladmin"));
        setPermission(Permissions.ADMIN.getName());
        this.plugin = plugin;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
        if (!testPermission(sender)) {
            return false;
        }

        if (args.length >= 4) {
            if (args[0].equalsIgnoreCase("setskill")) {
                setSkill(sender, args);
                return true;
            }
        } else if (args.length == 3) {
            if (args[0].equalsIgnoreCase("givepoints")) {
                givePoints(sender, args);
                return true;
            }
        } else if (args.length == 2) {
            if (args[0].equalsIgnoreCase("view")) {
                viewSkills(sender, args);
                return true;
            } else if (args[0].equalsIgnoreCase("fullreset")) {
                fullReset(sender, args);
                return true;
            }
        } else if (args.length == 1)  {
            if (args[0].equalsIgnoreCase("settings")) {
                openSettingsMenu(sender);
                return true;
            }
        }

        sender.sendMessage(ChatColor.RED + "/skillsadmin view <player>");
        sender.sendMessage(ChatColor.RED + "/skillsadmin givepoints <player> [-]<points>");
        sender.sendMessage(ChatColor.RED + "/skillsadmin setskill <player> <skill name> <level>");
        sender.sendMessage(ChatColor.RED + "/skillsadmin fullreset <player>");
        sender.sendMessage(ChatColor.RED + "/skillsadmin settings");
        return true;
    }

    private Optional<SPlayer> getPlayer(CommandSender sender, String arg) {
        Player player = Bukkit.getPlayer(arg);
        if (player == null) {
            sender.sendMessage(ChatColor.RED + "Player not found.");
            return Optional.empty();
        } else {
            SPlayer sPlayer = SPlayer.get(player.getUniqueId());
            if (sPlayer == null) {
                sender.sendMessage(ChatColor.RED + "ERROR: SPlayer could not be found.");
                return Optional.empty();
            }
            return Optional.of(sPlayer);
        }
    }

    private void setSkill(CommandSender sender, String[] args) {
        Optional<SPlayer> optionalSPlayer = getPlayer(sender, args[1]);
        if (!optionalSPlayer.isPresent()) return;
        SPlayer sPlayer = optionalSPlayer.get();
        String skillName = args[2];

        int level;
        try {
            level = Integer.parseInt(args[3]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number.");
            return;
        }

        Skill s = plugin.getSkills().get(skillName);

        if (s == null) {
            sender.sendMessage(ChatColor.RED + "Skill could not be found. Skill names used in configurations and commands should contain no spaces and" +
                    " should be all lower case.");
            return;
        }

        sPlayer.setLevel(s.getConfigName(), level);
        sender.sendMessage(ChatColor.GREEN + "Skill level for " + s.getName() + " updated to " + s.getLevel(sPlayer) + ".");
    }

    private void givePoints(CommandSender sender, String[] args) {
        Optional<SPlayer> optionalSPlayer = getPlayer(sender, args[1]);
        if (!optionalSPlayer.isPresent()) return;
        SPlayer sPlayer = optionalSPlayer.get();

        int points;
        try {
            points = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "Not a number.");
            return;
        }

        sPlayer.setPoints(sPlayer.getPoints() + points);

        sender.sendMessage(ChatColor.GREEN + "Points updated to " + sPlayer.getPoints() + ".");
    }

    private void viewSkills(CommandSender sender, String[] args) {
        Optional<SPlayer> optionalSPlayer = getPlayer(sender, args[1]);
        if (!optionalSPlayer.isPresent()) return;
        SPlayer sPlayer = optionalSPlayer.get();

        StringBuilder message = new StringBuilder();
        message.append(ChatColor.RED.toString()).append(ChatColor.BOLD).append("Skills").append("\n");
        for (Skill skill : plugin.getSkills().values()) {
            message.append(ChatColor.RED).append(skill.getName()).append(": ").append(ChatColor.GRAY).append(skill.getLevel(sPlayer)).append("\n");
        }
        message.append(ChatColor.RED.toString()).append(ChatColor.BOLD).append("Points: ").append(ChatColor.GRAY).append(sPlayer.getPoints());

        sender.sendMessage(message.toString());
    }

    private void fullReset(CommandSender sender, String[] args) {
        Optional<SPlayer> optionalSPlayer = getPlayer(sender, args[1]);
        if (!optionalSPlayer.isPresent()) return;
        SPlayer sPlayer = optionalSPlayer.get();

        sPlayer.setPoints(0);
        Set<String> skills = sPlayer.getSkills().keySet();
        for (String skill : skills) {
            sPlayer.setLevel(skill, 0);
        }
        sender.sendMessage(ChatColor.GREEN + "Skill data for (" + ChatColor.GRAY + sPlayer.getPlayer() + ChatColor.GREEN + ") " +
                "has been reset.");
    }

    private void openSettingsMenu(CommandSender sender) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Optional<SPlayer> optionalSPlayer = getPlayer(sender, player.getName());
            if (!optionalSPlayer.isPresent()) return;
            SPlayer sPlayer = optionalSPlayer.get();
            SkillsSettings menu = new SkillsSettings(plugin, player, sPlayer);
            menu.open(player);
        } else {
            sender.sendMessage(ChatColor.RED + "This command can only be executed by a player");
        }
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, String[] args) {
        if (args.length == 1) {
            // Tab complete subcommands
            String partialSubCommand = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            for (String subCommand : List.of("setskill", "givepoints", "view", "fullreset", "settings")) {
                if (subCommand.startsWith(partialSubCommand) && sender.hasPermission("playerskills.admin." + subCommand)) {
                    completions.add(subCommand);
                }
            }
            return completions;
        } else if (args.length == 2) {
            // Tab complete player names for specific subcommands
            String subCommand = args[0].toLowerCase();
            if (subCommand.equals("setskill") || subCommand.equals("givepoints") || subCommand.equals("view") || subCommand.equals("fullreset")) {
                String partialPlayerName = args[1].toLowerCase();
                List<String> playerNames = new ArrayList<>();
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    if (onlinePlayer.getName().toLowerCase().startsWith(partialPlayerName)) {
                        playerNames.add(onlinePlayer.getName());
                    }
                }
                return playerNames;
            }
        }

        return ImmutableList.of(); // Return an empty list if no matches
    }
}