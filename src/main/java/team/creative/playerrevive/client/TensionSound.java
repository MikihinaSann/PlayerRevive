package team.creative.playerrevive.client;

import net.minecraft.client.sound.AbstractSoundInstance;
import net.minecraft.client.sound.TickableSoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

public class TensionSound extends AbstractSoundInstance implements TickableSoundInstance {

    private boolean stopped;

    public TensionSound(Identifier resource, float volume, float pitch, boolean loop) {
        super(resource, SoundCategory.PLAYERS, Random.create());
        this.repeat = loop;
        this.volume = volume;
        this.pitch = pitch;
    }

    @Override
    public boolean isDone() {
        return this.stopped;
    }

    protected final void stop() {
        this.stopped = true;
        this.repeat = false;
    }

    @Override
    public void tick() {}

}
