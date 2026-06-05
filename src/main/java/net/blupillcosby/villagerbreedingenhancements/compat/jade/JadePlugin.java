package net.blupillcosby.villagerbreedingenhancements.compat.jade;

import net.minecraft.world.entity.npc.villager.Villager;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;

@WailaPlugin
public class JadePlugin implements IWailaPlugin {
    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerEntityDataProvider(VillagerBreedingProvider.INSTANCE, Villager.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.addConfig(net.minecraft.resources.Identifier.fromNamespaceAndPath("villagerbreedingenhancements", "breeding"), true);
        registration.registerEntityComponent(VillagerBreedingProvider.Client.INSTANCE, Villager.class);
    }
}
