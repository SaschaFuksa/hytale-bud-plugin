# TODO: Bud Worker Mode — Mining (Veri)

Dritter Durchlauf nach Farming (Keyleth, siehe [`TODO-worker-mode.md`](TODO-worker-mode.md)) und Lumbering (Gronkh, siehe [`TODO-lumbering-mode.md`](TODO-lumbering-mode.md)). Hintergrund/Architektur weiterhin in [`docs/bud-worker-mode-plan.md`](docs/bud-worker-mode-plan.md), Abschnitte "Mining Vorabrecherche" und "Mining Konzept final" enthalten die Grundlage für diese Datei — vor Phase 0 lesen statt neu recherchieren.

Nach jedem Block `.\gradlew build` laufen lassen, nicht erst am Ende. Null-Safety-Konvention aus `CLAUDE.md` von Anfang an mitziehen. Scope bewusst eng: **nur Mining/Veri**. Keine Code-Kommentare, siehe Projekt-Memory.

**Keine Duplikate:** `AbstractWorkAction` trägt bereits Tilen/Pflanzen/Gießen/Düngen gemeinsam für Farming+Lumbering. Mining nutzt für sein Abbauen dieselben `extra*`-Hooks (`executeExtraWork`/`extraToolItemFor`/`extraCooldownSecondsFor`/`extraAnimationNameFor`), keine dritte Kopie. Ob Mining die vier gemeinsamen Schritte (Tilen/Pflanzen/Gießen/Düngen) überhaupt braucht, ist unten geklärt: **nein** (siehe Kern-Loop).

## Gewähltes Konzept (Sascha, 2026-08-16) — kein Vanilla-Wachstumssystem, eigener Timer-Mechanismus

Ersetzt die vorher diskutierten Optionen a-e vollständig. Kein `BlockType.Farming`/`BlockState`-System involviert — dadurch entfallen beide in der Vorabrecherche offenen Risiken (`BlockState` zur Laufzeit auslesen, `SEED_PREFIX`-Hardcoding) komplett, siehe `docs/bud-worker-mode-plan.md`.

**Hauptknoten:**
- Anzahl abhängig von `FieldRadius`, analog `WorkConfig.TreeEdgePositionCount`.
- Abstand vom Anchor: **Radius-1**, nicht Radius (Saschas Vorgabe, Lesart von Claude bestätigt: die Pyramide braucht selbst noch Platz zum Wachsen/Erweitern, bei vollem Radius würde sie über den konfigurierten Feldrand hinausragen).
- Pro Hauptknoten und eingelegtem Ore-Typ im Input-Slot: eine 5er-Pyramide (genaue Form — z. B. 3 Basis + 1 + 1 Spitze, oder 1 Startblock + 4 durch Erweiterung — folgt in Phase 1, siehe unten).
- Tiefe: nur an Hauptknoten, maximal **-1 Y** (Saschas Vorgabe, verhindert dass Veri sich feststeckt).
- Nur Hauptknoten können zu Erz reifen — Zufallslöcher nie (siehe unten).

**Zufallslöcher (Nebenaktivität, kein Erz):**
- 1x1-Löcher irgendwo im Feld (frei, nicht an feste Positionen gebunden wie die Hauptknoten).
- Liefern ausschließlich Stein, nie Erz ("hat halt nie Erfolg, Shiny Dinge zu finden").
- Eigene, kürzere Reifezeit vor dem Nachwachsen — verhindert einen Sofort-Buddel-Abbau-Loop ohne Zeitkosten.

**Input-Slot:** Ore-Typ-Auswahl bleibt liegen (wird nicht wie ein Seedbag verbraucht) — näher am bestehenden `CARD_SLOT`-Muster (Workstation-Bindung) als am `SEEDBAG_SLOT`-Muster.

