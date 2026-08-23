package team.creative.playerrevive.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Identifier;

@Mixin(GameRenderer.class)
public interface GameRendererAccessor {

    @Invoker("loadPostProcessor")
    void invokeLoadPostProcessor(Identifier id);

    @Accessor("postProcessorEnabled")
    void setPostProcessorEnabled(boolean value);

    @Accessor("postProcessorEnabled")
    boolean getPostProcessorEnabled();

}
