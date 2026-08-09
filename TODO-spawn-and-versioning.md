# TODO: Spawn vor dem Charakter + Content-Versionierung

Hintergrund/Architektur/Entscheidungen: siehe [`docs/bud-spawn-and-content-versioning-plan.md`](docs/bud-spawn-and-content-versioning-plan.md) — lies das zuerst komplett. Zwei unabhängige Features (Phase 7, Phase 8), können in beliebiger Reihenfolge oder parallel bearbeitet werden. Nach jedem Block `.\gradlew build` laufen lassen, nicht erst am Ende. Folg der Null-Safety-Konvention aus `CLAUDE.md` (`@Nonnull`/`@Nullable` korrekt, keine `@SuppressWarnings` ohne Rücksprache) von Anfang an mit, nicht erst als Nachputz.

## Phase 7 — Buds vor dem Charakter spawnen ✅

Entschieden: Abstand max. 3 Blöcke (absteigend 3→2→1 probieren, siehe Plan), mehrere gleichzeitig gespawnte Buds nebeneinander auffächern (nicht hintereinander).

- [x] Spieler-Rotation lesen: über bytecode-verifizierte SDK-APIs (`javap`, s.u.), nicht über die geratenen Namen aus dem Plan-Entwurf:
  - `PlayerRef.getTransform()` liefert `com.hypixel.hytale.math.vector.Transform` (nicht `TransformComponent` — das ist eine andere Klasse für ECS-Components; `Transform` ist der reine Werttyp), `@Nonnull`. `transform.getRotation()` → `Rotation3f`, ebenfalls `@Nonnull`. `rotation.yaw()` liefert den Yaw als `float`.
  - Radiant vs. Grad verifiziert über `TrigMathUtil` (Parametername im Debug-Symbol ist wörtlich `radians`, plus `PI`/`degToRad`-Konstanten) und über `Rotation3f.getQuaternion(...)`, das `x/y/z` direkt ungewandelt in JOMLs radianten-basiertes `Quaterniond.rotationYXZ(...)` gibt → `yaw()`/`pitch()` sind Radiant, keine Umrechnung nötig.
  - Für den Vorwärtsvektor **nicht** `Vector3dUtil.FORWARD` + `setYawPitch(...)` verwendet (das war die Ausgangs-Hypothese im Plan), sondern die noch direktere, bereits vorhandene SDK-Methode `Transform.getDirection(float pitch, float yaw)` (statisch, `@Nonnull`, wirft bei NaN) — bytecodemäßig exakt dieselbe Sinus/Cosinus-Formel wie `setYawPitch`, aber zweckgebunden benannt und mit eingebauter Validierung. Pitch wird dabei bewusst auf `0` gesetzt (horizontale Ausrichtung, analog zum bestehenden `Y+0.5`-Fixoffset — Spieler-Blickwinkel nach oben/unten soll die Spawnhöhe nicht beeinflussen).
- [x] Neue Methode `BudManager.getSpawnPositionInFrontOfPlayer(PlayerRef playerRef, int index, int total)`:
  - Zielpunkt = Spielerposition + Vorwärtsvektor × Abstand, Abstand-Stufen `3, 2, 1` der Reihe nach probiert, je mit vorhandenem `isSpawnPositionFree(World, x, y, z)`.
  - Bei `total > 1`: seitlicher Versatz quer zur Blickrichtung (`right`-Vektor = `Transform.getDirection(0, yaw - 90°)`, per Kreuzprodukt-Vorzeichencheck verifiziert) basierend auf `index`, zentriert um die Mitte (`(index - (total-1)/2.0) * FRONT_SPAWN_FAN_SPACING`) — mittlerer Index zentriert, Rest symmetrisch links/rechts versetzt.
  - Kein Treffer über alle Distanz-Stufen → `null` zurückgeben (Signal für "kein Platz vorne").