**Der Kniff (Veri baut Steine immer sofort ab, sobald sie da sind):**
- Zwischenstufen der Pyramide dürfen echte, sichtbare `Stone`-Blöcke sein (schöner Aufbau-Effekt bleibt) — Veris Abbau-Kandidatenscan darf sie aber **nicht** über den rohen BlockType erkennen, sondern nur über eine eigene Wachstums-Komponente, die den Reifegrad trägt. Gleiches Prinzip wie Lumberings `hasTrunkBlock`-Fix (Block vorhanden ≠ automatisch Kandidat).
- Neue Chunk-Store-Komponente `OreGrowthBlock` (Arbeitsname), an der jeweiligen Weltposition, analog `TilledSoilBlock`: `growthStage` + `nextGrowthAt: Instant`, eigener `BuilderCodec` inkl. `Codec.INSTANT`.
- **Server-Neustart/Persistenz — verifiziert, kein offenes Risiko mehr:** `TilledSoilBlock` (nativ, hinter Gießen/Düngen) persistiert bereits exakt `Instant`-Felder (`wateredUntil`, `decayTime`) über denselben Chunk-Speichermechanismus, `Codec.INSTANT` existiert als eingebauter Codec-Typ. `OreGrowthBlock` im selben Muster gebaut wird automatisch über Neustarts hinweg korrekt persistiert — kein Sonderbau nötig.

**Werkzeuge:** fix `Tool_Shovel_Iron` (Buddeln) und `Tool_Pickaxe_Iron` (Steine/Erz abbauen), keine Tier-Progression. Beide existieren bereits im Content mit demselben `Power`/`GatherType`-Effizienzmuster wie Hatchet/Sickle/Hoe.

**Wässern/Düngen-Äquivalent:** **weggelassen** (von Sascha bestätigt, 2026-08-16) — Loop bleibt bewusst kürzer als bei Farming/Lumbering (nur Buddeln + Abbauen).

**Ore-Sorten:** alle gleich lange Wachstumsdauer (Saschas Vorgabe, anders als Farming/Lumbering wo Dauer pro Rolle aber nicht pro Sorte variiert) — eine einzige Config-Dauer pro Wachstumsstufe, keine Pro-Erz-Varianz.

## Phase 0 — Restverifikation (Claude, 2026-08-16, abgeschlossen für Slice 1)

- [x] Registrierungsmuster für eine neue Chunk-Store-Komponente nachvollzogen (`WorkstationBlockEntity`-Vorbild), `OreGrowthBlock` entsprechend geschrieben und in `BudPlugin` registriert.
- [x] Bench-Processing-Modell der Mining-Workstation gegengecheckt — **hier lag ein echter Fehler von Claude:** es wurde nur geprüft, DASS `Workstation_Mining.json` existiert, nicht ob ihr Inhalt dem produktiven Farming-/Lumbering-Aufbau entspricht. Die Datei hatte gar keine `BlockEntity.Components`-Sektion, dadurch war an der Station nichts ausführbar (von Sascha ingame gefunden). Nachträglich gefixt (`BenchBlock`/`ProcessingBenchBlock`/`WorkstationBlockEntity` mit `WorkRole: Mining`), siehe `docs/bud-worker-mode-plan.md`.
- [x] **Wichtiger Zusatzfund (nicht vorab erwartet):** Chunk-Store-Komponenten können nur an Blockpositionen hängen, deren `BlockType` sie selbst in `BlockEntity.Components` deklariert (verifiziert an `Soil_Dirt_Tilled.json`/`TilledSoilBlock`) — `OreGrowthBlock` kann nicht an beliebige (Luft-)Positionen. Fix: zwei neue eigene Blocktypen (`Mining_Growth_Hole.json`, `Mining_Growth_Ready.json`), siehe `docs/bud-worker-mode-plan.md`, "Mining Phase 1, Slice 1 umgesetzt".
- [x] `Tool_Pickaxe_Iron`/`Tool_Shovel_Iron` existieren mit dem etablierten `Power`/`GatherType`-Muster, direkt übernommen (`GatherType: "Rocks"` fürs Abbauen der Zufallslöcher-Steine, `Ore*`-`GatherType`s bereits im Pickaxe vorhanden für spätere Erzsorten).
- [ ] **Noch offen (nur für Hauptknoten-Pyramide, Slice 2):** `GatherType`/`Gathering.Breaking`-Werte je Erzsorte (Iron/Copper/Gold/Silver) für die finale Ore-Konvertierung.
- [ ] Veris Charakter/Persönlichkeit (`prompts/buds/veri.yml`) gegen das Konzept prüfen — noch nicht gemacht.
- [ ] Pyramiden-Form konkret festlegen (welche 5 Positionen relativ zum Hauptknoten) — Slice 2, noch offen.

## Phase 1 — Slice 1: Grundgerüst + Zufallslöcher-Loop (Claude, 2026-08-16, umgesetzt)

Vollständige technische Details in `docs/bud-worker-mode-plan.md`, "Mining Phase 1, Slice 1 umgesetzt" — hier nur der Statusüberblick.

