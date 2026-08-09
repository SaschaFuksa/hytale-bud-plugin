# Bud-Konfiguration: Migrationsplan (BudType-Enum → Config-Registry)

## Stand (2026-08-08) — für Fortsetzung in neuer Session

**Phase 1 ist fertig und lokal build-verifiziert** (`.\gradlew clean build` → BUILD SUCCESSFUL):

- Neu: `com.bud.core.registry.{BudDefinition, BudSoundDefinition, BudRoster, BudRegistry}`
- Neu: `src/main/resources/buds/{veri,keyleth,gronkh}.yml` + `roster.yml` (Packaged Defaults, Daten 1:1 aus den bisherigen `*Profile`/`*Sound`-Klassen übernommen)
- `BudPlugin.setup()`: `BudRegistry.getInstance().reloadMissing()` ergänzt — rein additiv, nichts Bestehendes liest bisher daraus.
- Noch nicht gegen echten Serverstart verifiziert (Debug-Log `[BUD] --- BudRegistry Debug ---` beim `runServer` bestätigt den YAML-Parse inkl. verschachteltem `sounds`-Feld — SnakeYAML sollte das über `Constructor(Class)` automatisch auflösen, aber ungetestet in diesem Repo).

**Phase 2 — Scope-Korrektur vor Fortsetzung:**

`BudType` ist in 32 Dateien verwendet (Liste unten). `IBudProfile` ist zusätzlich in ~50 Dateien verwendet, weil es der Rückgabetyp von `IPromptContext.getBudProfile()` ist, das praktisch jede `LLM*MessageCreation`-Klasse (Combat/Craft/Discover/Item/State/Teleport/Weather/World/Chat/FavoriteDay/BudReaction/Dialog/PlayerState/...) implementiert.

**Revidierter Ansatz (statt "IBudProfile/IBudSound löschen" aus der ursprünglichen Phase 2):** Interfaces `IBudProfile` und `IBudSound` bleiben bestehen — sie sind an dieser Stelle nur ein stabiler Datenvertrag für den Rest der Pipeline. `BudDefinition implements IBudProfile` und `BudSoundDefinition implements IBudSound` werden als Adapter ergänzt. `BudRegistry` liefert diese Instanzen statt `BudProfileMapper`. Dadurch ändern sich die ~30 `LLM*MessageCreation`-Konsumenten **gar nicht** — nur die 32 `BudType`-Fundstellen (Identität/Persistenz/Commands/Card-Interaktionen) plus die Erzeugungsstelle der `IBudProfile`-Instanzen (`BudProfileMapper` → `BudRegistry`).

Betroffene 32 `BudType`-Dateien (Stand dieser Analyse): `BudManager`, `PlayerJoinSystem`, `PlayerBudComponent`, `BudReactionChainTracker`, `BudCreationHandler`, `MoodTracker`, `StateChangeQueue`, `VeriProfile`/`GronkhProfile`/`KeylethProfile` (→ löschen), `IBudProfile` (Import von `BudType` in Methodensignatur entfernen, sonst unverändert), `DialogModeTracker`, `LLMInteractionManager`, `MemoryCommand`, `BudCreationEvent`, `DeletionCommand`, `BudCreationEntry`, `CardVeriInteraction`/`CardKeylethInteraction`/`CardGronkhInteraction` (→ Phase 3, verschmelzen zu `CardBudInteraction`), `CleanupUtil`, `PlayerChatReactionHandler`, `DebugCommand`, `TeleportQueue`, `IQueueEntry`, `BudProfileMapper` (→ löschen, ersetzt durch `BudRegistry`), `BudDebugInfo`, `BudComponent`, `ResetCommand`, `CreationCommand`, `BudType.java` (→ löschen), `BudPlugin` (Interaction-Registrierung, Phase 3).

Nächster Schritt bei Fortsetzung: diese ~34 Dateien (32 + `IBudProfile`/`IBudSound` Adapter-Ergänzung) durchgehen, `BudType budType` → `String budId`, `BudProfileMapper.getInstance().getProfileForBudType(x)` → `BudRegistry.getInstance().get(x)`, `PlayerBudComponent`-Codec von `EnumCodec` auf `Codec.STRING` (siehe Phase 4 unten), dann Phase 3 (Card-Interaktionen), dann Phase 5 (Doku).

