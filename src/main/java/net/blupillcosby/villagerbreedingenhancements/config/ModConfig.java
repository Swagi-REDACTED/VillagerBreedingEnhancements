package net.blupillcosby.villagerbreedingenhancements.config;

import me.fzzyhmstrs.fzzy_config.annotations.Version;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import me.fzzyhmstrs.fzzy_config.validation.number.ValidatedInt;
import net.minecraft.resources.Identifier;

@Version(version = 1)
public class ModConfig extends Config {

    public ValidatedBoolean requireBeds = new ValidatedBoolean(false);
    public ValidatedInt cooldownTimer = new ValidatedInt(5, 10, 0);
    public ValidatedInt breedSuccessChance = new ValidatedInt(100, 100, 0);
    public ValidatedInt breedFailChance = new ValidatedInt(0, 100, 0);
    public ValidatedInt chanceToTryAgainOnFailure = new ValidatedInt(50, 100, 0);

    private boolean syncing = false;

    public ModConfig() {
        super(Identifier.fromNamespaceAndPath("villagerbreedingenhancements", "main"));
        
        this.breedSuccessChance.listenToEntry(entry -> {
            if (!syncing) {
                syncing = true;
                this.breedFailChance.validateAndSet(100 - entry.get());
                syncing = false;
            }
        });
        
        this.breedFailChance.listenToEntry(entry -> {
            if (!syncing) {
                syncing = true;
                this.breedSuccessChance.validateAndSet(100 - entry.get());
                syncing = false;
            }
        });
    }

    @Override
    public int defaultPermLevel() {
        return 2;
    }
}