- [x] `WorkType.DIG`/`MINE`, `WorkToolItems.DIG_TOOL_ITEM`/`MINE_TOOL_ITEM`.
- [x] `com.bud.feature.work.mining`-Paket: `OreGrowthBlock`, `MiningGrowthUtil`, `MiningFieldScan`, `MiningWorkAction`, `BuilderActionMiningWork`, `OreGrowthTickSystem`.
- [x] Zwei neue Blocktypen `Mining_Growth_Hole`/`Mining_Growth_Ready` (siehe Phase-0-Fund oben). **Enthielten zunächst drei Fehler von Claude, alle behoben:** erfundene Icon-Pfade (Dateien existierten nicht), falscher Textur-Mechanismus beim Model-Block (`Textures` statt `CustomModelTexture`), und `Gathering.Breaking` ohne `ItemId` — letzteres hätte Veri unseren eigenen internen Block statt echtem Stein in den Output legen lassen. Beide Dateien jetzt strikt nach verifizierten Vanilla-Vorlagen aufgebaut, alle Asset-Pfade einzeln gegen `reference/assets` geprüft.
- [x] `WorkConfig`: `MiningGrowthSeconds`, `DigIntervalSeconds`, `MineIntervalSeconds`. `work/recipes.yml`: `diggableBlocks` (welche Böden Veri anbuddeln darf) + `digRefillBlock` (womit das Loch nach dem Abbauen wieder aufgefüllt wird).
- [x] **Zwei Logikfehler beim Nachprüfen gefunden und behoben:** `isDigCandidate` akzeptierte jeden nicht-leeren Block (Veri hätte Workstation/Spielerbauten/Bäume weggebuddelt), und das Loch wurde nach dem Abbauen nie wieder aufgefüllt (Stelle dauerhaft tot, Loop gestorben — entgegen Saschas Vorgabe "wir füllen dann automatisiert die Löcher"). Siehe `docs/bud-worker-mode-plan.md`.
- [x] `WorkstationFuelTickSystem` um Mining-Zweig erweitert (Mine-Kandidat vor Dig-Kandidat priorisiert, geteilte Farming-Schritte für Mining komplett übersprungen).
- [x] `WorkstationBindingHandler.withWorkTools` auf echte Pro-Rolle-Werkzeugliste umgebaut (Mining bekam vorher gar keine Werkzeuge).
- [x] `Template_Veri_Bud.json`: `HotbarSize: 8` + MODE-4-Sensor/Action-Verkabelung proaktiv ergänzt (beide bekannten Lumbering-Slice-A-Fallstricke vorab geschlossen, nicht erst nach einem Ingame-Fund).
- [x] Nebenbei generalisiert (dritte Rolle machte es nötig): `BlockEntityPositions`, `WorldBlockEntities`, `GameClock`, `BlockDrops` aus bestehenden privaten/rollenspezifischen Methoden extrahiert, jetzt geteilt genutzt.
- [x] `.\gradlew clean build` grün.
- [x] **Erster Ingame-Test durchgeführt (Sascha, 2026-08-18) — Station öffnet, Veri spawnt, `DIG` wird ausgeführt.** Dabei drei Fehler gefunden, alle behoben (Details in `docs/bud-worker-mode-plan.md`):
  1. **Serverabsturz (kritisch, Welt gestoppt):** `world.setBlock` wurde direkt aus `OreGrowthTickSystem.tick` aufgerufen — ein Blockwechsel entfernt intern die Block-Entity, das ist eine schreibende Store-Operation während der ChunkStore tickt (`IllegalStateException: Store is currently processing!`). Fix: Blockwechsel per `world.execute(...)` aus dem Tick heraus verschoben (per Bytecode geprüft: `World.execute` reiht immer in `taskQueue` ein, gedrained außerhalb von `Store.tick`). Strukturell neu, weil Mining die erste Rolle mit eigenem chunk-getickten System ist.
  2. **Wachstumsstufe wäre verloren gegangen:** Der Blockwechsel zerstört die alte Block-Entity samt `OreGrowthBlock`; der Stein wäre dauerhaft unabbaubar geblieben. Fix: erst Block setzen, dann die *neue* Komponente holen und auf `STAGE_READY` setzen.
  3. **Wachstum 30× zu schnell:** `GameClock.now` liefert Spielzeit, nicht Echtzeit — die konfigurierten 30 Sekunden waren real 1 Sekunde (Log: `DIG` 13:20:31, Wachstum 13:20:32). Faktor exakt bestimmt (`SECONDS_PER_DAY` 86400 / Tageslänge 2880 = 30×). Fix: neuer Helfer `GameClock.realSecondsToGameSeconds(...)`, rechnet über die echten Tag-/Nacht-Dauern des Servers um; Spielzeit bleibt bewusst die Timer-Basis, weil sie während eines Serverstopps stillsteht.