---

Ziel: Buds sind vollständig datengetrieben definiert (YAML außerhalb des ausgelieferten Plugin-Contents). Neue Buds = neue YAML-Datei + neue Assets, kein Java-Rebuild. `BudType` als Java-Enum entfällt komplett.

`BudType` wird aktuell in **32 Java-Dateien** referenziert — das ist der reale Umfang der Migration, nicht nur die in der letzten Analyse gefundenen Kernklassen.

---

## Phase 0 – Bestandsaufnahme (vor jeder Code-Änderung)

1. Vollständige Liste aller `BudType`-Verwendungsstellen erzeugen (`grep -rn "BudType" src/main/java`) und pro Datei klassifizieren: *Identität* (welcher Bud ist das), *Vergleich/Switch*, *Persistenz* (Codec).
2. Format-Frage aus dem `EnumCodec` (`reference/server/com/hypixel/hytale/codec/codecs/EnumCodec.class`) bereits geklärt, kein Ingame-Test nötig: `EnumCodec` delegiert vollständig an `StringCodec` — auf dem Wire steht so oder so nur ein String, nie ein Ordinal. Encode ruft `Enum.name()` auf (ggf. per `EnumStyle` in CamelCase umgeformt, per `detect()` automatisch anhand der Enum-Namen gewählt). Für `BudType.GRONKH/KEYLETH/VERI` (einfache Wörter ohne Unterstriche) ist der wahrscheinliche Wert `"GRONKH"/"KEYLETH"/"VERI"` — beim Umbau auf `Codec.STRING` bleibt das Bit-Format identisch, nur der Enum-Zwischenschritt entfällt. Einzige verbleibende Unsicherheit: ob `EnumStyle` LEGACY oder CAMEL_CASE gewählt wurde (wirkt sich nur auf Groß-/Kleinschreibung aus).
   Um diesen einen Punkt ohne neuen Command zu klären: ein einzeiliger `LoggerUtil.getLogger().fine(...)`-Log direkt in `PlayerJoinSystem` (dort wird `PlayerBudComponent` beim Join sowieso schon geladen), der `playerBudComponent.getBudTypes()` ausgibt. Danach wieder entfernen — kein dauerhafter Command nötig.

Damit lässt sich Phase 4 auch ohne belastbaren Ingame-Test entwerfen; der Log-Einzeiler ist nur die letzte Absicherung vor dem Rollout, kein Blocker für Phase 1–3.

---

## Phase 1 – Datenmodell

**`BudDefinition`** (Record/POJO, ersetzt `IBudProfile` + `IBudSound` + die drei `*Profile`-Klassen):

```
id: string              // "veri" – eindeutige, stabile ID
displayName: string      // "Veri"
color: string             // "#FFAA00"
npcTypeId: string          // "Veri_Bud" – muss zu einem registrierten NPC-Asset passen
weaponId: string
armorId: string
pronoun: enum (HE/SHE/THEY)
favoriteDay: enum (DayOfWeek)
sounds:
  defensive: string
  passive: string
  sitting: string
promptKey: string          // Referenz auf buds/<promptKey>.yml (bestehendes LLMPromptManager-System bleibt wie es ist)
```

`byState`-Map bewusst weggelassen: `BudState` hat aktuell genau drei Werte (`PET_DEFENSIVE`/`PET_PASSIVE`/`PET_SITTING`), und `IBudSound.getSoundForState()` ist schon heute nur ein `switch` über exakt diese drei, der auf `defensive`/`passive`/`sitting` durchreicht (siehe `VeriSound`). Eine offene Map wäre YAGNI — die drei Felder decken den vollständigen Zustandsraum ab. Sollte `BudState` später um Werte erweitert werden, kann man dann eine Map nachziehen; bis dahin unnötige Komplexität.

**`BudRegistry`** (Singleton, analog `LLMPromptManager`):

