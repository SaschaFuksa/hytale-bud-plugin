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

## Phase 3 — Verifikationsergebnisse (javap/grep gegen `reference/server`)

### 1. Einfacherer Multi-Slot-Container als `ItemContainer`?

Ja — `com.hypixel.hytale.server.core.modules.block.components.ItemContainerBlock`, ein `Component<ChunkStore>` mit nur `droplist`/`itemContainer` (`SimpleItemContainer`)/`capacity`-Feldern, verifiziert per `javap -p -c`. Er wird über **reines Block-JSON** aktiviert (`"BlockEntity": {"Components": {"ItemContainerBlock": {"Capacity": 18}}}`, siehe `reference/assets/Server/Item/Items/Furniture/Crude/Furniture_Crude_Chest_Small.json`) — kein eigenes Java nötig für den Container selbst.

`ItemContainerBlock` hat kein Feld für ein per-Instanz `WorkRole` — die geplante eigene `WorkstationBlockEntity` bleibt also nötig, aber **als zusätzliche, schlanke Komponente neben `ItemContainerBlock`, nicht als Ersatz dafür.**

**Korrektur einer früher hier festgehaltenen Entscheidung:** Ein vorheriger Rechercheschritt (siehe Git-Historie dieses Abschnitts) empfahl, `WorkstationBlockEntity` solle ein eigenes `SimpleItemContainer` embedden, statt `ItemContainerBlock` zu nutzen. Das ist **verworfen**, verifiziert per `javap -p -c` gegen `OpenContainerInteraction` (die Java-Klasse hinter der `"Open_Container"`-Interaction, die Chests/Furnace-artige Blöcke zum Öffnen benutzen): ihr Bytecode ruft `ItemContainerBlock.getComponentType()` fest verdrahtet auf (`invokestatic ... ItemContainerBlock.getComponentType`), um Fenster/Slots zu öffnen. Ein selbstgebautes, nicht-`ItemContainerBlock`-basiertes Inventar hätte also **keine** native Spieler-UI zum Öffnen — wir müssten ein komplett eigenes Fenster-/Netcode-System schreiben, was dem Ziel "einfachster Weg" widerspricht und in der ursprünglichen Analyse nicht mitbedacht wurde.

**Tatsächliche Umsetzung:** `WorkstationBlockEntity` (eigene, schlanke Komponente) trägt nur `WorkRole`; die eigentlichen zwei Slots kommen vom **nativen** `ItemContainerBlock` (`"Capacity": 2` im selben Block-JSON, Slot 0 = Produkt/Karte, Slot 1 = Futter). Beide Komponenten sitzen auf demselben Block-Entity, `"Interactions": {"Use": "Open_Container"}` funktioniert dadurch unverändert nativ (Fenster, Drag&Drop, Netcode — alles vom Engine-Chest-Mechanismus geerbt).

### 2. Insert-Validierung/Filter-Hook

`ItemContainer` (Basisklasse von `SimpleItemContainer`) hat einen **echten programmatischen Java-Hook**, kein JSON-Flag: `setSlotFilter(FilterActionType, short slot, SlotFilter)` und `setGlobalFilter(FilterType)`, beide `public abstract` auf `ItemContainer`, von `SimpleItemContainer` konkret implementiert (eigenes `slotFilters`-Feld, bytecode-verifiziert). `SlotFilter` ist ein funktionales Interface: `boolean test(FilterActionType, ItemContainer, short slot, ItemStack)` — gibt `test(...)` `false` zurück, lehnt der Container den Insert **nativ** ab (Item bleibt im Cursor/Ausgangs-Slot des Spielers, kein Transfer passiert), kein nachgelagertes Event/Zurückwerfen nötig.

Das `"FilterValidIngredients": true` aus `Bench_Furnace.json` ist dagegen **kein generisches JSON-Flag für beliebige Container** — es ist eine `ProcessingBenchBlock`-spezifische Rezept-Validierung (prüft gegen geladene Crafting-Rezepte) und für unseren Fall irrelevant, da wir kein Recipe-System brauchen (Phase 0, Punkt 3, bereits entschieden).