- [x] **Slot-Filter korrigiert (von Sascha gefordert):** Fuel-Slot nahm die Veri-Karte an (leere `allowedFuel`-Liste = "alles erlaubt") → `allowedFuel.MINING: [Food_Fish_Grilled]` ergänzt **und** Bud-Karten werden jetzt rollenunabhängig als Futter abgelehnt. Erz-Slot (Input 2) nahm umgekehrt gar nichts an (kein Fallback in `isAllowedSeed`) → alle zehn Hytale-Erze unter `allowedSeeds.MINING` eingetragen. Input-Slot 1 (Karte) war bereits korrekt auf `WorkRole.MINING` gefiltert.
- [x] **Zweiter Ingame-Test (Sascha, 2026-08-18): Buddeln, Wachsen und Abbauen laufen.** Zwei Befunde, beide behoben (Details in `docs/bud-worker-mode-plan.md`):
  1. **Veri grub das komplette Feld um.** Der Mining-Zweig nahm den ersten Dig-Kandidaten aus dem deterministischen Serpentinen-Scan, ohne Obergrenze — jede Bodenkachel im Radius war gültig. Fix: ein Durchlauf zählt die aktiven Grabstellen (Loch *und* fertiger Stein), gebuddelt wird nur unterhalb von `maxDigHoles()`, und die Obergrenze ist **aus der Feldgröße abgeleitet** (`= FieldRadius`, bei aktuell 5 also 5 Löcher) statt als eigener Config-Wert — skaliert automatisch mit dem Feld.
  2. **Verteilung:** Auswahl jetzt zufällig aus den freien Kandidaten statt immer der erste Treffer, mit horizontalem Mindestabstand über `OreMinDistance` (Default 2) — **ein Config-Wert, der bis dahin tot im `WorkConfig` lag und nirgends gelesen wurde** (README: "not yet implemented"). Saschas "nie auf einem anderen Buddelloch" war durch `isDigCandidate` ohnehin erfüllt; der Abstand macht daraus echte Streuung.
