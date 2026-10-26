# Gogeta Parry – Forge 1.20.1

Inspiriert vom Gogeta-Parry aus Dragon Ball Legends.

## So funktioniert's im Spiel
1. **Fusions-Amulett** craften und anlegen:
   - **mit Curios API** (empfohlen): Inventar öffnen (E) → links neben dem Spieler den **Halsketten-Platz**
     öffnen (kleiner Knopf am Inventar bzw. Taste G) → Amulett reinlegen. Nur dort wirkt es.
   - **ohne Curios**: einfach irgendwo ins Inventar legen.
   **So bekommst du es:** Zuerst einen **Ki-Kristall** finden – den gibt es nur in **Truhen**:
   Dungeons (Räume mit Monster-Spawner) und Netherfestungen (je ~15 %), Bastionen
   (Schatzkammer ~30 %, andere Bastion-Truhen ~10 %). Dann craften:
   **Ki-Kristall in der Mitte, Gold oben/unten/links/rechts, Lapis in den Ecken.**
   Im Kreativmodus: Amulett im Tab „Kampf", Kristall im Tab „Zutaten".
2. Neben der Hotbar erscheint der **Counter-Kreis wie in Dragon Ball Legends**: schwarzer Zacken-Kreis
   im flachen Spiel-Look mit grauem Ring und Symbol (Splitter + abgelenkter Pfeil) – Zacken und Maße aus dem Spiel übernommen.
   Mit **H** schaltest du die Position um: unten bei der Hotbar → oben rechts → klein am Fadenkreuz.
   Der Kreis ist animiert: wackelt bei Treffern (Funke an der Ladung), atmet und sendet Wellen, wenn er voll ist,
   zerspringt mit weißem Blitz beim Counter, macht „Ping“, wenn er wieder lädt, und sprüht Ki-Funken beim Power-Up.
3. Jeder Treffer von einem Mob (Zombie-Schlag, Skelett-Pfeil, Blaze-Feuerball …) lädt den grauen Ring
   ein Stück blau auf – nach **5 Treffern** ist er voll. Schaden ohne Angreifer (Fallen, Explosionen, Feuer …)
   lädt einen halben Treffer, höchstens einmal pro Sekunde. Auch gelbe Extra-Herzen (goldener Apfel) zählen.
4. Ist der Ring voll: Druckwelle, blaues Leuchten, knisternde Blitze, kreisende Funken,
   das Symbol wird weiß und die Zacken drehen sich = Counter bereit.
   Dabei bekommst du **5 Herzen zurück** (wie Gogetas Unique Gauge) – grüne „+5 ❤“ am Kreis.
5. Der **nächste Angriff wird automatisch geparryt** (wie Gogeta in Dragon Ball Legends):
   „Fyuu" – du drehst dich blitzschnell zum Angreifer, deine freie Hand schnellt nach vorne,
   aus der Handfläche explodiert ein **greller blauer Ki-Blitz** und eine **gebogene Ki-Welle**
   schwappt nach vorne. Der Angriff löst sich auf – 0 Schaden. Monster in der Nähe werden weggestoßen.
   Andere Spieler sehen Hand und Welle auch. Die Kamera zeigt dich dabei von vorne
   (abschaltbar: `cinematicCamera` in `gogetaparry-client.toml`).
6. Danach der **Konter-Kick** wie im Video: Hocke → Absprung mit Speed-Lines → Drehung in der Luft →
   **fliegender Kick** (40 Schaden – das überlebt kein Zombie oder Skelett), der dich noch mal um 10 % heilt. Beim Treffer wird der Bildschirm
   kurz dunkel mit weißen Schlag-Streifen, dann ein oranger Knall und der Gegner fliegt weg.
   Die Kamera bleibt dabei vorne, damit du dich und den Gegner siehst.
   **Fernkampf-Angriffe** (Skelett-Pfeil, Blaze-/Ghast-Feuerball …) kontert er anders: **Ki-Blast**!
   Hände tief zusammen, eine blaue Ki-Kugel wächst, dann beide Arme vor – die Kugel fliegt mit Leuchtspur
   zum Schützen und explodiert (40 Schaden). Die Kamera schaut dabei von hinten über die Schulter.