**Entscheidung:** Da der Container jetzt vom nativen `ItemContainerBlock` kommt (Punkt 1), sitzt der Filter dort statt in `WorkstationBlockEntity` selbst: ein neues `WorkstationFilterSystem` (`RefSystem<ChunkStore>`, `getQuery() = Query.and(WorkstationBlockEntity.getComponentType(), ItemContainerBlock.getComponentType())`) reagiert per `onEntityAdded(...)` auf neu geladene/platzierte Workstation-Blöcke und ruft dort `containerBlock.getItemContainer().setSlotFilter(FilterActionType.ADD, (short) 0, ourSlotFilter)`. `ourSlotFilter.test(...)` prüft das eingehende `ItemStack` gegen die Bud-ID/`workRole`-Prüfung aus Punkt 3 unten. Slot 1 (Futter) bekommt in Phase 3 noch keinen Filter (Futter-Validierung ist Phase 4).

### 3. BudId zuverlässig aus einem ItemStack lesen

Wichtigster Befund: **`BudId` ist keine Per-Stack-Instanzdaten (kein NBT/BSON auf dem `ItemStack`)**, sondern Teil der **statischen Item-Typ-Konfiguration** — sitzt auf der geteilten `CardBudInteraction`-Asset-Instanz, die einmal pro Item-Typ aus `Server/Item/Interactions/CardKeyleth.json` geladen wird (`"Type": "CardBud", "BudId": "keyleth"`), referenziert vom Item selbst nur über einen Namensstring (`CardKeyleth.json`: `"Interactions": {"Primary": "CardKeyleth", ...}`). Das beantwortet die Sorge aus der Frage direkt: **es kann beim Verschieben zwischen Slots nichts verloren gehen**, weil dort gar nichts Stack-Individuelles gespeichert ist — wir lesen bei jeder Prüfung frisch aus der Item-Typ-Konfiguration.

Konkreter Lese-Pfad, jedes Glied per `javap` verifiziert:
1. `itemStack.getItem()` (`ItemStack.class`) — liefert die `Item`-Konfiguration des Item-Typs.
2. `item.getInteractions()` (`Map<InteractionType, String>`, `Item.class`) — liefert für `InteractionType.Primary` den Interaction-Namen (z. B. `"CardKeyleth"`, da Item- und Interaction-Datei bei uns gleich benannt sind, aber technisch getrennte Namensräume).
3. `Interaction.getAssetMap().getAsset(String)` (`DefaultAssetMap<String, Interaction>.getAsset(K)`, geerbt über `IndexedLookupTableAssetMap`/`AssetMapWithIndexes`) — liefert die geladene `Interaction`-Instanz, zur Laufzeit tatsächlich ein `CardBudInteraction`. **Korrektur:** die ursprünglich hier vorgesehene `Interaction.getInteractionOrUnknown(String)` ist laut IDE/Compiler `@Deprecated` (per `javap -v` bestätigt: `Deprecated: true`, `RuntimeVisibleAnnotations: java.lang.Deprecated`) — `getAssetMap().getAsset(id)` ist der nicht-deprecated Ersatz mit identischem Verhalten (gleicher interner Aufrufpfad, nur ohne den Deprecated-Wrapper).
4. `instanceof CardBudInteraction` prüfen, danach `getBudId()` — der Getter existiert bereits in `CardBudInteraction` (kein Ergänzungsbedarf, anders als in einer früheren Notiz hier vermerkt).

**Entscheidung:** Slot-Filter für Slot 0 nutzt genau diesen 4-Schritt-Pfad, vergleicht `BudRegistry.getInstance().get(budId).getWorkRole()` gegen die `WorkRole` der Station und lehnt bei Mismatch über `SlotFilter.test(...) == false` ab (siehe Punkt 2). Leere/Nicht-Karten-ItemStacks (kein `CardBudInteraction`, z. B. `getAsset(...)` liefert `null` oder einen anderen Interaction-Typ) werden ebenfalls abgelehnt; `BudRegistry.getInstance().exists(budId)` wird vor `get(...)` geprüft, damit der Filter nicht bei jedem Fehlversuch eine `IllegalArgumentException` auslöst (`get(...)` wirft für unbekannte IDs).

### 4. Registrierung der eigenen BlockEntity-Komponente (Nachtrag, nicht in der ursprünglichen Fragenliste, aber Voraussetzung für Punkt 5/6 der Umsetzung)

