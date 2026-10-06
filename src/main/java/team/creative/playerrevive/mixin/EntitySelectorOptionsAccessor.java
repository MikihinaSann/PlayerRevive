package team.creative.playerrevive.mixin;

import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.command.EntitySelectorOptions;
import net.minecraft.command.EntitySelectorReader;
import net.minecraft.text.Text;

@Mixin(EntitySelectorOptions.class)
public interface EntitySelectorOptionsAccessor {

    @Invoker("putOption")
    static void putOption(String name, EntitySelectorOptions.SelectorHandler handler, Predicate<EntitySelectorReader> condition, Text description) {
        throw new AssertionError();
    }
}
