package com.redsmods.sound_physics_perfected.wrappers;

import com.redsmods.sound_physics_perfected.RaycastingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.*;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import static com.redsmods.sound_physics_perfected.RaycastingHelper.TICK_RATE;

public class RedTickableInstance implements TickableSoundInstance {
    private final Identifier soundID;
    private final Sound sound;
    private final SoundCategory category;
    private SoundInstance wrapped;
    private double x;
    private double y;
    private double z;
    private boolean done;
    private float volume;
    private float pitch;
    private int tickCount;
    private Vec3d targetPosition;
    private float targetVolume;
    private boolean wasWrappedDone = false; // Track previous state

    public RedTickableInstance(Identifier soundID, Sound sound, SoundCategory category, Vec3d position, float volume, float pitch, SoundInstance wrapped) {
        this.soundID = soundID;
        this.sound = sound;
        this.category = category;
        this.x = position.x;
        this.y = position.y;
        this.z = position.z;
        this.done = false;
        this.volume = volume;
        this.pitch = pitch;
        this.wrapped = wrapped;
        tickCount = 0;
        targetPosition = position;
        targetVolume = volume;
    }

    @Override
    public boolean isDone() {
        return this.done;
    }

    @Override
    public void tick() {
        // First, tick the wrapped sound if it's tickable
        if (wrapped instanceof TickableSoundInstance) {
            TickableSoundInstance tickableWrapped = (TickableSoundInstance) wrapped;
            tickableWrapped.tick();

            boolean wrappedDone = tickableWrapped.isDone();
            float wrappedVolume = wrapped.getVolume();

            // Debug logging
            System.out.println("Wrapped isDone: " + wrappedDone + ", Volume: " + wrappedVolume + ", Our done: " + this.done);

            // If wrapped sound just became done, mark ourselves as done
            if (wrappedDone && !wasWrappedDone) {
                System.out.println("Wrapped sound is done, stopping our wrapper");
                this.done = true;
                wasWrappedDone = true;
                return;
            }
            wasWrappedDone = wrappedDone;

            // Also check if wrapped volume is 0 (another indication it should stop)
            if (wrappedVolume <= 0.0f && !this.done) {
                System.out.println("Wrapped volume is 0, stopping our wrapper");
                this.done = true;
                return;
            }
        }

        // If we're done, don't continue processing
        if (this.done) return;

        tickCount++;
        if (TICK_RATE == 0) return;
        if (tickCount % TICK_RATE == 0)
            RaycastingHelper.tickQueue.add(this);
        updatePos();
        updateVolume();
    }

    @Override
    public Identifier getId() {
        return soundID;
    }

    @Override
    public @Nullable WeightedSoundSet getSoundSet(SoundManager soundManager) {
        return wrapped.getSoundSet(soundManager);
    }

    @Override
    public Sound getSound() {
        return sound;
    }

    @Override
    public SoundCategory getCategory() {
        return category;
    }

    @Override
    public boolean isRepeatable() {
        return wrapped.isRepeatable();
    }

    public Vec3d getOriginalPosition() {
        return new Vec3d(wrapped.getX(),wrapped.getY(),wrapped.getZ());
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getZ() {
        return z;
    }

    @Override
    public boolean shouldAlwaysPlay() {
        return wrapped.shouldAlwaysPlay();
    }

    @Override
    public boolean isRelative() {
        return wrapped.isRelative();
    }

    @Override
    public int getRepeatDelay() {
        return wrapped.getRepeatDelay();
    }

    @Override
    public float getVolume() {
        return volume;
    }

    @Override
    public float getPitch() {
        return pitch;
    }

    @Override
    public AttenuationType getAttenuationType() {
        return AttenuationType.LINEAR;
    }

    public void stop() {
        this.done = true;
    }

    public void setPos(Vec3d targetPosition) {
        this.targetPosition = targetPosition;
    }

    public void updatePos() {
        // Calculate the direction vector to the target
        double deltaX = targetPosition.getX() - x;
        double deltaY = targetPosition.getY() - y;
        double deltaZ = targetPosition.getZ() - z;

        // Calculate the distance to the target
        double distance = Math.sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ);

        // If we're already at the target or very close, set position directly
        if (distance <= 0.001) {
            x = targetPosition.getX();
            y = targetPosition.getY();
            z = targetPosition.getZ();
            return;
        }

        // Maximum speed in blocks per tick
        double maxSpeed = 17.15; // m per tick, speed of sound

        // Calculate how far we can move this tick
        double moveDistance = Math.min(maxSpeed, distance);

        // Normalize the direction vector and scale by move distance
        double moveX = (deltaX / distance) * moveDistance;
        double moveY = (deltaY / distance) * moveDistance;
        double moveZ = (deltaZ / distance) * moveDistance;

        // Update position
        x += moveX;
        y += moveY;
        z += moveZ;
    }

    public void setVolume(float targetVolume) {
        if (!this.soundID.toString().contains("rain")) {
            this.targetVolume = targetVolume;
        }
    }

    public void updateVolume() {
        // Calculate the difference between current and target volume
        float deltaVolume = targetVolume - volume;

        // If we're already at the target or very close, set volume directly
        if (Math.abs(deltaVolume) <= 0.001f) {
            volume = targetVolume;
            return;
        }

        // Maximum volume change per tick
        float maxVolumeChange = 0.05f * TICK_RATE;

        // Calculate how much we can change this tick
        float volumeChange = Math.min(maxVolumeChange, Math.abs(deltaVolume));

        // Apply the change in the correct direction
        if (deltaVolume > 0) {
            volume += volumeChange;
        } else {
            volume -= volumeChange;
        }
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public float getOriginalVolume() {
        return wrapped.getVolume();
    }
}