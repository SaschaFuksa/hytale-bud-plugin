#FUEL

Problem: Beim öffnen der Farming oder Lumbering Station, ist ein komisches Verhalten:
In den ersten Sekunden nach dem öffnen, werden Items aus meinem Invetar automatisch verschoben, als wäre ein Key gedrückt. Ich habe keine Tasten gedrückt, und es passiert auch nur in den ersten Sekunden nach dem öffnen der Station. Danach ist alles normal.
UPDATE HEUTE: GGF. NICHT MEHR DA?

Problem: Initaler "Turn on"-Bug (FUEL):
GGF Zusammenhang mit Problem darüber: Wenn man die Station öffnet, und dann das erste Mal Futter drin hat und bei Fuel auf "Turn on" klickt, verschwindet sofort das Futter, und die Station ist aber direkt wieder aus mit akustischem Signal.
UPDATE HEUTE: NOCH DA

Problem: Keine Fuel Slot Animation wie bei den anderen Hytale Stations:
Ohne diese ANimation sieht man nicht, dass es aktiv läuft + es zeigt auch in etwa die Dauer an, wie lange es noch läuft.

#FIELD RADIUS

Problem: Zahlenwerte für Field Radius:
Zurzeit erlauben wir Zahlenwerte, das kann aber ungünstig sein. Wir sollten Größen erlauben: SMALL, MEDIUM, LARGE
Farming Station: SMALL = 4, MEDIUM = 5, LARGE = 6
Lumbering Station: SMALL = 3, MEDIUM = 5, LARGE = 7: (2 Bäume (Nur Horizontal), 4 Bäume (Horizontal + Vertikal), 8 Bäume (Horizontal + Vertikal + Diagonal))
Mining: SMALL = 3, MEDIUM = 5, LARGE = 7: (2 "Main Nodes" (Nur Horizontal), 4 Main Nodes (Horizontal + Vertikal), 8 Main Nodes (Horizontal + Vertikal + Diagonal))

Problem: Feldradius ist nicht ebenerdig und Bud hat Probleme dadurch:
Ich habe es nun auch mal auf eienm Feld probiert, wo ich Blockhöhen drin habe (also -1, 0, +1) und da hat der Bud Probleme, die Felder richtig zu bearbeiten.
Gedanke: Wenn wir merken, dass die Aktion nicht ausgeführt werden konnte, den Bud direkt auf den Block schicken?

#ANIMATIONEN

Problem: Animationen:
Keyleth: Das aktuelle REST macht kein sitzen. SIT lässt hingegen Keyleth in den Boden versinken, da sie sogesehen dann auf etwas sitzt.
Beides ungünstig.
Gronkh: Aktuell ohne Arbeit sollte er liegen, aber er steht nur herum - Oder wie aktuell steht mit dem letzten Tool blöd herum.
Für Keyleth: Notfalls nutzen wir die Sitz Animation und lassen Keyleth davor auf die Station laufen, dann sieht es aus, als würde sie auf der Station sitzen. Für Gronkh: Vor die Station laufen und davor liegen.
Veri: Kann gerne auch auf der Station sitzen.

[ERLEDIGT 2026-08-18] Problem: Missing animation 'Interact' (WARN, kosmetisch, bekannte Lücke)
Gronkh_Bud.json hat kein AnimationSets.Interact (erbt nur von Trork), Veri_Bud.json hat dieselbe Lücke — nur Keyleth_Bud.json definiert explizit einen Interact-Clip. Keine Lumbering-Regression, sondern eine bereits vorher bestehende, kosmetische Lücke, die laut TODO ohnehin erst in Phase "Kosmetische Politur" angegangen wird. Das wäre nun der Fall.
Betrifft auch Veri, er hat auch die Meldung in den Logs gehabt.

[ERLEDIGT 2026-08-18] Lumbering: Gib Gronkh die selbe Axt, die er auch beim Kämpfen verwendet

#LLM