- [x] `getPlayerPositionWithOffset(PlayerRef)` bleibt unverändert als Fallback bestehen. Neue kombinierte Methode `BudManager.getSpawnPosition(PlayerRef, int, int)` hinzugefügt: erst `getSpawnPositionInFrontOfPlayer`, bei `null` auf `getPlayerPositionWithOffset` zurückfallen — beide bisherigen Aufrufer nutzen jetzt nur noch diese kombinierte Methode.
- [x] `BudCreationHandler.spawnBud()` umgestellt: `handleEvent()` zählt beim Durchlaufen von `event.budIds()` (`Set<String>`) einen `spawnCounter`/`total = event.budIds().size()` mit und reicht `index`/`total` durch `createBud(...)` bis zu `spawnBud(...)` durch.
- [x] `TeleportHandler.teleportBud()` umgestellt auf `getSpawnPosition(playerRef, 0, 1)`.
- [x] Abstand-Stufen (`FRONT_SPAWN_DISTANCES = {3,2,1}`) und Fächer-Versatz (`FRONT_SPAWN_FAN_SPACING = 1.5`) als benannte Konstanten in `BudManager`, analog `MAX_SPAWN_POSITION_ATTEMPTS`.
- [ ] Manueller Test: `/bud create` (alle auf einmal) — Buds stehen nebeneinander vor dir, nicht ineinander. `/bud create veri` einzeln — Bud steht mittig vor dir. Vor einer Wand stehen und `/bud create` — sollte auf 2, dann 1 Block Abstand ausweichen, notfalls auf die alte Random-Suche zurückfallen (kein Crash, kein Spawn im Block). **Noch nicht ingame verifiziert — offen.**

## Phase 8 — Content-Versionierung (Prompts & Bud-YAMLs) ✅

- [x] Neue Klasse `ContentVersion` (`com.bud.core.content`, `extends AbstractYamlMessage`, Feld `int version`) — wiederverwendbar für beide Bereiche über `load(Path)` (Runtime-Datei, `null` wenn fehlt), `loadFromClasspath(String)` (gepackte Datei direkt aus dem Jar via `BudPlugin.class.getResourceAsStream(...)`) und `versionOf(ContentVersion)` (liefert `0` bei `null`, damit eine fehlende Datei immer als veraltet zählt). Dafür `AbstractYamlMessage.loadFromStream(...)` von `private` auf `protected static` geweitet (reine Sichtbarkeits-Erweiterung, keine Verhaltensänderung), damit `ContentVersion` sie für den Klassenpfad-Stream mitverwenden kann.
- [x] `src/main/resources/prompts/version.yml` → `version: 1`
- [x] `src/main/resources/buds/version.yml` → `version: 1`
- [x] `LLMPromptManager.loadPrompts()`: neue private `checkContentVersion(...)`, läuft nach dem bestehenden Copy-Schritt. `version.yml` in die gepackte Resourcen-Liste von `copyDefaults(...)` aufgenommen (wird wie jede andere Datei bei fehlender Runtime-Kopie angelegt). Bei Mismatch (und nicht schon `--reset`): `LoggerUtil.warning(...)` mit Hinweis auf `/bud prompt --reset`, `contentVersionMismatch`-Flag gesetzt (abfragbar über neues `isContentVersionMismatch()`).
- [x] `BudRegistry.loadAll()`: identisches Prinzip für `buds/version.yml` (`checkContentVersion(...)`, `version.yml` in `copyPackagedDefaults(...)`-Liste, `isContentVersionMismatch()`), Warnung verweist auf `/bud reload buds --reset`.
- [x] Neuer Subcommand `/bud reload buds [--reset]`: `ReloadCommand` (neue Command-Gruppe `reload`, `extends AbstractCommandCollection` analog `BudCommandCollection`) mit `ReloadBudsCommand` (`extends AbstractPlayerCommand`, `--reset`-Flag) als Subcommand, 1:1 nach dem Vorbild von `PromptCommand`, aber gegen `BudRegistry.reloadMissing()`/`reset()`. In `BudCommandCollection` registriert.
- [x] Neues Flag `DebugConfig.AutoUpdateContentOnVersionMismatch` (Default `false`, gleiches Codec-Pattern wie die anderen `DebugConfig`-Flags). Bei `true` und Mismatch (nur beim normalen Start, nicht während eines expliziten `--reset`-Aufrufs): `checkContentVersion(...)` ruft selbst `copyDefaults(dataDir, true)` bzw. `copyPackagedDefaults(budsDir, true)` erneut auf — jeweils nur für den eigenen Bereich (Prompts bzw. Buds getrennt, da zwei unabhängige Methoden in zwei Klassen), Mismatch-Flag wird danach wieder auf `false` gesetzt (kein Reminder nötig, da schon aktualisiert).
- [x] Einmaliger Reminder-Log: `PlayerJoinSystem.VERSION_MISMATCH_REMINDER_PENDING` (statisches `AtomicBoolean`, `compareAndSet` verbraucht es beim ersten `onEntityAdded`-Aufruf nach Start, unabhängig vom Ergebnis) → `remindContentVersionMismatchOnce()` prüft `LLMPromptManager`/`BudRegistry`-Mismatch-Flags und loggt bei Bedarf einmalig eine Warnung mit beiden Reset-Hinweisen.
- [x] README: neues Flag in der Debug-Configuration-Tabelle dokumentiert, neuer "Reload Commands"-Abschnitt für `/bud reload buds [--reset]`. `CLAUDE.md`: "Versioning / changelog"-Abschnitt um die `version.yml`-Hochzähl-Pflicht ergänzt, "Prompt management"-Abschnitt um einen Verweis auf `ContentVersion`/`BudRegistry`s identisches Muster ergänzt.
- [ ] Manueller Test: `version.yml` im Datenordner manuell auf `0` zurücksetzen, Server neu starten → Warnung im Log muss erscheinen; mit `AutoUpdateContentOnVersionMismatch: true` sollte die Datei automatisch wieder auf den gepackten Stand zurückgesetzt werden. **Noch nicht ingame verifiziert — offen (Server wurde für diese Phase nicht gestartet, nur `.\gradlew build`/`clean build` gegen den Compiler geprüft).**

