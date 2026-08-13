# TODO: Bud Worker Mode (Farming zuerst)

Hintergrund/Architektur/Entscheidungen: siehe [`docs/bud-worker-mode-plan.md`](docs/bud-worker-mode-plan.md) — bitte zuerst lesen. Diese Datei ist die Abarbeitungsliste, Häkchen setzen während du durchgehst.

Nach jedem Block `.\gradlew build` laufen lassen, nicht erst am Ende (Cowork kann nicht kompilieren — das läuft über deine lokale Session). Null-Safety-Konvention aus `CLAUDE.md` von Anfang an mitziehen, nicht erst als Nachputz. Scope bewusst eng: **nur Farming/Keyleth bis Phase 8**, Foresting/Mining/LLM-Reaktionen sind spätere, separate Durchläufe (siehe unten).

## Phase 0 — Grundlagen verifizieren (kein Code)

- [x] `NPCPlugin.registerCoreComponentType(...)` und die Sensor-/Action-Basisklassen (`SensorBase`, `ActionBase`, `Builder*`) per Bytecode gegen `reference/server` verifiziert (`javap -p -c`, lokales JDK unter `jdk-25.0.2/bin/javap.exe`). Ergebnis in `docs/bud-worker-mode-plan.md` ("Phase 0 — Verifikationsergebnisse", Punkt 1).
- [x] Bestehenden State/Role-Mechanismus gelesen (`StateChangeHandler`, `StateChangeEvent`, `BudComponent.getCurrentState()`): neuer Enum-Wert `WORKING("Working")` in `BudState`, kein Boolean-Feld — Begründung in `docs/bud-worker-mode-plan.md`, Punkt 2. Voraussetzung: neuer `"State": "Working"`-Zweig in jeder Bud-Rollen-JSON.
- [x] Fuel-Burn-Vorbild per `javap` gegen `ProcessingBenchBlock`/`BenchSystems$ProcessingBenchTick` verifiziert (kein eigenes Furnace-Java, generisches `Bench`-System, per `EntityTickingSystem<ChunkStore>` getickt, unabhängig von offenem UI-Fenster). Entscheidung: kein Reuse (zu stark an Recipe/Output-Crafting gekoppelt), stattdessen eigener simpler Scheduled-Tick auf der `WorkstationBlockEntity` (Orchestrator-Pattern). Details in `docs/bud-worker-mode-plan.md`, Punkt 3.

## Phase 1 — Datenmodell (WorkRole, Config)

- [x] `WorkRole`-Enum (`FARMING`, `FORESTING`, `MINING`) — `com.bud.core.types.WorkRole`.
- [x] `BudDefinition` + `buds/{veri,keyleth,gronkh}.yml` um `workRole` erweitert (`keyleth: FARMING`, `veri: FORESTING`, `gronkh: MINING`; `getWorkRole()` folgt dem `getNpcTypeId()`-Muster — wirft, wenn im YAML nicht gesetzt, da fest pro Bud vorausgesetzt). `versions.yml`/`budVersion` auf 2 hochgezählt. `.\gradlew build` grün.
  - [x] Ingame-Test bestätigt (Sascha): `/bud reload buds`, Log zeigt korrekt geparste Rolle je Bud.
- [x] Neue Config-Sektion `WorkConfig` (`FieldRadius`, `TreeMinDistance`, `OreMinDistance`, `FuelDurationSeconds`) nach `LLMConfig`-Muster, in `BudPlugin` registriert/gespeichert. `.\gradlew build` grün.
  - [x] Ingame-Test bestätigt (Sascha): `Work.json` wird erzeugt, Werte änderbar, kein Absturz bei fehlender Datei.
- [x] Nachtrag: `WorkRole` um `COMPANION` erweitert, `getWorkRole()` liefert das als Default statt zu werfen, wenn `buds/<id>.yml` kein `workRole` setzt (Entscheidung siehe `docs/bud-worker-mode-plan.md`, Abschnitt "Rollen-Zuordnung"). `.\gradlew build` grün.

## Phase 2 — Working-State (Bud aus Kampf-/Begleit-Pipeline nehmen)

- [x] `BudState.WORKING("Working")` ergänzt. Alle bislang exhaustiven `switch`/`case`-Stellen über `BudState` (`BudSoundDefinition.getForState`, `StateChangeQueue.triggerStateChangeReaction`) für den Compiler ergänzt — Working liefert dort bewusst keinen Sound bzw. wirft (siehe Codekommentare, dieser Zweig ist unerreichbar, da `handleStateChange` für Working vorher zurückkehrt).
- [x] Neuer `"Sensor": {"Type": "State", "State": "Working"}`-Zweig (MODE 4, Platzhalter `"BodyMotion": {"Type": "Nothing"}`) in allen drei `Template_{Veri,Keyleth,Gronkh}_Bud.json`, analog zu MODE 1 (PetSitting). Mit PowerShell `ConvertFrom-Json` auf gültiges JSON geprüft.
- [x] Silenter `StateChangeEvent.dispatch(...)`-Pfad für Working ergänzt, analog zum Teleport-Fix (`TeleportHandler`): `StateCommand` bekommt einen Debug-Flag `--working`, der `budComponent.setCurrentState(WORKING)` + `StateChangeEvent.dispatch(...)` **direkt** aufruft statt über `StateChangeQueue` zu gehen (die Queue triggert sonst immer LLM-Chat + Bud-zu-Bud-Reaktion). Zusätzliche Absicherung in `StateChangeQueue.handleStateChange`: falls doch ein `Working`-Eintrag in der Queue landet (z. B. weil `StateChangeSystem` einen nativen Rollen-State-Wechsel erkennt), wird nach dem Dispatch früh zurückgekehrt, bevor LLM/Reaction-Code läuft.
- [x] `StateChangeHandler`: `LockedTarget` wird nur noch gesetzt, wenn der neue State **nicht** `WORKING` ist — ein arbeitender Bud lockt/folgt dem Spieler nicht mehr.
- [x] Reaktions-Trigger zentral statt pro Filter-System gesperrt: `BudManager.getRandomBudComponent`/`getRandomOtherBud` (der gemeinsame Chokepoint, über den praktisch alle Filter-Systeme — Block, Crafting, Combat, Discover, Item-Pickup, Chat, Wetter/World-Tracker, Mood — ihren reagierenden Bud auswählen) schließen Buds im `WORKING`-State jetzt aus. Zusätzlich in `DamageFilterSystem`: der Kampf-Assist-Loop (Attitude-Override auf Hostile für alle Buds des Spielers) überspringt arbeitende Buds explizit. Zusätzlich in `Orchestrator.dispatch(...)`: Events für einen inzwischen arbeitenden Bud (z. B. vor dem Arbeitsbeginn eingereiht) werden verworfen statt zugestellt.
- [x] `.\gradlew build` grün nach jedem Teilschritt.
  - [x] Ingame-Test bestätigt (Sascha): `/bud state --working` funktioniert, Bud bleibt stehen/kämpft nicht, `/bud state --defensive` setzt zurück.

## Regression nach Phase 2 — Role-Validierung (behoben)

Erster Serverstart nach Phase 2 (Cowork-Session) schlug fehl: `[NPC|P] FAIL: .../Template_{Veri,Keyleth,Gronkh}_Bud.json: State sensor or State setter action/motion exists without accompanying state/setter: Working`, gefolgt von `Reference to unknown builder: Template_*_Bud` (Folgefehler, weil der Builder als ungültig markiert wird) und Spawn-Fehlern (`/bud create` fehlgeschlagen, `[BUD] Player has no buds or mismatched bud ids.`).

