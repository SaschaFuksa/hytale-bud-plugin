# Plan: Spawn-Position vor dem Charakter + Content-Versionierung (Prompts & Bud-YAMLs)

Zwei unabhängige Features, getrennt umsetzbar/testbar.

## Teil A — Buds vor dem Charakter spawnen

### Ist-Zustand

`BudManager.getPlayerPositionWithOffset()`: zufälliger Offset ±3 Blöcke in X/Z um den Spieler, Y+0.5, bis zu 8 Versuche (`MAX_SPAWN_POSITION_ATTEMPTS`), geprüft über `isSpawnPositionFree()` — Block an Zielposition **und** der Block darüber müssen `BlockType.EMPTY_ID` sein. Fallback, falls alle Versuche scheitern: exakt auf der Spielerposition spawnen. Genutzt sowohl beim Bud-Spawn (`BudCreationHandler.spawnBud`) als auch beim Bud-Teleport zurück zum Spieler (`TeleportHandler.teleportBud`). Die Blockiert-Prüfung, die du meintest, gibt's also schon — nur die Blickrichtung des Spielers fließt aktuell gar nicht ein.

### Ziel

Bevorzugt vor dem Spieler spawnen, wenn dort Platz ist; sonst unverändert wie heute (Random-Offset-Suche) als Fallback. Kein bestehendes Verhalten wird schlechter.

### Umsetzung

1. Spieler-Rotation lesen: `TransformComponent` des Spielers holen (gleiches Muster wie `TeleportHandler` es fürs Bud macht, hier für den Spieler-`Ref`), `transform.getRotation()` liefert yaw/pitch (`com.hypixel.hytale.math.vector.Rotation3f`). Vorwärtsvektor darüber ableiten — `com.hypixel.hytale.math.vector.Vector3dUtil` hat laut Bytecode ein `FORWARD`-Feld und `setYawPitch(...)`, die exakte Methode/Signatur bei der Umsetzung über IDE/`javap` verifizieren statt zu raten (gleiches Vorgehen wie beim Null-Safety-Fix).
2. Neue Methode `BudManager.getSpawnPositionInFrontOfPlayer(PlayerRef, int index, int total)`: Zielpunkt = Spielerposition + Vorwärtsvektor × Abstand, Abstand absteigend `3 → 2 → 1` Blöcke probiert (je mit `isSpawnPositionFree`-Check), bei `total > 1` zusätzlich seitlich um `index` versetzt (Fächer nebeneinander, quer zur Blickrichtung, z. B. ±1 Block pro Index-Schritt von der Mitte aus) — sonst spawnen bei mehreren Buds alle exakt übereinander, weil `isSpawnPositionFree` nur Blocktypen prüft, keine bereits dort stehenden Entities.
3. `getPlayerPositionWithOffset` wird zur Fallback-Stufe: erst `getSpawnPositionInFrontOfPlayer` (alle Distanz-Stufen) versuchen, bei Misserfolg auf die bestehende Random-Suche zurückfallen.
4. `BudCreationHandler.spawnBud()` und `TeleportHandler.teleportBud()` auf die neue Methode umstellen (beide rufen aktuell `getPlayerPositionWithOffset` direkt auf); Bud-Erstellung übergibt Index/Total bei Mehrfach-Spawn (Reihenfolge aus `event.budTypes()`), Teleport nutzt `index=0, total=1`.
5. Abstand-Stufen/Fächer-Versatz als benannte Konstanten in `BudManager`, analog zu `MAX_SPAWN_POSITION_ATTEMPTS` — kein neuer Config-Eintrag nötig, außer ihr wollt das später Server-seitig einstellbar machen (dann gleiches Codec-Pattern wie in jeder anderen Config-Klasse).

### Entscheidungen (2026-08-09)

- Abstand vor dem Spieler: **max. 3 Blöcke**. Umsetzung: bei Punkt 2 nicht nur einen fixen Abstand probieren, sondern absteigend 3 → 2 → 1 Blöcke vor dem Spieler versuchen (jeweils mit `isSpawnPositionFree`-Check), erst danach auf die bestehende Random-Suche zurückfallen — so wird eng bebautes Terrain vor dem Spieler nicht sofort komplett aufgegeben, sondern erst näher rangerückt.
- Mehrere Buds gleichzeitig: **nebeneinander auffächern** (nicht hintereinander gestaffelt).