Problem: Direkte LLM-Interaktion mit F:
Aktuell kann man direkt mit F eine LLM Reaktion vom Bud erhalten. Das Problem: Diese kommt nicht sofort sondern versetzt. Für eien direkte Interkation ungünstig, sollte wie ein State Change z.B. sofort die LLM Reaktion erfolgen.

Nice to have: Anvisieren bei F-Interaktion:
Wäre cool wenn der Bud seinen Kopf zum Spieler dreht wenn man mit F-Interaktion den Bud anspricht. Aktuell schaut er immer in die Richtung in die er gerade schaut, egal ob der Spieler links oder rechts steht.

#CONFIG
Wir haben in der WorkConfig zu viele spezifische Configs, die nur für Lumbering oder Farming gelten. Wir sollten diese ggf. in eigene Configs auslagern, die von einer gemeinsamen WorkConfig-Klasse erben.
Bitte auch prüfen, ob welche weg können, die inital mal geplant wurden.
[ERLEDIGT/HINFÄLLIG 2026-08-18] Auch recipes.yml und andere config Dateien von Work prüfen. Aufräumem z.B. diggableBlocks sind selbe wie tillableBlocks, also nur tillableBlocks verwenden.

#GGF OBSOLETE?

Problem: Store is currently processing! (SEVERE) — ERLEDIGT/FEHLDIAGNOSE, siehe Nachtrag unten

URSPRÜNGLICHE NOTIZ:
Problem: Store is currently processing! (SEVERE, unser Code, real)
findNextWorkAssignment ruft am Anfang unbedingt currentGameTime(world) auf (Zeile 277), das intern world.getEntityStore().getStore().getResource(...) macht — ein EntityStore-Store-Zugriff aus dem ChunkStore-Tick von WorkstationFuelTickSystem heraus. Wenn der EntityStore gerade selbst tickt, wirft das die IllegalStateException. Passiert bei jedem Tick jeder gebundenen Workstation (für den WATER_REFRESH-Check), nicht nur bei Lumbering — vorbestehender Bug aus der Farming-Runde, keine Regression durch die 8-Punkte-Änderung. Der catch (RuntimeException) fängt es ab, aber die gesamte Prioritätskette (Till/Plant/Water/Fertilize/Harvest) wird für diesen Tick übersprungen, nicht nur der Water-Refresh-Teil — bei häufigem Auftreten könnte das Gronkh sichtbar stocken lassen. Kein Blocker fürs Wachstumstesten, aber ein echter Fix wäre sinnvoll (z. B. now nur lazy holen, wenn waterRefreshWinner-Zweig tatsächlich gebraucht wird, oder Game-Time anders cachen statt live pro Tick aus dem EntityStore zu ziehen).

Problem: Cards/Items Verschwunden nach Server neustart:
Problem: Ich habe aus der Station die Karte bzw. Items rausgenommen, und nach einem Serverneustart waren diese aus meinem Inventar verschwunden. Ich habe die Station nicht verlassen, sondern nur den Server neugestartet. Die Items waren weg. Es ist immer wieder passiert.


## Nachtrag Claude (2026-08-18) — Prüfung des "#GGF OBSOLETE?"-Blocks

**"Store is currently processing!" — die Diagnose in der Notiz oben war falsch, das Symptom ist weg.**
Die Notiz vermutete `GameClock.now(world)` (EntityStore-Zugriff aus dem ChunkStore-Tick) als Auslöser. Per `javap` geprüft: `Store.getResource` enthält **keine** Processing-Assertion — es validiert nur die Registry und liest über einen Index. Lesende Zugriffe lösen die Exception also gar nicht aus; sie kommt ausschließlich aus **schreibenden** Operationen (`assertWriteProcessing`). Der einzige reale Fall war das `world.setBlock` aus `OreGrowthTickSystem` (Mining Slice 1), das den Server zum Absturz brachte und per `world.execute(...)` behoben wurde. In den letzten sechs Server-Logs kommt der String kein einziges Mal vor. **Kein Handlungsbedarf.**

