package com.leonardobishop.playerskills2.player;

import com.leonardobishop.playerskills2.PlayerSkills;
import com.leonardobishop.playerskills2.config.MainConfig;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;

public class SPlayer {

    // ******************************
    // STATIC
    // ******************************
    private static final HashMap<UUID, SPlayer> players = new HashMap<>();
    // ******************************
    // INSTANCE
    // ******************************
    private final UUID player;
    private final HashMap<String, Integer> skills;
    private int points;

    public SPlayer(UUID player) {
        this.player = player;
        this.skills = new HashMap<>();
    }

    public static SPlayer load(PlayerSkills plugin, UUID uuid) {
        File users = new File(plugin.getDataFolder() + File.separator + "users");
        if (!users.isDirectory()) {
            boolean success = users.mkdirs();
            if (success) {
                plugin.logInfo("New folder for users created.");
            } else {
                plugin.logError("Error occurred creating users folder.");
            }
        }

        File user = new File(users + File.separator + uuid + ".yml");
        SPlayer sPlayer = new SPlayer(uuid);
        if (user.exists()) {
            YamlConfiguration data = YamlConfiguration.loadConfiguration(user);
            if (data.contains("skills")) {
                for (String s : data.getConfigurationSection("skills").getKeys(false)) {
                    int level = data.getInt("skills." + s);
                    sPlayer.getSkills().put(s, level);
                }
            }
            int points = data.getInt("points");
            sPlayer.setPoints(points);
            if (plugin.isVerboseLogging()) {
                plugin.logInfo("Loaded SPlayer from disk for " + uuid + ".");
            }
        } else if (plugin.isVerboseLogging()) {
            plugin.logInfo("No SPlayer was found for " + uuid + " on disk. Creating new config...");
        }

        players.put(uuid, sPlayer);
        return sPlayer;
    }

    public static SPlayer get(UUID uuid) {
        return players.get(uuid);
    }

    public static void unload(UUID uuid) {
        players.remove(uuid);
    }

    public static void save(PlayerSkills plugin, SPlayer sPlayer) {
        if (sPlayer == null) {
            if (plugin.isVerboseLogging()) {
                plugin.logInfo("Plugin tried to save an SPlayer for a null player.");
            }
            return;
        }
        File users = new File(plugin.getDataFolder() + File.separator + "users");
        if (!users.isDirectory()) {
            boolean success = users.mkdirs();
            if (success) {
                plugin.logInfo("New folder for users created.");
            } else {
                plugin.logError("Error occurred creating users folder.");
            }
        }

        File user = new File(users + File.separator + sPlayer.getPlayer() + ".yml");
        if (!user.exists()) {
            try {
                user.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                plugin.logError("Error occurred creating user save for " + sPlayer.getPlayer() + ". Aborting.");
                return;
            }
        }
        YamlConfiguration data = YamlConfiguration.loadConfiguration(user);
        data.set("skills", null);
        for (String s : sPlayer.getSkills().keySet()) {
            data.set("skills." + s, sPlayer.getLevel(s));
        }
        data.set("points", sPlayer.getPoints());

        try {
            data.save(user);
            if (plugin.isVerboseLogging()) {
                plugin.logInfo("Saved SPlayer for " + sPlayer.getPlayer() + ".");
            }
        } catch (IOException e) {
            e.printStackTrace();
            plugin.logError("Error occurred creating user save for " + sPlayer.getPlayer() + ". Aborting.");
        }
    }

    public static HashMap<UUID, SPlayer> getPlayers() {
        return players;
    }

    public UUID getPlayer() {
        return player;
    }

    public HashMap<String, Integer> getSkills() {
        return skills;
    }

    public int getLevel(String skill) {
        return skills.getOrDefault(skill, 0);
    }

    public void setLevel(String skill, int level) {
        skills.put(skill, level);
    }

    public int getPoints() {
        return points;
    }

    public void setPoints(int points) {
        this.points = points;
    }

    public int getNextPointPrice() {
        int base = MainConfig.POINTS_PRICE.getValue();
        if (MainConfig.POINTS_DYNAMIC_PRICE_ENABLED.getValue()) {
            int playerPoints = getPoints();
            for (int i : getSkills().values()) {
                playerPoints += i;
            }
            return base + (playerPoints * MainConfig.POINTS_DYNAMIC_PRICE_PRICE_INCREASE_PER_POINT.getValue());
        } else {
            return base;
        }
    }
}