---

## Teil B — Versionierung + Update-Hinweis für Prompts & Bud-YAMLs

### Ist-Zustand

`LLMPromptManager` und `BudRegistry` kopieren gepackte Defaults nur, wenn die Runtime-Datei **fehlt** (`Files.exists`-Check). Ein geändertes gepacktes YAML wird nie automatisch nachgezogen — nur per explizitem, destruktivem `--reset` (überschreibt jede eigene Anpassung). Keine Versionsnummer irgendwo vorhanden.

### Ziel (wie beschrieben)

Erkennen, wenn die Runtime-Kopie älter ist als die im Plugin gepackte Version. Lokal/Dev: optional automatisch neu laden. Live-Server: mindestens im Log sichtbar, idealerweise nochmal beim ersten Spieler-Login nach Start als Reminder.

### Umsetzung

1. Kleine Versions-YAML pro Content-Bereich, gepackt und wie jede andere Ressource kopiert:
   - `src/main/resources/prompts/version.yml` → `version: 1`
   - `src/main/resources/buds/version.yml` → `version: 1`
   Eine kleine `ContentVersion`-Klasse (`extends AbstractYamlMessage`, ein `int version`-Feld), für beide Bereiche wiederverwendbar.
2. Beim Laden (`LLMPromptManager.loadPrompts()`, `BudRegistry.loadAll()`) zusätzlich zur bestehenden "kopiere wenn fehlt"-Logik:
   - Gepackte Version direkt aus dem Jar lesen (`BudPlugin.class.getResourceAsStream("/prompts/version.yml")` bzw. `/buds/version.yml`) — das ist die Soll-Version, unabhängig vom Datenordner.
   - Runtime-Version aus `<dataDir>/.../version.yml` lesen, falls vorhanden.
   - Runtime < gepackt → Mismatch.
3. Bei Mismatch immer: `LoggerUtil.warning(...)` beim Plugin-Start, z. B. `"[BUD] Prompt-Inhalte veraltet (Runtime v1, gepackt v2). '/bud prompt --reset' zum Aktualisieren (überschreibt eigene Anpassungen!)."` — analog für Buds, dafür wird aber erst ein `--reset`-Äquivalent für die Bud-Registry gebraucht (siehe Punkt 5).
4. Beim ersten Spieler-Login nach Start (nicht bei jedem einzelnen Login, sonst Log-Spam) einmalig einen kurzen Reminder loggen, falls beim Start ein Mismatch erkannt wurde — deckt den Fall ab, dass ein Admin die Startup-Logs übersieht, aber bei "Spieler online" nochmal reinschaut.
5. Auto-Reload nur hinter einem neuen, standardmäßig **deaktivierten** Flag (Default `false`, damit Live-Server nie ungefragt überschreiben) — z. B. `DebugConfig.AutoUpdateContentOnVersionMismatch`, gleiches Codec-Pattern wie die anderen Flags in `DebugConfig`. Bei `true`: beim Start automatisch das Äquivalent von `--reset` für den betroffenen Bereich ausführen; in README + CLAUDE.md klar als "für lokale Entwicklung, überschreibt Anpassungen" kennzeichnen.
6. Bewusst nicht eingeplant (Scope-Cut, nur falls gewünscht): ein "nur automatisch updaten, wenn die Datei seit dem letzten Sync unangetastet blieb"-Diffing per Checksumme — sauberer als pauschales Auto-Reset, aber mehr Aufwand als beschrieben. Sag Bescheid, falls das relevant ist, dann kommt's mit rein.

### Neu nötig

- `/bud reload buds [--reset]`-Subcommand, analog zu `/bud prompt [--reset]`, aber für `BudRegistry` statt `LLMPromptManager` — gibt's aktuell nicht, wird für Punkt 3/5 gebraucht.

### Pflege-Regel

Analog zum bestehenden Changelog-Prozess: `prompts/version.yml` / `buds/version.yml` hochzählen, wann immer sich gepackter Inhalt dieser YAMLs inhaltlich ändert. Als Regel in `CLAUDE.md` unter "Versioning / changelog" ergänzen, sonst vergisst man's beim nächsten Prompt-Tweak.
