package dev.gogetaparry;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * Client-Einstellungen: .minecraft/config/gogetaparry-client.toml
 * Die Position kann man auch im Spiel mit der Taste H umschalten.
 */
public final class ClientConfig {

    public enum HudPosition {
        /** Unten links neben der Hotbar, wie in Dragon Ball Legends. */
        HOTBAR,
        /** Oben rechts (rutscht unter die Trank-Effekte, falls welche aktiv sind). */
        TOP_RIGHT,
        /** Klein ums Fadenkreuz. */
        CROSSHAIR;

        public HudPosition next() {
            HudPosition[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.EnumValue<HudPosition> HUD_POSITION;
    public static final ForgeConfigSpec.BooleanValue CINEMATIC_CAMERA;

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.push("hud");
        HUD_POSITION = b
                .comment("Wo die Parry-Anzeige ist: HOTBAR (unten, wie in Legends), TOP_RIGHT oder CROSSHAIR.")
                .defineEnum("gaugePosition", HudPosition.HOTBAR);
        CINEMATIC_CAMERA = b
                .comment("true = beim Parry und Konter schaltet die Kamera kurz in die Dritte Person (wie in Legends).")
                .define("cinematicCamera", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {
    }
}
