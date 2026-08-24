# Spawn vor dem Spieler + Content-Versionierung

Quelle: `TODO-spawn-and-versioning.md` (abgeschlossen, gelöscht 2026-08-19)

## Was gemacht wurde

**Phase 7:** Buds spawnen vor dem Spieler statt an zufälliger Position. `BudManager.getSpawnPositionInFrontOfPlayer` probiert Abstand 3→2→1, mehrere gleichzeitig gespawnte Buds fächern seitlich auf (`FRONT_SPAWN_FAN_SPACING`), Fallback auf die alte Zufallssuche wenn kein Platz.

**Phase 8:** Eine gemeinsame `versions.yml` (`promptVersion`/`budVersion` + `excludedPrompts`/`excludedBuds`) vergleicht gepackten gegen Runtime-Content und warnt bei Drift. `DebugConfig.AutoUpdateContentOnVersionMismatch` aktualisiert automatisch, `/bud reload buds [--reset]` manuell.

## Stolpersteine

- **Version-Datei im gescannten Ordner.** `buds/version.yml` lag im selben Verzeichnis, das `BudRegistry.loadDefinitions()` nach Bud-YAMLs scannt → wurde als kaputte Bud-Definition geparst. Strukturell gelöst durch eine gemeinsame `versions.yml` auf Root-Ebene statt nur den Scan-Filter zu erweitern.
- **Auto-Update persistierte die neue Version nicht** — der Mismatch wurde bei jedem Start erneut erkannt und der Content redundant synchronisiert.
- **Der teuerste Bug:** `ensurePackagedCopy(..., true)` überschrieb die **gesamte** `versions.yml`. Da `LLMPromptManager` vor `BudRegistry` bootet, war `budVersion` schon auf den gepackten Stand gesetzt, bevor `BudRegistry` überhaupt prüfte — Bud-Updates wurden still übersprungen, und ein reines `/bud prompt --reset` setzte nebenbei `budVersion` mit zurück. Fix: `persistBudVersion`/`persistPromptVersion` ändern nur ihr eigenes Feld.

## Was geholfen hat

Bytecode-Verifikation statt Namensraten: `PlayerRef.getTransform()` liefert `Transform` (Werttyp), nicht `TransformComponent` (ECS). Dass `yaw()` Radiant ist, wurde über `Rotation3f.getQuaternion` gegen JOMLs radiantenbasiertes `rotationYXZ` bestätigt — nicht angenommen.

## Erkenntnisse

- Bei geteiltem Zustand zwischen zwei Managern zählt die **Boot-Reihenfolge**. Wer die ganze Datei schreibt, zerstört das Feld des anderen.
- Zwei Konfigurationsdateien für dieselbe Sache sind eine zu viel — `auto-update-exclude.yml` wurde wieder in `versions.yml` gefaltet.
- Ein Feature gilt erst nach echtem Serverlauf als fertig: die Bud-Seite war statisch plausibel und funktionierte trotzdem nicht.
