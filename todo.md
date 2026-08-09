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
- [x] `.\gradlew runServer`: Login, `/bud create` (alle drei), Card-Item benutzen (Primary = spawn, Secondary = despawn) — vom Nutzer manuell getestet, funktioniert.
- [x] Alter Spielstand lädt ohne Fehler, bestehende Buds werden erkannt (Save enthielt `"BudTypes": ["Gronkh","Veri","Keyleth"]`, wird korrekt normalisiert).
- [x] `grep -rn "BudType\|IBudProfile\|IBudSound\|BudProfileMapper" src/main/java` liefert keine Treffer mehr (nur noch ein wire-kompatibler Codec-Key-String `"BudTypes"` und zwei erklärende Doc-Kommentare).
- [x] Migration wird jetzt geloggt: `PlayerBudComponent` schreibt beim Laden pro geändertem Eintrag `[BUD] Migrated legacy 'BudTypes' entry '<alt>' to bud id '<neu>' on load.` (nur bei tatsächlicher Änderung).

Phasen 2/3/5 sind damit vollständig abgeschlossen. Weiter unten: neuer Punkt, der beim Review der Migration aufgefallen ist.

## Phase 6 — Null-Safety-Lint aufräumen ✅

Der Java-Linter in VS Code (Eclipse JDT, `nullanalysis=enabled` in `.settings/org.eclipse.jdt.core.prefs`, `@Nonnull`/`@Nullable` aus `javax.annotation`) meldet nach der Migration mehrere Hinweise. **Nicht mit `@SuppressWarnings` wegdrücken — nur nach Rücksprache ignorieren.** Sauber lösen heißt: entweder echten Null-Check einbauen, oder `Objects.requireNonNull(...)` an der Stelle, wo eine JDK-/SDK-Methode (z. B. `Collections.unmodifiableSet`, `List.of`, `String.join`) laut Compiler nicht als `@Nonnull` deklariert ist, obwohl sie es faktisch ist.

**Ein echtes Problem, kein reines Lint-Hint** — zuerst das:

- [x] `BudIdArgumentType.java` (Zeile 27, 37): "Illegal redefinition of parameter" + "Missing non-null annotation" beim Override von `SingleArgumentType<String>.parse(...)` und `ArgumentType<String>.suggest(...)`. Tatsächliche Signatur per `javap -v` gegen die Hytale-Server-`.class`-Dateien verifiziert (nicht geraten): `SingleArgumentType<String>.parse(String, ParseResult)` hat **beide Parameter unannotiert** (Return `@Nullable`) → `@Nonnull` auf `input`/`result` in der Überschreibung entfernt (Rückgabe bleibt `@Nonnull`, da `BudRegistry.normalize(...)` nie `null` liefert — zulässige Verschärfung des Rückgabewerts). `ArgumentType<String>.suggest(CommandSender, String, int, SuggestionResult)` hat **alle drei Referenz-Parameter `@Nonnull`** (`sender`, `textAlreadyEntered`/`currentInput`, `result`) → alle drei in der Überschreibung mit `@Nonnull` versehen (dabei fiel auf, dass ein erster Fix-Versuch `currentInput` fälschlich als unannotiert eingestuft hatte — per Sanity-Check mit einem bewusst kaputten Symbol verifiziert, dass der Linter die Datei überhaupt neu bewertet, dann den echten Fehler gefunden und korrigiert). Der überflüssige `currentInput == null`-Check im Body wurde entfernt, da der Parameter jetzt vertraglich nie `null` ist.

**Reine "unchecked conversion to @Nonnull"-Hints** (JDT traut unannotierten JDK-Rückgabewerten nicht, wo unsere eigenen Methoden `@Nonnull` versprechen) — je Zeile geprüft, ob wirklich nie `null` möglich ist (dann `Objects.requireNonNull(...)` beim Return) oder ob doch ein Null-Fall existiert (dann echten Check + ggf. `@Nullable` auf die eigene Methode):