7. Nach dem Kick bzw. Ki-Blast: **Power-Up** – 10 Sekunden lang **30 % mehr Schaden** und **10 % schneller** mit blauer Ki-Aura (Anzeige unter dem Kreis).
   Dann **20 Sekunden Abklingzeit** (der Ring läuft grau voll), damit der Counter nicht übermächtig ist.

Der Kreis bleibt geladen, bis du ihn benutzt (auch beim Essen oder wenn du lange nicht getroffen wirst).

## Eigene Stimme
Beim Parry und beim Kick spielt die Mod einen Ki-Schrei ab (`sounds/voice/parry.ogg` und `kick.ogg`).
Eigene Stimme einbauen: Aufnahme als `stimme_parry.mp3` bzw. `stimme_kick.mp3` (auch .m4a, .wav, .ogg …)
in den Downloads-Ordner legen und `gogeta-installieren.sh` starten – maximal 3 Sekunden.
Abschalten: `voice = false` in `gogetaparry-common.toml`.

## Mod bauen (einmalig)
1. **JDK 17** installieren (z. B. Eclipse Temurin 17 von adoptium.net). Java 17 ist Pflicht für 1.20.1.
2. Diesen Ordner entpacken.
3. Im Ordner eine Konsole öffnen (Windows: in die Adresszeile `cmd` tippen, Enter) und ausführen:
   - Windows: `gradlew build`
   - Mac/Linux: `./gradlew build`
4. Beim ersten Mal lädt Gradle Minecraft + Forge herunter, das dauert ein paar Minuten.
5. Fertige Mod: `build/libs/gogetaparry-1.20.1-2.4.1.jar`
6. Die .jar in deinen `.minecraft/mods`-Ordner kopieren und mit **Forge 1.20.1** starten.

Direkt testen ohne Launcher: `gradlew runClient` startet Minecraft mit der Mod.

## Einstellungen
Anzeige-Position: Taste **H** (änderbar unter Optionen → Steuerung) oder `.minecraft/config/gogetaparry-client.toml`.

Spielwerte nach dem ersten Start: `.minecraft/config/gogetaparry-common.toml`
- `requireAmulet` – auf `false` stellen, wenn der Parry immer aktiv sein soll
- `kiCrystalChestChancePercent` – Chance auf einen Ki-Kristall in Dungeon-/Netherfestungs-Truhen (Standard 15)
- `hitsToFill` – Treffer bis der Kreis voll ist (Standard 5)
- `environmentCharge` – wie viel Fallen/Explosionen laden (Standard 0.5 = halber Treffer)
- `cooldownAfterCounterSeconds` (20), `powerUpTimeSeconds` (10), `powerUpDamagePercent` (30), `powerUpSpeedPercent` (10)
- `healOnFullHearts` (5) – Herzen zurück, wenn der Kreis voll ist
- `healOnCounterPercent` (10) – Heilung beim Treffer des Konters in Prozent der maximalen Leben
- `counterDamageTotal` – Schaden des Kicks (Standard 40 = Zombies/Skelette sofort besiegt)
- `shockwaveDamage`, `shockwaveRadius`,
  `faceAttacker` (zum Angreifer drehen), `teleportCounter`, `counterRange`

## Code-Übersicht
- `ParryEvents.java` – die ganze Logik (Aufladen, Parry, Welle, Konter-Kick)
- `ParryEffects.java` – Partikel und Sounds
- `client/ParryHud.java` – Counter-Kreis (Grafik: `textures/gui/counter_emblem.png`), blauer Blitz, Finisher-Impact
- `client/CinematicCamera.java` – Kamerafahrt beim Parry und Konter
- `client/ParryAnimations.java` – Parry-Hand und Kick-Animation (Hocke, Sprint, Drehung, Kick)
- `client/KiWaveRenderer.java` – Handflächen-Blitz und gebogene Ki-Welle
- `client/KiBlastRenderer.java` – Ki-Kugel des Fernkampf-Konters (Aufladen, Flug, Einschlag)
- `client/ClientForgeEvents.java` – Kamera-Wackeln, Taste H
- `ParryConfig.java` – Einstellungen
