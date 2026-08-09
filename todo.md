# TODO: Bud-Konfiguration auf Config-Registry umstellen

Hintergrund/Architektur/Entscheidungen: siehe [`docs/bud-config-architecture-plan.md`](docs/bud-config-architecture-plan.md). Dieses TODO ist die Abarbeitungsliste für den Rest — bitte Häkchen setzen, während du durchgehst.

Du hast hier etwas, das die vorherige Session nicht hatte: einen funktionierenden Compiler. Nutz ihn. Nach jedem der Blöcke unten `.\gradlew build` laufen lassen, nicht erst am Ende.

## Ausgangslage (bereits erledigt, nicht anfassen)

- [x] `com.bud.core.registry.{BudDefinition, BudSoundDefinition, BudRoster, BudRegistry}` existieren bereits.
- [x] `src/main/resources/buds/{veri,keyleth,gronkh}.yml` + `roster.yml` existieren bereits (Packaged Defaults).
- [x] `BudPlugin.setup()` ruft bereits `BudRegistry.getInstance().reloadMissing()` auf.
- [x] `.\gradlew clean build` war zuletzt grün.

Noch offen/ungetestet: ob `runServer` das `sounds`-Feld in den Bud-YAMLs korrekt über SnakeYAML in `BudSoundDefinition` mapped (verschachteltes Bean-Feld, in diesem Repo bisher nirgends genutztes Muster). Als erstes prüfen: `runServer`, Log `[BUD] --- BudRegistry Debug ---` sollte `Bud definitions loaded: [veri, keyleth, gronkh]` zeigen, keine Exceptions beim Parsen.

## Phase 2 — `BudType`-Enum entfernen ✅

**Wichtig, explizite Entscheidung:** `IBudProfile` und `IBudSound` sollen **komplett gelöscht** werden, nicht als Adapter-Interface stehen bleiben. `BudDefinition` und `BudSoundDefinition` ersetzen sie direkt überall, auch als Typ in Methodensignaturen (`IPromptContext.getBudProfile()` → gibt `BudDefinition` zurück, etc.). Sauberer Schnitt statt Kompromiss.

1. [x] Frisch grep'en, der Stand kann sich seit dieser Liste verschoben haben:
   - `grep -rn "BudType" src/main/java` (Stand bei Planerstellung: 32 Treffer)
   - `grep -rn "IBudProfile\|getBudProfile" src/main/java` (Stand: ~29 Treffer, u. a. **jede** `LLM*MessageCreation`-Klasse unter `com.bud.feature.**`, weil `IPromptContext.getBudProfile()` den Typ zurückgibt)
   - `grep -rn "IBudSound" src/main/java`

2. [x] Löschen:
   - `com.bud.core.types.BudType`
   - `com.bud.llm.profiles.IBudProfile`
   - `com.bud.core.sound.IBudSound`
   - `com.bud.feature.profiles.{VeriProfile, GronkhProfile, KeylethProfile}`
   - `com.bud.core.sound.{VeriSound, GronkhSound, KeylethSound}`
   - `com.bud.feature.profiles.BudProfileMapper`

3. [x] Ersetzen (überall wo obige Typen referenziert werden):
   - `BudType` → `String` (Bud-ID, normalisiert über `BudRegistry.normalize(...)`)
   - `IBudProfile` → `BudDefinition`
   - `IBudSound` → `BudSoundDefinition`
   - `BudProfileMapper.getInstance().getProfileForBudType(x)` → `BudRegistry.getInstance().get(x)`