- [x] **Wachstumsdauer aus dem Vanilla-System übernommen** (Saschas Vorschlag). Recherchiert statt geschätzt — Vanilla nutzt `BlockType.Farming.Stages[].Duration` mit `Min`/`Max`: Crop-Stage 28800–30600 Spielsekunden (16–17 reale Minuten), Baum-Stage 40000–60000 (22–33 Minuten). Gewählt: **Crop-Stage**, da Mining nur eine Wachstumsstufe hat (ein Baum durchläuft fünf). `MiningGrowthSeconds` (Echtzeit) ersetzt durch `MiningGrowthGameSecondsMin`/`-Max` (28800/30600) — gleiche Einheit und gleiche Min/Max-Streuung wie Vanilla, dadurch reifen nicht alle Grabstellen im Gleichschritt. Die Umrechnung `GameClock.realSecondsToGameSeconds` aus der vorigen Runde ist damit überflüssig und wurde entfernt statt als toter Code stehenzubleiben.
- [x] **Hauptknoten liefen nie an — echte Ursache gefunden (2026-08-18):** `BlockType.fromString("Ore_Adamantite_Stone")` liefert zur Laufzeit `null`. Die vorherige "Richtigstellung" (Ores.json listet neun `_Stone`-Blöcke) war eine **Überkorrektur** — die BlockTypeList ist kein Nachweis für einen existierenden BlockType. Fix ohne Config-Mapping: geordnete Wirtsgesteins-Suffixliste (`_Stone`, `_Magma`, `_Slate`, `_Shale`, `_Sandstone`, `_Basalt`, `_Volcanic`, `_Calcite`, `_Mud`), erstes von der Engine bekanntes gewinnt — Adamantite → `_Magma`. Weiterhin lazy aufgelöst. **Lehre: bei Block-Ids ist nur die Laufzeit verlässlich.**
- [x] **Knotenanzahl skaliert mit der Feldgröße:** `FieldRadius <= 6` → 2, `<= 9` → 4, darüber → 8; Mittelpunkte gleichmäßig auf einem Kreis mit Abstand `FieldRadius - 1`, Start bei 90° (2er-Variante = die abgestimmten ±Z-Knoten). Geometriefehler vorab abgefangen: bei 8 Knoten wären zwei Diagonal-Arme aus dem Feldradius gefallen und nie gebuddelt worden.
- [x] **Knoten haben jetzt oberste Priorität:** Knotenloch → Erz am Knoten → Zufallsstein → Zufallsloch.
- [x] **Zufallsloch-Grenze im Log bestätigt:** 0/5 bis 5/5, danach `chosen: none`.
- [x] **Kreuz wurde nicht gebuddelt (2026-08-18):** Knotenkandidaten kamen aus `serpentinePositions`, deren vertikales Band nur `FieldMaxHeight` (=2) beträgt — an einer Kante fielen Arme und der zweite Knoten heraus. Fix: `scanNodes` sucht **spaltenweise** in einem eigenen, breiteren Höhenband (±4) von oben nach unten. Log zeigt jetzt `[growing=n, ready=n, noGround=n]`.
- [x] **Erz wird verbraucht:** ein Erz je Pyramide, entnommen beim Kernloch; die Arme erben den Zielblock vom Kern (sonst wäre die Pyramide nach Verbrauch des Erzes stehengeblieben).
- [x] **Ertrag = 5:** Deckstein reift zu gewöhnlichem abbaubarem Stein statt zu Erz — 5 erztragende Blöcke (Kreuz) + Stein-Nebenprodukt, statt 6 Erz.
- [x] **Deckstein reift wieder zu Erz** (Saschas Entscheidung) → 6 erztragende Blöcke, Gewinn +5. Rolle `KIND_NODE_CAP` wieder entfernt statt ungenutzt stehenzulassen; "Deckstein zuerst abbauen" trägt die Geometrie (höchste Position gewinnt), nicht die Rolle.
- [x] **Knoten werden am Stück abgearbeitet:** `scanNodes` sammelt pro Knoten und liefert nur den ersten mit offener Arbeit. Zusätzlich global **Abbauen vor Buddeln** — sonst hätte der erste Knoten nach dem Leerräumen dauerhaft Vorrang behalten und der zweite wäre nie abgebaut worden. Log nennt jetzt `node=n`.
- [ ] **Neue Pyramide braucht neues Erz im zweiten Input-Slot** (angefangene laufen unabhängig zu Ende).
- [ ] **Tiefen-Erweiterung (max. -1 Y)** aus dem Ursprüngskonzept weiterhin nicht umgesetzt. — ohne auflösbares Erz werden Knotenlöcher gar nicht erst ausgehoben (Absicht: ein Knoten ohne Zielerz könnte nie reifen). Erklärt das Testverhalten vom 2026-08-18.
- [ ] **Diagnosezeile jetzt sichtbar:** lag auf `fine`, Server steht auf `LogLevel: INFO` — hängt jetzt auf `info` hinter `DebugConfig.EnableBudDebugInfo` (in `run/.../Debug.json` auf `true` gesetzt).
- [ ] **Noch offen: erneuter Ingame-Test nach den Fixes.** Insbesondere: Buddeln → Timer (jetzt eine Vanilla-Crop-Stage, ~16 reale Minuten) → höchstens 5 Grabstellen gleichzeitig, zufällig verteilt → Stein erscheint (nicht vorher abbaubar) → Abbauen → Loch wird wieder aufgefüllt. **Und explizit: Serverneustart während eines laufenden Wachstumszyklus** — das Kernversprechen dieses Konzepts, weiterhin nicht am echten Server verifiziert.
- [ ] **Offen (Saschas Entscheidung):** YAML-Key `allowedSeeds` heißt weiterhin so, obwohl darunter für Mining Erze stehen — Farming-Name auf einer rollenneutralen Sache. Umbenennung (z. B. `allowedSecondaryInput`) berührt `WorkRecipeYaml`/`WorkRecipeConfig`/`WorkstationSeedUtil` + beide `recipes.yml`-Kopien, daher bewusst nicht mitten im Bugfix mitgenommen.

## Phase 1+ — weitere Slices (noch nicht begonnen)