`PluginBase` (Basisklasse von `JavaPlugin`, `javap`-verifiziert) hat neben dem bereits genutzten `getEntityStoreRegistry()` (siehe `BudComponent`/`PlayerBudComponent`-Registrierung in `BudPlugin.setup()`) ein exakt analoges `public ComponentRegistryProxy<ChunkStore> getChunkStoreRegistry()`. `WorkstationBlockEntity` wird also genau wie `BudComponent` registriert: `this.getChunkStoreRegistry().registerComponent(WorkstationBlockEntity.class, "WorkstationBlockEntity", WorkstationBlockEntity.CODEC)` in `BudPlugin.setup()`, der zurückgegebene `ComponentType<ChunkStore, WorkstationBlockEntity>` wird wie bei `BudComponent.setComponentType(...)` statisch gehalten. Der registrierte Name (`"WorkstationBlockEntity"`) ist der String, den das Block-JSON unter `"BlockEntity": {"Components": {"WorkstationBlockEntity": {...}}}` referenziert — gleiches Funktionsprinzip wie das eingebaute `"ItemContainerBlock"` aus Punkt 1.

### Umsetzung (Code)

- `com.bud.core.types.WorkRole` als Codec: `com.hypixel.hytale.codec.codecs.EnumCodec<WorkRole>` (`new EnumCodec<>(WorkRole.class)`) — dedizierte, nicht-deprecated Enum-Codec-Klasse der Engine, gefunden über Durchsuchen von `reference/server/com/hypixel/hytale/codec/codecs/` (die ursprünglich naheliegende `FunctionCodec<String,T>` ist ebenfalls `@Deprecated`).
- `com.bud.feature.work.WorkstationBlockEntity` — `Component<ChunkStore>`, ein `@Nonnull WorkRole`-Feld, `BuilderCodec` mit `KeyedCodec<>("WorkRole", enumCodec)`, sonst identisches Registrierungs-Boilerplate zu `BudComponent` (`getComponentType()`/`setComponentType()`/`clone()`).
- `com.bud.feature.work.WorkstationFilterSystem` — `RefSystem<ChunkStore>`, installiert den `SlotFilter` aus Punkt 2 auf Slot 0, sobald ein Block mit beiden Komponenten (`WorkstationBlockEntity` + `ItemContainerBlock`) geladen/platziert wird.
- `src/main/resources/Server/Item/Items/Workstation_Farming.json` — Item+Block-Definition, referenziert `Blocks/Benches/Furnace.blockymodel`/`Furnace_Texture_Off.png` (nur Optik, kein `"Bench"`-Config-Block), einfaches Wood/Rock-Rezept an einer normalen Workbench, `"BlockEntity": {"Components": {"ItemContainerBlock": {"Capacity": 2}, "WorkstationBlockEntity": {"WorkRole": "Farming"}}}`, `"Interactions": {"Use": "Open_Container"}`.
- Beide Komponente+System in `BudPlugin.setup()` registriert, nach dem gleichen Muster wie `BudComponent`/`PlayerBudComponent`.

### Regression: `EnumCodec` erwartet PascalCase, nicht den rohen Enum-Namen (behoben)

Erster Serverstart nach der Umsetzung schlug fehl: `Failed to find enum value for FARMING` beim Decodieren von `Workstation_Farming.json` (`WorkstationBlockEntity.WorkRole`). Root Cause per `javap -p -c` gegen `EnumCodec`/`EnumCodec$EnumStyle` verifiziert:

- `new EnumCodec<>(WorkRole.class)` (1-Arg-Konstruktor) ruft intern den 2-Arg-Konstruktor mit **fest verdrahtetem** `EnumStyle.CAMEL_CASE` auf — das wird als `this.enumStyle`-Feld gespeichert und ist die Style, gegen die beim Decodieren gematcht wird.
- Derselbe 2-Arg-Konstruktor baut zusätzlich ein `enumKeys`-Array (eine formatierte String-Repräsentation pro Enum-Konstante), aber **mit einer separat per `EnumStyle.detect(enumConstants)` erkannten Style** — nicht mit der übergebenen/gespeicherten `enumStyle`. `detect(...)` prüft pro Konstantenname, ob irgendwo ab Index 1 ein Kleinbuchstabe vorkommt (Signal für "das ist schon CamelCase geschrieben") — bei `WorkRole` (`FARMING`, `FORESTING`, `MINING`, `COMPANION`, komplett großgeschrieben) trifft das nie zu, `detect(...)` liefert also `LEGACY`.
- `LEGACY.formatCamelCase("FARMING")` splittet an `_` (hier: kein Split, ein Wort) und baut daraus "Großbuchstabe + Rest klein" — Ergebnis `"Farming"`. `enumKeys` wird also `["Farming", "Foresting", "Mining", "Companion"]`.
- Beim Decodieren ruft `getEnum(input)` `this.enumStyle.match(constants, enumKeys, input)` auf; mit `enumStyle = CAMEL_CASE` nimmt `match(...)` den Zweig, der `input` **case-sensitiv exakt** gegen `enumKeys` vergleicht (nicht gegen die rohen `name()`-Werte, das wäre der `LEGACY`-Zweig mit `equalsIgnoreCase`). `"FARMING"` matcht keinen Eintrag in `["Farming", ...]` → `IllegalArgumentException`/Decode-Fehler.

