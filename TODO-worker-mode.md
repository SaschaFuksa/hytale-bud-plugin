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

- [ ] **Roster/`/bud create` (Spawn All) dupliziert einen bereits arbeitenden Bud.** Vermuteter Grund: die Reaktions-Sperre aus Phase 2 lief über den zentralen Chokepoint `BudManager.getRandomBudComponent`/`getRandomOtherBud`, den evtl. auch die Spawn-Dedup-Prüfung ("hat der Spieler diesen Bud schon?") nutzt — Working-Buds werden dort ausgeschlossen, die Dedup-Prüfung hält den Bud fälschlich für "nicht vorhanden". Muss geprüft werden: alle Aufrufer dieses Chokepoints (und ggf. verwandter Working-Ausschlüsse aus Phase 2) durchgehen und trennen zwischen "Reaktions-Auswahl" (soll Working weiter ausschließen) und "Existenz-/Dedup-/Roster-Prüfung" (darf Working NICHT ausschließen, ein arbeitender Bud gilt als vorhanden).
- [x] **Despawn/`/bud delete` — kein Fix nötig, bestätigtes Verhalten:** ein Working-Bud wird korrekt mit-despawnt/gelöscht (von Sascha bestätigt, macht inhaltlich Sinn).
- [ ] **`TeleportHandler.teleportBud` kennt Working-State noch nicht.** Ein arbeitender Bud darf beim Waystone-Teleport des Spielers NICHT mitgezogen werden, bleibt an der Workstation stehen. Fix: Working-Buds vor dem Teleport-Loop herausfiltern.
- [ ] **Design-Entscheidung für Phase 4 (aus Sascha-Frage abgeleitet):** was passiert, wenn beim Karteneinlegen kein Bud-Entity existiert (despawnt)? Entscheidung: Station soll ihn spawnen (analog `/bud create`, aber an der Stationsposition statt vor dem Spieler), falls nicht vorhanden — sonst eine bereits existierende Instanz heranholen/teleportieren. In Phase 4 mit umsetzen, nicht separat.

## Phase 4 — Bindung + Fuel-Timer (noch keine Feldarbeit)

- [ ] Bei gültiger Karte + Futter in Slot 2: Bud pathed/teleportet zur Station, Working-State (Phase 2) wird gesetzt. Karte raus → Working-State wird zurückgenommen, Bud normal wieder verfügbar.
- [ ] Fuel-Verbrauch: 10 Min/Gericht (Config aus Phase 1), mehrere eingelegte Gerichte werden nacheinander verbraucht, nicht parallel.
  - Test: 1 Gericht rein, Intervall für den Test kurz stellen (Config), verbrauchen lassen → Slot leert sich, Bud wechselt in Ruhepose. 2. Gericht vor Ablauf nachlegen → wird korrekt nachgereiht, nicht sofort mitverbraucht.
- [ ] Ruhepose (sitzen/liegen) bei leerem Fuel-Slot, zurück in Arbeits-Idle bei Nachfüttern.

## Phase 5 — Farming-Loop: Boden

- [ ] `FindUntilledSoilSensor` — scannt Feld (Radius aus `WorkConfig`) um Station nach Dirt/Grass ohne `TilledSoilBlock`-State.
- [ ] `TillSoilAction` — löst die native Till-Interaction aus (kein eigener Till-Code, siehe Plan-Doc).
  - Test: Bud tillt sichtbar Boden im Feld, bleibt innerhalb der Feldgrenze.

## Phase 6 — Farming-Loop: Pflanzen/Gießen/Wachstum

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
