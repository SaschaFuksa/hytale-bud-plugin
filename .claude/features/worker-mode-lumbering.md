# Worker Mode 2/3 — Lumbering (Gronkh)

Quelle: `TODO-lumbering-mode.md` (abgeschlossen, gelöscht 2026-08-19). Architektur-Hintergrund: `docs/bud-worker-mode-plan.md`.

## Was gemacht wurde

Zweite Rolle auf Farmings Fundament. Baumplätze auf festen Kompasspunkten statt freier Feldsuche, `WorkType.FELL` mit BFS über den zusammenhängenden Holzkörper, Drops direkt in den Output-Container statt Bodenloot, Loch-Auffüllung nach dem Fällen.

Der strukturell wichtigste Teil: **die Trennung von Farming und Lumbering.** `FarmWorkAction` wurde in eine gemeinsame Basis `AbstractWorkAction` (Tilen/Pflanzen/Gießen/Düngen für alle Rollen) plus rollenspezifische Unterklassen mit `extra*`-Hooks aufgeteilt. `FarmingRecipeConfig` → `WorkRecipeConfig`, `work/farming.yml` → `work/recipes.yml`.

## Stolpersteine

- **Gronkh bewegte sich gar nicht.** `Template_Gronkh_Bud.json` hatte im `.Default`-Sub-State nur einen Platzhalter aus einer früheren Phase, ohne Sensor/Action-Verdrahtung.
- **Die `y-1`-Heuristik für den Stammfuß griff nie**, weil die Wurzelstruktur unter dem Stamm ebenfalls `Wood_`-präfigiert ist — `fellWinner` blieb praktisch immer `null`.
- **Der Scheduler-Fix erzeugte den nächsten Bug.** Nach dem Gating der Farming-Kaskade auf Rollen machte Gronkh nach dem Fällen aller Bäume gar nichts mehr — Tilen/Pflanzen/Gießen/Düngen waren für Lumbering komplett abgeklemmt, obwohl er das laut Kern-Loop können soll.
- **Der Dirt-Fill brauchte drei Anläufe** und war danach immer noch eine Y-Stufe zu hoch: `anchorY` ist die Y-Position des Bench-Blocks selbst, nicht des Bodens davor.
- **Gronkh fällte zu junge Bäume.** Die Phase-0-Annahme "Stage_00–2 haben keine `Wood_`-Blöcke" war schlicht falsch — sie haben `Wood_*_Branch_Long`, nur keinen Trunk.

## Was geholfen hat

Gezieltes Logging, das nach dem Fund wieder auf `FINE` zurückgestuft wurde statt dauerhaft auf `INFO` zu bleiben. Und die Prefab-Rohdaten unter `reference/assets/Server/Prefabs/Trees/` direkt auszuwerten, statt über Baumstufen zu spekulieren.

## Erkenntnisse

- **Block vorhanden ≠ Kandidat.** `hasTrunkBlock` verlangt einen echten `_Trunk` in der BFS-Struktur. Dasselbe Prinzip trägt später Minings `OreGrowthBlock`-Komponente.
- **Operatives Gotcha, gilt für jede `recipes.yml`-Änderung:** `copyPackagedDefault` kopiert nur, wenn die Zieldatei **nicht** existiert. Eine vorhandene `run/`-Kopie wird nie aktualisiert — bei geänderten Defaults muss die Runtime-Datei gelöscht oder von Hand angepasst werden. Das hat mehrfach Zeit gekostet.
- Kleine, konkrete Mechaniken (Config-Maps, Blocklisten, Auflösungs-Pattern) sofort generalisieren, sobald ein zweiter Rollen-Fall existiert. Die große strukturelle Abstraktion (`WorkTier` o. ä.) bewusst zurückgestellt, bis ein ehrlicher Vergleichsfall existiert.
- Ein Fix, der eine Kaskade gatet, muss geprüft werden auf das, was er **abklemmt** — nicht nur auf das, was er repariert.
