# Worker Mode 1/3 — Farming (Keyleth)

Quelle: `TODO-worker-mode.md` (abgeschlossen, gelöscht 2026-08-19). Architektur-Hintergrund: `docs/bud-worker-mode-plan.md`.

## Was gemacht wurde

Der erste Arbeitsmodus überhaupt und damit das Fundament für Lumbering und Mining. Workstation als Processing-Bench mit Karten-/Saatgut-/Futter-Slots, Bindung eines Buds an die Station, Fuel-Timer, `Working`-State mit `.Default`/`.Resting`-Sub-States, und der Kern-Loop Tilen → Pflanzen → Gießen → Düngen → Ernten. Dazu LLM-Reaktionen auf Arbeitsereignisse.

## Stolpersteine

- **Kurskorrektur mitten in Phase 4:** die Workstation wurde erst als eigener Blocktyp gebaut und dann auf das native Bench-Processing-Modell umgebaut. Der eigene Fuel-Tick blieb, weil der native Bench-Verbrauch sich nicht ansteuern ließ — das wurde zurückgemeldet statt improvisiert.
- **Drei Regressionen bei der Slot-Filterung**, davon eine, die den Fehlerort komplett verschob (siehe Meta-Lektion).
- **World-Thread-Crash:** schreibende Operationen aus einem tickenden Store heraus.
- Kosmetische Nebenschritte konnten die Kernfunktion mitreißen — `tryEquipToolFor` läuft seitdem **nach** der eigentlichen Arbeit und in `try`/`catch`.

## Was geholfen hat

Ingame-Tests **pro Phase**, nicht am Ende. Phase 2 und 4 wurden bewusst vor Phase 5 bestätigt — sonst testet man Feldarbeit auf einem kaputten State-Lock.

## Erkenntnisse

**Meta-Lektion (die wichtigste des ganzen Projekts):** Sobald die Frage "wie reagiert der Client auf diese Konfiguration" lautet statt "wie funktioniert diese Server-API", ist `javap` gegen `reference/server` prinzipiell blind. Regression 2 war ein falsch angenommener Java-Hook — dort war Bytecode-Analyse richtig. Regression 3 wurde derselben Klasse zugeordnet, lag aber in der client-seitigen UI-Darstellung einer untypischen `Capacity`-Größe. Kein noch so gründlicher Bytecode-Sweep hätte das gefunden.

Drei tragende Regeln, die für alle drei Rollen gelten:

- **a)** Mehrschrittige Abläufe gehören in **eine** Java-Action, nicht in mehrere JSON-Actions oder `ActionTimeout`. JSON bleibt dünne Anbindung.
- **b)** Zielwahl und Tempo gehören in die Station (`WorkstationFuelTickSystem`), nicht in Sensoren — native Sensoren cachen und kennen keinen Anker.
- **c)** Animationen kommen aus `AnimationSets` im Modell-Asset (`Server/Models/*.json`, Parent-gemerged), nicht aus dem Mesh.

Faustregel für Rollen-JSONs: gemeinsames Bud-Verhalten → alle drei Dateien; rollenspezifisches Arbeitsverhalten → nur die Datei des zuständigen Buds. Die Till-Instructions landeten anfangs versehentlich in allen drei.

Recherche-Ergebnis zur Rollen-Vererbung (975 Dateien gescannt, nicht Stichproben): `Variant`+`Reference`+`Modify` überschreibt **ausschließlich Parameterwerte** — 0 von 465 Variants kombinieren `Reference` mit eigenen `Instructions`. Der passende Mechanismus für geteiltes Verhalten wäre `"Type": "Component"`. Empfohlen, bewusst nicht umgesetzt.