**Nebenbefund zum selben Punkt:** `findNextWorkAssignment` berechnet `GameClock.now(world)` weiterhin unbedingt, obwohl der Wert nur im Farming-Zweig (Water-Refresh) gebraucht wird — für Mining und Lumbering ist es verschenkte Arbeit pro Tick. Harmlos, aber unsauber.

**#CONFIG — "prüfen, ob welche weg können": keine.** Alle 20 `WorkConfig`-Getter haben mindestens einen Aufrufer außerhalb der Klasse. `OreMinDistance` war bis zu dieser Sitzung tot (im README als "not yet implemented" geführt) und wird jetzt für den Mindestabstand der Zufallslöcher genutzt. Die Aufteilung in rollenspezifische Configs bleibt offen; rollenspezifisch sind: `HarvestIntervalSeconds` (nur Farming), `FellIntervalSeconds`/`TreeMinDistance`/`TreeEdgePositionCount` (nur Lumbering), `OreMinDistance`/`MiningGrowthGameSeconds*`/`DigIntervalSeconds`/`MineIntervalSeconds` (nur Mining).

**#CONFIG — "diggableBlocks sind selbe wie tillableBlocks": nicht ganz.** Die Listen sind bis auf zwei Einträge identisch: `tillableBlocks` enthält zusätzlich `Soil_Leaves` und `Soil_Needles`. Ein Zusammenlegen wäre also kein reines Aufräumen, sondern eine Verhaltensänderung (Veri dürfte dann auch Laub- und Nadelboden anbuddeln). **Saschas Entscheidung.**


## Statusdurchgang Claude (2026-08-18) — alle Punkte einzeln geprüft

**ERLEDIGT in dieser Runde**

- **Missing animation 'Interact'.** Die Notiz war teilweise veraltet: `Gronkh_Bud.json` hat inzwischen sowohl `Interact` (Battleaxe) als auch `Fell` (Axe/Swing_Down), `Keyleth_Bud.json` hat `Interact`. Fehlend war ausschließlich **Veri** — `Veri_Bud.json` hatte überhaupt keinen `AnimationSets`-Block. Ergänzt mit `NPC/Intelligent/Feran/Animations/Flavor/Scratch.blockyanim` (einzige Flavor-Animation, die Feran mitbringt; gegen `reference/assets` verifiziert). Das behebt zugleich die Arbeitsanimation beim Mining, denn `AbstractWorkAction.WORK_ANIMATION` ist genau `"Interact"` — Veri hat also bisher bei jedem Buddeln und Abbauen ins Leere animiert.
- **Gronkhs Axt.** Er kämpfte mit `Weapon_Mace_Stone_Trork` (Keule) und fällte mit `Tool_Hatchet_Cobalt` — zwei verschiedene Gegenstände, und **beide passten nicht zu seinen eigenen Animationen**, die Battleaxe bzw. Axe referenzieren. Beides auf `Weapon_Axe_Stone_Trork` vereinheitlicht (`buds/gronkh.yml` `weaponId` + `WorkToolItems.FELL_TOOL_ITEM`). Gefahrlos, weil das Werkzeug beim Fällen rein kosmetisch ist: Drops rechnet `WorkstationWoodUtil`/`BlockDrops` selbst, der Blockabbau läuft über `world.setBlock`, nicht über den Gather-Typ des Werkzeugs. `budVersion` auf 2 gezogen, Runtime-Kopie synchronisiert.
- **recipes.yml diggableBlocks vs tillableBlocks.** Hinfällig: Die Listen sind nicht mehr deckungsgleich und sollen es auch nicht sein. `tillableBlocks` hat zusätzlich `Soil_Leaves`/`Soil_Needles`, `diggableBlocks` inzwischen sechs Gesteinstypen (`Rock_Stone`, `Rock_Stone_Cobble`, Sandstein-Varianten …). Zusammenlegen wäre jetzt eine Verhaltensänderung, kein Aufräumen.
- **Store is currently processing.** Siehe Nachtrag oben — Fehldiagnose, Symptom nachweislich weg.

**GEPRÜFT, ABER NOCH OFFEN**

