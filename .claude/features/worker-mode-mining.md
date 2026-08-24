# Worker Mode 3/3 — Mining (Veri)

Quelle: `TODO-mining-mode.md` (gelöscht 2026-08-19, mit offenen Punkten — siehe unten). Architektur-Hintergrund: `docs/bud-worker-mode-plan.md`.

## Was gemacht wurde

Dritte Rolle, bewusst **ohne** das Vanilla-Wachstumssystem: eigener Timer über die Chunk-Store-Komponente `OreGrowthBlock` (`growthStage` + `nextGrowthAt: Instant`). Zwei Mechaniken — Hauptknoten (Kreuz aus 5 Blöcken, reift zu Erz, verbraucht ein Erz aus dem Input-Slot) und Zufallslöcher (liefern nur Stein, kürzere Reifezeit). Werkzeuge fix: Schaufel zum Buddeln, Spitzhacke zum Abbauen.

Nebenbei generalisiert, weil die dritte Rolle es erzwang: `BlockEntityPositions`, `WorldBlockEntities`, `GameClock`, `BlockDrops`.

## Stolpersteine

- **Serverabsturz.** `world.setBlock` direkt aus `OreGrowthTickSystem.tick` — ein Blockwechsel entfernt intern die Block-Entity, also eine schreibende Store-Operation während der ChunkStore tickt (`Store is currently processing!`). Fix: `world.execute(...)`, per Bytecode bestätigt, dass die Task-Queue außerhalb von `Store.tick` gedrained wird.
- **Wachstum 30× zu schnell.** `GameClock.now` liefert Spielzeit, nicht Echtzeit. Faktor exakt bestimmt: `SECONDS_PER_DAY 86400 / Tageslänge 2880 = 30`.
- **Veri grub das komplette Feld um** — der Dig-Kandidat kam aus dem deterministischen Serpentinen-Scan ohne Obergrenze.
- **`Workstation_Mining.json` hatte gar keine `BlockEntity.Components`-Sektion.** Es wurde nur geprüft, *dass* die Datei existiert, nicht ob ihr Inhalt dem produktiven Aufbau entspricht.
- **Hauptknoten liefen nie an.** Zwei aufeinanderfolgende Fehldiagnosen: erst wurde `Ore_*_Stone` für nicht existent erklärt (reine Dateinamen-Suche), dann als Überkorrektur die BlockTypeList als Existenznachweis genommen. Die echte Ursache war beides Mal nicht das: die Validierung lief in `setup()`, drei Sekunden bevor die Engine die BlockTypes lädt.

## Was geholfen hat

Persistenz vorab an einem nativen Vorbild verifizieren statt zu hoffen: `TilledSoilBlock` persistiert bereits `Instant`-Felder über denselben Chunk-Mechanismus, `Codec.INSTANT` existiert — damit war `OreGrowthBlock` kein Risiko mehr.

Wachstumsdauern aus dem Vanilla-System übernommen statt geschätzt (Crop-Stage 28800–30600 Spielsekunden, mit Min/Max-Streuung, damit nicht alle Grabstellen im Gleichschritt reifen).

## Erkenntnisse

- **Bei Block-Ids ist nur die Laufzeit verlässlich.** Weder Dateinamen noch BlockTypeList-Einträge beweisen, dass ein BlockType existiert. Auflösung deshalb lazy, mit geordneter Wirtsgesteins-Suffixliste.
- **Chunk-Store-Komponenten hängen nur an Blockpositionen, deren `BlockType` sie in `BlockEntity.Components` deklariert.** Nicht an beliebigen Luftpositionen — dafür brauchte es zwei eigene Blocktypen (`Mining_Growth_Hole`, `Mining_Growth_Ready`).
- **Obergrenzen aus vorhandener Config ableiten** statt neue Werte einzuführen: max. Grabstellen `= FieldRadius`, skaliert automatisch mit dem Feld. Dabei fiel auf, dass `OreMinDistance` bis dahin tot im `WorkConfig` lag und nirgends gelesen wurde.

## Offen geblieben

- Tiefen-Erweiterung (max. −1 Y) der Pyramide nie umgesetzt.
- Serverneustart mitten im Wachstumszyklus nie am echten Server verifiziert — das war das Kernversprechen des Konzepts.
- `Workstation_Mining.json` kosmetisch unvollständig (Slot-Icons, Sounds).
- YAML-Key `allowedSeeds` trägt für Mining Erze — Name passt nicht mehr.
