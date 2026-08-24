# Baumwachstum, Wurzelraum, Ruheposition, Workstation-Hitbox

Session 2026-08-19. Steht in keinem TODO — hier festgehalten, weil die Engine-Details sonst neu recherchiert werden müssten.

## Was gemacht wurde

- `treeGrowthStageSeconds` funktioniert (patcht `FarmingStageData.getDuration()` über die **gesamte** `BlockTypeAssetMap`), Config akzeptiert jetzt `default` für alle Stufen.
- Neuer Arbeitsschritt `WorkType.PREPARE_SOIL`: Gronkh gräbt vor dem Pflügen mit der Schaufel Wurzelraum frei (`TreeRootDepth`/`TreeRootRadius`).
- Keyleth sitzt im Ruhezustand **auf** der Farm-Station statt daneben.
- Eigene Workstation-Hitbox `Workstation_Bench` (2×1×1).

## Die Kette der Fehldiagnosen (lehrreicher als das Ergebnis)

Bäume blieben nach der zweiten Stufe stehen. Vier Theorien wurden aufgestellt und **durch Daten widerlegt**, bevor die richtige kam:

1. *Kronen der Nachbarbäume blockieren* — widerlegt: auch isolierte Bäume betroffen.
2. *Entity-Löschung räumt das Prefab ab* — widerlegt: `OnFarmBlockAdded.onEntityRemove` ist ein reines `return`.
3. *Rotationsdrift zwischen Stufen* — widerlegt: Rotation ist `HashUtil.randomInt(x,y,z,…)`, also über alle Stufen identisch.
4. *Der Baum blockiert sich mit eigenen Resten* — plausibel gerechnet (53–97 % Varianten-Konflikte), aber nicht die Ursache.

**Die Ursache:** ab Stage_1 platzieren die Prefabs `Wood_*_Trunk` auf y −1 bis −3 — **Wurzeln unter dem Sapling**. Der Hindernis-Check erlaubt nur `Empty` oder `ReplaceMaskTags: ["Soil"]`. Liegt darunter Stein, wird die Platzierung **still** verworfen: der Wachstumszähler läuft weiter, und wenn er die letzte Stufe erreicht (die hat keine `Duration` → das ist das Fertig-Signal), wird die Block-Entity gelöscht. Der Baum ist dann dauerhaft eingefroren. Kein Log, kein Rückgabewert.

Was die Diagnose gebracht hat: ein temporäres Logging-System, das pro Sapling `stage`, `progress`, `blockAtPos` und `sinceLastFarmTick` protokollierte. Entscheidend war `blockAtPos` — alle 11 Varianten von Stage_1/2/3 haben `Wood_Oak_Trunk` an der Baumposition, der Weltblock blieb aber `Wood_Oak_Branch_Long` (Origin-Block von Stage_00/Stage_0). Damit war bewiesen, dass die Prefabs nie geschrieben wurden.

## Engine-Fakten (verifiziert, nicht angenommen)

- **Spielzeit läuft 30× schneller als Echtzeit.** `86400 / (DaytimeDurationSeconds 1728 + NighttimeDurationSeconds 1152)`.
- **Wachstums-Modifier multiplizieren:** Dünger 2 × Wasser 2,5 × Licht 2 = **10×** schneller. Sie verlangsamen nie.
- **Die letzte Stufe jedes Saplings hat keine `Duration`** — das ist das Fertig-Signal, nicht ein vergessener Wert.
- **Wurzeltiefen:** Oak/Poisoned 3, Redwood 1 (Radius 2), Cedar-burnt 4, Fir 6 (Radius 4), Fir-snow 8, Wisteria 9 (Radius 10). Die übrigen 71 Baumordner haben gar keine Wurzeln.
- **Hitbox-Koordinaten:** Blocklokal `[0,1]³`, negative X reichen in den −x-Nachbarn. Modellumrechnung: `blocklokal = 0.5 + modelX/32` (32 Modelleinheiten = 1 Block), verifiziert am Vanilla-Furnace.
- **`BlockType` hat zwei Hitboxen:** `HitboxType` (Kollision) und `InteractionHitboxType`. Fehlt die zweite, fällt die F-Interaktion auf die erste zurück — deshalb schrumpfte die Interaktionsfläche mit, als die Kollisionshitbox entfernt wurde.
- **`Material` defaultet auf `Empty`** und wird über Templates aufgelöst — aus dem rohen JSON nicht ablesbar. Zur Laufzeit ist `getMaterial() != Empty` die verlässliche "fester Block"-Prüfung und deckt sich mit der engine-eigenen `canReplace`-Regel.

## Erkenntnisse

- **`treeEdgePositions` liefert pro Baumstelle eine ganze Säule** (`anchorY ± FieldMaxHeight`), nicht eine Position. Jede neue Kandidatenprüfung braucht deshalb einen eigenen Anker auf der Bodenoberfläche — `isTillCandidate` hat den implizit, weil es einen bestimmten Blocktyp verlangt. Ohne Anker feuerte `PREPARE_SOIL` auch auf Sapling-Höhe und machte den frisch gepflanzten Sapling zu Dirt.
- **Wachsende Bäume nie umgraben:** ihre Wurzeln sind `Wood_`-Blöcke im selben Volumen. Werden die ersetzt, scheitert `isPrefabIntact` und der Baum friert genauso ein wie vorher.
- **Debug-Logging nicht hinter ein Config-Flag hängen**, wenn das Flag beim Neuaufsetzen des Plugins auf Default zurückfällt — eine ganze Testrunde ging dadurch verloren.
- Volatile Felder (Zeitstempel) gehören **nicht** in den Änderungsvergleich eines Loggers, sonst schreibt er pro Tick.
- Bei wiederholten Fehldiagnosen: aufhören zu theoretisieren und ein Messinstrument bauen. Die vierte Theorie war rechnerisch überzeugend und trotzdem falsch.