- **Feldradius nicht ebenerdig.** Für **Mining-Hauptknoten** inzwischen gelöst: `MiningFieldScan` sucht pro Kreuzspalte in einem eigenen Höhenband (±4 statt `FieldMaxHeight`=2) von oben nach unten die gültige Bodenposition. Genau daran scheiterte das Kreuz an einer Kante. **Farming und Lumbering hängen weiterhin am schmalen `FieldMaxHeight`-Band** — dieselbe Klasse von Fehler ist dort also noch zu erwarten. Saschas Vorschlag ("Bud direkt auf den Block schicken") ist nicht umgesetzt.
- **WorkConfig-Aufteilung nach Rollen.** Nicht umgesetzt. Geprüft: **kein einziger der 20 Getter ist ungenutzt**, es kann also nichts ersatzlos weg. Rollenspezifisch sind `HarvestIntervalSeconds` (Farming), `FellIntervalSeconds`/`TreeMinDistance`/`TreeEdgePositionCount` (Lumbering), `OreMinDistance`/`MiningGrowthGameSeconds*`/`DigIntervalSeconds`/`MineIntervalSeconds` (Mining).

**NOCH NICHT ANGEFASST** (unverändert offen)

1. Fuel "Turn on"-Bug (Futter verschwindet sofort, Station geht direkt wieder aus) — vermutlich Eigenlogik des Vanilla-`ProcessingBenchBlock`, das ohne gültiges Rezept selbst abschaltet. Braucht Instrumentierung am Bench-Zustand.
2. Items verschwinden nach Serverneustart aus dem Inventar.
3. Fuel-Slot-Animation wie bei Vanilla-Stationen.
4. Item-Verschiebung in den ersten Sekunden nach dem Öffnen — laut Notiz "ggf. nicht mehr da", braucht eine Bestätigung ingame.
5. Kopfdrehung zum Spieler bei F (nice to have).
6. Keyleth/Veri "auf der Station sitzen" — die Pose allein reicht nicht, der Bud müsste dazu auf/vor die Station positioniert werden (siehe Runde 3).

Neu: Roster Card hat mit Rechtsklick die Buds despawned. Auch hier sollen Buds die bei der Workstation drin sind, nicht mehr per Card gespawned oder despawned werden dürfen.

## Runde 2 (Claude, 2026-08-18) — Prio 1 und 2 erledigt

**Feldradius als SMALL/MEDIUM/LARGE.** `FieldRadius` (freie Zahl) und `TreeEdgePositionCount` sind aus `WorkConfig` verschwunden, stattdessen gibt es `FarmingFieldSize`/`LumberingFieldSize`/`MiningFieldSize` mit den Werten aus dieser Datei: Farming 4/5/6, Lumbering und Mining je 3/5/7. Aufgelöst wird über `getFieldRadius(WorkRole)`; ein unbekannter Wert fällt mit einer Warnung auf `MEDIUM` zurück statt still auf 0. Neuer Typ `com.bud.core.types.FieldSize`.

Die Strukturanzahl hängt jetzt an derselben Größe (`getFieldStructureCount`): **SMALL 2, MEDIUM 4, LARGE 8** — für Mining die Hauptknoten, für Lumbering die Baumplätze. Damit ist die bisherige mining-eigene Schwellenlogik (`mainNodeCount(radius)`) entfallen, und Lumbering hat erstmals eine 2er-Variante bekommen (`EDGE_ANGLES_DEGREES_HORIZONTAL`, vorher konnte `treeEdgePositions` nur 4 oder 8).

**Achtung, Verhaltensänderung:** Bei `MEDIUM` (Radius 5) gibt es jetzt **4** Hauptknoten. In den Testrunden davor waren es bei Radius 5 zwei — die alte Schwellenlogik sagte "Radius <= 6 → 2". Wer die zwei Knoten will, setzt `MiningFieldSize: SMALL` (Radius 3). Das folgt der Vorgabe in dieser Datei ("MEDIUM = 5 → 4 Main Nodes"), weicht aber von dem ab, was zuletzt ingame zu sehen war.

