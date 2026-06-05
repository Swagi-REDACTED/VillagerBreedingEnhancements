package net.blupillcosby.villagerbreedingenhancements.mixin;

import net.blupillcosby.villagerbreedingenhancements.access.InLoveVillager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.blupillcosby.villagerbreedingenhancements.VillagerBreedingEnhancements;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(Villager.class)
public abstract class VillagerMixin extends AbstractVillager implements InLoveVillager {

    @Unique
    private int inLoveTicks = 0;

    @Shadow
    private int foodLevel;


    public VillagerMixin(EntityType<? extends AbstractVillager> entityType, Level level) {
        super(entityType, level);
    }

    @Override
    public int getInLoveTicks() {
        return this.inLoveTicks;
    }

    @Override
    public void setInLoveTicks(int ticks) {
        this.inLoveTicks = ticks;
    }

    @Override
    public void setInLove(ServerLevel level) {
        if (this.inLoveTicks <= 0) {
            this.inLoveTicks = 600; // 30 seconds
            level.broadcastEntityEvent(this, (byte) 12);
        }
    }

    /**
     * @author blupillcosby
     * @reason Universal food support with 1.6x multiplier
     */
    @Overwrite
    private int countFoodPointsInInventory() {
        SimpleContainer inventory = this.getInventory();
        int total = 0;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.has(DataComponents.FOOD)) {
                int nutrition = stack.get(DataComponents.FOOD).nutrition();
                int value = Math.round(nutrition * 0.8F);
                if (value >= 1) {
                    total += stack.getCount() * value;
                }
            }
        }
        return total;
    }

    /**
     * @author blupillcosby
     * @reason Universal food support with 1.6x multiplier
     */
    @Overwrite
    private void eatUntilFull() {
        if (this.foodLevel < 12 && this.countFoodPointsInInventory() != 0) {
            for (int slot = 0; slot < this.getInventory().getContainerSize(); slot++) {
                ItemStack itemStack = this.getInventory().getItem(slot);
                if (!itemStack.isEmpty() && itemStack.has(DataComponents.FOOD)) {
                    int nutrition = itemStack.get(DataComponents.FOOD).nutrition();
                    int value = Math.round(nutrition * 0.8F);
                    if (value >= 1) {
                        int itemCount = itemStack.getCount();

                        for (int count = itemCount; count > 0; count--) {
                            this.foodLevel = this.foodLevel + value;
                            this.getInventory().removeItem(slot, 1);
                            if (this.foodLevel >= 12) {
                                return;
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * @author blupillcosby
     * @reason Universal food support
     */
    @Overwrite
    public boolean hasExcessFood() {
        return this.countFoodPointsInInventory() >= 24;
    }

    /**
     * @author blupillcosby
     * @reason Universal food support
     */
    @Overwrite
    public boolean wantsMoreFood() {
        return this.countFoodPointsInInventory() < 12;
    }

    @Inject(method = "wantsToPickUp", at = @At("HEAD"), cancellable = true)
    private void onWantsToPickUp(final ServerLevel level, final ItemStack itemStack, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
        if (itemStack.has(DataComponents.FOOD)) {
            if (this.isBaby() || this.getAge() > 0 || this.inLoveTicks > 0) {
                cir.setReturnValue(false); // Baby, cooldown lock, or already in love
            } else {
                int nutrition = itemStack.get(DataComponents.FOOD).nutrition();
                int value = Math.round(nutrition * 0.8F);
                if (value >= 1) {
                    cir.setReturnValue(this.getInventory().canAddItem(itemStack));
                }
            }
        }
    }

    @Inject(method = "pickUpItem", at = @At("TAIL"))
    private void onPickUpItem(ServerLevel level, ItemEntity entity, CallbackInfo ci) {
        if (!VillagerBreedingEnhancements.CONFIG.requireBeds.get() && this.getAge() == 0 && this.foodLevel + this.countFoodPointsInInventory() >= 12) {
            this.setInLove(level);
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(CallbackInfo ci) {
        if (VillagerBreedingEnhancements.CONFIG.requireBeds.get()) return;

        if (this.inLoveTicks > 0) {
            this.inLoveTicks--;
            if (this.inLoveTicks % 10 == 0) {
                if (this.level() instanceof ServerLevel serverLvl) {
                    serverLvl.broadcastEntityEvent(this, (byte) 12);
                }
            }

            if (this.inLoveTicks % 20 == 0 && this.level() instanceof ServerLevel serverLevel) {
                Villager me = (Villager)(Object)this;
                Brain<Villager> brain = me.getBrain();
                if (!brain.hasMemoryValue(MemoryModuleType.BREED_TARGET)) {
                    List<Villager> near = serverLevel.getEntitiesOfClass(Villager.class, me.getBoundingBox().inflate(8.0D), 
                        v -> v != me && v.getAge() == 0 && ((InLoveVillager)v).getInLoveTicks() > 0 && !v.getBrain().hasMemoryValue(MemoryModuleType.BREED_TARGET));
                    
                    if (!near.isEmpty()) {
                        Villager target = near.get(0);
                        brain.setMemory(MemoryModuleType.BREED_TARGET, target);
                        target.getBrain().setMemory(MemoryModuleType.BREED_TARGET, me);
                    }
                }
            }
        }
    }

    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void failsafeTick(CallbackInfo ci) {
        // Failsafe: If the global config cooldown is 0, forcefully remove any existing breeding cooldowns from adults
        if (this.getAge() > 0 && net.blupillcosby.villagerbreedingenhancements.VillagerBreedingEnhancements.CONFIG.cooldownTimer.get() == 0) {
            this.setAge(0);
        }
    }
}
