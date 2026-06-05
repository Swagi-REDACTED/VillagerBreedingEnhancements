package net.blupillcosby.villagerbreedingenhancements;

import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import net.blupillcosby.villagerbreedingenhancements.config.ModConfig;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VillagerBreedingEnhancements implements ModInitializer {
    public static final String MOD_ID = "villagerbreedingenhancements";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static ModConfig CONFIG;

    @Override
    public void onInitialize() {
        CONFIG = ConfigApiJava.registerAndLoadConfig(ModConfig::new);
        LOGGER.info("Villager Breeding Enhancements initialized.");
    }
}