- [x] **Root Cause bestätigt:** Die Engine verlangt für jeden per Sensor referenzierten State auch mindestens eine explizite State-Setter-Action/Motion irgendwo in der Rollen-Datei. `PetSitting`/`PetPassive`/`PetDefensive` bekommen ihren Setter aus dem bestehenden Interaction-Cycle-Block (`"Actions": [..., {"Type": "State", "State": "PetPassive"}, ...]`), `Idle` ist der `StartState`. Der MODE-4-Working-Zweig aus Phase 2 hatte nur einen Sensor (`BodyMotion: Nothing`), keinen Setter — daher der Validierungsfehler.
- [x] **Fix (alle drei `Template_{Veri,Keyleth,Gronkh}_Bud.json` identisch):** Im MODE-4-Block selbst einen selbstreferenziellen No-op-Setter ergänzt (`"Actions": [{"Type": "State", "State": "Working"}]`), **nicht** im Interaction-Cycle — sonst wäre Working über die normale Cycle-Interaktion von außen erreichbar, was explizit nicht gewollt ist (Working wird ausschließlich programmatisch über `StateChangeEvent.dispatch(...)` gesetzt, siehe Phase 2). Kommentar im JSON dokumentiert, dass der Setter nur die statische Validierung erfüllt und keinen neuen Übergang öffnet, weil der Sensor bereits `State == Working` voraussetzt. Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün.
- [x] **Tick-Overhead eingeschätzt (per `javap -p -c` gegen `ActionList.execute()` in `reference/server`, nicht geraten):** Ohne `ActionsBlocking`/„Once“ ruft `ActionList.execute()` (non-blocking/Default-Pfad) für **jede** enthaltene Action bei **jedem** Aufruf `canExecute()` + `execute()` auf — bytecode-bestätigt, kein Skip für bereits abgeschlossene/identische Actions. Da unser Working-Sensor dauerhaft matched, solange der native Rollen-State `Working` bleibt, feuert der No-op-Setter effektiv jeden Tick erneut. Das läuft aber komplett nativ innerhalb des NPC-Instruction-Baums und geht **nicht** durch unsere Java-Pipeline (`com.bud.feature.state.StateChangeEvent`/`StateChangeQueue`) — die früh-Return-Absicherung aus Phase 2 wird dabei gar nicht erst erreicht: Unser eigenes `StateChangeSystem.tick()` erkennt nur eine *Änderung* des nativen State-Namens gegenüber `BudComponent.currentState`; da beide dauerhaft `"Working"` bleiben (der No-op-Setter setzt ja denselben Wert erneut), entsteht dort nie ein Diff und nie ein neuer `StateChangeEntry` — kein LLM-/Reaction-Spam-Risiko. Verbleibender Kostenpunkt ist rein nativ (wiederholter, günstiger `setState`-Aufruf pro Tick pro arbeitendem Bud). `ActionsBlocking: true` würde das laut Bytecode für eine 1-Action-Liste **nicht** lösen (ändert nur die Sequenzierungs-Semantik bei mehreren Actions, `execute()` wird trotzdem einmal pro Tick aufgerufen); ein „Once“/`SetOnce`-JSON-Attribut ist in keiner bestehenden Rollen-Datei belegt, für unbelegte Schema-Attribute wollte ich nicht raten (siehe CLAUDE.md „Verify, don't assume"). **Entscheidung: kein weiterer Fix jetzt** — MODE 4 ist ohnehin ein Platzhalter, der in Phase 5+ durch echte Sensoren/Actions ersetzt wird, die von sich aus gaten; der native Tick-Overhead ist klein und geht nicht in unsere Java-Pipeline.
- [x] **Serverstart verifiziert:** `.\gradlew runServer`, Log auf die vorherigen `FAIL`/`Reference to unknown builder`-Zeilen geprüft — treten nicht mehr auf (siehe Log-Auszug unten).
- [x] **Ingame-Test bestätigt (Sascha):** `/bud create` + `/bud state --working`/`--defensive` funktionieren, Regression vollständig behoben.

## Aufräum-Punkt (vor Phase 4 final)

- [ ] `--working`-Debug-Flag an `/bud state` (aus Phase 2, reines Test-Provisorium, solange die Workstation-Bindung noch nicht existiert) entfernen oder hinter eine Debug-Config sperren, sobald Phase 3/4 den echten Trigger (Karte in Workstation-Slot) liefert. `WORKING` darf kein normaler, dauerhaft erreichbarer Spielerbefehl bleiben.

## Phase 3 — Workstation-Block (Platzhalter-Modell)

- [x] Vorab verifiziert: `BudComponent` trägt bereits `private PlayerRef playerRef` + `getPlayerRef()`. Grundlage für eine spätere Besitzer-Bindung ist also vorhanden, siehe `docs/bud-worker-mode-plan.md`, Abschnitt "Besitzer-Bindung (Logout/Login, Mehrspieler)" — die Bindungslogik selbst (`ownerPlayerId` auf der Workstation speichern) ist **nicht** Teil dieses Durchlaufs gewesen (nicht im Auftrag enthalten) und bleibt offen.
- [x] Recherche (javap/grep gegen `reference/server`, Ergebnisse mit Begründung in `docs/bud-worker-mode-plan.md`, "Phase 3 — Verifikationsergebnisse"):
  - Einfacherer Multi-Slot-Container: ja, natives `ItemContainerBlock` (`Component<ChunkStore>`, rein JSON-aktiviert `"Capacity": N`) — **Korrektur einer früheren Notiz hier**: nicht durch ein selbstgebautes `SimpleItemContainer`-Embed ersetzen, weil die native `"Open_Container"`-Interaktion (`OpenContainerInteraction`, bytecode-verifiziert) fest an `ItemContainerBlock.getComponentType()` gekoppelt ist — ohne das native Component keine Spieler-UI ohne kompletten Eigenbau.
  - Insert-Validierung: echter Java-Hook `ItemContainer.setSlotFilter(FilterActionType, short, SlotFilter)`, `SlotFilter.test(...)` lehnt den Insert nativ ab (kein Nach-Insert-Auswerfen nötig). `"FilterValidIngredients"` aus Bench-JSON ist Bench-spezifisch, nicht generisch nutzbar.
  - BudId-Lesepfad: nicht auf dem `ItemStack` gespeichert, sondern auf der geteilten `CardBudInteraction`-Asset-Instanz je Item-Typ — `itemStack.getItem().getInteractions().get(InteractionType.Primary)` → `Interaction.getAssetMap().getAsset(id)` (Ersatz für das deprecated `getInteractionOrUnknown`) → `instanceof CardBudInteraction` → `getBudId()`. Kann beim Slot-Wechsel nichts verlieren, da nichts Stack-Individuelles gelesen wird.
- [x] Neues Block/Item-JSON `Workstation_Farming` (`src/main/resources/Server/Item/Items/Workstation_Farming.json`), Furnace-Blockymodel/Textur aus `Blocks/Benches/` referenziert (nur Optik), einfaches Wood/Rock-Rezept an der Workbench. `"BlockEntity": {"Components": {"ItemContainerBlock": {"Capacity": 2}, "WorkstationBlockEntity": {"WorkRole": "Farming"}}}`, `"Interactions": {"Use": "Open_Container"}`.
- [x] Neue Komponente `com.bud.feature.work.WorkstationBlockEntity` (`Component<ChunkStore>`, `WorkRole`-Feld, `EnumCodec`-basiert) + `com.bud.feature.work.WorkstationFilterSystem` (`RefSystem<ChunkStore>`), installiert beim Laden/Platzieren einen `SlotFilter` auf Slot 0 des nativen `ItemContainerBlock`-Containers, der die Bud-ID der eingelegten Karte gegen `workRole` der Station prüft. Beide in `BudPlugin.setup()` registriert (`getChunkStoreRegistry()`, analog zu `BudComponent`).
- [x] `.\gradlew build` grün nach jedem Block.
- [x] **Regression (Serverstart blockiert, behoben):** Decode-Fehler `Failed to find enum value for FARMING` beim Laden von `Workstation_Farming.json`. Root Cause per `javap -p -c` gegen `EnumCodec`/`EnumCodec$EnumStyle` bestätigt (nicht geraten): der 1-Arg-Konstruktor `new EnumCodec<>(WorkRole.class)` setzt `enumStyle` fest auf `CAMEL_CASE`; `getEnum(...)` matcht dann case-sensitiv gegen `enumKeys`, die aber aus einer **separat erkannten** Style gebaut werden (`EnumStyle.detect(...)`, nur für die Key-Liste, nicht für `enumStyle` selbst) — für unser rein großbuchstabiges `WorkRole`-Enum (`FARMING`, `FORESTING`, ...) erkennt `detect(...)` `LEGACY` (kein Kleinbuchstabe im Namen), `LEGACY.formatCamelCase("FARMING")` liefert `"Farming"`. D. h. `enumKeys = ["Farming", "Foresting", "Mining", "Companion"]`, gegen die der `CAMEL_CASE`-Matcher exakt (case-sensitiv) vergleicht — `"FARMING"` matcht nicht. **Fix:** JSON-Wert auf `"Farming"` geändert (PascalCase, einziger konsistenter Ersatz-Wert), nicht der Codec-Wrapper — der 1-Arg-Konstruktor mit `CAMEL_CASE` ist die von der Engine selbst vorgesehene Default-Wahl und deckt sich mit dem durchgängigen PascalCase-Stil aller nativen Hytale-Asset-JSONs (`"Material": "Solid"`, `"Type": "Processing"`, ...) — ein Umbau auf `EnumStyle.LEGACY` hätte dem widersprochen und wäre ohne Präzedenzfall im Projekt gewesen. `buds/*.yml`s `workRole: FARMING` ist NICHT betroffen (SnakeYAML-Pfad, komplett anderer Codec, exakter Enum-Name-Match). `.\gradlew build` grün, JSON mit PowerShell geprüft.
- [x] Owner-Binding (Nachtrag, siehe `docs/bud-worker-mode-plan.md` "Besitzer-Bindung"): stabiler Spieler-Identifier im Code verifiziert — `PlayerRef.getUuid()` (`java.util.UUID`), bereits an anderer Stelle im Projekt für genau diesen Zweck verwendet (`DeletionCommand`: `PermissionsModule.get().getGroupsForUser(playerRef.getUuid())`), nicht das `PlayerRef`-Objekt selbst (das ist Session-/Verbindungs-gebunden, siehe `Universe`/`WindowManager`-Konstruktion). `ownerPlayerId` als `@Nullable UUID` (Codec: `Codec.UUID_BINARY`, nicht deprecated, bytecode-geprüft) auf `WorkstationBlockEntity` ergänzt.
  - **Wer inserted, ermitteln:** `SlotFilter.test(...)` selbst bekommt keinen Akteur/Spieler übergeben (`ItemContainer`/`ItemContainerChangeEvent`/`Transaction` sind bytecode-verifiziert akteur-agnostisch — bewusstes Engine-Design, da ein Container generisch von vielem verändert werden kann). Einzige Stelle mit Spielerbezug: das offene `ContainerBlockWindow` (`Window.getPlayerRef()`, bytecode-verifiziert), erreichbar über `ItemContainerBlock.getWindows(): Map<UUID, ContainerBlockWindow>`. `WorkstationFilterSystem` weist die Owner-ID daher zu, wenn beim akzeptierten Insert genau ein Spieler die Station gerade offen hat (`resolveSoleViewer(...)`) — bei 0 oder mehreren gleichzeitigen Betrachtern bleibt die Zuweisung aus, statt falsch zuzuordnen.
  - **Einschätzung "anderer Spieler an gebundener Station" (Kommentar, keine Zugriffskontrolle):** Ownership wird bei jedem erneut akzeptierten Insert überschrieben (kein Lock) — ein zweiter Spieler, der die Karte ersetzt, wird der neue Owner. Bewusst simpel für Phase 3; Phase 4 (echte Bud-Bindung/Fuel-Logik) muss entscheiden, ob eine gebundene Station Karten-Änderungen von Nicht-Ownern ablehnen soll.
  - [x] Ingame-Test teilbestätigt (Sascha): Station bauen funktioniert, Keyleth-Karte einlegen funktioniert, Karte nach Relog wieder rausnehmbar (Bindung übersteht Relog). **Noch nicht explizit getestet:** Veri-/Gronkh-Karte wird abgelehnt.

## Cross-Cutting-Fixes: Working-State-Interop mit bestehenden Bud-Systemen (vor Phase 4)

Beim ersten echten Ingame-Test von Phase 3 aufgefallen (Sascha) — Working-State existierte vorher nur über den Debug-Flag kurzzeitig, jetzt zum ersten Mal "in echt" über längere Zeit aktiv, dadurch werden Lücken in Systemen sichtbar, die den Working-State noch nicht kannten, als sie geschrieben/angepasst wurden.

- [x] **Roster/`/bud create` (Spawn All) dupliziert einen bereits arbeitenden Bud — Root Cause verifiziert, ursprüngliche Vermutung widerlegt.** Alle 16 Aufrufer von `BudManager.getRandomBudComponent`/`getRandomOtherBud` einzeln durchgegangen (grep + Lesen): **ausnahmslos** Reaktions-Auswahl (Chat/Sound-Trigger nach Block/Crafting/Combat/Discover/Item-Pickup/Chat/Wetter/World/Mood-Events, inkl. `BudCreationHandler`s eigener Begrüßungs-Reaktion) — keiner davon ist eine Existenz-/Dedup-Prüfung. Die eigentliche Dedup-Prüfung (`BudManager.playerHasValidBud`, aufgerufen aus `BudCreationHandler.createBud`) nutzt den Chokepoint **gar nicht** — sie prüft direkt über `PlayerBudComponent.getBudIds()`/`getCurrentBuds()` + Referenz-Validität, komplett unabhängig vom `WORKING`-State. Kein Aufrufer des Chokepoints musste geändert werden.
  - **Tatsächliche Root Cause:** `BudCreationHandler.handleEvent` hat einen zweiten, vom Chokepoint komplett unabhängigen Pfad: `existingBudTeleports` (jeder bereits besessene Bud, dessen ID erneut angefragt wird, wird — unabhängig vom Dedup-Skip in `createBud` — zusätzlich per `TeleportEvent.dispatch(...)` "zum Spieler geholt"). Diese Sammel-Schleife kannte `WORKING` nicht und hat den arbeitenden Bud jedes Mal per Remove+Respawn (`TeleportHandler.teleportBud`) von der Workstation weg direkt neben den Spieler versetzt — das erzeugt das von Sascha beobachtete "Duplizieren" (kurzzeitig zwei sichtbare Instanzen durch Remove-dann-Spawn, bzw. der Bud "taucht doppelt auf", einmal an der Station, dann daneben beim Spieler).
  - **Gleicher Bug zusätzlich in `TeleportFilterSystem.onComponentRemoved`** (Waystone-Trigger) gefunden — sammelt ebenfalls alle Buds für `TeleportEvent.dispatch(...)`, ohne `WORKING` zu prüfen. Beide Sammelstellen füttern dieselbe `TeleportHandler.teleportBud`-Pipeline — siehe Punkt 2 unten, ein gemeinsamer Fix deckt beides ab.
- [x] **Despawn/`/bud delete` — kein Fix nötig, bestätigtes Verhalten:** ein Working-Bud wird korrekt mit-despawnt/gelöscht (von Sascha bestätigt, macht inhaltlich Sinn).
- [x] **`TeleportHandler.teleportBud` kennt Working-State jetzt.** Primärer Guard direkt in `teleportBud` (kehrt sofort zurück, wenn `budComponent.getCurrentState() == WORKING`, bevor Remove+Respawn passiert) — deckt automatisch **beide** Aufrufer ab (`BudCreationHandler`s `existingBudTeleports` und `TeleportFilterSystem`s Waystone-Follow), auch künftige. Zusätzlich (Defense-in-Depth, verhindert nutzlose 250ms-verzögerte No-op-Aufrufe und dass ein Working-Bud den zufälligen "wer kommentiert den Teleport"-Slot verschwendet): beide Sammelstellen (`BudCreationHandler.handleEvent`s `existingBudTeleports`, `TeleportFilterSystem.onComponentRemoved`s `budComponents`) schließen Working-Buds jetzt schon beim Einsammeln aus.
- [x] **Design-Entscheidung für Phase 4 (aus Sascha-Frage abgeleitet):** was passiert, wenn beim Karteneinlegen kein Bud-Entity existiert (despawnt)? Entscheidung: Station soll ihn spawnen (analog `/bud create`, aber an der Stationsposition statt vor dem Spieler), falls nicht vorhanden — sonst eine bereits existierende Instanz heranholen/teleportieren. **Wird in Phase 4 umgesetzt, hier nur als Entscheidung festgehalten, kein Code in diesem Durchlauf.**
- [x] `.\gradlew build` grün nach jedem Punkt.
  - [x] Ingame-Test bestätigt (Sascha): keine Duplizierung mehr bei `/bud create` (all), Working-Bud bleibt bei Waystone-Teleport stehen, andere Buds teleportieren normal mit.

## Phase 4 — Bindung + Fuel-Timer (noch keine Feldarbeit)

- [x] Vorab-Investigation (kein Code):
  - **Spawn-Pfad:** `BudManager.getSpawnPosition`/`getSpawnPositionInFrontOfPlayer`/`getPlayerPositionWithOffset` sind alle spieler-relativ und für Phase 4 ungeeignet. `BudSpawner` selbst (verifiziert) ist dagegen komplett positions-agnostisch — nimmt einfach einen `Vector3d` entgegen, keine versteckte Spieler-Abhängigkeit. Für eine feste Weltposition braucht es also keinen neuen Spawner, nur einen neuen Weg, diese Position zu berechnen (die Workstation kennt ihre eigene Position nicht direkt — nur `index`+`chunkRef` über `BlockModule.BlockStateInfo`). Lösung per `javap -p -c` gegen `BenchSystems$ProcessingBenchTick` verifiziert (identischer interner Umrechnungspfad wie im nativen Bench-System): `BlockStateInfo.getIndex()` → `ChunkUtil.{x,y,z}FromBlockInColumn(index)` (lokale Koordinaten) → `ChunkUtil.worldCoordFromLocalCoord(blockChunk.getX()/getZ(), lokal)` (Welt-X/Z, mit `BlockChunk` über `blockStateInfo.getChunkRef()`) → `Vector3d`. Neuer Helper `WorkstationBindingHandler.resolveWorkstationPosition(...)`.
  - **"Vorheriger State":** im Code geprüft (`grep -rn "previousState\|priorState\|lastState"` — keine Treffer), kein bestehendes Konzept. `BudManager.getNextState(...)` ist ein reiner Vorwärts-Zyklus (Defensive→Passive→Sitting→Defensive), keine Historie. `BudComponent.CODEC` ist zudem komplett leer (keine Felder persistiert) — `currentState` selbst übersteht also schon heute keinen Server-Neustart, ein `previousState`-Feld muss das also auch nicht. **Entscheidung:** echtes `previousState`-Feld auf `BudComponent` ergänzt (statt fixem `PetDefensive`-Rückfall) — bessere UX (Bud kehrt in den Modus zurück, in dem sie vor der Arbeit war), minimaler Mehraufwand, gleiches Nicht-Persistenz-Muster wie `currentState`/`currentMood`.
- [x] **Bindung (Increment 1):** `WorkstationFilterSystem` registriert jetzt zusätzlich zum Slot-Filter (Phase 3) einen `ItemContainer.registerChangeEvent(...)`-Listener (bytecode-verifiziert, öffentlicher Hook auf `ItemContainer`, feuert bei jeder Slot-0/Slot-1-Änderung), der an `WorkstationBindingHandler.reevaluate(...)` delegiert. Bindung passiert, sobald Slot 0 eine gültige Karte **und** Slot 1 Futter enthält:
  - Bud bereits gespawnt (bei `ownerPlayerId` gesucht über `PlayerBudComponent.getCurrentBuds()`) → silentes Remove+Respawn an der Stationsposition (`WorkstationBindingHandler.teleportToStation`, gleiches Muster wie `TeleportHandler.teleportBud`, aber ohne dessen Delay/`TeleportQueue`/Reaction — passt zur bereits getroffenen Entscheidung "keine LLM-Reaktionen" für Work).
  - Bud nicht gespawnt → `WorkstationBindingHandler.spawnAtStation` spawnt sie direkt an der Stationsposition, registriert `BudComponent`, fügt sie `ownerPlayerId`s `PlayerBudComponent` hinzu (dedup-kompatibel mit `/bud create`/`/bud delete`, wie in den Cross-Cutting-Fixes bestätigt).
  - `previousState` wird vor dem Setzen auf `WORKING` gespeichert (nur wenn aktueller State nicht bereits `WORKING` ist, verhindert Überschreiben durch einen zweiten Bind-Zyklus).
  - `ownerPlayerId` aus Phase 3 wird direkt verwendet; ist er `null` (sollte laut Slot-Filter-Logik nicht passieren) oder der Besitzer nicht online/auflösbar, wird der Bind übersprungen und geloggt statt zu crashen.
- [x] **Entbindung (Increment 1):** Jede Änderung, nach der Slot 0 keine gültige Karte mehr enthält (Karte raus **oder** Rollen-Mismatch durch Kartentausch), löst bei gebundenem Bud `WorkstationBindingHandler.unbind(...)` aus — `previousState` wird wiederhergestellt (silent dispatch, kein LLM/Reaction), `boundBud`/Fuel-Tracking auf der Station zurückgesetzt.
- [x] **Fuel-Verbrauch (Increment 2):** neues `WorkstationFuelTickSystem` (`EntityTickingSystem<ChunkStore>`, eigener simpler Tick statt `ProcessingBenchBlock`-Reuse, Entscheidung aus Phase 0 bestätigt umgesetzt) zählt `WorkstationBlockEntity.fuelSecondsRemaining` pro Tick herunter; bei Erreichen von 0 wird ein Item aus Slot 1 entfernt (`ItemContainer.removeItemStackFromSlot(slot, 1)`) und der Timer auf `WorkConfig.FuelDurationSeconds` zurückgesetzt — mehrere eingelegte Gerichte werden dadurch automatisch nacheinander verbraucht (nicht parallel), da immer nur exakt eins pro Ablauf entfernt wird.
- [x] **Ruhepose (Increment 3) — bewusst reduzierter Scope, siehe Begründung unten:** ist Slot 1 beim Ablauf leer, wird `WorkstationBlockEntity.resting = true` gesetzt (kein Fuel-Verbrauch mehr, Bud bleibt `WORKING`/an der Station). Der Tick-System prüft bei `resting == true` jeden Tick erneut auf Slot 1 und nimmt die Arbeit bei Nachfüttern automatisch wieder auf (`resting = false`, Timer neu gestartet) — funktional vollständig und testbar. **Kein visuell unterschiedlicher Pose-Wechsel in dieser Runde**: die native Rollen-JSON-Recherche (Sub-State-Mechanismus, wie ihn MODE 3 mit `.Fighting`/`.Default` schon nutzt) hätte einen neuen `.Resting`-Sub-State + einen weiteren selbstreferenziellen No-op-Setter gebraucht, dessen genaues Namens-/Matching-Verhalten (mit/ohne führendem Punkt, siehe `StateHelper.getSubStateIndex(...)`) ich nicht bytecode-verifizieren konnte, ohne den Rahmen dieser ohnehin sehr großen Phase zu sprengen — und ein falscher Griff hier hätte laut bisheriger Erfahrung (siehe Regressionen in Phase 2/3) das Risiko eines weiteren blockierenden Serverstart-Fehlers. Zusätzlich: selbst `PetSitting` nutzt aktuell nur `"BodyMotion": {"Type": "Nothing"}` (keine echte Sitz-Animation im Projekt vorhanden) — ein visueller Unterschied wäre ohnehin nur kosmetisch draufgesetzt, kein bestehendes Muster wiederverwendet. **Für dich beim Test:** die Ruhepose ist aktuell nicht sichtbar (Bud sieht weiterhin wie im Working-Idle aus), aber im Log (`fine`-Level: "Workstation out of fuel, Bud ... is resting." / "Workstation refed, Bud ... resumes work.") und über den Fuel-Verbrauch selbst (Slot leert sich, kein weiterer Verbrauch bis Nachfüttern) nachvollziehbar. Ein echter Pose-Wechsel bleibt offen — entweder als kleiner Folgeauftrag (Sub-State-Mechanismus zuerst per `javap` klären) oder wird mit Phase 5+ (wenn MODE 4 ohnehin durch echte Sensoren/Actions ersetzt wird) miterledigt.
  - Test: 1 Gericht rein, Intervall für den Test kurz stellen (`WorkConfig.FuelDurationSeconds`), verbrauchen lassen → Slot leert sich, Log zeigt "is resting". 2. Gericht vor Ablauf nachlegen → wird korrekt nachgereiht (Timer läuft normal weiter, zweites Gericht wird erst beim NÄCHSTEN Ablauf verbraucht, nicht sofort).
- [x] **`--working`-Debug-Flag gesperrt (Aufräum-Punkt aus Phase 2):** neue `DebugConfig.EnableWorkingStateDebugCommand` (Default `false`) gated `/bud state --working` — ohne die Config bleibt der Befehl wirkungslos (loggt eine Warnung), `WORKING` ist damit kein normaler, dauerhaft erreichbarer Spielerbefehl mehr, jetzt wo die Workstation-Bindung der echte Trigger ist.
- [x] `.\gradlew build` grün nach jedem Increment (Bindung/Entbindung → Fuel-Timer → Ruhepose → Debug-Flag-Gate, vier separate Build-Durchläufe).
  - Test noch offen (braucht laufenden Server + Client, von dir): Karte + Futter rein → Bud kommt zur Station (auch wenn vorher despawnt), steht dort im Working-State. Karte raus → Bud normal wieder verfügbar (im vorherigen Modus, nicht immer PetDefensive). Futter aufbrauchen lassen (Testintervall verkürzen) → Log zeigt Resting, zweites Gericht vorab einlegen → korrekt nachgereiht.

**Bekannte Lücke (nicht in diesem Durchlauf behoben):** `/bud state --defensive`/`--passive`/`--sitting` (auch mit gesetztem `EnableWorkingStateDebugCommand`) prüfen nicht, ob ein Bud gerade über eine Workstation gebunden ist — ein manueller State-Wechsel während der Bindung würde `BudComponent.currentState` von der Station lösen, ohne dass `WorkstationBlockEntity.boundBud`/Fuel-Tracking davon erfährt (Desync). Für den normalen Spielfluss unkritisch (kein regulärer Spielerbefehl kann das auslösen, `--defensive` etc. sind selbst schon debug-artige Flags ohne Berechtigungsprüfung), aber als bekannte Einschränkung festgehalten.

## Regression 1 nach Phase 4 — Futter (Slot 1) ließ sich nicht einlegen (behoben)

Vermuteter Root Cause aus der Cowork-Session ("SlotFilter ist global statt pro Slot") **per `javap -p -c` widerlegt**:

- [x] `SimpleItemContainer.setSlotFilter`/`testFilter` bytecode-verifiziert: Filter werden pro `FilterActionType` in einer `Map<FilterActionType, Int2ObjectConcurrentHashMap<SlotFilter>>` gehalten, `testFilter` liest exakt `slotFilters.get(actionType).get(slot)` — Slot 1 hatte nie einen Eintrag und ist damit strukturell unblockiert. Auch der zweite Insert-Pfad (`setItemStackForSlot`, für Fenster-Drag&Drop) ruft denselben `cantAddToSlot(slot, ...)`-Check auf derselben Slot-Basis auf. Kein Cross-Slot-Blocking, egal auf welchem Weg eingelegt wird.
- [x] `ItemContainerBlock.CODEC`s `"Capacity"`-Schlüssel bytecode-bestätigt exakt wie im JSON verwendet (`Codec.SHORT`, Validator `> 0`) — keine Fehlkonfiguration, `Capacity: 2` bindet korrekt.
- [x] **Tatsächliche Root Cause (bytecode-verifiziert gegen `ItemContainerSystems$OnAddedOrRemoved`):** `WorkstationFilterSystem.onEntityAdded` las die Komponenten über `Store.getComponent(...)` statt über `CommandBuffer.getComponent(...)`. Für einen frisch platzierten Block (`AddReason.SPAWN`) sind die soeben hinzugefügten Komponenten zu diesem Zeitpunkt im `Store` noch nicht committet — `Store.getComponent(...)` lieferte `null`, der frühe Return griff, **weder SlotFilter noch Change-Listener wurden je registriert**. Die native Engine-Entsprechung liest an exakt dieser Stelle bewusst über den `CommandBuffer`, nicht den `Store` (bytecode-verifiziert, inkl. `assert`-Absicherung dort). Nach einem Server-Neustart/Reload (`AddReason.LOAD`) sind die Komponenten dagegen längst committet, `Store.getComponent(...)` funktioniert dort zufällig — das erklärt, warum frühere Tests nach einem Relog liefen.
- [x] **Fix:** `onEntityAdded` liest beide Komponenten jetzt über `commandBuffer.getComponent(...)`.
- [x] `.\gradlew build` grün.

## Regression 2 nach Phase 4 — Slot-0-Kartenfilter griff nie, auf keinem Slot (behoben)

Nach Regression 1 gemeldeter Folgebefund: auch mit dem CommandBuffer-Fix akzeptierte Slot 0 **jede** Karte, unabhängig von der Rolle. Beide vom Auftrag vorgegebenen Hypothesen per `javap -p -c` geprüft:

- [x] **Hypothese 1 (Klonen der Container-Instanz beim Fenster-Öffnen) — widerlegt.** `OpenContainerInteraction` übergibt `containerBlock.getItemContainer()` unverändert (dieselbe Objektreferenz, kein `.clone()`-Aufruf) an den `ContainerBlockWindow`-Konstruktor — bytecode-bestätigt. Zusätzlich, falls doch irgendwo geklont würde: `SimpleItemContainer`s Kopier-Konstruktor kopiert `slotFilters` explizit über `Map.putAll(...)` mit — ein Filter würde einen Klon also ohnehin überleben.
- [x] **Hypothese 2 (falscher `FilterActionType` beim UI-Insert) — bestätigt, aber präziser als vermutet.** Spieler-Drag&Drop läuft **nicht** über `Window.handleAction`/`ItemContainerBlock` direkt, sondern über ein komplett eigenes Client→Server-Paket (`MoveItemStack`) → `InventoryPacketHandler` → `InventoryUtils.moveItem(...)` → `InventoryUtils.getSectionById(...)` (liefert korrekt dieselbe `ItemContainer`-Instanz über `player.getWindowManager().getWindow(sectionId)` → `ItemContainerWindow.getItemContainer()`) → `ItemContainer.moveItemStackFromSlotToSlot(...)`. Dieser Pfad ruft **nicht** `cantAddToSlot` (dort sitzt unser `SlotFilter`), sondern `cantMoveToSlot(ItemContainer, short)` auf — und `SimpleItemContainer.cantMoveToSlot(...)` ist bytecode-bestätigt ein **hartcodierter Stub, der immer `false` (nie blockiert) zurückgibt**, ohne `slotFilters`/`globalFilter` überhaupt zu konsultieren. Unser `SlotFilter` (`FilterActionType.ADD` über `setSlotFilter`) greift also nur für einen hypothetischen programmatischen `addItemStackToSlot`/`setItemStackForSlot`-Aufruf, nie für einen echten Spieler-Insert übers Fenster.
- [x] **Wichtiger Nebenbefund:** `assignOwner(...)` (Besitzer-Zuweisung aus Phase 3) hing bisher im `SlotFilter`-Callback — lief also aus demselben Grund **ebenfalls nie** bei einem echten Spieler-Insert. Das erklärt (zusätzlich zu Regression 1) das ursprünglich gemeldete "kein Spawn": selbst mit funktionierendem Fix für Regression 1 wäre `ownerPlayerId` nie gesetzt worden, `bind(...)` hätte immer beim frühen "kein Owner"-Log abgebrochen.
- [x] **Fix (da `cantMoveToSlot` nicht überschreibbar ist — Engine-Klasse, kein Erweiterungspunkt dafür vorhanden): reaktive statt proaktive Durchsetzung.** `WorkstationBindingHandler.reevaluate(...)` (bereits als Change-Listener registriert, feuert bei jeder Slot-Änderung unabhängig vom Einlege-Pfad) entfernt eine ungültige Karte aus Slot 0 sofort wieder (`ejectInvalidCard`) und gibt sie — sofern genau ein Spieler die Station gerade offen hat — über `SimpleItemContainer.addOrDropItemStack(...)` zurück in dessen Hotbar (oder droppt sie, falls kein Platz). Besitzer-Zuweisung (`assignOwner`) läuft jetzt ebenfalls aus `reevaluate(...)`, nicht mehr aus dem wirkungslosen `SlotFilter`-Callback. Der `SlotFilter` selbst bleibt als reine Doku/Defense-in-Depth für einen künftigen programmatischen Insert-Pfad bestehen, mit Kommentar, dass er Spieler-Inserts nicht abdeckt.
- [x] `.\gradlew build` grün.
  - [x] Ingame-Test durchgeführt (Sascha, frischer Build + Neustart, keine Slot-Verwechslung): **funktioniert weiterhin nicht** — beide Slots akzeptieren alles, keine Bindung. Siehe Regression 3 unten.

## Regression 3 nach Phase 4 — Bindung greift immer noch nicht (Untersuchung läuft, Diagnose-Logging eingebaut)

Regression 2s Fix hat laut Sascha's echtem Test **nicht** geholfen — Symptom identisch zu vorher. Verdacht diesmal: der `registerChangeEvent(...)`-Listener aus `WorkstationFilterSystem` feuert bei einem echten Spieler-Drag&Drop-Insert eventuell gar nicht — dieselbe Fehlerklasse wie beim `SlotFilter` zuvor (eine bytecode-plausible, aber real nicht zutreffende Annahme über einen Player-Interaction-Hook).

**Ausdrücklich diesmal nicht wieder nur bytecode-verifiziert, sondern real bewiesen:**

- [x] Temporäres `LoggerUtil.info(...)`-Logging eingebaut:
  - `WorkstationFilterSystem.onEntityAdded`: loggt bei jedem Aufruf `addReason` + Bestätigung, dass `SlotFilter`+Change-Listener registriert wurden.
  - `WorkstationBindingHandler.reevaluate(...)`: loggt bei jedem Aufruf den aktuellen Inhalt von Slot 0/Slot 1.
- [x] `.\gradlew build` grün (Erinnerung an mich selbst: das allein beweist nichts über Laufzeitverhalten — Sascha hat das zurecht eingefordert).
- [x] **Ingame-Test durchgeführt (Sascha) — Nachweis erbracht, ursprünglicher Verdacht widerlegt:**
  - `onEntityAdded fired for Workstation, addReason=LOAD` erscheint beim Serverstart. `reevaluate() called` erscheint zuverlässig bei jedem Insert/Remove. **Der `registerChangeEvent`-Listener feuert also korrekt — der in diesem Auftrag geäußerte Verdacht ("gleiche Fehlerklasse wie beim SlotFilter") ist widerlegt.**
  - Log zeigte bei vier Insert-Versuchen (2× Karte rein/raus) durchgehend `Slot0=null Slot1=<Karte>` — nie umgekehrt, nie beide gleichzeitig befüllt.
  - Gezielter Folgetest (Karte rein lassen, dann Futter in die zweite Box legen): **Sascha konnte physisch kein Futter in eine zweite Box legen** ("Ich kann da kein Futter dann reinlegen!") — es gibt clientseitig gar keine zwei unabhängig ansprechbaren Slots.
  - **Tatsächliche Root Cause:** nicht ein Hook, der nicht feuert (das war diesmal nicht das Problem), sondern das generische `ItemContainerBlock`+`"Open_Container"`-Fenster stellt bei `Capacity: 2` offenbar nur einen einzigen wirklich interaktiven Slot dar. Passt zu einem in Phase 3 bereits notierten, damals nicht weiterverfolgten Fund: alle nativen "Open_Container"-Beispiele im Spiel (Chests) verwenden `Capacity: 18` oder größer, keine einzige kleine/2er-Capacity als Präzedenzfall.
- [x] **Nächster, gezielter Test (kein blinder Fix, sondern minimal-invasiver Beweis-Versuch):** `Capacity` in `Workstation_Farming.json` von `2` auf `18` erhöht (kleinste real bestätigte Chest-Größe, z. B. `Furniture_Crude_Chest_Small.json`), Java-Logik unverändert (nutzt weiterhin nur Index 0/1, Rest bleibt ungenutzt). `.\gradlew build` grün.
  - **Wichtig für den Test:** `ItemContainerBlock`s eigener Codec persistiert den Container (inkl. Capacity) pro platziertem Block in der Welt-Save-Datei — die JSON-Änderung wirkt sich nicht rückwirkend auf die bereits platzierte Station aus. Für den Test muss eine **neue** Station gecraftet/platziert werden, nicht die alte wiederverwenden.
  - [ ] Noch offen (braucht laufenden Server + Client, von dir): neue Station platzieren, Karte in Slot 0 UND Futter in Slot 1 gleichzeitig einlegen (Diagnose-Logging ist noch aktiv) — funktionieren jetzt beide Slots unabhängig?
- [ ] Diagnose-Logging nach bestätigtem Fix wieder entfernen, `.\gradlew build` grün.

**Meta-Lektion (aktueller Stand, wird bei endgültiger Bestätigung finalisiert):** Zwei unterschiedliche Fehlerklassen wurden in dieser Phase fälschlich für dieselbe gehalten. Bei Regression 2 lag der Fehler tatsächlich in einem falsch angenommenen Java-Hook (`SlotFilter` wird nie konsultiert) — dort war bytecode-Verifikation die richtige Methode und hat auch funktioniert. Bei Regression 3 war die Ausgangsvermutung ("Hook feuert nicht") **falsch übertragen** von Regression 2 — der Hook feuerte die ganze Zeit einwandfrei, das Problem lag eine Ebene tiefer, in einem Bereich, den Bytecode-Analyse der Server-Klassen prinzipiell **nicht** sichtbar machen kann: der **Client-seitigen UI-Darstellung/Slot-Interaktion** für eine untypische `Capacity`-Größe. Das ist kein Java-Server-Bug, sondern eine Engine-Verhaltenseigenschaft, die nur durch echtes Ingame-Ausprobieren auffällt. Lektion: sobald der Fehlerort "wie reagiert der Client/das Spiel auf eine Konfiguration" statt "wie funktioniert eine Server-API" ist, ist `javap` gegen `reference/server` grundsätzlich blind — dort hilft nur der reale Test, keine noch so gründliche Bytecode-Analyse.

## Phase 3b — KURSKORREKTUR: Workstation auf Bench-Processing umbauen

Ingame-Test von Phase 3/4 gescheitert (Truhen-Fenster mit ~20 Slots statt 2, Karten landen in beliebigen Slots, Futter nicht einlegbar, keine Bindung, kein Spawn). Root Cause + Entscheidung: siehe `docs/bud-worker-mode-plan.md`, Abschnitt **"Kurskorrektur (nach gescheitertem Phase-3/4-Ingame-Test)"**. Kurz: `ItemContainerBlock` ist der Truhen-Baustein und war nie der richtige; Umstieg auf das native `Bench`-System mit `"Type": "Processing"` (Furnace-Muster).

**Erst verifizieren, dann bauen** — bei Fehlschlag einer der beiden Vorab-Fragen nicht improvisieren, sondern zurückmelden:

- [x] Kann eine Processing-Bench **ohne jedes Rezept/Output** existieren (nur Input-Slot + Fuel-Slot, kein Crafting)? **Ja, verifiziert.** Per `javap -p -c` gegen `BenchSystems$ProcessingBenchTick.tick(...)` komplett durchverfolgt: ist `recipe == null` und `ProcessingBench.shouldAllowNoInputProcessing()` (JSON `"AllowNoInputProcessing": true`) liefert `true`, läuft der Tick trotzdem in `advanceProcessing(...)`/`consumeFuelForDuration(...)` — reiner zeitbasierter Fuel-Verbrauch ganz ohne Rezept-Match. Furnace/Campfire nutzen das bereits so. Details in `docs/bud-worker-mode-plan.md`, "Vorab-Frage 1".
- [x] Wie wird ein Input-Slot auf einen bestimmten Item-Typ gefiltert, sodass es bei **Spieler-Drag&Drop** wirkt? **Geklärt: `SlotFilter`/`FilterActionType.ADD` wirkt zuverlässig, `ArmorSlotAddFilter` bestätigt exakt den bereits bekannten `setSlotFilter`-Mechanismus.** Der frühere Regression-2-Befund ("SlotFilter feuert nie bei Spieler-Inserts, `cantMoveToSlot` ist ein Stub") war **keine truhenspezifische Einschränkung, sondern eine unvollständige Ablaufverfolgung**: `cantMoveToSlot` ist tatsächlich ein Stub (`return false`), aber `InventoryUtils.moveItem(...)` → `ItemContainer.moveItemStackFromSlotToSlot(...)` ruft direkt danach zusätzlich `cantAddToSlot(...)` auf der Zielcontainer-Instanz auf — und genau dort wird der registrierte `SlotFilter` konsultiert. Das liegt auf der `ItemContainer`-Basisklasse, gilt also identisch für Chest/Rüstung/Bench. Details inkl. vollständiger Bytecode-Kette in `docs/bud-worker-mode-plan.md`, "Vorab-Frage 2".
- [x] `Workstation_Farming.json` auf `"Bench": {"Type": "Processing", "AllowNoInputProcessing": true, "Input": [{}], "Fuel": [{}], "OutputSlotsCount": 0}` + `"BlockEntity": {"Components": {"BenchBlock": {}, "ProcessingBenchBlock": {}, "WorkstationBlockEntity": {...}}}` + `"Interactions": {"Use": "Open_Processing_Bench"}` umgebaut, `ItemContainerBlock` entfernt. `Input`/`Fuel` bewusst ohne `ResourceTypeId`/`FilterValidIngredients`, damit kein natives Rezept-Filtering installiert wird — unser eigener `SlotFilter` übernimmt das exklusiv. Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün.
- [x] `WorkstationCardUtil`/`WorkstationFilterSystem`/`WorkstationBindingHandler`/`WorkstationFuelTickSystem` auf die Bench-Container umgezogen (`ProcessingBenchBlock.getInputContainer()`/`getFuelContainer()`, je eigener Slot 0, statt einem gemeinsamen Slot-0/1-Container). `WorkstationFilterSystem` installiert den `SlotFilter` jetzt nach exakt dem `ArmorSlotAddFilter`-Muster (`setSlotFilter(FilterActionType.ADD, ...)`) auf `getInputContainer()` — wirkt laut Vorab-Frage 2 jetzt nachweislich bei Spieler-Drag&Drop, kein reaktives Austauschen mehr nötig. **Fuel-Timer bewusst NICHT auf die native `ProcessingBenchBlock`/`BenchSystems$ProcessingBenchTick`-Mechanik umgestellt** — zwar bytecode-bestätigt, dass `AllowNoInputProcessing` den Tick ganz ohne Rezept laufen lässt (Vorab-Frage 1), aber die genaue Bootstrap-Bedingung, unter der `ProcessingBenchBlock.isActive()` im rezeptlosen Pfad erstmals `true` wird (Voraussetzung für `consumeFuelForDuration`), ließ sich nicht ohne Raten zu Ende verifizieren. Bewusste Entscheidung gegen weiteres Improvisieren (siehe Auftrag): der bereits funktionierende eigene `WorkstationFuelTickSystem`-Tick aus Phase 4 bleibt, nur auf `getFuelContainer()` umgehängt.
- [x] Toter Code aus dem Truhen-Ansatz entfernt: reaktives `ejectInvalidCard` komplett gestrichen (nicht mehr nötig, da der SlotFilter Inserts jetzt tatsächlich blockiert). `resolveSoleViewer`-Heuristik für den Besitzer bleibt bestehen (nur `ItemContainerBlock`→`BenchBlock`/`ContainerBlockWindow`→`BenchWindow` umgehängt) — geprüft, ob `ProcessingBenchWindow` den Akteur direkter liefert: nein, `BenchBlock.getWindows(): Map<UUID, BenchWindow>` ist bytecode-verifiziert strukturell identisch zum bisherigen `ItemContainerBlock.getWindows()`, kein direkterer Hook vorhanden.
- [x] Diagnose-Logging aus Regression 3 (`[BUD][DIAG] ...`) beim Neuschreiben der Dateien entfernt, per `grep` verifiziert (keine Treffer mehr).
- [x] `.\gradlew build` grün nach dem gesamten Umbau (ein Durchlauf, da alle vier Dateien zusammenhängen).
  - [x] **Ingame-Test teilbestätigt (Sascha):** Bench-Fenster zeigt korrekt FUEL/INPUT/OUTPUT statt Truhen-UI, Karte wird angenommen, Keyleth spawnt und geht in `WORKING` (folgt nicht, kämpft nicht). Falsche-Rollen-Karte-Ablehnung noch nicht explizit gegengetestet. **Futter wird nie verbraucht** (auch mit `FuelDurationSeconds = 10`) — siehe Regression unten.

## Regression nach Phase 3b — Futter wird nie verbraucht (behoben — war kein Bug, Instrumentierungs-Erkenntnis + drei Folgeaufträge umgesetzt)

Arbeitshypothese (Cowork, Code gelesen, **nicht** verifiziert): `WorkstationFilterSystem.onEntityAdded` fängt die `WorkstationBlockEntity`-Instanz aus dem `CommandBuffer` in den `registerChangeEvent`-Lambdas ein; `WorkstationBindingHandler.bind()` setzt `boundBud`/`fuelSecondsRemaining` auf dieser Instanz. Klont die Engine Komponenten beim Commit, läse `WorkstationFuelTickSystem.tick()` über `archetypeChunk.getComponent(...)` eine andere (geklonte) Instanz mit `boundBud == null` → früher Return, nie Fuel-Verbrauch, nie "resting"-Log.

- [x] Temporäres Diagnose-Logging eingebaut (`LoggerUtil.info` + `System.identityHashCode(workstation)`) an drei Stellen: `WorkstationFilterSystem.onEntityAdded` (Capture), `WorkstationBindingHandler.bind()` (nach `setBoundBud`), `WorkstationFuelTickSystem.tick()` (jeder Tick-Aufruf, inkl. `boundBud`/`resting`/`fuelSecondsRemaining`). `.\gradlew build` grün.
- [x] Zusätzlicher (nicht abschließender) Gegen-Check: `javap -p -c` gegen `CommandBuffer.addComponent(...)` zeigt keinen `.clone()`-Aufruf im Commit-Pfad selbst (Lambda reicht die Komponente unverändert an `Store.addComponent` durch) — schließt eine Klon-Quelle an anderer Stelle (Archetype-Migration) nicht aus, ersetzt nicht den verlangten Laufzeit-Beweis.
- [x] **Ingame-Test/Log-Auswertung (Sascha): Hypothese widerlegt.** `identityHashCode` in `bind()` und jedem `tick()`-Aufruf identisch (`1890585416`), `boundBud=keyleth`, `fuelSecondsRemaining` zählte sichtbar korrekt herunter. Kein Klon, keine Instanz-Divergenz — der Fuel-Tick funktionierte die ganze Zeit. Der Bytecode-Gegen-Check oben war also zutreffend, nicht nur ein Indiz: `CommandBuffer`-Komponenten werden unverändert committet. In `docs/bud-worker-mode-plan.md` als widerlegt festgehalten, damit nicht erneut untersucht wird.
- [x] Diagnose-Logging entfernt, `.\gradlew build` grün.
- [x] Nebenbefund behoben: `"is resting."`/`"resumes work."`-Logs standen auf `fine` (im normalen Serverlog unsichtbar) — auf `info` angehoben.

**Tatsächliche Ursache des von Sascha beobachteten Symptoms** ("TURN OFF" gedrückt, dachte an zwei parallele Verbrauchssysteme): siehe Folgeauftrag 1 unten — der native Verbrauch lief nie parallel mit, `AllowNoInputProcessing` aktiviert ihn strukturell gar nicht ohne Rezept. "TURN OFF" ist ein reiner Client-Toggle ohne Bezug zum (funktionierenden) eigenen Tick.

## Folgeauftrag 1 — eigenen Fuel-Tick durch nativen Bench-Verbrauch ersetzen: NICHT möglich, zurückgemeldet statt improvisiert

- [x] `isActive()`-Bootstrap im `AllowNoInputProcessing`-Pfad per `javap -p -c` bis zu Ende geklärt (nicht nur bis zum Tick-Level wie bei der ursprünglichen "Vorab-Frage 1"): `ProcessingBenchBlock.advanceProcessing(...)` — die einzige Methode, die `consumeFuelForDuration(...)` aufruft — kehrt bytecode-bestätigt **sofort mit 0 zurück, sobald `recipe == null`**, unabhängig von `AllowNoInputProcessing` (das Flag beeinflusst nur den vorgelagerten Tick-Zweig: unterdrückt den "Failed"-Sound, macht `advanceProcessing` aber nicht rezeptlos lauffähig). Zusätzlich alle `setActive(...)`-Aufrufstellen in `ProcessingBenchBlock`/`ProcessingBenchTick`/`ProcessingBenchWindow` durchsucht: `active` wird nur `true`, wenn die Bench **keinen** Fuel-Slot konfiguriert hat (nicht unser Fall) — der "TURN OFF"-Knopf im Fenster ist nur ein manueller Client-Toggle, unabhängig vom Verbrauch. Details in `docs/bud-worker-mode-plan.md`, "Vorab-Frage 1, Korrektur".
- [x] **Ergebnis: native Fuel-Konsumption ist für die Workstation strukturell unerreichbar**, da unser Input-Slot nie eine echte Crafting-Zutat enthält (nur die Bud-Karte) und `recipe` deshalb dauerhaft `null` bleibt. Ein Umstieg wäre nur über ein Dummy-Rezept möglich gewesen — genau die Art Krücke, die vermieden werden sollte. **Zurückgemeldet statt improvisiert, wie beauftragt.**
- [x] Entscheidung: `WorkstationFuelTickSystem` bleibt bestehen (funktioniert bereits, per Diagnose-Test bestätigt). Doku-Kommentar in der Klasse korrigiert (vorher fälschlich "native Mechanik funktioniert, nur `isActive`-Bootstrap unklar" — jetzt korrekt: strukturell unmöglich, nicht nur unklar).
- [x] Die ursprüngliche "Vorab-Frage 1"-Antwort in `docs/bud-worker-mode-plan.md` als teilweise falsch markiert und korrigiert (Analyse hatte am Tick-Level aufgehört, ohne `advanceProcessing` selbst zu prüfen).
- [x] `.\gradlew build` grün.

## Folgeauftrag 2 — Karte raus despawnt den Bud (Design-Änderung)

- [x] `WorkstationBindingHandler`: `unbind(...)` in zwei Methoden aufgeteilt, je nach Auslöser — siehe `docs/bud-worker-mode-plan.md`, "Karte raus despawnt den Bud (Design-Änderung)":
  - `despawnBoundBud(...)` — ausgelöst durch `reevaluate(...)` bei explizit entfernter/getauschter Karte. Despawnt den Bud über dieselben drei Schritte wie `CleanupUtil.cleanupAllBuds`/`/bud delete` (`PlayerBudComponent.removeCurrentBud`, `Store.removeEntity(..., RemoveReason.REMOVE)`, `Orchestrator.purgeBud(...)`) — nicht neu erfunden.
  - `restoreOnStationRemoved(...)` — ausgelöst durch `WorkstationFilterSystem.onEntityRemove` (vorher No-op), wenn die Station selbst zerstört/entladen wird während ein Bud gebunden ist. Hier keine Karten-Aktion, also kein Despawn — `previousState` wird wiederhergestellt, Bud bleibt in der Welt (identisch zum alten Phase-4-Verhalten).
- [x] **`previousState`-Frage begründet entschieden:** entfällt nicht komplett — bleibt der Mechanismus für den "Station abbauen"-Fall (jetzt tatsächlich implementiert statt nur ein No-op-Kommentar wie vorher).
- [x] `.\gradlew build` grün.
- [ ] Ingame-Test noch offen (braucht Sascha): Karte raus bei gebundenem Bud → Bud despawnt (wie `/bud delete`). Station abbauen bei gebundenem Bud → Bud bleibt, kehrt in vorherigen Modus zurück.

## Folgeauftrag 3 — Bindung muss Relog/Serverneustart überleben (umgesetzt)

Siehe `docs/bud-worker-mode-plan.md`, "Persistenz über Relog/Neustart" — zwei Optionen ausgearbeitet, Sascha hat Option B bestätigt und zusätzlich verlangt, die Login-Timing-Lücke ohne neuen Lookup mitzulösen.

- [x] **Option B umgesetzt:** `WorkstationFilterSystem.onEntityAdded` ruft nach Filter-/Listener-Installation einmalig `WorkstationBindingHandler.reevaluate(...)` auf (für `AddReason.LOAD` **und** `AddReason.SPAWN`) — nutzt den bereits nativ persistierten Karten-/Futter-Inhalt (`ProcessingBenchBlock.CODEC`s `InputContainer`/`FuelContainer`, bytecode-verifiziert), kein neues Persistenz-Feld, bestehende `bind()`-Logik unverändert wiederverwendet.
- [x] **Login-Timing-Lücke mitgelöst, ohne neuen "alle Stationen eines Spielers"-Lookup:** `WorkstationFuelTickSystem` prüft jetzt bei `boundBud == null` zusätzlich, ob eine gültige Rollen-Karte im Input-Slot liegt, und stößt gedrosselt (neue `WorkConfig.RebindRetrySeconds`, Default 10s) erneut `reevaluate(...)` an — läuft huckepack auf dem ohnehin schon pro Tick über jede Station laufenden Fuel-System, kein Spam bei länger offline bleibendem Besitzer (neues transientes `WorkstationBlockEntity.rebindRetrySecondsRemaining`-Feld, nicht persistiert, gleiches Muster wie `fuelSecondsRemaining`).
- [x] `.\gradlew build` grün.
- [x] **Ingame-Test (Sascha), Runde 2: Punkte 1 (Karte raus → Despawn), 2 (Relog → Bindung wiederhergestellt), 5 (Fuel/Resting/Resume-Logs) bestätigt.** Vier weitere Punkte gemeldet (Slot-Filter, verwaister Bud bei Stations-Abbau, TURN-OFF-Knopf, fehlende Animation) — siehe eigener Abschnitt unten.

## Phase-4-Ingame-Test, Runde 2 — vier offene Punkte (A–E)

Details/Begründungen in `docs/bud-worker-mode-plan.md`, "Phase-4-Ingame-Test, Runde 2".

### A) Slot-Filter blockiert immer noch nicht (dritter Anlauf) — NUR Logging, kein Fix ohne Test (gelöst)

- [x] Diagnose-Logging direkt im `SlotFilter`-Callback selbst eingebaut (`WorkstationFilterSystem`, `[BUD][DIAG]`, loggt `actionType`/`slot`/`itemStack`/`workRole`/Ergebnis bei jedem Aufruf). `.\gradlew build` grün.
- [x] Vergleich mit nativem Furnace (`"FilterValidIngredients": true`) vorbereitet: nutzt bytecode-verifiziert exakt denselben `setSlotFilter(FilterActionType.ADD, ...)`-Mechanismus wie wir — kein alternativer nativer Ansatz zum Kopieren vorhanden.
- [x] **Ingame-Test (Sascha): funktioniert jetzt** — Veri-/Gronkh-/Roster-Karten werden im Karten-Slot korrekt abgelehnt. Root Cause nicht eindeutig rekonstruierbar (am Filter-Code selbst wurde zwischen den Runden nichts geändert) — plausibelste, nicht verifizierte Erklärung (stale Station-Instanz aus früherer Testrunde, siehe Regression-3-Präzedenzfall) im Plan-Doc festgehalten, damit nicht erneut danach gesucht wird.
- [x] Diagnose-Logging entfernt, `.\gradlew build` grün.

### B) Station abbauen ließ den Bud verwaist zurück (behoben, siehe auch Punkt 1 unten)

- [x] Zwei Pfade (Container-Change-Despawn vs. `onEntityRemove`-State-Restore) zu einem vereinheitlicht — Sascha's Vorschlag übernommen: Station abbauen = Despawn, wie Karte rausnehmen. Begründung (Karte verlässt den Slot so oder so) siehe Plan-Doc.
- [x] `WorkstationBindingHandler.despawnBoundBud(...)` ist jetzt der einzige Cleanup-Pfad (aus `reevaluate(...)` **und** `WorkstationFilterSystem.onEntityRemove`), idempotent (`getBoundBud() == null` bricht sofort ab) — sicher, falls doch beide Pfade feuern.
- [x] `restoreOnStationRemoved(...)` entfernt, `BudComponent.previousState`/Getter/Setter komplett entfernt (per `grep` verifiziert: keine anderen Aufrufer im Projekt) — totes Feld nach der Design-Änderung.
- [x] `.\gradlew build` grün.
- [x] **Ingame-Test (Sascha): Bud despawnte NICHT** — `IllegalStateException: Store is currently processing!` beim `removeEntity`-Aufruf, da dieser synchron aus dem System-Callback lief. Root Cause + Fix siehe Runde 3, Punkt 1 unten.

### C) "TURN OFF"-Knopf wirkte nicht (behoben, bestätigt)

- [x] Per `javap -p -c` gegen `ProcessingBenchWindow.handleAction(...)` verifiziert: Knopf sendet `SetActiveAction`-Paket → direkt an `ProcessingBenchBlock.setActive(boolean, ...)`, lesbar über `isActive()`. Für eine Bench mit Fuel-Slot (unser Fall) wird `active` nirgends automatisch `true` — Default bleibt `false`, bis der Spieler den Knopf drückt.
- [x] `WorkstationFuelTickSystem.tick(...)` liest jetzt `isActive()` — bei "aus" vollständige Pause (kein Countdown, kein Verbrauch, kein Resume-Versuch), bewusst kostenlos wieder aufnehmbar (anders als `isResting()`, das Fuel-Verbrauch fürs Aufwachen kostet).
- [x] `WorkstationBindingHandler.bind(...)` setzt beim Binden explizit `setActive(true, ...)` — sonst hätte jede frisch gebundene/geladene Station sofort pausiert (Regression des bereits bestätigten Fuel-Verbrauchs).
- [x] `.\gradlew build` grün. Kein Fall für "Knopf ausblenden" — sauber serverseitig auslesbar.
- [x] **Ingame-Test (Sascha) bestätigt:** TURN OFF pausiert den Verbrauch korrekt.

### D) Keine Animation (umgesetzt, Working-/Resting-Sub-States — Serverstart/Sichtbarkeit bestätigt, Zustände waren vertauscht, siehe Runde 3 Punkt 4)

- [x] Sub-State-Mechanismus geklärt: `StateSupport.setSubState(String)` (öffentlich, bytecode gelesen) — ruft `getSubStateIndex(topLevelStateIndex, name)`, no-op bei ungültigem Namen (kein Crash-Risiko, nur kosmetisch).
- [x] Führender-Punkt-Frage aus Phase 4 beantwortet: `javap` gegen `BuilderBase.getDefaultSubState(...)` zeigt den JSON-Fallback-Literal `"Default"` **ohne** Punkt — der Punkt in `.Fighting`/`.Default`/`.Resting` ist reine JSON-Konvention, intern kein Bestandteil des Namens. Java-seitiger `setSubState(...)`-Aufruf nutzt entsprechend `"Resting"`/`"Default"` ohne Punkt.
- [x] Alle drei `Template_{Veri,Keyleth,Gronkh}_Bud.json` identisch umgebaut, Struktur 1:1 an MODE 3s bewiesenem `.Fighting`/`.Default`-Muster orientiert.
- [x] `WorkstationFuelTickSystem.setRestingSubState(BudComponent, boolean)` — einmal pro Tick berechnet (`pausedByBench || isResting()`) und angewandt, deckt beide Pause-Gründe einheitlich ab, idempotent.
- [x] Mit PowerShell `ConvertFrom-Json` auf allen drei Dateien geprüft, `.\gradlew build` grün.
- [x] **Ingame-Test (Sascha):** Serverstart sauber, Crouch wechselte sichtbar — **aber vertauscht** (Working=Crouch, Resting=stehend, statt umgekehrt). Fix siehe Runde 3, Punkt 4 unten.

### E) "No BudComponent found for player"-Log (geklärt, behoben)

- [x] Root Cause: `getRandomBudComponent(...)` filtert `WORKING`-Buds heraus (Phase-2-Chokepoint) — ein Spieler mit genau einem arbeitenden Bud hat legitim null "eligible" Buds für die Dauer der Arbeit. Kein Zusammenhang mit dem verwaisten Bud aus B, kein Bug — nur falscher Log-Level.
- [x] Alle drei Fundstellen (`WorldTracker`, `WeatherTracker`, `PlayerStateTracker`) von `warning` auf `fine` heruntergestuft, mit erklärendem Kommentar.
- [x] `.\gradlew build` grün.

## Phase-4-Ingame-Test, Runde 3 — sechs Punkte (1–6)

Details/Begründungen in `docs/bud-worker-mode-plan.md`, "Phase-4-Ingame-Test, Runde 3".

### 1) "Store is currently processing!" — Entity-Operationen aus System-Callbacks verlagert (behoben, blockierte zwei Fehler)

- [x] Root Cause bestätigt (Sascha, aus echtem Stacktrace): alle Entity-Mutationen in `WorkstationBindingHandler` liefen synchron direkt aus System-Callbacks — die ECS `Store` verbietet das reentrant.
- [x] Mechanismus verifiziert: `World.execute(Runnable)` reiht bytecode-bestätigt **unbedingt** in eine Task-Queue ein (nie inline), gedraint an sicheren Punkten im World-Tick über `consumeTaskQueue()` — derselbe Mechanismus, den `CleanupUtil`/`/bud delete` bereits nutzt.
- [x] `WorkstationBindingHandler.bind(...)`/`despawnBoundBud(...)` validieren/räumen synchron auf (reine Lesevorgänge bzw. POJO-Feldwrites), verlagern die eigentlichen Entity-Mutationen über `world.execute(...)` in neue `performBind(...)`/`performDespawn(...)`-Methoden.
- [x] Geprüft, ob die bisher funktionierenden Bind-Pfade zufällig unkritisch liefen: nein, **alle** Aufrufer von `reevaluate(...)` sind System-Callbacks — kein bisher genutzter sicherer Pfad, jetzt einheitlich deferred.
- [x] `.\gradlew build` grün.
- [ ] Ingame-Test noch offen (braucht Sascha): Station abbauen → Bud despawnt jetzt wirklich. Relog mit Karte in der Station → Bud gebunden statt auf Spieler fokussiert.

### 2) Bindung ohne Fuel-Bedingung (Design-Änderung, umgesetzt)

- [x] `reevaluate(...)` bindet jetzt bei gültiger Karte allein, ohne Fuel-Bedingung. `performBind(...)` prüft Fuel-Stand separat und startet ohne Futter direkt in `resting = true`.
- [x] `.\gradlew build` grün.
- [ ] Ingame-Test noch offen (braucht Sascha): Nur Karte einlegen (kein Futter) → Bud spawnt sofort, im Ruhezustand.

### 3) Spawn-Position vor der Station statt drin (umgesetzt)

- [x] `BudManager.findFreeLateralPosition(...)` auf `public static` verbreitert (Parametername generalisiert, keine Verhaltensänderung) — wiederverwendet statt neu gebaut.
- [x] Neue `WorkstationBindingHandler.resolveSpawnPositionInFrontOfStation(...)`: Ausrichtung über native `VariantRotation`/`BlockSection.getRotationIndex(...)` (bytecode-verifiziert, gleicher Pfad wie nativer Bench-Tick), 1 Block Abstand, Freiraum-Suche über die wiederverwendete Methode.
- [x] Nicht laufzeit-verifiziert: exaktes Mapping Rotations-Index → `Rotation`-Enum-Reihenfolge — rein kosmetisches Risiko (falsche Blickrichtung, kein Absturz), im Plan-Doc vermerkt.
- [x] `.\gradlew build` grün.
- [ ] Ingame-Test noch offen (braucht Sascha): Bud spawnt/erscheint einen Block vor der Station, nicht mehr drin.

### 4) Working-/Resting-Animation vertauscht (behoben)

- [x] `.Resting` → `Crouch: true` (sitzend/geduckt), `.Default` → `Crouch: false` (stehend/aktiv) — in allen drei Rollen-JSONs getauscht.
- [x] Bewegung/Laufen im Working-State bewusst nicht angefasst (kommt mit Phase 5, von Sascha für Phase 4 explizit als OK bestätigt).
- [x] Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün.
- [ ] Ingame-Test noch offen (braucht Sascha): Working = stehend/aktiv, Resting = Crouch/sitzend.

### 5) Zweiter Input-Slot ungefiltert (behoben)

- [x] `WorkstationFilterSystem` installiert zusätzlich einen `SlotFilter` auf `SECONDARY_INPUT_SLOT = 1`, der Bud-Karten ablehnt (`resolveBudId(itemStack) == null`) — echter Seedbag-Filter folgt mit Phase 5/8.
- [x] `.\gradlew build` grün.
- [ ] Ingame-Test noch offen (braucht Sascha): Bud-Karte lässt sich nicht mehr in den zweiten Slot ziehen.

### 6) "No FuelDropItemId defined..."-Warnung (geklärt, harmlos, keine Änderung)

- [x] Per `javap` nachvollzogen: `ProcessingBenchBlock.dropFuelItems(...)` loggt das, wenn `FuelDropItemId` fehlt, und setzt danach `fuelTime = 0`. Der geloggte Wert (`0.0`) ist für unsere Workstation strukturell immer 0, da unser eigener Fuel-Tick das native `fuelTime`-Feld nie anfasst — es geht nichts verloren.
- [x] `reference/assets` durchsucht: kein natives Bench-Asset (auch nicht Furnace) setzt `FuelDropItemId` — kein Präzedenzwert zum Kopieren, deshalb bewusst kein Rateversuch, JSON unverändert gelassen, als harmlos dokumentiert.

**Keine Phase 5** — Punkte 1–5 brauchen noch Ingame-Bestätigung.

## Phase 5 — Farming-Loop: Boden

Blaupause komplett gelesen: `AncientConstructs-1.2.2`s `Construct_Worker_Gardener.json`. Details/Begründungen in `docs/bud-worker-mode-plan.md`, "Phase 5 — Farming-Loop: Boden, Vorab-Verifikation".

### Vorab-Verifikation (nativer Weg vs. eigene Klassen) — abgeschlossen

- [x] **Finden (Sensor): nativ ausreichend, kein `FindUntilledSoilSensor` nötig.** Nativer `"Type": "Block"`-Sensor + eigene `BlockSet`-Asset (kein Java) — verifiziert über `BuilderSensorBlock`s `requireAsset(...)`/`BlockSetExistsValidator`.
- [x] **Tillen (Action): nativ NICHT ausreichend, eigene `TillSoilAction` nötig — konkret bewiesen, nicht nur vermutet.** `ActionPlaceBlock.canExecute(...)` (komplett durchverfolgt) validiert über `BlockPlacementHelper.canPlaceBlock`/`canPlaceUnitBlock` — dieselbe "Ziel muss leer sein"-Prüfung wie normales Blockplatzieren, würde das Ersetzen eines massiven Dirt-Blocks strukturell ablehnen. Kein natives `ChangeBlock`-NPC-Action, `BlockHitInteraction` hart an Charge-Attacken gekoppelt.
- [x] **Positionsübergabe Sensor→Action verifiziert** (nicht geraten): `Action.canExecute(Ref, Role, InfoProvider, double, Store)` — `InfoProvider` ist real Teil der Signatur (Phase-0-Zusammenfassung hatte das nicht vollständig wiedergegeben). `ActionPlaceBlock` liest darüber generisch die Position via `infoProvider.getPositionProvider().providePosition(...)` — unsere `TillSoilAction` bekommt automatisch dieselbe Position vom vorangehenden nativen Block-Sensor.
- [x] `NPCPlugin.get()` (öffentlicher Static-Singleton) löst die in Phase 0 zurückgestellte Frage nach einer `NPCPlugin`-Instanz für `registerCoreComponentType(...)`.
- [x] `World`/`BlockAccessor.setBlock(int,int,int,String)` (genauer: `IChunkAccessorSync.setBlock(...)`, `void`, nicht `boolean`) reicht für den eigentlichen Blockwechsel, kein manuelles `WorldChunk`/Chunk-Key-Handling nötig.
- [x] **Feld-Radius:** `WorkConfig.FieldRadius` bleibt die einzige durchgesetzte Grenze — geprüft in Java (`TillSoilAction` gegen einen neuen `BudComponent.workstationAnchor`), nicht im JSON. Die JSON-Sensor-`Range` ist bewusst nur ein großzügiger nativer Suchradius, kein Duplikat. Begründung: `"Compute"`-Parameter sind rein statisch beim Rollen-Laden geparst, kein Weg zu einem Live-Java-Wert ohne unverifizierten eigenen Compute-Provider.

### Umsetzung

- [x] `src/main/resources/Server/Item/Block/Sets/Bud_Tillable_Soil.json` — eigene `BlockSet`-Asset, exakt die 16 Quell-Block-IDs aus `Hoe_Till.json`s `Changes`-Map (Format an `Feran_Bed.json` orientiert).
- [x] `BudComponent.workstationAnchor` (`@Nullable Vector3d`, nicht persistiert, gleiches Muster wie `currentState`/`currentMood`) — gesetzt in `WorkstationBindingHandler.performBind(...)` (neue `resolveStationGroundPosition(...)`, aus der bestehenden Spawn-Positions-Auflösung extrahiert statt dupliziert), gelöscht in `despawnBoundBud(...)`.
- [x] `com.bud.feature.work.farming.BuilderActionTillSoil` + `TillSoilAction` (`ActionBase`, keine JSON-Extra-Felder) — `canExecute` liest Position via `InfoProvider`, prüft `WorkConfig.FieldRadius` gegen `BudComponent.workstationAnchor`; `execute` ruft `world.setBlock(x,y,z,"Soil_Dirt_Tilled")`. In `BudPlugin.setup()` registriert: `NPCPlugin.get().registerCoreComponentType("TillSoil", BuilderActionTillSoil::new)`.
- [x] Alle drei `Template_{Veri,Keyleth,Gronkh}_Bud.json` identisch erweitert: zwei neue Sibling-Instructions innerhalb MODE 4s `Instructions`-Array (nicht in `.Default` verschachtelt, um die unbelegte "Actions+Instructions im selben Objekt"-Kombination zu vermeiden — stattdessen `{"Type":"And","Sensors":[{State:.Default},{Block-Sensor}]}` als Gate, exakt das Gardener-Sibling-Priority-Muster). Priorität 1: großer Range (10) → `Seek`. Priorität 2: kleiner Range (1.75) → `Timeout` → `{"Type":"TillSoil"}`. Nur aktiv während `.Default` (nicht `.Resting`) — Futter-/Bench-Aus-Gate damit automatisch geerbt aus Phase 4, kein Extra-Code nötig.
- [x] Mit PowerShell `ConvertFrom-Json` auf allen drei Dateien geprüft, `.\gradlew build` grün nach jedem Block.
- [ ] **Ingame-Test noch offen (braucht Sascha, wichtigster Punkt hier — Rollen-JSONs wurden erneut angefasst):** Serverstart sauber (keine `FAIL`/`Reference to unknown builder`-Zeilen)? Keyleth tillt sichtbar Boden im Feld um die Station? Bleibt innerhalb der Feldgrenze (`WorkConfig.FieldRadius`, aktuell Default `1` — sehr klein, ggf. für einen sichtbaren Test hochsetzen)? Hört bei leerem Futter/TURN OFF auf zu tillen und geht in die Ruhepose?

### Serverstart-Regression nach Phase 5 (behoben) — `FAIL: ...Template_*_Bud.json: Once`/`Enabled`

Details/Bytecode-Belege in `docs/bud-worker-mode-plan.md`, "Serverstart-Regression nach Phase 5: `FAIL: ...Template_*_Bud.json: Once`/`Enabled` — Ursache & Fix".

- [x] Ursache per `javap` bestätigt: `BuilderActionTillSoil.readConfig(JsonElement)` rief `readCommonConfig(json)` ein zweites Mal auf — `BuilderBase.readConfig(BuilderContext, ...)` (engine-verwaltet) ruft das bereits automatisch vor dem Subklassen-`readConfig` auf. Vergleich mit nativem `BuilderActionSetBlockToPlace` bestätigt: kein erneuter Aufruf im Subklassen-Override.
- [x] Sascha-Frage 3 (Registrierungs-Timing) geprüft und widerlegt: `BudPlugin.setup()` läuft laut Log um 17:02:45, zwei Sekunden vor `"Loading NPC assets phase..."` (17:02:47). `AncientConstructs-1.2.2` registriert seine sechs Actions/Sensoren nach `javap`-Befund über dasselbe Muster (`registerCoreComponentType(...)` in `setup()`) — kein Timing-Problem.
- [x] Fix: `readConfig(JsonElement)`-Override ersatzlos entfernt (geerbter Default `return this;` reicht, `TillSoilAction` hat keine eigenen Felder), ungenutzte Imports entfernt. `.\gradlew build` grün.
- [x] **Ingame-Test:** Serverstart sauber, aber Keyleth blieb in einer Endlosschleife stehen (Deadlock, s. u.) — siehe nächster Abschnitt.

### Deadlock durch gecachten nativen Block-Sensor — Architekturumstellung (behoben)

Details/Bytecode-Belege in `docs/bud-worker-mode-plan.md`, "Phase 5, Redesign: Deadlock durch gecachten nativen Block-Sensor — Architekturumstellung".

- [x] Ursache per `javap` gegen `SensorBlock` bestätigt: der native Sensor cacht sein gefundenes Ziel (`BlockTarget`) und prüft bei Wiederverwendung nur gegen seine eigene, NPC-relative Range — kennt `TillSoilAction`s `FieldRadius`-Ablehnung nicht, das abgelehnte Ziel bleibt dauerhaft gecacht.
- [x] `ActionStorePosition`/`SensorReadPosition`/`ActionSetLeashPosition`/`SensorLeash` vorab geprüft (`javap -p -c`, alle vier) — kein Java-seitiger Setter für einen `MarkedEntitySupport`-Slot vorhanden, native Lösung damit nicht möglich, minimaler eigener Sensor bestätigt als kleinste tragfähige Option.
- [x] Neue Architektur umgesetzt: `WorkstationFuelTickSystem.updateWorkTarget(...)` wählt das Ziel (Station bestimmt, Bud führt nur aus), `BudComponent.workTarget` (neues Feld) trägt es, `WorkTargetSensor`/`BuilderWorkTargetSensor` (`"Type": "WorkTarget"`) lesen es nur zurück, kein Scannen/Range im Sensor.
- [x] Einfacher Skip-Mechanismus: `WorkstationBlockEntity.recentlyFailedTargets` (4er-FIFO, kein TTL nötig) + `targetElapsedSeconds` gegen neues `WorkConfig.TargetTimeoutSeconds` (Default 8s) — nicht erreichbare/tillbare Ziele werden übersprungen statt die Schleife zu blockieren.
- [x] Keyleth-Rollen-JSON: nativer Block-Sensor + `Range: 10` entfernt, eine Instruction kombiniert `WorkTarget`-Sensor + `Seek`-BodyMotion + `Timeout→TillSoil`-Action (Präzedenz für Actions+BodyMotion im selben Objekt: `.Resting`-Instruction, MODE 4). Mit PowerShell `ConvertFrom-Json` geprüft.
- [x] Detail 1 (Distanz war 3D statt horizontal+Höhe getrennt): `TillSoilAction.isWithinFieldRadius` prüft jetzt `dx²+dz²` gegen `FieldRadius` und Höhe separat gegen neues `WorkConfig.FieldMaxHeight` (Default 3). `TillSoilAction` prüft zusätzlich eine `INTERACTION_RANGE` (1.75, per `TransformComponent`) — ersetzt die frühere Sensor-Prioritätsstufe.
- [x] Detail 2 (`WorkConfig` Live-Reload): geprüft, `WorkConfig.setInstance(...)` läuft nur einmal in `BudPlugin.setup()`, kein Reload-Mechanismus vorhanden (kein Bug) — **für den Testablauf wichtig: nach einer `WorkConfig`-Änderung Server neu starten, nicht nur Config-Datei speichern.**
- [x] `[BUD-TEMP-DEBUG]`-Logging aus `TillSoilAction` wieder entfernt.
- [x] README-Konfigurationstabelle um neuen "Work Configuration"-Abschnitt ergänzt (bestand vorher komplett nicht, obwohl `WorkConfig` seit Phase 4 existiert).
- [x] `.\gradlew build` grün nach jedem Block.
- [x] **Ingame-Test:** Serverstart + Deadlock behoben, aber neuer Absturz: `IllegalArgumentException: Unknown key! Bud_Tillable_Soil` in `BlockSetModule.blockInSet(...)` auf dem World-Thread — siehe nächster Abschnitt.

### Server-Crash: `BlockSetModule` kannte unser BlockSet nicht (behoben)

Details/Log-Belege in `docs/bud-worker-mode-plan.md`, "Server-Crash: `BlockSetModule` kannte unser BlockSet nicht — Ursache & Fix".

- [x] Ursache per Server-Log-Zeitstempel bestätigt: `BlockSetModule` baut seine Lookup-Tabelle beim Core-Modul-Setup, **vor** dem Laden der Plugin-Assets — kennt `Bud_Tillable_Soil` deshalb nie.
- [x] Alternative (a) geprüft (BlockSet-Asset live über den Asset-Store lesen) — verworfen: `BlockSet` (die Konfig-Klasse selbst) ist per `javap -v` ebenfalls `@Deprecated(forRemoval=true)`, zweiter unabhängiger Fund.
- [x] Umgesetzt: Alternative (b) — die 16 tillbaren Blocktyp-Namen als Java-Konstante (`WorkstationFuelTickSystem.TILLABLE_BLOCK_TYPES`), Zugehörigkeit über `BlockType.getId()` (nicht deprecated), kein Asset-Store-Zugriff mehr, keine Timing-Abhängigkeit.
- [x] Absicherung: `WorkstationFuelTickSystem.tick()` umschließt `updateWorkTarget(...)` jetzt mit `try/catch(RuntimeException)` — ein Fehler beim Ziel-Scan darf den World-Thread nie wieder crashen, egal welcher Art.
- [x] Regel fürs Plan-Doc festgehalten: Plugin-Assets laden nach Core-Modul-Init — jede Java-API mit einer beim Start einmalig gebauten Index-Tabelle kennt Plugin-Assets grundsätzlich nicht, unabhängig vom Deprecation-Status.
- [x] `provideFeature(Feature.Position)`-Fix in `BuilderWorkTargetSensor` (Sascha) gegengeprüft: Signatur/Platzierung korrekt, kein doppelter `readCommonConfig`-Aufruf.
- [x] `.\gradlew build` grün.
- [x] **Ingame-Test:** kein Absturz mehr, aber Keyleth bleibt regungslos stehen, ohne jede Log-Zeile — Ursache in der fünfstufigen Kette (Kandidat finden → workTarget setzen → WorkTargetSensor → Seek → TillSoil) unklar.

### Diagnoselauf für stille Endlosschleife (läuft, wartet auf Saschas Log)

- [x] Gedrosseltes `[BUD-TEMP-DEBUG]`-Logging (WARNING, alle 2s je Klasse) über alle sechs Stufen gebaut: `WorkstationFuelTickSystem.tick` (boundBud/isResting/isActive), `updateWorkTarget` (Aufruf, currentTarget, Scan-Ergebnis), `findNearestTillableBlock` (Anker, Radius/MaxHeight, Anzahl geprüfter Positionen, Stichprobe der tatsächlich gelesenen `world.getBlockType(...).getId()`-Namen — Hauptverdacht: weichen von `TILLABLE_BLOCK_TYPES` ab), `WorkTargetSensor.matches` (Aufruf, workTarget, Ergebnis), `TillSoilAction.canExecute`/`execute` (erreicht, Interaction-/Field-Radius-Ergebnis, tatsächliches Tillen).
- [x] `.\gradlew build` grün (inkl. Saschas `addWeapon`-Entfernung aus `spawnAtStation`/`teleportToStation` gegengeprüft).
- [x] **Hinweis:** beide gelieferten Logs (`2026-08-13_13-39-42`/`13-51-07`) enthielten keine `[BUD-TEMP-DEBUG]`-Zeile — gegen ein älteres Jar getestet, Diagnoselauf muss wiederholt werden. Instrumentierung unverändert gelassen.
- [x] **Echte Lücke (Sascha beobachtet + korrekt diagnostiziert), unabhängig gefixt:** `findNearestTillableBlock` akzeptierte auch vergrabene tillbare Blöcke (Erde unter Gras/unter der Workstation) - nie tillbar, da Tillen eine freie Oberseite voraussetzt (native `Seed_Condition.json`: `"Face": "Up"`). Erklärt das beobachtete "läuft in die Workstation, bleibt stehen". Fix per `javap` gegen `BlockPlacementHelper` verifiziert (dieselbe Klasse aus der Phase-5-Vorab-Verifikation): `BlockType.getMaterial() == BlockMaterial.Empty` ist die native Prüfung für "Position frei" - jetzt als `hasFreeTopFace(...)` auf den Block über jedem Kandidaten angewendet, siehe `docs/bud-worker-mode-plan.md`, "Nur Oberflächenblöcke sind tillbar".
- [x] `.\gradlew build` komplett grün.
- [x] Server neu gestartet, aber wieder keine `[BUD-TEMP-DEBUG]`-Zeile trotz belegt tickendem System (`"Workstation refed..."`-Log aus derselben Klasse) — zwei Ursachen gefunden, siehe die zwei folgenden Abschnitte.

### Walk-MotionController: fehlende Stufen-Parameter (behoben, alle drei Dateien)

Details/Begründung in `docs/bud-worker-mode-plan.md`, "Walk-MotionController: fehlende Stufen-Parameter".

- [x] Native Defaults per `javap` geprüft: `MaxClimbHeight=1.3`, `DescendFlatness=0.7`, `DescendSpeedCompensation=0.9` — keines davon `0`. Saschas Test-Kriterium damit nicht erfüllt, exakte Ursache des Stehenbleibens bleibt offen bis zum wiederholten Diagnoselauf.
- [x] Trotzdem ergänzt (generisches Bud-Verhalten, nicht farming-spezifisch — Begründung im Plan-Doc): alle drei `Template_*_Bud.json` bekommen `MaxClimbHeight: 1`, `DescendFlatness: 0.7`, `DescendSpeedCompensation: 0.5` (Gardener-Werte) im Walk-Eintrag.
- [x] **Für Phase 7 vermerkt:** Gardener hat `InventorySize: 36`, unsere Buds nicht — für die Ernte relevant, jetzt noch nicht nötig.
- [x] Mit PowerShell `ConvertFrom-Json` auf allen drei Dateien geprüft, `.\gradlew build` grün.

### Instrumentierung lieferte weiterhin nichts — Overflow im Drossel-Timer gefunden (behoben)

Details in `docs/bud-worker-mode-plan.md`, "Instrumentierung lieferte weiterhin keine `[BUD-TEMP-DEBUG]`-Zeilen".

- [x] Ursache gefunden: `lastDebugLogNanos = Long.MIN_VALUE` als Sentinel gegen `System.nanoTime()` (beliebiger Ursprung laut JavaDoc) diffen kann bei der Subtraktion überlaufen und den allerersten Check fälschlich `false` ergeben lassen — in allen drei instrumentierten Klassen identisch.
- [x] Drossel für diesen Lauf komplett deaktiviert (`shouldLogDebug()` gibt immer `true` zurück) in `WorkstationFuelTickSystem`, `WorkTargetSensor`, `TillSoilAction`.
- [x] Zusätzliche, völlig ungedrosselte erste Zeile ganz am Anfang von `WorkstationFuelTickSystem.tick()` (`"tick: entered"`), um "System tickt nicht" von "Logging kommt nicht durch" zu trennen.
- [x] Logger/Methode gegengeprüft: identisch zu den sichtbaren Zeilen (`LoggerUtil.getLogger()`), kein Unterschied gefunden.
- [x] `.\gradlew build` grün.
- [x] Diagnoselauf lieferte diesmal eindeutige Zeilen: `tick`/`updateWorkTarget`/`WorkTargetSensor.matches`/`TillSoilAction.canExecute` laufen jeden Tick, alle Prüfungen grün — aber nie eine `execute`-Zeile. Siehe nächster Abschnitt.

### Action-Lebenszyklus: `ActionTimeout`-Wrapper als Ursache identifiziert, Gegentest umgesetzt

Details/Bytecode-Belege in `docs/bud-worker-mode-plan.md`, "Diagnoselauf eindeutig: `canExecute` grün, `execute` nie".

- [x] Lebenszyklus-Methoden geprüft (`javap` gegen `ActionCrouch`/`ActionPlaceBlock`): beide überschreiben nur `canExecute`/`execute`, keine Extra-Methode — `TillSoilAction` fehlt nichts, das ist nicht die Ursache.
- [x] `ActionTimeout`/`ActionWithDelay` per `javap -p -c` verfolgt: `canExecute()` ruft die innere Action immer auf (erklärt die grünen `TillSoilAction.canExecute`-Zeilen), gibt selbst aber nur `true` zurück, wenn der eigene Delay-Countdown abgelaufen ist (`!isDelaying()`) — `ActionList.execute()` ruft `execute()` nachweislich nur bei `canExecute()==true` auf. Der Countdown hängt von einer externen `EntitySupport.registerDelay`/`processDelay`-Tick-Kette ab, die sich ohne Live-Trace nicht abschließend verifizieren ließ, aber exakt zum beobachteten Symptom passt.
- [x] `ActionSequence` (Gardener-Vergleich) per `javap` geprüft: delegiert 1:1 an ihre eigene `ActionList`, keine strukturelle Sonderbehandlung gegenüber einer nackten Action — erklärt laut Bytecode keinen Unterschied, warum der Gardener funktioniert.
- [x] Gegentest umgesetzt: `Timeout`-Wrapper in `Template_Keyleth_Bud.json`s Till-Loop entfernt, `{"Type": "TillSoil"}` direkt in `Actions`. Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün.
- [x] Nebenbefund (Ziel 1,5 Blöcke unter Anker) geprüft: plausibel — Höhenlimit (`FieldMaxHeight=3`) greift korrekt, Oberflächenfilter prüft lokal unabhängig vom Höhenunterschied, kein Logikfehler identifiziert, aber ohne visuelle in-game-Kontrolle nicht 100% von echtem Geländeversatz zu unterscheiden.
- [x] **Bestätigt (Sascha):** Gegentest erfolgreich — Keyleth tillt nachweislich Feld für Feld, `ActionTimeout`-Wrapper zweifelsfrei als Ursache bestätigt.

### Phase 5 Abrundung: geordnete Feldbearbeitung, Arbeitstempo, Werkzeug/Animation, Bewuchs (behoben)

Details/Begründung in `docs/bud-worker-mode-plan.md`, "Phase 5 abgeschlossen: Abrundung".

- [x] Diagnose-Logging (`[BUD-TEMP-DEBUG]`) vollständig entfernt aus `WorkstationFuelTickSystem`, `WorkTargetSensor`, `TillSoilAction`.
- [x] `ActionTimeout`-Befund im Plan-Doc als bestätigte Ursache festgehalten, inkl. Warnung: nicht wieder als Taktgeber verwenden (auch nicht mit `Sequence`), solange die `processDelay`-Kette bei uns ungeklärt bleibt.
- [x] Geordnete Feldbearbeitung: `findNearestTillableBlock` → `findNextTillableBlock`, Boustrophedon/Serpentine statt nächstgelegener Kandidat — deterministisch, kein eigener Scan-Cursor nötig (bereits getillte Blöcke fallen selbst aus `isTillable` heraus).
- [x] `WorkConfig.TillIntervalSeconds` (Default `1`) — Arbeitstempo jetzt in Java (`TillSoilAction`/`WorkstationFuelTickSystem`/`BudComponent.tillCooldownSecondsRemaining`), kein `ActionTimeout` mehr im Spiel.
- [x] Werkzeug: `Tool_Hoe_Crude` equippen in `.Default` (jeden Tick, No-op-Muster), `ClearHeldItem` in `.Resting`. Item-Id aus `reference/assets` verifiziert. Animation ursprünglich als zweite `PlayAnimation`-Action vor `TillSoil` geplant — siehe Regression unten, jetzt direkt aus Java.
- [x] Bewuchs über getilltem Block: `Hoe_Till.json` geprüft (reines `ChangeBlock`, keine explizite Räumung), native Support-Mechanik (`Plant_Grass_Lush.json`) geprüft, aber `Soil_Dirt_Tilled` behält `Type=Soil` — erklärt das native Verhalten nicht vollständig. Eigener, begründeter Ansatz: `TillSoilAction.execute()` räumt den Block über dem Ziel, falls er nicht `BlockType.EMPTY` ist (deckt sich mit der bereits von `hasFreeTopFace` verlangten Durchlässigkeit an der Zielauswahl selbst, keine blinde Räumung).
- [x] Nebenbefund `InventorySize` (Gardener: 36) für Phase 7 im Plan-Doc vermerkt.
- [x] Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün nach jedem Punkt.
- [x] **Regression gemeldet (Sascha):** mit der zweiten `PlayAnimation`-Action in der `ActionsBlocking`-Liste tillte Keyleth gar nicht mehr, keine Animation sichtbar — siehe nächster Abschnitt.

### Regression: zwei-Actions-Liste stoppte das Tillen komplett (behoben)

Details/Bytecode-Belege in `docs/bud-worker-mode-plan.md`, "Regression nach der Abrundung: Tillen komplett gestoppt".

- [x] Verdacht (gleiche Fehlerklasse wie `ActionTimeout`) per `javap -p -c` gegen `ActionPlayAnimation` geprüft: kein `canExecute`-Override, kein `startDelay`/`registerDelay` — ein einziger synchroner `execute()`-Aufruf, kein Delay-Mechanismus wie bei `ActionTimeout`. Exakte Fehlerklasse bytecode-seitig nicht bestätigt, aber Saschas Beobachtung ("steht vor dem Grasblock") belegt: Zielsuche/Cooldown/Seek funktionieren, der Fehler sitzt ausschließlich im zweiten `Actions`-Eintrag.
- [x] Fix (Sascha-Vorgabe): Animation direkt aus `TillSoilAction.execute()` per `NPCEntity.playAnimation(Ref, AnimationSlot, String, ComponentAccessor)` (Signatur per `javap` bestätigt) statt zweiter JSON-Action — komplett außerhalb jeder `ActionList`-Sequenzierung. `Template_Keyleth_Bud.json`s Till-Loop hat wieder nur `{"Type": "TillSoil"}` in `Actions`, exakt die zuvor bestätigt funktionierende Struktur.
- [x] Kein neues Diagnose-Logging nötig — Ursache über Saschas Beobachtung + Bytecode-Vergleich zur Vorversion eingegrenzt.
- [x] Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün.
- [x] **Bestätigt (Sascha):** Tillen, Reihenfolge, Tempo, Hacke, Gras — alles funktioniert. Nur die Animation blieb unsichtbar, auch nach zwei Namens-/Slot-Varianten (`Action`/`Swing_Right`, `Status`/`Interact`).

### Till-Animation unsichtbar — Ursache im Modell-Asset gefunden (behoben)

Details/Bytecode-Belege in `docs/bud-worker-mode-plan.md`, "Till-Animation unsichtbar (auch nach Java-Umzug)".

- [x] Temporäres Logging in `TillSoilAction.playTillAnimation` ergänzt: Methode erreicht, `NPCEntity`/`ModelComponent`/`ActiveAnimationComponent` vorhanden, aufgelöstes Modell + verfügbare Animationsliste, aktuell im Slot gespeicherte Animation, Aufrufparameter — noch drin, bis Sascha bestätigt.
- [x] `NPCEntity.playAnimation` per `javap -p -c` vollständig durchverfolgt: Modell-Validierung wird bei `Slot: Action` übersprungen (erklärt Saschas ersten Versuch), fehlende `ActiveAnimationComponent` würde eigene Engine-Warnung loggen, Nicht-`Action`-Slots triggern dieselbe Animation nicht neu.
- [x] **Ursache gefunden:** die Animationsliste kommt aus dem `AnimationSets`-Feld im Modell-Asset selbst (`Server/Models/*.json`), nicht automatisch aus dem per `"Model"` referenzierten `.blockymodel`-Ordner. `Keyleth_Bud.json` hat `"Parent": "Player"`, aber kein eigenes `AnimationSets` — die tatsächlich aufgelöste Liste ist die von `Player` geerbte, die weder `Swing_Right` noch `Interact` kennt (bestätigt u. a. durch das bereits funktionierende `.Spin`/`IdlePassive`, das nur in `Player.json` existiert).
- [x] Vor dem Fix geklärt statt angenommen: `ModelAsset`s `AnimationSets`-Setter per `javap` geprüft — `MapUtil.combineUnmodifiable(inheritedMap, ownMap)`, echter Merge, keine Ersetzung. Ein eigenes, partielles `AnimationSets` ist damit gefahrlos (Player-Animationen wie Walk/Run/Idle bleiben erhalten).
- [x] Fix: `Server/Models/Keyleth_Bud.json` um `"AnimationSets": {"Interact": {...}}` ergänzt — identischer Eintrag wie in `Kweebec_Sapling.json` (dasselbe Rig). `TillSoilAction` bleibt bei `Slot: Status`/`Interact` (Saschas letzter Stand).
- [x] Mit PowerShell `ConvertFrom-Json` geprüft, `.\gradlew build` grün.
- [x] **Bestätigt (Sascha):** Animation sichtbar. Diagnose-Logging aus `TillSoilAction.playTillAnimation` vollständig entfernt, `.\gradlew build` grün.

## Phase 5 — abgeschlossen (Sascha bestätigt ingame)

Keyleth tillt mit Hacke und sichtbarer Animation, zeilenweise (Boustrophedon), ~1 Block/Sekunde, Gras verschwindet mit dem Boden, Feldgrenze wird eingehalten.

Drei tragende, wiederverwendbare Erkenntnisse für Phase 6–10 im Plan-Doc festgehalten (siehe `docs/bud-worker-mode-plan.md`, "Phase 5 — Drei tragende Regeln für Phase 6–10"):
- [x] a) Mehrschrittige Abläufe gehören in eine einzige Java-Action, nicht in mehrere JSON-Actions/`ActionTimeout` — JSON bleibt dünne Anbindung.
- [x] b) Zielwahl und Tempo gehören in die Station (`WorkstationFuelTickSystem`), nicht in Sensoren — native Sensoren cachen und kennen keinen Anker.
- [x] c) Animationen kommen aus `AnimationSets` im Modell-Asset (`Server/Models/*.json`, Parent-gemerged), nicht aus dem Mesh — Gronkh/Veri brauchen in Phase 9/10 denselben eigenen Eintrag.