## Verifikation (beide Phasen)

- [x] `.\gradlew build` grün (zuletzt als `.\gradlew clean build` nach allen Phase-7/8-Änderungen, inkl. neuer `version.yml`-Resourcen im Jar via `build/resources/main/{prompts,buds}/version.yml` geprüft).
- [x] Diagnostics nach jedem Edit über den Post-Edit-Hook geprüft (zeigt jeweils sofort erwartete Zwischenzustände wie "import never used"/"cannot find symbol" bis zum nächsten Edit-Schritt, am Ende jeder Datei keine Warnungen mehr offen) — **aber**: wie in Phase 6 gelernt, ist das kein verlässlicher Ersatz für einen echten Blick ins VS Code Problems-Panel (der Hook deckt nur die editierte Region ab, kein Full-File-Sweep). Bitte einmal selbst das Problems-Panel für die geänderten/neuen Dateien gegenchecken, bevor dieser Haken als endgültig bestätigt gilt.
- [ ] Ingame getestet wie oben beschrieben (Phase 7: Spawn-Fächer vor dem Spieler + Wand-Ausweich-Test; Phase 8: `versions.yml`-Mismatch-Test mit/ohne `AutoUpdateContentOnVersionMismatch`) — **offen, Server wurde für diese Runde nicht gestartet.**
- [ ] Diese Datei (`TODO-spawn-and-versioning.md`) komplett abgehakt — **offen bis die beiden Punkte oben (Problems-Panel, Ingame-Test) bestätigt sind.**

## Nachtrag — Phase 8 auf eine gemeinsame `versions.yml` konsolidiert

Beim ersten echten Serverstart trat ein Bug auf: `BudRegistry.loadDefinitions()` scannt `buds/` nach `*.yml` und filtert nur `roster.yml` explizit raus — die gepackte `buds/version.yml` (Zeile 27/29 oben) landete also im selben Ordner und wurde fälschlich als Bud-Definition geparst → `SEVERE`-Log beim Start (Datei hat kein `id`-Feld, SnakeYAML wirft). Fehler war nicht fatal (der bestehende try/catch pro Datei überspringt sie einzeln), aber falsch und unnötig laut.