**Roster-Karte despawnt keine Workstation-Buds mehr.** `CardBudInteraction` ermittelt vor Primär- **und** Sekundärklick, welche Buds des Spielers gerade an einer Workstation hängen (über `BudComponent.getWorkstationAnchor() != null`, das beim Binden gesetzt und beim Despawn geleert wird), zieht sie aus der Menge ab und meldet das dem Spieler im Chat. Betrifft die Roster-Karte wie auch die Einzelkarten, und gilt in beide Richtungen — ein gebundener Bud kann also weder weggeklickt noch daneben nochmal gespawnt werden. Bleibt nach dem Abzug nichts übrig, passiert gar nichts (kein Sound, kein Event).


## Runde 3 (Claude, 2026-08-18) — Prio 3 und 4

**Direkte F-Reaktion ohne Verzögerung.** Ursache gefunden: Der `PLAYER`-Kanal umging zwar die Cooldowns (`servePlayerChannel`), wurde aber trotzdem nur im **Orchestrator-Tick** bedient — Default `OrchestratorTickIntervalMs` = 2500 ms. Jede F-Reaktion wartete also bis zu 2,5 Sekunden, bevor der LLM-Aufruf überhaupt startete. `Orchestrator.enqueue` setzt `PLAYER`-Ereignisse jetzt **sofort** ab (`dispatchDirect`) und stellt sie gar nicht erst in die Queue; `servePlayerChannel` ist damit überflüssig und entfernt.

Dabei ging die bisherige Schutzfunktion der Queue verloren (Deduplizierung nach `eventType` und die Tiefenbegrenzung), die bei schnellem F-Drücken Mehrfachaufrufe verhindert hätte. Ersatz: ein `directDispatchInFlight`-Set pro `Spieler|eventType`; ein zweites F während eines laufenden Aufrufs wird ignoriert statt einen parallelen LLM-Call zu starten. Freigegeben wird der Schlüssel in einem `finally`, damit ein Fehler im Dispatch die Interaktion nicht dauerhaft blockiert. Betrifft alle `PLAYER`-Kanal-Nutzer, also auch `WorkTalkAction` (F an der Workstation) und den Spieler-Chat.

**Ruhe-Animationen.** Ursache für "Gronkh steht nur herum, mit dem letzten Tool in der Hand": Sein `.Resting`-Block nutzte `Crouch: true` — also gar keine Pose-Animation — und räumte das Werkzeug nicht weg. Keyleth machte beides bereits richtig (`PlayAnimation` + `Inventory ClearHeldItem`).

Vorab geprüft, welche Posen überhaupt **registriert** sind (nicht nur, welche `.blockyanim`-Dateien existieren — die Unterscheidung hat in dieser Sitzung schon zweimal Zeit gekostet): `AnimationSets` in `Models/Intelligent/<Spezies>.json` listet für **Trork** und **Kweebec_Sapling** je `Sit`/`Laydown`/`Sleep`, für **Feran** `Sit`, `Laydown`, `Sleep`, `Crouch` — bemerkenswert, weil Feran unter `Animations/Flavor` nur `Scratch.blockyanim` mitbringt, die Namen also aus einer geteilten Quelle kommen. Beide gewünschten Posen sind damit verfügbar.

Gronkh und Veri bekommen jetzt `PlayAnimation Laydown` (Status-Slot) plus `ClearHeldItem` im `.Resting`, und im `.Default` den leeren `PlayAnimation`-Clear, damit die Pose beim Wiederaufnehmen der Arbeit nicht hängen bleibt — exakt Keyleths Muster.

