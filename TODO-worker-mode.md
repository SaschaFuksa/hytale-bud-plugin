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

- [ ] Neuer Block/BlockEntity `WorkstationBlock`, 2x1 wie Furnace, **vorhandenes Modell wiederverwenden** (Furnace o.ä. als Platzhalter). Zwei Slots: Produkt, Futter.
- [ ] Card-Insert in Slot 1 validiert Bud-ID (aus Karte) gegen `workRole` der Station — falsche Rolle wird abgelehnt.
  - Test: passende Karte reinlegen funktioniert, falsche Rolle (z. B. Veri-Karte in Farming-Station) wird abgelehnt. Karte wieder rausnehmen, normal spawnen — Kartendaten bleiben intakt.

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
