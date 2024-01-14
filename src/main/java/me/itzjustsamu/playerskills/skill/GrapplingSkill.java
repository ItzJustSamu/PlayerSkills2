package me.itzjustsamu.playerskills.skill;

import com.cryptomorin.xseries.XMaterial;
import me.hsgamer.hscore.bukkit.item.ItemBuilder;
import me.hsgamer.hscore.bukkit.item.modifier.LoreModifier;
import me.hsgamer.hscore.bukkit.item.modifier.NameModifier;
import me.hsgamer.hscore.config.path.ConfigPath;
import me.hsgamer.hscore.config.path.impl.Paths;
import me.itzjustsamu.playerskills.PlayerSkills;
import me.itzjustsamu.playerskills.player.SPlayer;
import me.itzjustsamu.playerskills.util.Utils;
import me.itzjustsamu.playerskills.util.modifier.XMaterialModifier;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class GrapplingSkill extends Skill {

    private final ConfigPath<Double> grapplingStrength = Paths.doublePath("grappling-strength", 2.0);
    private final ConfigPath<Boolean> airGrappling = Paths.booleanPath("air-grappling", true);
    private final ConfigPath<Boolean> entityGrappling = Paths.booleanPath("entity-grappling", true);
    private final ConfigPath<String> cooldownMessage = Paths.stringPath("cooldown-message", "&cGrappling cooldown: &e{remaining_time} seconds.");
    private final ConfigPath<Long> cooldownDuration = Paths.longPath("cooldown-duration", 30000L); // 3000 milliseconds cooldown (adjust as needed)

    private final double scalingFactor = 0.3;
    private final HashMap<Player, Long> cooldownMap = new HashMap<>();

    public GrapplingSkill(PlayerSkills plugin) {
        super(plugin, "Grappling", "grappling", 3, 10,0);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        SPlayer sPlayer = SPlayer.get(player.getUniqueId());

        if (!isFishingRod(player) || Worlds_Restriction(player) || sPlayer == null) {
            return;
        }

        Entity caughtEntity = event.getCaught();
        PlayerFishEvent.State state = event.getState();

        // Check if the fishing rod is aimed at a valid target based on configuration
        if ((!airGrappling.getValue() && state == PlayerFishEvent.State.IN_GROUND) ||
                (!entityGrappling.getValue() && caughtEntity != null && state == PlayerFishEvent.State.CAUGHT_ENTITY)) {
            return;
        }

        // Check cooldown
        if (cooldownMap.containsKey(player)) {
            Long cooldownEndTime = cooldownMap.get(player);
            if (cooldownEndTime != null && System.currentTimeMillis() < cooldownEndTime) {
                // Check if the player's level is above 1 before sending the cooldown message
                if (getLevel(sPlayer) > 1) {
                    sendActionBar(player, cooldownEndTime);
                }
                return;
            }
        }

        double originalStrength = grapplingStrength.getValue() * getLevel(sPlayer);
        double scaledStrength = originalStrength * scalingFactor;

        Vector direction = player.getLocation().getDirection();
        player.setVelocity(direction.multiply(scaledStrength));

        // Set cooldown
        cooldownMap.put(player, System.currentTimeMillis() + cooldownDuration.getValue());
    }

    private boolean isFishingRod(Player player) {
        return player.getInventory().getItemInMainHand().getType() == XMaterial.FISHING_ROD.parseMaterial();
    }

    @Override
    public List<ConfigPath<?>> getAdditionalConfigPaths() {
        List<ConfigPath<?>> additionalPaths = new ArrayList<>(super.getAdditionalConfigPaths());
        additionalPaths.add(grapplingStrength);
        additionalPaths.add(airGrappling);
        additionalPaths.add(entityGrappling);
        additionalPaths.add(cooldownMessage);
        additionalPaths.add(cooldownDuration);
        return additionalPaths;
    }

    @Override
    public ItemBuilder getDefaultItem() {
        return new ItemBuilder()
                .addItemModifier(new NameModifier().setName("&cGrappling Hook"))
                .addItemModifier(new XMaterialModifier(XMaterial.FISHING_ROD))
                .addItemModifier(new LoreModifier().setLore(
                        "&eLeft-Click &7to upgrade this skill using &e{skillprice} &7point(s).",
                        "&7This skill allows you to use a grappling hook for fast movement.",
                        "&7Level: &e{level}&7/&e{max}&7",
                        " ",
                        "&cGrappling Strength: ",
                        "   &e{prev}x &7 >>> &e{next}x"
                ));
    }

    @Override
    public String getPreviousString(SPlayer player) {
        double originalStrength = grapplingStrength.getValue() * getLevel(player);
        double scaledStrength = originalStrength * scalingFactor;
        return Utils.getPercentageFormat().format(scaledStrength);
    }

    @Override
    public String getNextString(SPlayer player) {
        double originalStrength = grapplingStrength.getValue() * (getLevel(player) + 1);
        double scaledStrength = originalStrength * scalingFactor;
        return Utils.getPercentageFormat().format(scaledStrength);
    }

    private void sendActionBar(Player player, long cooldownEndTime) {
        long remainingTime = (cooldownEndTime - System.currentTimeMillis()) / 1000L;

        new BukkitRunnable() {
            long timeLeft = remainingTime;

            @Override
            public void run() {
                if (timeLeft > 0) {
                    String actionBarMessage = ChatColor.translateAlternateColorCodes('&', cooldownMessage.getValue())
                            .replace("{remaining_time}", String.valueOf(timeLeft));
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(actionBarMessage));
                    timeLeft--;
                } else {
                    player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new TextComponent(""));
                    cancel(); // Stop the task when the cooldown ends
                }
            }
        }.runTaskTimer(getPlugin(), 0L, 20L); // Update every second
    }
}
