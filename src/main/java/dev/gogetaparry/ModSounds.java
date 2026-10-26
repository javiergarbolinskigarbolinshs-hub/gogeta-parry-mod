package dev.gogetaparry;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Die zwei Stimmen-Plaetze der Mod:
 *  - voice_parry: beim Parry   -> Datei assets/gogetaparry/sounds/voice/parry.ogg
 *  - voice_kick:  beim Kick    -> Datei assets/gogetaparry/sounds/voice/kick.ogg
 * Einfach die .ogg-Dateien austauschen, um eine eigene Stimme einzubauen.
 */
public final class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GogetaParry.MODID);

    public static final RegistryObject<SoundEvent> VOICE_PARRY = register("voice_parry");
    public static final RegistryObject<SoundEvent> VOICE_KICK = register("voice_kick");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name,
                () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GogetaParry.MODID, name)));
    }

    private ModSounds() {
    }
}