Statt nur den Scan-Filter zu erweitern: auf Wunsch direkt auf **eine gemeinsame `src/main/resources/versions.yml`** (`budVersion`/`promptVersion`, ein Feld je Bereich) umgestellt statt zwei getrennter `prompts/version.yml` / `buds/version.yml` — behebt den Bug strukturell (keine Version-Datei mehr innerhalb von `buds/`, die der Definitions-Scan sehen könnte) und macht das Versionsmanagement wie gewünscht an einer Stelle:

- `ContentVersion` (`com.bud.core.content`): zwei Felder (`budVersion`, `promptVersion`) statt einem generischen `version`; neue statische `ensurePackagedCopy(Path rootDataDir, boolean overwrite)` kapselt den Copy-Schritt einmal zentral (beide Manager rufen sie unabhängig/idempotent auf, wer zuerst startet legt sie an).
- `LLMPromptManager`/`BudRegistry`: `checkContentVersion(...)` liest/schreibt jetzt `<dataDir>/versions.yml` (Root-Ebene, nicht mehr `prompts/`- bzw. `buds/`-Unterordner) und vergleicht jeweils nur das für sie relevante Feld (`promptVersionOf`/`budVersionOf`).
- `version.yml` aus beiden `copyDefaults`/`copyPackagedDefaults`-Ressourcenlisten entfernt.
- Alte `src/main/resources/{prompts,buds}/version.yml` sowie die bereits erzeugten Runtime-Kopien unter `run/mods/Bud_BudPlugin/{prompts,buds}/version.yml` gelöscht.
- README/CLAUDE.md entsprechend nachgezogen (`versions.yml` statt zwei getrennter Dateien).

Verifiziert: nur statisch (Lektüre + Bytecode-Check von `AbstractYamlMessage.loadFromStream`, das bereits in Phase 8 von `private` auf `protected` geweitet wurde und unverändert weiterverwendet wird). **Nicht** erneut gegen `.\gradlew build` geprüft (Cowork-Sandbox kann nicht kompilieren, siehe frühere Sessions) — bitte bei der nächsten Runde als Erstes `.\gradlew build` gegenchecken, bevor der Ingame-Test läuft.

## Nachtrag 2 — Auto-Update persistierte die neue Versionsnummer nicht

Ursache für "Auto-Update hat sich nichts geändert" trotz erhöhter `versions.yml`: der Auto-Update-Zweig in beiden `checkContentVersion(...)` rief zwar `copyDefaults(..., true)`/`copyPackagedDefaults(..., true)` auf, um den Inhalt zu erneuern, hat aber nie die eigene Runtime-`versions.yml` mit der neuen Versionsnummer überschrieben — der Mismatch wäre also bei jedem weiteren Start erneut erkannt (und der Content erneut, redundant, synchronisiert) worden. Fix: `ContentVersion.ensurePackagedCopy(rootDataDir, true);` direkt nach dem jeweiligen Content-Refresh in beiden Managern ergänzt.

Ebenfalls nur statisch verifiziert, nicht ingame getestet.

## Nachtrag 3 — Datei-Level Auto-Update-Exclusion (`auto-update-exclude.yml`)

Wunsch: einzelne Prompt-/Bud-Dateien vom automatischen Auto-Update ausnehmen können, wenn ein Operator sie selbst angepasst hat ("auf eigenes Risiko"). Erste Ausbaustufe bewusst nur auf Datei-Ebene (kein Key-Level-YAML-Merge, siehe Diskussion im Chat).