4. [x] `PlayerBudComponent` (`src/main/java/com/bud/core/components/PlayerBudComponent.java`):
   - Feld `Set<BudType> budTypes` → `Set<String> budIds`
   - Codec: `new SetCodec<>(new EnumCodec<>(BudType.class), HashSet::new, false)` → `new SetCodec<>(Codec.STRING, HashSet::new, false)`
   - Vorsicht bei bestehenden Spielständen: `EnumCodec` serialisiert intern ohnehin nur als String (siehe `docs/bud-config-architecture-plan.md`, Abschnitt Phase 4/Migration) — der Umbau auf `Codec.STRING` ist wire-kompatibel. Trotzdem: IDs in `BudRegistry`/den YAMLs so wählen bzw. beim Lesen normalisieren, dass sie zu den alten Enum-Namen passen (Groß-/Kleinschreibung!). Kurzer Test: alten Spielstand laden, `/bud debug --componentData` sollte weiterhin die korrekten Buds zeigen.
   - Umgesetzt: Codec-Key bleibt `"BudTypes"` (wire-kompatibel), Decode normalisiert jeden geladenen Wert über `BudRegistry::normalize` (Großschreibung alter Enum-Werte → kleingeschriebene IDs).

5. [x] `CreationCommand`: feste Flags (`--veri`, `--keyleth`, `--gronkh`) durch einen generischen ID-Parameter ersetzt (Tab-Completion über neuen `BudIdArgumentType` aus `BudRegistry.getInstance().getIds()`), "alle erzeugen ohne Flag" nutzt `BudRegistry.getInstance().getDefaultBudIds()` statt hartkodiertem `Set.of(VERI, KEYLETH, GRONKH)`.

6. [x] `MemoryCommand`, `DeletionCommand`, `ResetCommand`: dieselbe Umstellung — feste Bud-Flags bleiben (UX), intern jetzt String-Literale (`"veri"`/`"keyleth"`/`"gronkh"`) statt `BudType.VERI` etc.

## Phase 3 — Card-Interaktionen generalisieren ✅

- [x] `CardVeriInteraction`, `CardKeylethInteraction`, `CardGronkhInteraction` gelöscht.
- [x] `CardBudInteraction` ist jetzt konkret (nicht mehr abstract), mit `BudId`-Codec-Feld: `new KeyedCodec<>("BudId", Codec.STRING)` an `SimpleInteraction.CODEC` angehängt.
- [x] `BudPlugin.setup()`: die drei `getCodecRegistry(Interaction.CODEC).register("CardXxx", ...)`-Aufrufe durch einen einzigen `register("CardBud", CardBudInteraction.class, CODEC_CARD_BUD)` ersetzt.
- [x] Item-JSONs angepasst: `src/main/resources/Server/Item/Interactions/Card{Veri,Keyleth,Gronkh}.json` → `"Type": "CardBud", "BudId": "veri"` (bzw. `keyleth`/`gronkh`) statt `"Type": "CardVeri"` etc.

## Phase 5 — Doku ✅

- [x] `README.md`: Config-Tabelle um `BudRegistry`/`buds/*.yml`/`roster.yml` ergänzt (neuer Abschnitt "🧬 Bud Registry"), Command-Referenz für generisches `/bud create <id>` aktualisiert.
- [x] `CLAUDE.md`: Abschnitt "Bud identity/profile system" umgeschrieben (kein `BudType`-Enum mehr), Bootstrap-Abschnitt zu `CardBud`-Registrierung aktualisiert.
- [x] `CHANGELOG.md` (Eintrag unter bestehendem `[2.0.0]`) + Version-Bump in `build.gradle.kts` (1.9.0 → 2.0.0).

## Verifikation

- [x] `.\gradlew build` grün
- [ ] `.\gradlew runServer`: Login, `/bud create` (alle drei), `/bud create veri` (einzeln), Card-Item benutzen (Primary = spawn, Secondary = despawn), `/bud debug --componentData`, `/bud debug --mood` — alles sollte wie vorher funktionieren, nur ohne `BudType` im Code.
- [ ] Alter Spielstand (falls vorhanden) lädt ohne Fehler, bestehende Buds werden erkannt.
- [x] `grep -rn "BudType\|IBudProfile\|IBudSound\|BudProfileMapper" src/main/java` liefert keine Treffer mehr (nur noch ein wire-kompatibler Codec-Key-String `"BudTypes"` und zwei erklärende Doc-Kommentare).