**Bewusst nicht gemacht:** Veri sitzt nicht, obwohl gewünscht. `Sit` ist zwar registriert, aber für Keyleth ist im Rollen-JSON dokumentiert, dass `Sit` eine Sitzhöhe (Möbel) annimmt und den Bud ohne Sitzgelegenheit **in den Boden versinken** lässt. Dieselbe Falle wäre bei Feran zu erwarten. `Laydown` ist eine bodennahe Pose und funktioniert ohne Sitzgelegenheit. Die ursprüngliche Idee ("auf der Station sitzen") löst das Problem zwar, verlangt aber, den Bud beim Wechsel in `.Resting` auf bzw. vor die Station zu **positionieren** — das ist Bewegungslogik, kein Animationstausch, und bleibt als eigener Punkt offen. Wer `Sit` trotzdem testen will: ein Wort in `Template_Veri_Bud.json`.

Keyleth bleibt unverändert auf `Rest`; das ist laut der Dokumentation im JSON bereits die bewusst gewählte bodennahe Alternative zu `Sit`.

`.\gradlew build` zweimal grün, alle drei Rollen-JSONs syntaktisch validiert.

## Runde 4 (Claude, 2026-08-18)

**Work-Logging spammte.** Die Zeile `"<Action> arrived, invoking tryExecuteWork for ..."` in `AbstractWorkAction.canExecute` lief auf `info` und feuerte bei jedem Arbeitsschritt (im Log doppelt, weil `canExecute` mehrfach pro Zuweisung ausgewertet wird). Sie hängt jetzt wie die Mining-Auswahlzeile hinter `DebugConfig.EnableBudDebugInfo`, ebenso "Mining node started", "Workstation out of fuel" und "Workstation refed". Warnungen und Fehler bleiben unabhängig vom Schalter sichtbar.

**Resting-Animation: Ursache gefunden, mein Fehler aus Runde 3.** Sascha meldete "Gronkh hatte sich kurz hingelegt aber stand sofort wieder". Genau das ist das Symptom einer **Übergangs- statt Dauerpose**: In `Models/Intelligent/<Spezies>.json` trägt `Laydown` ein explizites `"Looping": false`, ebenso `SitDown`/`SitUp`, `RestDown`/`RestUp` und `Wake`. Die gehaltenen Posen sind die ohne Looping-Flag: `Sit`, `Rest`, `Sleep`, `Crouch`. Das Namensmuster ist durchgehend `<Pose>Down` (rein) → `<Pose>` (halten) → `<Pose>Up`/`Wake` (raus); `Laydown` gehört zur ersten Gruppe, die gehaltene Liegepose heißt `Sleep`.

In Runde 3 wurde nur geprüft, ob der Name **registriert** ist — nicht, ob er eine Dauerpose ist. Die Registrierung war die richtige Frage für das Interact-Problem, aber für eine Ruhepose ist sie nur die halbe Miete. Gronkh und Veri stehen jetzt auf `Sleep`; Keyleths `Rest` war schon immer korrekt (Dauerpose), was auch erklärt, warum bei ihr als einziger etwas zu sehen war.

**Noch zu klären (Saschas zweite Beobachtung):** "auch die anderen haben nichts zu tun, aber stehen nur". Der `.Resting`-Sub-State existiert nur innerhalb von MODE 4 (WORKING) und wird ausschließlich von `WorkstationFuelTickSystem.setRestingSubState` gesetzt — also nur für Buds, die an einer Workstation gebunden sind und deren Station kein Futter hat oder pausiert ist. Ein Bud ohne Workstation ist in Passive/Sitting und durchläuft diesen Zweig nie. Falls die Beobachtung Buds ohne Station betraf, ist das ein anderer Zustand und kein Fehler an dieser Stelle.

**Baumwachstum beschleunigen — technisch möglich, Mechanik verifiziert:**
- Saplings tragen dieselbe `FarmingBlock`-Komponente wie Feldfrüchte und deklarieren `ActiveGrowthModifiers: ["Fertilizer", "Water", "LightLevel"]` — Düngen und Gießen wirken also bereits nativ auf Bäume, nicht nur auf Feldfrüchte.
- `FarmingBlock` hat `growthProgress` (float) und `generation` (int), beide mit Setter im Codec.
- In `FarmingUtil` wird `growthProgress` gegen `FarmingStageData.getDuration()` geprüft (ein `Rangef`, dieselbe Spielzeit-Einheit wie die 40000–60000 pro Baum-Stage), und `generation` wird beim Stufenwechsel gelesen und hochgezählt — `generation` ist damit der **Stufenindex**.

