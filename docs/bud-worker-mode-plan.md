# Bud Worker Mode — Architektur & Entscheidungen

Hintergrund für [`../TODO-worker-mode.md`](../TODO-worker-mode.md). Diese Datei hält die Entscheidungen fest, die im Chat getroffen wurden, damit sie beim Abarbeiten nicht erneut diskutiert werden müssen.

## Ziel

Ein neuer Modus, in dem ein Bud statt Spieler zu begleiten/kämpfen autonom an einer **Workstation** arbeitet (Gardener/Farmer, Forester, Miner). Ausgangsidee war der Gardener aus dem Referenzplugin `AncientConstructs-1.2.2` (`reference/`-Analyse siehe Chat-Verlauf) — dessen "Gehirn" ist reine Hytale-NPC-Engine (`Role`/`StateSupport`/`MarkedEntitySupport`, gleiche Bausteine wie beim Waystone-Teleport-Fix) plus 7 eigene Sensor/Action-Java-Klassen, registriert über die generische `NPCPlugin.registerCoreComponentType(...)`-API.

## Rollen-Zuordnung

Fest pro Bud, nicht wählbar — Keyleth → Farming, Veri → Foresting, Gronkh → Mining. Datengetrieben als neues `workRole`-Feld in `BudDefinition`/`buds/<id>.yml`, gleiches Prinzip wie alle anderen Bud-Eigenschaften (Farbe, Sounds, ...). Workstation validiert beim Karteneinlegen die `workRole` der Bud-ID gegen den Rollentyp der Station.

**Entscheidung (Nachtrag):** `WorkRole` bekommt einen vierten Wert `COMPANION` als Default, statt `getWorkRole()` bei fehlendem YAML-Feld werfen zu lassen bzw. `null` zurückzugeben. Ein Bud ohne gesetzte Rolle ist damit einfach ein reiner Begleiter (aktuell keiner der drei Buds, aber sauberer Default für künftige reine Kampf-Buds) — kein Sonderfall/Exception, passt besser zum sonstigen datengetriebenen Stil.

## Feld-Konzept

Jede Workstation hat ein konfigurierbares Arbeitsfeld um sich herum (Start: 3x3, Radius konfigurierbar über neue `WorkConfig`-Sektion, gleiches `Config<T>`-Muster wie `LLMConfig`/`ReactionConfig`/etc.):

- **Farming**: jeder Dirt/Grass-Block im Feld ist Ziel. Kein Mindestabstand nötig (Feld-Radius reicht als Limit).
- **Foresting**: feste Quadranten-Positionen (ein Sampling pro Quadrant), zusätzlich konfigurierbarer Mindestabstand als Sicherheitsnetz — auch wenn das Baum-Prefab-System selbst schon physischen Platzbedarf erzwingt (s.u.), ist unklar, ob ein blockiertes Prefab-Wachstum sauber fehlschlägt oder das Sampling einfach nie fertig wird.
- **Mining**: kein Weltgen-Scan, keine Tunnel — die Station selbst spawnt Erz/Stein in konfigurierbaren Intervallen und Abständen im Feld, der Bud baut ab. Damit ist Mining der technisch einfachste der drei Jobs (kein neuer Sensor für Zielsuche in der Welt nötig).

## Native Engine-Mechanik (Recherche in `reference/server` + `reference/assets`)

Farming/Wachstum ist in Hytale **kein Custom-Bau**, sondern vollständig nativ vorhanden (`com.hypixel.hytale.builtin.adventure.farming`):

- **Tilling** ist eine normale Item-Interaction (wie Spieler-Hacke) — der Bud löst dieselbe Interaction aus, kein eigener "Till"-Code.
- **Wachstum** ist ein echtes ECS-Component (`FarmingBlock`: `currentStageSet`, `growthProgress`, `generation`), kein Timer-Rätselraten. Unser Sensor liest das direkt.
- **Crops und Bäume teilen sich dasselbe Stage-System**, nur der Endschritt unterscheidet sich: `BlockTypeFarmingStageData`/`BlockStateFarmingStageData` (Crops, Blockzustand wechselt) vs. `PrefabFarmingStageData` (Bäume, letzte Stufe tauscht Sampling gegen komplettes Baum-Prefab — bestätigt über `Spawners_Trees_Oak/Birch`-Prefabs in `reference/assets`).
- **Art-Matching ist automatisch**: das eingewechselte Baum-Prefab passt exakt zur gepflanzten Sampling-Sorte, keine zusätzliche Spezies-Prüfung nötig.
- Ernte über vorhandene `HarvestCropInteraction`.

