package com.leonardobishop.playerskills2.skill;

import com.cryptomorin.xseries.XMaterial;
import com.leonardobishop.playerskills2.PlayerSkills;
import com.leonardobishop.playerskills2.player.SPlayer;
import com.leonardobishop.playerskills2.utils.modifier.XMaterialModifier;
import me.hsgamer.hscore.bukkit.item.ItemBuilder;
import me.hsgamer.hscore.bukkit.item.modifier.LoreModifier;
import me.hsgamer.hscore.bukkit.item.modifier.NameModifier;
import me.hsgamer.hscore.config.ConfigPath;
import me.hsgamer.hscore.config.path.DoubleConfigPath;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.FoodLevelChangeEvent;

import java.util.Collections;
import java.util.List;

import static com.leonardobishop.playerskills2.utils.Utils.getPercentageFormat;

public class GluttonySkill extends Skill {
    private final DoubleConfigPath percentIncrease = new DoubleConfigPath("percent-increase", 50D);

    public GluttonySkill(PlayerSkills plugin) {
        super(plugin, "Gluttony", "gluttony", 4, 21);
    }

    @EventHandler
    public void onFood(FoodLevelChangeEvent event) {
        Player player = (Player) event.getEntity();
        if (isWorldNotAllowed(player)) {
            return;
        }

        if (player.getFoodLevel() >= event.getFoodLevel()) {
            return;
        }

        event.setCancelled(true);
        SPlayer sPlayer = SPlayer.get(player.getUniqueId());

        if (sPlayer == null) {
            if (super.getPlugin().isVerboseLogging()) {
                super.getPlugin().logError("Failed event. SPlayer for " + player.getUniqueId() + " is null.");
            }
            return;
        }

        int diff = event.getFoodLevel() - player.getFoodLevel();
        int gluttonyLevel = getLevel(sPlayer);
        double multiplier = 1D + (gluttonyLevel * (percentIncrease.getValue() / 100D));

        double newLevel = diff * multiplier;
        player.setFoodLevel(player.getFoodLevel() + (int) newLevel);
    }

    @Override
    public List<ConfigPath<?>> getAdditionalConfigPaths() {
        return Collections.singletonList(percentIncrease);
    }

    @Override
    public ItemBuilder getDefaultItem() {
        return new ItemBuilder()
                .addItemModifier(new NameModifier().setName("&cGluttony Overview"))
                .addItemModifier(new XMaterialModifier(XMaterial.WHEAT))
                .addItemModifier(new LoreModifier().setLore(
                        "&eLeft-Click &7to upgrade this skill using &e{skillprice} &7point(s).",
                        "&7This skill increases the amount of food ingested from a single item.",
                        "&7Level: &e{level}&7/&e{max}&7",
                        " ",
                        "&cFood heal amount: ",
                        "   &e{prev}% &7 >>> &e{next}%"
                ));
    }

    @Override
    public String getPreviousString(SPlayer player) {
        int gluttonyLevel = getLevel(player);
        double heal = 100 + (gluttonyLevel * percentIncrease.getValue());
        return getPercentageFormat().format(heal);
    }

    @Override
    public String getNextString(SPlayer player) {
        int gluttonyLevel = getLevel(player) + 1;
        double heal = 100 + (gluttonyLevel * percentIncrease.getValue());
        return getPercentageFormat().format(heal);
    }
}