**Zurückgestellt, bewusst noch offen:** charakterspezifische Ruheposen (Keyleth: `Sit`/`Sit_Down`/`Sit_Up`; Gronkh später: `Laydown`/`Rest`/`Sleep`) statt des aktuellen generischen `Crouch` in `.Resting`. Kein Blocker für Phase 6, aber vor Phase 9/10 (Gronkh) aufgreifen, da Gronkhs Charakter ("ausgesprochen faul") explizit davon profitiert.

### Korrektur nach Phase 5: Farming-Instructions gehören nur zu Keyleth (behoben)

- [x] Till-Loop-Instructions (Seek + `TillSoil`) versehentlich in alle drei Rollen-Dateien eingefügt — aus `Template_Veri_Bud.json`/`Template_Gronkh_Bud.json` wieder entfernt, nur in `Template_Keyleth_Bud.json` (FARMING) belassen. Generisches Gerüst (Working/`.Resting`/`.Default`) bleibt unverändert in allen drei.
- [x] **Faustregel festgehalten** (Plan-Doc, Phase 5): gemeinsames Bud-Verhalten → alle drei Dateien; rollenspezifisches Arbeitsverhalten → nur die Datei des zuständigen Buds. Vor jeder künftigen Rollen-JSON-Änderung kurz prüfen, welche Kategorie zutrifft.
- [x] Mit PowerShell `ConvertFrom-Json` auf allen drei Dateien geprüft, `.\gradlew build` grün.