Genaue Klassensignaturen (Sensor/Action-Basisklassen, `NPCPlugin.registerCoreComponentType`, Furnace-Fuel-Pattern) sind jetzt per `javap -p -c` gegen `reference/server` verifiziert (nicht mehr nur String-Scan) — siehe "Phase 0 — Verifikationsergebnisse" unten.

## Working-State / Kampf-Lock

Ein Bud in Arbeit darf nicht dem Spieler folgen oder kämpfen. Analog zum Teleport-Fix: `StateChangeEvent.dispatch(...)` setzt Rolle/Target ohne Nebenwirkungen (keine LLM-Nachricht, keine Social-Reaction). Zusätzlich müssen Orchestrator/Filter-Systeme arbeitende Buds überspringen (kein `LockedTarget` auf Spieler, keine Kampf-/Ambient-Trigger). Details siehe "Phase 0 — Verifikationsergebnisse", Punkt 2.

**Umsetzung (Phase 2):** `StateChangeQueue.handleStateChange` triggert für jeden State-Wechsel LLM-Chat (`LLMStateMessageCreation`) + eine Bud-zu-Bud-Social-Reaction auf dem `SOCIAL`-Orchestrator-Channel — das ist der bestehende Pfad für die drei Pet-Modi. Für `WORKING` darf das nie laufen, deshalb zwei Ebenen:

1. **Primärpfad (silent dispatch):** Aufrufer, die einen Bud in Working schicken wollen (Phase 2: `StateCommand --working` als Debug-Pfad; Phase 4: die Workstation-Bindung), rufen `budComponent.setCurrentState(WORKING)` + `StateChangeEvent.dispatch(...)` **direkt** auf — exakt das Muster aus `TeleportHandler`, das die Queue komplett umgeht.
2. **Absicherung:** `StateChangeQueue.handleStateChange` kehrt nach dem Dispatch früh zurück, falls trotzdem ein `WORKING`-Eintrag in der Queue landet (z. B. weil `StateChangeSystem`, das jeden erkannten nativen Rollen-State-Wechsel automatisch in die Queue einreiht, irgendwann auch `"Working"` erkennt). Der bestehende, jetzt exhaustive `switch` in `triggerStateChangeReaction` wirft für `WORKING` absichtlich (unerreichbar, dokumentiert den Vertrag).

`LockedTarget` (verhindert Folgen/Kampf-Fokus auf den Spieler) wird in `StateChangeHandler` nur noch gesetzt, wenn der neue State nicht `WORKING` ist.

Reaktions-Trigger ("Bud reagiert auf X") laufen in praktisch jedem Feature (`com.bud.feature.block/crafting/combat/discover/item/chat/world/...`) über denselben Chokepoint: `BudManager.getRandomBudComponent`/`getRandomOtherBud` wählt den reagierenden Bud aus. Statt jedes Filter-System einzeln zu patchen, filtert dieser eine Ort jetzt Buds im `WORKING`-State komplett heraus — deckt alle bestehenden und künftigen Aufrufer ab. Zwei Stellen mussten trotzdem einzeln angefasst werden, weil sie nicht über diesen Chokepoint laufen: `DamageFilterSystem`s Kampf-Assist-Loop (setzt Attitude-Override auf Hostile für alle Buds des Spielers, unabhängig von einer Reaktion) und `Orchestrator.dispatch(...)` (verwirft ein Event, wenn der zugehörige Bud zwischen Enqueue und Dispatch in Working gewechselt ist).

### Regression nach Phase 2: fehlender State-Setter (behoben)

Serverstart nach dem ersten Phase-2-Durchlauf schlug fehl: `[NPC|P] FAIL: .../Template_*_Bud.json: State sensor or State setter action/motion exists without accompanying state/setter: Working`, gefolgt von `Reference to unknown builder`-Folgefehlern und Spawn-Fehlern. **Root Cause:** Die Engine verlangt für jeden per Sensor referenzierten State (hier: `"Sensor": {"Type": "State", "State": "Working"}` in MODE 4) mindestens eine explizite State-Setter-Action/Motion irgendwo in derselben Rollen-Datei. `PetSitting`/`PetPassive`/`PetDefensive` bekommen ihren Setter aus dem bestehenden Interaction-Cycle-Block (die "Cycle: X -> Y"-Instructions weiter unten in der Datei, mit `"Actions": [..., {"Type": "State", "State": "PetPassive"}]`), `Idle` ist der `StartState` der Rolle. Working hatte weder das eine noch das andere.