1. ~~**Hauptknoten-Pyramide**~~ — umgesetzt 2026-08-18 (Details in `docs/bud-worker-mode-plan.md`, "Mining Slice 2"): Knotenmittelpunkte bei `FieldRadius - 1` an beiden Z-Enden (bei Radius 5 also zwei), je ein Kreuz aus Mittelpunkt + vier horizontalen Nachbarn, Deckstein mittig obendrauf nach Stufe 1. Kette `Loch → Mining_Node_Stone (nicht abbaubar) → Erzblock (abbaubar)`, je Block eigener Timer, kein Cross-Block-Koordinationscode. **Offene Punkte daraus:**
   - **Achse ist eine Annahme:** "vertikales Ende" wurde als Z-Achse gelesen. Falls X gemeint war — eine Zeile in `MiningFieldScan.nodeCenterColumns`.
   - **KORREKTUR (2026-08-18):** Die untenstehende Behauptung war **falsch** — sie beruhte auf einer reinen Dateinamen-Suche. `Server/BlockTypeList/Ores.json` listet neun `Ore_*_Stone`-Blöcke (alle außer Prisma); Sascha lag mit Wiki/Ingame richtig. Das Config-Mapping ist deshalb wieder entfernt, der Blockname wird als `<OreItemId>_Stone` abgeleitet und **lazy** geprüft. **Eigentlicher Bug war ein anderer:** die Validierung lief in `setup()`, drei Sekunden bevor die Engine die BlockTypes lädt — dadurch wurde jeder Eintrag verworfen, die Zuordnung blieb leer und der Knoten-Zweig wurde komplett übersprungen. Details in `docs/bud-worker-mode-plan.md`, "Nachtrag: Hauptknoten liefen nie an".
   - ~~**Recherche widerlegte die Erz-Annahme:**~~ `Ore_Thorium_Stone` existiert nicht. Nur Copper/Gold/Iron/Mithril/Silver haben eine `_Stone`-Variante; Adamantite/Cobalt/Thorium nur anderes Wirtsgestein; **Onyxium und Prisma haben gar keinen Weltblock**. Zuordnung deshalb explizit als `oreTargetBlocks` in `recipes.yml` statt Namensableitung (die wäre bei der Hälfte still gescheitert). **Saschas Entscheidung:** Onyxium/Prisma aus `allowedSeeds.MINING` streichen oder auf Ersatzgestein mappen.
   - **Unverifiziert:** `Ore_Iron_Stone` nutzt eine *inline* `DropList`, `BlockDrops` liest `getDropListId()` (String). Ob die Engine inline-Listen als Asset registriert, war statisch nicht klärbar — `executeMine` loggt jetzt eine Warnung bei leerer Auflösung, der Ingame-Test entscheidet.
   - **Tiefen-Erweiterung (max. -1 Y)** aus dem ursprünglichen Konzept ist **noch nicht** umgesetzt — die Pyramide wächst aktuell nur nach oben.
2. **`Workstation_Mining.json` vervollständigen** (kosmetisch, blockiert nichts): Input-/Fuel-Slot-Icons fehlen (generische `{}`-Platzhalter, Farming/Lumbering haben je eigene), `OutputSlotsCount` = 4 statt Lumberings 6, kein `FailedSoundEventId`/`AmbientSoundEventId`.
3. ~~**`allowedFuel` für `MINING`**~~ — erledigt 2026-08-18: `Food_Fish_Grilled` (Saschas Wahl: anspruchsvolleres Futter, dafür Erz als Ertrag).
4. **Erz-Slot wirkt noch nicht** — das Erz in Input-Slot 2 ist in Slice 1 reine Dekoration, `resolveCropBlockType` wird für Mining gar nicht aufgerufen. Die Ausbeute hängt erst ab Slice 2 (Hauptknoten-Pyramide) am gewählten Erz.
5. **LLM-Reaktionen** — prüfen, ob die vier bestehenden Farming/Lumbering-Anlässe (`com.bud.feature.work.reaction`) 1:1 passen.
4. **Kosmetische Politur** — erst nachdem der funktionale Teil steht und getestet ist.
5. **Mehrere Erzsorten** über Iron/Copper/Gold/Silver hinaus (reine Datenerweiterung).

## Verifikation

- [x] `.\gradlew build` grün nach jedem Schritt (Slice 1).
- [ ] Ingame-Test wie bei Farming/Lumbering je Phase, nicht erst am Ende — insbesondere: übersteht ein halbfertiger Wachstumszyklus einen Serverneustart korrekt (Kernversprechen dieses Konzepts). **Noch offen.**
- [ ] Diese Datei komplett abgehakt, bevor Mining als abgeschlossen gilt.
