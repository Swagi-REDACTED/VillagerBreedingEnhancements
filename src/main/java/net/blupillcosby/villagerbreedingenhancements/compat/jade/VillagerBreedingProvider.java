package net.blupillcosby.villagerbreedingenhancements.compat.jade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.blupillcosby.villagerbreedingenhancements.access.InLoveVillager;
import snownee.jade.api.EntityAccessor;
import snownee.jade.api.IEntityComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;
import org.jspecify.annotations.Nullable;

public class VillagerBreedingProvider implements StreamServerDataProvider<EntityAccessor, Integer> {
    public static final VillagerBreedingProvider INSTANCE = new VillagerBreedingProvider();
    private static final int IN_LOVE = -1;

    @Override
    public @Nullable Integer streamData(EntityAccessor accessor) {
        Entity entity = accessor.getEntity();
        if (entity instanceof Villager villager) {
            if (((InLoveVillager)villager).getInLoveTicks() > 0) {
                return IN_LOVE;
            }
            int age = villager.getAge();
            if (age > 0) return age;
        }
        return null;
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, Integer> streamCodec() {
        return ByteBufCodecs.VAR_INT.cast();
    }

    @Override
    public Identifier getUid() {
        return Identifier.fromNamespaceAndPath("villagerbreedingenhancements", "breeding");
    }

    public static class Client implements IEntityComponentProvider {
        public static final Client INSTANCE = new Client();

        @Override
        public void appendTooltip(ITooltip tooltip, EntityAccessor accessor, IPluginConfig config) {
            int time = VillagerBreedingProvider.INSTANCE.decodeFromData(accessor).orElse(0);
            if (time == IN_LOVE) {
                tooltip.add(Component.translatable("jade.mobbreeding.fed"));
            } else if (time > 0) {
                tooltip.add(Component.translatable("jade.mobbreeding.time", IThemeHelper.get().seconds(time, accessor.tickRate())));
            }
        }

        @Override
        public Identifier getUid() {
            return Identifier.fromNamespaceAndPath("villagerbreedingenhancements", "breeding");
        }
    }
}