- lädt Packaged-Defaults aus `src/main/resources/buds/*.yml` (Veri/Keyleth/Gronkh) beim ersten Start in den Runtime-Datenordner, wie es `LLMPromptManager.copyDefaults()` heute schon für die Prompt-YAMLs macht.
- lädt zusätzlich *alle* `.yml`-Dateien im `buds/`-Unterordner des Runtime-Datenordners — eigene, nicht ausgelieferte Definitionen landen dort und werden nie von `--reset` überschrieben (nur die drei Packaged-Defaults sind reset-fähig, exakt wie beim Prompt-Reset).
- eigene Datei `buds/roster.yml` mit `default_buds: [veri, keyleth, gronkh]` — das ist die *aktive* Auswahl, getrennt von "welche Bud-Definitionen existieren überhaupt". So kann man mehr als 3 Buds definieren und trotzdem nur 3 aktiv anbieten.
  - Konkrete Bedeutung: `default_buds` ist genau die Liste, die `CreationCommand`s "alle erzeugen"-Fall (aktuell `Set.of(VERI, KEYLETH, GRONKH)` ohne Flag) verwendet — der Shorthand liest also künftig `BudRegistry.getDefaultBudIds()` statt einer hartkodierten Menge.
  - Validierung beim Laden: falls `default_buds` mehr als 3 IDs enthält, beim Start warnen (`LoggerUtil.getLogger().warning(...)`) statt es stillschweigend laufen zu lassen — sonst greift beim "alle erzeugen" irgendwann `PlayerBudComponent.addBud()`s `>= 3`-Grenze und der vierte Bud verschwindet kommentarlos.
  - Einzelne, nicht in `default_buds` gelistete Definitionen bleiben trotzdem über `/bud create <id>` gezielt erzeugbar — `default_buds` steuert nur den Shorthand, nicht die Verfügbarkeit einzelner Buds.
- `/bud prompt` bzw. ein neues `/bud reload buds` liest hinzugekommene Dateien ohne Neustart nach (gleiche Mechanik wie `reloadMissingPrompts()`).

---

## Phase 2 – `BudType`-Enum ersetzen

- `BudType.java` löschen. Wo bisher `BudType budType` stand, steht künftig `String budId` (kein neuer Wrapper-Typ nötig — Codecs im Repo arbeiten überall direkt mit `Codec.STRING`).
- `BudProfileMapper` löschen, ersetzen durch `BudRegistry.getInstance().get(budId)`.
- `IBudProfile`, `IBudSound`, `VeriProfile`/`GronkhProfile`/`KeylethProfile`, `VeriSound`/… löschen — die Felder wandern vollständig in `BudDefinition`.
- `PlayerBudComponent`: `Set<BudType> budTypes` → `Set<String> budIds`; Codec-Feld `SetCodec<>(new EnumCodec<>(BudType.class), …)` → `SetCodec<>(Codec.STRING, …)`. Die `>= 3`-Grenze in `addBud()` bleibt unverändert (das ist die "max. 3 gleichzeitig beschworen"-Regel und unabhängig von der Anzahl definierter Buds).
- Alle 32 Fundstellen durchgehen (`BudManager`, `BudComponent`, `BudDebugInfo`, `BudCreationHandler`, `BudCreationEvent`, `BudCreationEntry`, `BudReactionChainTracker`, `MoodTracker`, `DialogModeTracker`, `CleanupUtil`, `TeleportQueue`, `StateChangeQueue`, `PlayerChatReactionHandler`, `PlayerJoinSystem`, `LLMInteractionManager`, `IQueueEntry`, die Commands `CreationCommand`/`DeletionCommand`/`MemoryCommand`/`DebugCommand`/`ResetCommand`) — die meisten sind reine Signaturänderungen (`BudType` → `String`), keine Logikänderung, weil dort schon generisch mit dem Wert gearbeitet wird statt mit `switch`.
- `CreationCommand`: feste Flags (`--veri`, `--keyleth`, `--gronkh`) durch einen generischen String-Parameter ersetzen, Tab-Completion aus `BudRegistry.getIds()` befüllen. Ohne diesen Schritt bleibt die Enum-Kopplung an der Befehlsoberfläche bestehen, selbst wenn die Profile datengetrieben sind.

---

## Phase 3 – "Coin"/Card-Interaktion generalisieren

*Antwort auf deine Frage: kein struktureller Fallstrick, aber aktuell genau dasselbe Problem wie bei den Profilen — ein Java-Objekt pro Bud statt Daten.*

Ist-Zustand:

- Drei Klassen `CardVeriInteraction`/`CardKeylethInteraction`/`CardGronkhInteraction`, jede setzt ihren `BudType` fest im Konstruktor (`super("card_veri", BudType.VERI)`), keine der Codecs hat ein eigenes Datenfeld.
- Drei Registrierungen in `BudPlugin.setup()`: `getCodecRegistry(Interaction.CODEC).register("CardVeri", CardVeriInteraction.class, CODEC_CARD_VERI)` (analog für Gronkh/Keyleth).
- Jede Karte referenziert ihren Typ per String im Item-Interaction-JSON: `Server/Item/Interactions/CardVeri.json` → `"Type": "CardVeri"`.

Zielzustand:

- Eine konkrete Klasse `CardBudInteraction` (nicht mehr abstract), Codec um ein Feld erweitert: `new KeyedCodec<>("BudId", Codec.STRING)` an `SimpleInteraction.CODEC` angehängt — exakt das gleiche Builder-Pattern, das im Repo schon überall verwendet wird (siehe `PlayerBudComponent.CODEC`).
- Eine Registrierung: `register("CardBud", CardBudInteraction.class, CODEC_CARD_BUD)`.
- Item-JSONs bleiben pro Bud bestehen (jede Karte ist weiterhin ein eigenes Item mit eigenem Icon/Rezept/Textur), ändern sich aber zu:
  ```json
  { "Type": "CardBud", "BudId": "veri", "Effects": { "ItemAnimationId": "SwingLeft" } }
  ```
- Neuer Bud mit eigener Karte = neue `Server/Item/Items/Card<Id>.json` + `Server/Item/Interactions/Card<Id>.json` (mit `BudId`) + `Server/Item/RootInteractions/Card<Id>.json` + Textur/Icon-Assets. Keine neue Java-Klasse, keine neue Registrierung mehr nötig.

---

## Phase 4 – Migration bestehender Spielstände

- Betrifft `PlayerBudComponent.BudTypes` (gespeicherte Sets) auf existierenden Servern/Welten.
- Falls Phase 0.2 zeigt, dass `EnumCodec` den `.name()`-String schreibt (typischerweise Großschreibung, z. B. `"VERI"`): entweder die drei Default-IDs in `BudDefinition` ebenfalls großgeschrieben wählen (hässlich, aber Zero-Migration) **oder** sauber `"veri"/"keyleth"/"gronkh"` als IDs verwenden und beim ersten Laden mit dem neuen `StringCodec` einen Kompatibilitäts-Mapper einbauen (`VERI`→`veri` usw.), der nur beim Lesen alter Werte greift und nach dem ersten Save nicht mehr gebraucht wird.
- Diesen Mapper an einer einzigen Stelle bauen (z. B. `PlayerBudComponent`-Codec-Decode-Funktion), nicht verteilt.
- Vor dem Rollout auf einem Produktiv-Save-Backup testen: alten Spielstand laden, prüfen dass bestehende Buds korrekt erkannt werden (`hasBuds()`, `getBudTypes()`/`getBudIds()`).

---

## Phase 5 – Doku-Nacharbeit

- `README.md`: Config-Tabelle um den neuen Bud-Registry-Abschnitt ergänzen, Command-Referenz für den generischen `/bud create <id>` aktualisieren.
- `CLAUDE.md`: Abschnitt "Bud identity/profile system" umschreiben (beschreibt aktuell `BudType`-Enum → `BudProfileMapper` → `IBudProfile`-Impls, das entfällt).
- `CHANGELOG.md` + Version-Bump in `build.gradle.kts`, da Save-Format sich ändert (Breaking-ish für alte Welten, siehe Phase 4).

---

## Reihenfolge / Risiko

Empfohlene Umsetzungsreihenfolge: **Phase 0 → 1 → 2 → 3 → 4 → 5**, in einem Feature-Branch, mit Zwischencommit nach Phase 2 (Enum weg, alles kompiliert) bevor Phase 3 (Card-Interaktionen) angefasst wird — das sind unabhängige Teilrisiken (Persistenz vs. Item/Interaction-Registry) und lassen sich getrennt testen (`runServer`, alten Spielstand laden, `/bud create <id>`, Karte benutzen).
