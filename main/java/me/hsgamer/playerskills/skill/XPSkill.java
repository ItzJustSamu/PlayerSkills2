package me.hsgamer.playerskills.skill;

import com.cryptomorin.xseries.XMaterial;
import me.hsgamer.hscore.bukkit.item.ItemBuilder;
import me.hsgamer.hscore.bukkit.item.modifier.LoreModifier;
import me.hsgamer.hscore.bukkit.item.modifier.NameModifier;
import me.hsgamer.hscore.config.path.ConfigPath;
import me.hsgamer.hscore.config.path.impl.Paths;
import me.hsgamer.playerskills.PlayerSkills;
import me.hsgamer.playerskills.config.MainConfig;
import me.hsgamer.playerskills.player.SPlayer;
import me.hsgamer.playerskills.util.Utils;
import me.hsgamer.playerskills.util.modifier.XMaterialModifier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Collections;
import java.util.List;

import static me.hsgamer.playerskills.util.Utils.getPercentageFormat;

public class XPSkill extends Skill {
    private final ConfigPath<Double> XP_Increment = Paths.doublePath("xp-increment", 0.2);

    public XPSkill(PlayerSkills plugin) {
        super(plugin, "XP", "xp", 20, 23);
    }

    // Event handler for hit event
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() == null) {
            return; // The entity wasn't killed by a player
        }

        Player killer = (Player) event.getEntity().getKiller();

        if (isWorldNotAllowed(killer)) {
            return;
        }

        SPlayer sPlayer = SPlayer.get(killer.getUniqueId());

        if (sPlayer == null) {
            if (MainConfig.isVerboseLogging()) {
                Utils.logError("Failed event. SPlayer for " + killer.getUniqueId() + " is null.");
            }
            return;
        }

        double increment = XP_Increment.getValue();

        // Calculate additional XP from killing an entity
        int xpLevel = (int) (getLevel(sPlayer) * increment);

        int xp = killer.getTotalExperience() + xpLevel;

        // Set the new experience points
        killer.setTotalExperience(xp);

        // You might also want to update the player's visible XP bar
        killer.setLevel(killer.getLevel() + xpLevel);
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();

        if (isWorldNotAllowed(player)) {
            return;
        }

        SPlayer sPlayer = SPlayer.get(player.getUniqueId());

        if (sPlayer == null) {
            if (MainConfig.isVerboseLogging()) {
                Utils.logError("Failed event. SPlayer for " + player.getUniqueId() + " is null.");
            }
            return;
        }

        int xpSkillLevel = getLevel(sPlayer);
        double XPIncrement = XP_Increment.getValue();

        // Check if the mined block should yield experience
        if (blockDropsExperience(block.getType())) {
            // Calculate the additional XP from mining
            double additionalXP = xpSkillLevel * XPIncrement;

            int currentExperience = player.getTotalExperience();
            int newExperience = currentExperience + (int) additionalXP;

            // Set the new experience points
            player.setTotalExperience(newExperience);
        }
    }

    // Helper method to check if a block drops experience
    private boolean blockDropsExperience(Material blockType) {
        // You can define which block types drop experience here
        return blockType == Material.COAL_ORE || blockType == Material.IRON_ORE
                || blockType == Material.GOLD_ORE || blockType == Material.DIAMOND_ORE;
        // Add more block types as needed
    }

    @Override
    public List<ConfigPath<?>> getAdditionalConfigPaths() {
        return Collections.singletonList(XP_Increment);
    }

    @Override
    public ItemBuilder getDefaultItem() {
        return new ItemBuilder()
                .addItemModifier(new NameModifier().setName("&cXPSkill Overview"))
                .addItemModifier(new XMaterialModifier(XMaterial.EXPERIENCE_BOTTLE))
                .addItemModifier(new LoreModifier().setLore(
                        "&eLeft-Click &7to upgrade this skill using &e{skillprice} &7point(s).",
                        "&7This skill increases the amount of XP gained per hit.",
                        "&7Level: &e{level}&7/&e{max}&7",
                        " ",
                        "&cXP Increase: ",
                        "   &e{prev}% &7 >>> &e{next}%"
                ));
    }

    @Override
    public String getPreviousString(SPlayer player) {
        int xpLevel = getLevel(player);
        double xp = xpLevel * XP_Increment.getValue();
        return getPercentageFormat().format(xp);
    }

    @Override
    public String getNextString(SPlayer player) {
        int xpLevel = getLevel(player) + 1;
        double xp = xpLevel * XP_Increment.getValue();
        return getPercentageFormat().format(xp);
    }
}