**Fix:** Selbstreferenzieller No-op-Setter direkt im MODE-4-Block (`"Actions": [{"Type": "State", "State": "Working"}]`), **nicht** im Interaction-Cycle-Block ergänzt — der Cycle-Block ist über eine normale Spieler-Interaktion erreichbar, und Working soll explizit *nicht* darüber erreichbar sein (nur programmatisch via `StateChangeEvent.dispatch(...)`, s. o.). Der Sensor verlangt bereits `State == Working`, der Setter setzt also nur den bereits aktiven State erneut — erfüllt die statische Validierung, ohne einen neuen Übergang zu öffnen. Kommentar im JSON dokumentiert das.

**Tick-Overhead (per `javap -p -c` gegen `ActionList.execute()` verifiziert):** Ohne `ActionsBlocking`/ein „Once"-Attribut ruft der non-blocking Default-Pfad von `ActionList.execute()` bytecode-bestätigt bei **jedem** Aufruf `canExecute()`+`execute()` für jede Action auf, ohne Skip bei bereits abgeschlossenen/identischen Actions — der No-op-Setter feuert also nativ jeden Tick, solange der Bud in Working bleibt. Das bleibt aber vollständig innerhalb des nativen NPC-Instruction-Baums; es geht nicht durch unsere Java-Pipeline. Die früh-Return-Absicherung in `StateChangeQueue.handleStateChange` (s. o., Punkt 2) wird dabei nicht einmal erreicht: `StateChangeSystem.tick()` erzeugt nur bei einer *erkannten Änderung* des nativen State-Namens gegenüber `BudComponent.currentState` einen neuen `StateChangeEntry` — da beide dauerhaft `"Working"` bleiben, entsteht dort nie ein Diff. Kein LLM-/Reaction-Spam-Risiko, nur ein kleiner rein nativer Tick-Kostenpunkt. `ActionsBlocking: true` löst das laut Bytecode für eine 1-Action-Liste nicht (ändert nur Mehrfach-Action-Sequenzierung, `execute()` läuft trotzdem jeden Tick); ein JSON-„Once"-Attribut ist in keiner bestehenden Rollen-Datei belegt — dafür wollte ich nicht raten. Entscheidung: kein weiterer Fix, MODE 4 ist ohnehin Platzhalter bis Phase 5+ echte, selbst-gatende Sensoren/Actions liefert.

## Phase 0 — Verifikationsergebnisse (javap gegen `reference/server`)

### 1. `NPCPlugin.registerCoreComponentType` / `registerCoreFactories`

```
public <T> NPCPlugin registerCoreComponentType(String name, Supplier<Builder<T>> supplier)
protected void registerCoreFactories()
```

- `registerCoreComponentType` ist **public** und generisch nutzbar: es ruft `supplier.get().category()` auf (liefert die `Class<T>`, z. B. `Action.class`/`Sensor.class`/`ActionList.class`), holt darüber via `BuilderManager.getFactory(Class)` die passende, bereits von `registerCoreFactories()` (engine-intern, `protected`, nicht selbst aufzurufen) registrierte `BuilderFactory`, und ruft `factory.add(name, supplier)`. D. h. eigene Sensoren/Actions registriert man einfach über `npcPlugin.registerCoreComponentType("MyAction", MyAction::new)` — die Zuordnung zur richtigen Factory passiert automatisch über `category()`; die `FACTORY_CLASS_*`-Konstanten (`"Action"`, `"Sensor"`, `"ActionList"`, …) sind nur die String-Keys, unter denen die Rollen-JSON den `"Type"` referenziert.
- `Action` (Interface, extends `RoleStateChange, IAnnotatedComponent, IComponentExecutionControl`): abstrakt `canExecute(...)`, `execute(...)`, `activate(Role, InfoProvider)`, `deactivate(Role, InfoProvider)`, `isActivated()`.
- `Sensor` (Interface, gleiche Basis-Interfaces): abstrakt `matches(Ref<EntityStore>, Role, double, Store<EntityStore>)`, `getSensorInfo()`; `done()` hat einen Default.
- `ActionBase` (abstract, extends `AnnotatedComponentBase`, implements `Action`): implementiert bereits `canExecute`/`execute`/`activate`/`deactivate`/`isActivated`/`isTriggered`/`clearOnce`/`setOnce`/`processDelay(float)` generisch; Konstruktor nimmt `BuilderActionBase`. Eigene Actions erben typischerweise von `ActionBase` und überschreiben nur die konkrete Ausführungslogik (kein vollständiger `Action`-Neubau nötig).
- `SensorBase` (abstract, extends `AnnotatedComponentBase`, implements `Sensor`): implementiert `matches`/`clearOnce`/`setOnce`/`isTriggered`/`processDelay(float)`; Konstruktor nimmt `BuilderSensorBase`.
- Fazit: Vorarbeit aus der Cowork-Session war korrekt. Für Phase 5+ (Sensoren/Actions) reicht `ActionBase`/`SensorBase` erben + `registerCoreComponentType(...)` in `BudPlugin.setup()` aufrufen (`NPCPlugin`-Instanz muss dafür verfügbar sein — über `HytaleServer`/Plugin-Lookup, in Phase 5 konkret prüfen).