### Duplikations-Analyse (Sascha-Auftrag, Recherche abgeschlossen, NICHT umgesetzt)

Details/Begründung in `docs/bud-worker-mode-plan.md`, "Rollen-Vererbung: Duplikations-Analyse".

- [x] Vollständiger Scan von `reference/assets/Server/NPC/Roles` (975 Dateien, nicht nur Stichproben): kein Abstract referenziert ein anderes Abstract (0/54).
- [x] **Entscheidende Frage beantwortet:** `Variant`+`Reference`+`Modify` (Präzedenz: `Template_Animal_Neutral.json` ← `Chicken.json` u. a., >50 Beispiele) überschreibt nachweislich **ausschließlich Parameterwerte** — 0 von 465 Variants kombinieren `Reference` mit eigenem `Instructions`, 0 `Modify`-Blöcke enthalten je den Schlüssel `"Instructions"`. Eigene Instructions lassen sich über diesen Mechanismus **nicht** hinzufügen.
- [x] Passenderer Mechanismus gefunden: `"Type": "Component"` (159 native Dateien) — echte, komponierbare Instruction-Fragmente, referenzierbar an beliebiger Stelle im Baum, mehrfach kombinierbar. Nicht auf kleine Schnipsel beschränkt (`Component_Trork_Instruction_Idle.json` z. B. 1032 Zeilen).
- [x] **Empfehlung abgegeben, nicht umgesetzt:** generisches Gerüst (MODE 1–4) könnte als `Component`-Datei(en) extrahiert werden, referenziert von allen drei weiterhin `"Type": "Generic"` bleibenden Bud-Dateien; rollenspezifisches Arbeitsverhalten bliebe zusätzliche, eigene Instructions je Datei. Restrisiko benannt: `_ImportStates`/`_ExportStates`/`ParentState`-Ummapping nicht laufzeit-verifiziert, Fehler würde alle drei Buds gleichzeitig treffen (Single Point of Failure) statt nur eine Datei wie heute. Vorschlag: falls gewünscht, klein anfangen (nur Working/`.Resting`/`.Default`, nicht alle vier Modes auf einmal) und ingame verifizieren, bevor mehr angefasst wird — vor oder nach Phase 9/10, keine Präferenz meinerseits. Die Faustregel oben verhindert die konkrete Fehlerklasse aus diesem Auftrag bereits unabhängig davon.
- [ ] **Entscheidung liegt bei Sascha** — nichts weiter zu tun, bis er sich äußert.

