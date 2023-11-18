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
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collections;
import java.util.List;

import static me.hsgamer.playerskills.util.Utils.getPercentageFormat;

public class LootingSkill extends Skill {
    private final ConfigPath<Double> lootingIncrement = Paths.doublePath("looting-increment", 0.3D);

    public LootingSkill(PlayerSkills plugin) {
        super(plugin, "Looting", "looting", 20, 18);
    }
    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getKiller() != null) {
            Player player = event.getEntity().getKiller();
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

            int lootingLevel = getLevel(sPlayer);

            if (lootingLevel > 0) {
                double lootingIncrementValue = lootingIncrement.getValue();
                List<ItemStack> drops = event.getDrops();

                for (ItemStack drop : drops) {
                    // Apply looting bonus
                    int amount = (int) (drop.getAmount() * lootingLevel * lootingIncrementValue);
                    drop.setAmount(amount);
                }
            }
        }
    } @Override
    public List<ConfigPath<?>> getAdditionalConfigPaths() {
        return Collections.singletonList(lootingIncrement);
    }

    @Override
    public ItemBuilder getDefaultItem() {
        return new ItemBuilder()
                .addItemModifier(new NameModifier().setName("&cLooting Overview"))
                .addItemModifier(new XMaterialModifier(XMaterial.BONE))
                .addItemModifier(new LoreModifier().setLore(
                        "&eLeft-Click &7to upgrade this skill using &e{skillprice} &7point(s).",
                        "&7This skill increases loot drop rates.",
                        "&7Level: &e{level}&7/&e{max}&7",
                        " ",
                        "&cLoot Bonus: ",
                        "   &e{prev}x &7 >>> &e{next}x"
                ));
    }

    @Override
    public String getPreviousString(SPlayer player) {
        int lootingLevel = getLevel(player);
        double lootBonus = lootingLevel * lootingIncrement.getValue();
        return getPercentageFormat().format(lootBonus);
    }

    @Override
    public String getNextString(SPlayer player) {
        int lootingLevel = getLevel(player)+1;
        double lootBonus = lootingLevel * lootingIncrement.getValue();
        return getPercentageFormat().format(lootBonus);
    }
}