### 2. Working-State: wo andocken

`BudState` (`com.bud.core.types.BudState`) ist ein Java-Enum mit `PET_DEFENSIVE("PetDefensive")`, `PET_PASSIVE("PetPassive")`, `PET_SITTING("PetSitting")`. `StateChangeHandler` übersetzt `event.newState().getStateName()` über `role.getStateSupport().getStateHelper().getStateIndex(name)`/`setState(...)` — das ist ein reiner Name-Lookup gegen die States, die in der **NPC-Rollen-JSON** (`src/main/resources/Server/NPC/Roles/Template_*_Bud.json`) als String in `"Sensor": {"Type": "State", "State": "PetSitting"}`-Knoten vorkommen (verifiziert: `PetDefensive`/`PetPassive`/`PetSitting` erscheinen dort z. B. Zeilen 133/143/513 in `Template_Keyleth_Bud.json`, als Top-Level-Zweige unter `Instructions`, keine separate States-Deklarationsliste). `BudComponent.currentState` ist bereits ein generisches `@Nonnull BudState`-Feld ohne harte Kopplung an die drei bestehenden Werte.

**Entscheidung: neuer Enum-Wert, kein Boolean-Feld.** Ein `WORKING("Working")` in `BudState` passt exakt in den bestehenden Mechanismus (State-Name wird 1:1 durchgereicht, `StateChangeHandler`/`StateChangeEvent.dispatch(...)` brauchen keine Änderung) und bleibt orthogonal zu Pet-Mode — ein Bud ist entweder `PET_*` oder `WORKING`, nie beides, ein Enum bildet das exklusiv ab, ein zusätzliches Boolean-Flag würde einen zweiten Zustand parallel zum bestehenden führen und Inkonsistenzen erlauben (z. B. `PET_SITTING` + `working=true` gleichzeitig). Voraussetzung für Phase 2: jede Bud-Rollen-JSON (`Template_Veri_Bud.json`, `Template_Keyleth_Bud.json`, `Template_Gronkh_Bud.json`) braucht einen neuen Top-Level-Zweig `"Sensor": {"Type": "State", "State": "Working"}` mit eigenen `Instructions` (zunächst `"BodyMotion": {"Type": "Nothing"}` als Platzhalter, analog zu `PetSitting`), sonst läuft `setState(...)` zwar durch, aber der Bud hat keine Instruction-Branch für den neuen State-Index und bleibt in seiner letzten Pose stecken.

### 3. Fuel-Burn-Timer: natives Engine-Feature existiert, ist nur nicht "Furnace" benannt

`reference/server` hat **keine** Klassen mit "Furnace"/"Smelt"/"Cook"/"Kiln" im Namen — der Furnace-Ofen im Spiel ist ein generisches, config-getriebenes **`Bench`**-System:

- `com.hypixel.hytale.builtin.crafting.component.BenchBlock` (Tier-Level, Upgrade-Items, offene Fenster) + `ProcessingBenchBlock` (das eigentliche Arbeitspferd) sind `Component<ChunkStore>`, registriert wie jedes andere ECS-Component über `getComponentType()`.
- `ProcessingBenchBlock` hat exakt das gesuchte Fuel-Pattern: `ItemContainer inputContainer/fuelContainer/outputContainer`, `float inputProgress`, `float fuelTime`, `int lastConsumedFuelTotal`, sowie `consumeFuelForDuration(float, Store<EntityStore>, int x, int y, int z, BlockType, int)`, `advanceProcessing(float, ...)`, `getFuelTime()`, `dropFuelItems(...)`. Mehrere eingelegte Fuel-Items werden nacheinander verbraucht (`consumeOneFuel`/`calculateTotalAvailableFuel` iterieren über den Fuel-Container) — genau das im Plan geforderte Verhalten.
- Getickt wird das über `com.hypixel.hytale.builtin.crafting.system.BenchSystems$ProcessingBenchTick`, ein `EntityTickingSystem<ChunkStore>` (`getQuery()`/`tick(float, int, ArchetypeChunk<ChunkStore>, Store<ChunkStore>, CommandBuffer<ChunkStore>)`) — läuft passiv pro Block-Entity, unabhängig davon, ob ein Spieler gerade ein Bench-Fenster offen hat (`BenchSystems$ProcessingBenchLifecycle`/`OnAddOrRemoved` regeln nur Fenster-Open/Close).
- Konfiguriert wird das rein über Block-JSON (`BlockType.Bench`, Typ `"Processing"`), verifiziert an `reference/assets/Server/Item/Items/Bench/Bench_Furnace.json`: `"Fuel": [{"ResourceTypeId": "Fuel", ...}]`, `"BlockEntity": {"Components": {"BenchBlock": {}, "ProcessingBenchBlock": {}}}`, `"ExtraOutput": {"PerFuelItemsConsumed": 2, ...}`.

**Einschätzung:** Das ist konzeptionell näher an einem klassischen Input→Output-Crafting-Rezept (Recipe-basiert, mit `CraftingRecipe`, Output-Slots, Tier-Upgrades) als an "Bud isst 10 Min lang ein Gericht, dann Ruhepose". Slot-2-Fuel-Verbrauch (`fuelTime` runterzählen, mehrere Items nacheinander) ist 1:1 wiederverwendbar; das Recipe/Output-System der Bench ist Overkill für die Workstation (kein Input→Output-Crafting, sondern der *Bud* verbraucht das Futter, nicht die Station ein Rezept). **Entscheidung: kein `ProcessingBenchBlock`-Reuse, eigener simpler Scheduled-Tick** (gleiches Pattern wie `Orchestrator.tick()`) auf der `WorkstationBlockEntity` — eigenes `float fuelTimeRemaining`-Feld, pro Tick runterzählen, bei 0 nächstes Slot-2-Item konsumieren oder in Ruhepose wechseln. Deutlich weniger Kopplung an das Crafting-Subsystem, gleiche Kernidee (Fuel-Container leert sich über Zeit) ohne dessen Recipe-Overhead.

## Workstation

Größe 2x1 wie ein Furnace. Zum Start **ein vorhandenes Modell wiederverwenden** (Platzhalter, z. B. Furnace/Bench) — eigenes Modell baut Sascha erst, wenn die Funktion steht. Pro Job später eigenes Modell (Mini-Farm-Optik, Ore-Mix, Holzblöcke+kleiner Baum), aber das ist rein Asset-seitig, keine neue Java-Klasse (eine `WorkstationBlockEntity` mit `WorkRole`-Feld, Modell/Textur variieren je Block-Registrierung).

- **Slot 1**: Produkt-Input (Seedbag bei Farming/Foresting, Erz/Stein-Typ bei Mining) — bestimmt über ein Rezept-Mapping (YAML, gleiche Konvention wie `prompts/`/`buds/`) gleichzeitig das Zielprodukt und die validierte Rolle.
- **Slot 2**: Futter — wird wie Furnace-Brennstoff verbraucht, 10 Min/Gericht, mehrere eingelegte Gerichte werden nacheinander verbraucht (nicht parallel). Bei leerem Slot 2 geht der Bud in Ruhepose (sitzen/liegen), bis nachgefüttert wird.

## LLM-Reaktionen (bewusst zurückgestellt)

Für v1 komplett weggelassen — erst reiner Arbeits-Loop, dann später:

- Chat+Sound bei "zur Arbeit schicken".
- Bud-individuelle Reaktion (Gronkh hasst Arbeit, Keyleth mag Farming) über Prompt-YAML wie gehabt.
- Periodischer Arbeits-Kommentar (neuer Trigger-Typ auf bestehendem `AMBIENT`-Orchestrator-Channel, gleiche Cooldown-Mechanik).
- Reaktion beim Faulenzen/Ruhezustand.

## Reihenfolge der Jobs

**Farming zuerst** (Keyleth) — größte Wiederverwendung nativer Engine-Mechanik (Tilling, Wachstum, Ernte), geringstes Risiko. Foresting danach (gleicher Sensor/Action-Bau, andere Parameter + feste Quadranten statt freier Bodensuche). Mining zuletzt (einfachster Loop, aber neue Spawner-Logik statt Wiederverwendung).