**Fix:** JSON-Wert auf `"Farming"` geändert (PascalCase), **nicht** der Codec-Wrapper. Begründung: der 1-Arg-Konstruktor mit `CAMEL_CASE` ist die von der Engine selbst vorgesehene Default-Wahl (kein expliziter `EnumStyle.LEGACY`-Aufruf existiert irgendwo sonst im Projekt als Präzedenzfall) und deckt sich mit dem durchgängigen PascalCase-Stil aller nativen Hytale-Asset-JSONs, die wir bisher gesehen haben (`"Material": "Solid"`, `"Type": "Processing"`, `"DrawType": "Model"`, ...). `buds/*.yml`s `workRole: FARMING` ist von diesem Bug **nicht** betroffen — das ist ein komplett anderer Codec-Pfad (SnakeYAML über `AbstractYamlMessage`, matcht Java-Enum-Konstanten nach exaktem Namen, kein `EnumCodec` beteiligt).

## Workstation

Größe 2x1 wie ein Furnace. Zum Start **ein vorhandenes Modell wiederverwenden** (Platzhalter, z. B. Furnace/Bench) — eigenes Modell baut Sascha erst, wenn die Funktion steht. Pro Job später eigenes Modell (Mini-Farm-Optik, Ore-Mix, Holzblöcke+kleiner Baum), aber das ist rein Asset-seitig, keine neue Java-Klasse (eine `WorkstationBlockEntity` mit `WorkRole`-Feld, Modell/Textur variieren je Block-Registrierung).

- **Slot 1**: Produkt-Input (Seedbag bei Farming/Foresting, Erz/Stein-Typ bei Mining) — bestimmt über ein Rezept-Mapping (YAML, gleiche Konvention wie `prompts/`/`buds/`) gleichzeitig das Zielprodukt und die validierte Rolle.
- **Slot 2**: Futter — wird wie Furnace-Brennstoff verbraucht, 10 Min/Gericht, mehrere eingelegte Gerichte werden nacheinander verbraucht (nicht parallel). Bei leerem Slot 2 geht der Bud in Ruhepose (sitzen/liegen), bis nachgefüttert wird.

### Besitzer-Bindung (Logout/Login, Mehrspieler)

Die `CardBud`-Karte trägt nur `BudId` (z. B. `"keyleth"`), keinen Spielerbezug — beim normalen Spawn/Despawn-Interact kein Problem, weil die Zuordnung kontextuell über den handelnden Spieler passiert. Für die Workstation reicht das nicht, weil die Bindung über Zeit bestehen bleibt (Bud arbeitet weiter, auch offline). **Entscheidung:** Besitzer beim Karten-Einlegen genauso kontextuell auflösen (über den einlegenden Spieler) und als `ownerPlayerId` auf der `WorkstationBlockEntity` selbst speichern, nicht auf der Karte. Ein gebundener Bud bleibt dann unabhängig vom Login-Status des Besitzers aktiv — Logout/Login braucht keine Sonderbehandlung, solange die zugrunde liegende Bud-NPC-Entity ohnehin (wie heute schon) unabhängig vom Spieler-Session-Status in der Welt persistiert.

**Offene Verifikation — beantwortet:** `BudComponent` trägt bereits `private PlayerRef playerRef` + `@Nonnull getPlayerRef()` (siehe `com.bud.core.components.BudComponent`, gesetzt in `BudComponent.create(...)`). `PlayerRef` selbst ist aber **kein stabiler Persistenz-Identifier** — es ist an Verbindung/Session gebunden (siehe `WindowManager`/`Universe`-Konstruktion, `PlayerRef` wird bei jeder Session neu aufgebaut). Der im Projekt bereits verwendete stabile Identifier ist `PlayerRef.getUuid()` (`java.util.UUID`, `final` auf `PlayerRef`) — grep-bestätigt bereits an anderer Stelle für exakt diesen Zweck genutzt: `DeletionCommand`: `PermissionsModule.get().getGroupsForUser(playerRef.getUuid())` (Berechtigungsgruppen müssen zwangsläufig über Login/Logout stabil bleiben, sonst würde diese Prüfung nicht funktionieren).