- Neue Klasse `AutoUpdateExclusions` (`com.bud.core.content`, `extends AbstractYamlMessage`) liest eine optionale, nie vom Plugin erzeugte/überschriebene `<dataDir>/auto-update-exclude.yml` mit zwei Listenfeldern (`prompts`, `buds`), Pfade relativ zu `prompts/` bzw. `buds/`. `load(Path)` liefert bei fehlender Datei eine leere Instanz (keine Exclusions).
- `LLMPromptManager.copyDefaults(...)`/`BudRegistry.copyPackagedDefaults(...)` haben je eine neue 3-Parameter-Überladung mit `Set<String> excludedPaths` bekommen; die bestehende 2-Parameter-Variante delegiert mit `Set.of()` (keine Änderung an `loadPrompts()`/`loadAll()`/explizitem `--reset`-Aufruf).
- Nur der Auto-Update-Zweig in `checkContentVersion(...)` lädt `AutoUpdateExclusions` und reicht die passende Pfad-Menge (`getPromptPaths()`/`getBudPaths()`) durch — ein explizites `/bud prompt --reset` bzw. `/bud reload buds --reset` ignoriert die Exclusion-Liste weiterhin vollständig.
- README (`AutoUpdateContentOnVersionMismatch`-Zeile + neuer Beispiel-Block) und `CLAUDE.md` ("Prompt management"-Abschnitt) entsprechend ergänzt.

Nur statisch verifiziert (Lektüre, keine `.\gradlew build`-Prüfung möglich in der Cowork-Sandbox). Noch offen: `versions.yml`-Konsolidierung, Persistenz-Fix und diese Exclusion-Funktion zusammen in einem echten Serverlauf testen (Build → Version erhöhen → `auto-update-exclude.yml` mit z.B. `buds/gronkh.yml` anlegen → `AutoUpdateContentOnVersionMismatch: true` → Start → prüfen, dass alles außer der exkludierten Datei aktualisiert wird).

## Nachtrag 4 — echter Ingame-Test deckte zwei Bugs auf, `auto-update-exclude.yml` in `versions.yml` gefaltet

Erster echter Test (Version erhöht, Prompts geändert, `AutoUpdateContentOnVersionMismatch: true`): Prompts + Exclusion funktionierten. Bud-Seite (Roster, Bud-Definitionen) hat sich **nicht** aktualisiert, obwohl kein Mismatch mehr geloggt wurde. Ursache: `ContentVersion.ensurePackagedCopy(rootDataDir, true)` hat beim Sync-Abschluss die **gesamte** `versions.yml` (beide Felder `budVersion`+`promptVersion`) mit dem gepackten Stand überschrieben, statt nur des eigenen Felds. Da `LLMPromptManager` beim Boot vor `BudRegistry` läuft, war `budVersion` durch den Prompt-Sync schon auf den gepackten Stand gesetzt, bevor `BudRegistry.checkContentVersion()` überhaupt geprüft hat — kein Mismatch mehr sichtbar, Bud-Update übersprungen. Aus demselben Grund hätte ein reines `/bud prompt --reset` (ohne Bud-Reset) nebenbei auch `budVersion` zurückgesetzt.

Fix: `ContentVersion` bekommt `persistBudVersion(...)`/`persistPromptVersion(...)` — lesen die aktuelle Datei, ändern **nur** das eigene Feld, schreiben Rest (anderes Versionsfeld + beide Exclusion-Listen) unverändert zurück. `ensurePackagedCopy(...)` wird nur noch für den Bootstrap-Fall (Datei fehlt) verwendet, nie mehr für ein Voll-Überschreiben.

Zusätzlich auf Wunsch ("nur eine Stelle zum Verwalten") die separate `auto-update-exclude.yml` wieder entfernt: `excludedPrompts`/`excludedBuds` sind jetzt Felder direkt in `versions.yml`, gelesen über `ContentVersion.excludedPromptPathsOf(runtime)`/`excludedBudPathsOf(runtime)`. `AutoUpdateExclusions.java` gelöscht. Da `persistBudVersion`/`persistPromptVersion` die Exclusion-Listen beim Schreiben unverändert übernehmen, überleben sie jedes automatische Update (nur `--reset` ignoriert sie weiterhin bewusst).

README/CLAUDE.md aktualisiert. Nur statisch verifiziert — bitte im nächsten Serverlauf erneut testen (insbesondere: Roster + Bud-Definitionen aktualisieren sich jetzt tatsächlich, `excludedBuds`/`excludedPrompts` in `versions.yml` überleben den Auto-Update-Durchlauf).
