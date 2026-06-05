package net.blupillcosby.villagerbreedingenhancements.mixin;

import net.blupillcosby.villagerbreedingenhancements.VillagerBreedingEnhancements;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ai.behavior.BehaviorUtils;
import net.minecraft.world.entity.ai.behavior.VillagerMakeLove;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(VillagerMakeLove.class)
public abstract class VillagerMakeLoveMixin {

    @Shadow
    private long birthTimestamp;

    @Shadow
    protected abstract Optional<Villager> breed(ServerLevel level, Villager source, Villager target);

    @Shadow
    protected abstract Optional<BlockPos> takeVacantBed(ServerLevel level, Villager body);

    @Shadow
    protected abstract void giveBedToChild(ServerLevel level, Villager child, BlockPos bedPos);

    /**
     * @author blupillcosby
     * @reason Respects requireBeds toggle and success/fail chance config
     */
    @Overwrite
    private void tryToGiveBirth(final ServerLevel level, final Villager body, final Villager target) {
        boolean requireBeds = VillagerBreedingEnhancements.CONFIG.requireBeds.get();
        Optional<BlockPos> childsBed = Optional.empty();

        if (requireBeds) {
            childsBed = this.takeVacantBed(level, body);
            if (childsBed.isEmpty()) {
                level.broadcastEntityEvent(target, (byte) 13);
                level.broadcastEntityEvent(body, (byte) 13);
                return;
            }
        }

        int roll = body.getRandom().nextInt(100) + 1; // 1-100
        int successChance = VillagerBreedingEnhancements.CONFIG.breedSuccessChance.get();

        if (roll <= successChance) {
            Optional<Villager> child = this.breed(level, body, target);
            if (child.isPresent()) {
                if (requireBeds && childsBed.isPresent()) {
                    this.giveBedToChild(level, child.get(), childsBed.get());
                }

                // Apply custom cooldown
                int cooldownMins = VillagerBreedingEnhancements.CONFIG.cooldownTimer.get();
                int cooldownTicks = cooldownMins * 1200; // 0 mins = 0 ticks
                body.setAge(cooldownTicks);
                target.setAge(cooldownTicks);
                ((net.blupillcosby.villagerbreedingenhancements.access.InLoveVillager)body).setInLoveTicks(0);
                ((net.blupillcosby.villagerbreedingenhancements.access.InLoveVillager)target).setInLoveTicks(0);
            } else {
                if (requireBeds && childsBed.isPresent()) {
                    level.getPoiManager().release(childsBed.get());
                    level.debugSynchronizers().updatePoi(childsBed.get());
                }
                level.broadcastEntityEvent(target, (byte) 13);
                level.broadcastEntityEvent(body, (byte) 13);
            }
        } else {
            // Failure
            if (requireBeds && childsBed.isPresent()) {
                level.getPoiManager().release(childsBed.get());
                level.debugSynchronizers().updatePoi(childsBed.get());
            }

            level.broadcastEntityEvent(target, (byte) 13);
            level.broadcastEntityEvent(body, (byte) 13);

            int tryAgainRoll = body.getRandom().nextInt(100) + 1;
            int tryAgainChance = VillagerBreedingEnhancements.CONFIG.chanceToTryAgainOnFailure.get();

            if (tryAgainRoll <= tryAgainChance) {
                // Success in trying again: No cooldown, keep love status
                body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
                target.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            } else {
                // Failure to try again: Apply cooldown and lose love status
                int cooldownMins = VillagerBreedingEnhancements.CONFIG.cooldownTimer.get();
                int cooldownTicks = cooldownMins * 1200; // 0 mins = 0 ticks
                body.setAge(cooldownTicks);
                target.setAge(cooldownTicks);
                
                ((net.blupillcosby.villagerbreedingenhancements.access.InLoveVillager)body).setInLoveTicks(0);
                ((net.blupillcosby.villagerbreedingenhancements.access.InLoveVillager)target).setInLoveTicks(0);

                body.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
                target.getBrain().eraseMemory(MemoryModuleType.BREED_TARGET);
            }
        }
    }

    @Inject(method = "start", at = @At("HEAD"), cancellable = true)
    protected void onStart(final ServerLevel level, final Villager body, final long timestamp, CallbackInfo ci) {
        if (!VillagerBreedingEnhancements.CONFIG.requireBeds.get()) {
            AgeableMob breedTarget = body.getBrain().getMemory(MemoryModuleType.BREED_TARGET).get();
            BehaviorUtils.lockGazeAndWalkToEachOther(body, breedTarget, 0.5F, 2);
            level.broadcastEntityEvent(breedTarget, (byte) 18);
            level.broadcastEntityEvent(body, (byte) 18);
            int duration = 60; // 3 seconds matching AnimalMakeLove
            this.birthTimestamp = timestamp + duration;
            ci.cancel();
        }
    }
}