- [x] `MemoryCommand.java:121` — `budId` aus `for (String budId : budIds)` (Set-Element-Nullability unbekannt) an `BudRegistry.get(@Nonnull)`; nie `null` (Set kommt aus String-Literalen) → `Objects.requireNonNull(budId)`.
- [x] `PlayerBudComponent.java:273` — dieselbe Situation in `resolveBudId(...)`, `budId` aus `BudRegistry.getIds()` → `Objects.requireNonNull(budId)`.
- [x] `BudDefinition.java:33` — `String.trim()/toLowerCase()` sind unannotierte JDK-Methoden, aber im `id != null`-Zweig nie `null` → `Objects.requireNonNull(...)` um den Ausdruck.
- [x] `BudRegistry.java:167,185,191` — `String.trim()/toLowerCase()`, `Collections.unmodifiableSet(...)`, `Collections.unmodifiableList(...)` sind unannotierte JDK-Methoden, aber faktisch nie `null` → je `Objects.requireNonNull(...)`.
- [x] `BudRoster.java:21` — `List.of()`-Zweig unannotiert, aber nie `null` → `Objects.requireNonNull(List.of())`.
- [x] `PlayerChatReactionHandler.java:106` — `budId` aus `BudRegistry.getIds()` an `containsWord(@Nonnull, @Nonnull)` → `Objects.requireNonNull(budId)`.
- [x] `LLMInteractionManager.java:89` — `Message.join(...)` ist eine unannotierte Hytale-SDK-Methode, aber nie `null` → `Objects.requireNonNull(...)`.
- [x] `PlayerJoinSystem.java:135` + `PlayerStateTracker.java:87` — gleiche Ursache: `PlayerStateTracker.resolveActiveEffectIds(...)` war selbst nicht `@Nonnull` deklariert, obwohl es nie `null` zurückgibt (baut immer ein `HashSet` auf). Statt an beiden Call-Sites zu wrappen, die Quelle mit `@Nonnull` annotiert — behebt beide Stellen sauber an der Wurzel.
- [x] `PlayerStateTracker.java:93,106` — `effectId` aus `for (String effectId : newEffectIds)` (Set-Element-Nullability unbekannt), aber nie `null` (Set wird aus bereits gefilterten Werten gebaut) → Schleifenvariable einmal per `Objects.requireNonNull(rawEffectId)` umbenannt/zugewiesen, deckt beide Verwendungen ab.
- [x] `CleanupUtil.java:42,49` — dieselbe Situation für `budId` aus `for (String budId : budIds)`, ein `Objects.requireNonNull(rawBudId)` am Schleifenkopf deckt beide Zeilen ab.
- [x] `WeatherEntry.java:13` — `WeatherInterpreter.resolveDisplayName(...)` ist bereits `@Nonnull` deklariert und ruft intern nur `@Nonnull`-Methoden auf, trotzdem meldet JDT hier unchecked conversion (Record-Compact-Constructor-Eigenheit) → `Objects.requireNonNull(...)` um die Zuweisung.
- [x] `WorldTracker.java:90` — **echter Null-Fall, kein reines Lint-Hint**: `Weather.getId()` ist im Hytale-SDK unannotiert *und* kann laut anderer Stellen im Code (`DebugCommand`) tatsächlich `null` sein. Die bisherige Ternary prüfte nur `weather != null`, nicht `weather.getId() != null` — dadurch hätte ein `null`-Ergebnis direkt in `WeatherEntry`s `@Nonnull`-Konstruktor-Parameter durchgereicht werden können (dort wäre es in `cleanWeatherName(...)` ohnehin mit NPE gecrasht). Fix: `weather.getId() != null` zusätzlich geprüft, Fallback bleibt `"unknown"` — echte Robustheitsverbesserung, keine reine Annotation-Kosmetik.
- [x] `CardBudInteraction.java:63,68` — `Set.of(budId)` ist eine unannotierte JDK-Methode, aber nie `null` → `Objects.requireNonNull(Set.of(budId))` bei der Zuweisung deckt beide Verwendungsstellen ab.

Verifikation: nach jedem Fix per gezieltem No-Op-Touch (Edit + Revert) erneut geprüft, dass der Sprachserver für die betroffene Datei tatsächlich 0 Diagnostics meldet (nicht nur "keine Antwort bekommen") — inkl. eines bewussten Fehler-Sanity-Checks in `BudIdArgumentType.java`, um zu bestätigen, dass der Linter die Datei überhaupt neu auswertet.

- [x] Problems-Panel leer (für alle 14 betroffenen Dateien einzeln per Touch-Test verifiziert; keine neuen `@SuppressWarnings` hinzugefügt — `grep -rn "@SuppressWarnings" src/main/java` zeigt nur die zwei vor dieser Phase bereits vorhandenen, unabhängigen Stellen in `BudPlugin.java` und `RecentItemCache.java`)
- [x] `.\gradlew build` grün (`.\gradlew clean build` erfolgreich)