Damit ließe sich "Stufe 2 doppelt so schnell" sauber umsetzen: beim Erreichen der Zielstufe einmalig die Hälfte der Stufendauer auf `growthProgress` addieren (statt pro Tick nachzurechnen). Offen und Saschas Entscheidung: welche Stufe genau gemeint ist (`generation` 1 oder 2) und ob der Faktor fest oder als Config kommen soll.

## Runde 5 (Claude, 2026-08-18) — Baumwachstum per Config, stufenweise

**Stufenaufbau bestätigt** (`Plant_Sapling_Oak.json`, `Farming.Stages.Default`): fünf Einträge, Index **0 bis 4**. Die Indizes 0–3 haben je `Duration {Min: 40000, Max: 60000}` (22–33 reale Minuten pro Stufe), **Index 4 hat gar keine Duration** — das ist der fertige Baum, der nicht weiterwächst. Saschas Beobachtung passt damit exakt: was er als "verharrt bei Stufe 4" sieht, ist Index 3, die letzte Stufe mit Wartezeit.

**Umsetzung:** Neuer Config-Key `treeGrowthStageSeconds` in `work/recipes.yml` — eine Map von Stufenindex auf Spielsekunden. Ein Eintrag überschreibt die Dauer dieser Stufe, eine Stufe ohne Eintrag behält die Dauer des Spiels. Leer (Default) heißt: überall Vanilla-Verhalten, das System tut nichts.

**Wie es wirkt, ohne gegen die Engine zu arbeiten:** `FarmingUtil` vergleicht `FarmingBlock.growthProgress` gegen die Stufendauer aus dem Asset und schaltet weiter, sobald der Wert erreicht ist. Das neue `TreeGrowthTickSystem` setzt deshalb nur den Fortschritt hoch: Sobald der konfigurierte (kürzere) Wert erreicht ist, wird `growthProgress` auf die Engine-Dauer der aktuellen Stufe gezogen, woraufhin die Engine beim nächsten eigenen Tick regulär weiterschaltet. Kein Blockwechsel, kein Eingriff in den Stufenwechsel selbst — und damit auch keine Gefahr, erneut in den "Store is currently processing"-Fehler zu laufen, der beim Mining durch `setBlock` im Tick entstanden war.

**Eingegrenzt auf Setzlinge:** Das System läuft über alle `FarmingBlock`-Komponenten, greift aber nur, wenn der Blocktyp mit dem Lumbering-`seedTargetPattern`-Präfix beginnt (`Plant_Sapling_`). Feldfrüchte bleiben unberührt. Es greift allerdings bei **allen** Setzlingen in geladenen Chunks, nicht nur denen im Feld einer Workstation — bewusst so gewählt, weil der Wert per Default leer ist und damit nichts passiert, bis Sascha ihn setzt.

Beispiel für "letzte Wartestufe halbieren": `treeGrowthStageSeconds: {3: 20000}` (statt 40000–60000). Für alle Stufen: `{0: 20000, 1: 20000, 2: 20000, 3: 20000}`.

Nächste Todos:
1. Fuel "Turn on"-Bug (Futter verschwindet sofort, Station geht direkt wieder aus) — vermutlich Eigenlogik des Vanilla-`ProcessingBenchBlock`, das ohne gültiges Rezept selbst abschaltet. Braucht Instrumentierung am Bench-Zustand.
2. Fuel-Slot-Animation wie bei Vanilla-Stationen.
3. Keyleth: "auf der Station sitzen" — die Pose allein reicht nicht, der Bud müsste dazu auf/vor die Station positioniert werden (siehe Runde 3). Tut bisher gar nichts weiter? Bleibt nur stehen während Rest
- Gronhk_ Passt, er schläft nun dauerhaft solange er nichts zu tun hat.
- Veri: Keine Schlafanimation bitte, soll irgendwo vor der Station liegen.