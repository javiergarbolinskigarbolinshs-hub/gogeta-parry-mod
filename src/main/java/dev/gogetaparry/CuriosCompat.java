package dev.gogetaparry;

import net.minecraft.world.entity.player.Player;
import top.theillusivec4.curios.api.CuriosApi;

/**
 * Verbindung zur Curios API (extra Ausruestungs-Plaetze).
 * Wird NUR aufgerufen, wenn Curios installiert ist - sonst wird diese Klasse nie geladen.
 */
final class CuriosCompat {
    private CuriosCompat() {
    }

    /** Steckt das Fusions-Amulett im Halsketten-Platz? */
    @SuppressWarnings("deprecation")
    static boolean hasAmulet(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, ModItems.FUSION_AMULET.get()).isPresent();
    }
}