**Phase 5 abgeschlossen und bestätigt — Phase 6 startet mit einem Umsetzungsvorschlag (siehe Plan-Doc), Code erst nach Rückmeldung.**

## Phase 6 — Farming-Loop: Pflanzen/Gießen/Wachstum

Umsetzungsvorschlag (Struktur, noch nicht umgesetzt) in `docs/bud-worker-mode-plan.md`, "Phase 6 — Umsetzungsvorschlag (Rückmeldung ausstehend)".

- [ ] `PlantSeedAction` — liest Slot-1-Item, pflanzt über Rezept-Mapping (siehe Phase 8) das passende Gemüse.
- [ ] `WaterSoilAction`.
- [ ] `FindGrownCropSensor` — liest `FarmingBlock.currentStageSet`/`generation`, meldet "erntereif" erst bei finaler Stufe.
  - Test: kompletter Zyklus Pflanzen → Gießen → Warten. Sensor-Log mit tatsächlich sichtbarem Wachstum abgleichen — kein Vorschnellernten.

## Phase 7 — Ernte + Lieferung

- [ ] `HarvestCropAction` (native `HarvestCropInteraction`), Ertrag geht ins Bud-eigene Inventar.
- [ ] `FindContainerSensor` + `PlaceInContainerAction` (Konzept aus Referenzplugin, eigene Implementierung).
  - Test: End-to-End mit leerem Feld — Bud durchläuft Tillen → Pflanzen → Gießen → Warten → Ernten → Abliefern an Kiste, mehrfach hintereinander automatisch.

## Phase 8 — Rezept/Config-Politur

- [ ] Seed→Crop-Mapping als YAML (`work/farming.yml`, gleiche Konvention wie `prompts/`/`buds/`), keine Hardcodes mehr aus Phase 5–7.
  - Test: anderes Gemüse per Config-Änderung, kein Recompile nötig außer YAML-Edit.

## Verifikation

- [ ] `.\gradlew build` grün nach jeder Phase (nicht nur am Ende).
- [ ] Ingame-Test wie oben je Phase beschrieben — Phase 2 und 4 insbesondere vor Phase 5 bestätigen, sonst testet man Feldarbeit mit kaputtem State-Lock.
- [ ] Diese Datei komplett abgehakt.

## Explizit nicht Teil dieses Durchlaufs

Foresting (Veri), Mining (Gronkh), LLM-Reaktionen (Chat/Sound bei Arbeitsbeginn, periodische Kommentare, Faulenz-Reaktionen), eigenes Workstation-Modell — folgen als eigene TODOs, erst wenn Farming-v1 vollständig läuft und getestet ist (siehe Plan-Doc, Abschnitt "Reihenfolge der Jobs").
