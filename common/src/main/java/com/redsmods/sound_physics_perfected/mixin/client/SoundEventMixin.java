package com.redsmods.sound_physics_perfected.mixin.client;

import com.redsmods.sound_physics_perfected.Config;
import net.minecraft.sound.SoundEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(SoundEvent.class)
public class SoundEventMixin {

    private static final float SOUND_DISTANCE_MULTI = Config.getInstance().SoundMult;

    @ModifyConstant(method = "getDistanceToTravel", constant = @Constant(floatValue = 16F), expect = 2)
    private float allowance1(float value) {
        return value * SOUND_DISTANCE_MULTI;
    }
}