**Umsetzung:** `WorkstationBlockEntity` bekommt ein `@Nullable UUID ownerPlayerId`-Feld, codec-persistiert über `Codec.UUID_BINARY` (bytecode-geprüft, nicht `@Deprecated`, anders als `Codec.UUID_STRING`/`FunctionCodec`). Offene Frage dabei: **wie kommt man beim `SlotFilter.test(...)`-Aufruf an den einlegenden Spieler?** Bytecode-Recherche ergab: gar nicht direkt — `ItemContainer`, `ItemContainerChangeEvent` (`container()`/`transaction()`) und `Transaction` (`succeeded()`/`wasSlotModified(short)`) sind alle akteur-agnostisch, kein Feld/Parameter trägt eine Spieler-Referenz (verifiziert, bewusstes Engine-Design — ein Container kann generisch von vielem verändert werden, nicht nur von einem Spieler-Fenster). Die einzige Stelle mit Spielerbezug ist das geöffnete Fenster selbst: `Window` (Basisklasse von `BlockWindow`/`ContainerBlockWindow`) hat `private PlayerRef playerRef` + `public PlayerRef getPlayerRef()` (bytecode-verifiziert), erreichbar über `ItemContainerBlock.getWindows(): Map<UUID, ContainerBlockWindow>`.

**Konkrete Lösung:** `WorkstationFilterSystem` weist `ownerPlayerId` zu, sobald der Slot-Filter einen Insert akzeptiert (Seiteneffekt in der Filter-Prädikat-Lambda, mit Kommentar dokumentiert) — dabei wird der Besitzer über `resolveSoleViewer(...)` bestimmt: wenn `containerBlock.getWindows()` **genau ein** offenes Fenster hat, ist dessen `getPlayerRef().getUuid()` der Akteur. Bei 0 oder mehreren gleichzeitig offenen Fenstern (nicht eindeutig zuordenbar) bleibt `ownerPlayerId` unverändert, statt falsch zu raten — bewusst konservativ, kein Versuch einer garantiert korrekten Lösung (die würde tiefer in die Interaction/Window-Verarbeitung eingreifen müssen, deutlich über Phase-3-Scope hinaus).

**Einschätzung "anderer Spieler an gebundener Station" (nur Kommentar, keine Zugriffskontrolle, wie beauftragt):** Aktuell überschreibt jeder erneut akzeptierte Insert `ownerPlayerId` — es gibt keinen Lock. Ein zweiter Spieler, der die Karte entnimmt und eine andere (rollen-passende) einlegt, wird kommentarlos zum neuen Besitzer. Das ist für Phase 3 bewusst so belassen (kein Zugriffsschutz gefordert); Phase 4, wenn die eigentliche Bud-Bindung/Fuel-Logik an `ownerPlayerId` andockt, muss entscheiden, ob Karten-Änderungen durch Nicht-Owner an einer bereits gebundenen Station verhindert werden sollen.

## LLM-Reaktionen (bewusst zurückgestellt)

Für v1 komplett weggelassen — erst reiner Arbeits-Loop, dann später:

- Chat+Sound bei "zur Arbeit schicken".
- Bud-individuelle Reaktion (Gronkh hasst Arbeit, Keyleth mag Farming) über Prompt-YAML wie gehabt.
- Periodischer Arbeits-Kommentar (neuer Trigger-Typ auf bestehendem `AMBIENT`-Orchestrator-Channel, gleiche Cooldown-Mechanik).
- Reaktion beim Faulenzen/Ruhezustand.

## Reihenfolge der Jobs

**Farming zuerst** (Keyleth) — größte Wiederverwendung nativer Engine-Mechanik (Tilling, Wachstum, Ernte), geringstes Risiko. Foresting danach (gleicher Sensor/Action-Bau, andere Parameter + feste Quadranten statt freier Bodensuche). Mining zuletzt (einfachster Loop, aber neue Spawner-Logik statt Wiederverwendung).
