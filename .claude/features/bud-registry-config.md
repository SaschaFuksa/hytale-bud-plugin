# Bud-Registry: BudType-Enum → datengetriebene YAMLs

Quelle: `todo.md` (abgeschlossen, gelöscht 2026-08-19)

## Was gemacht wurde

`BudType`-Enum, `IBudProfile`, `IBudSound`, die drei `*Profile`/`*Sound`-Klassen und `BudProfileMapper` ersatzlos gelöscht. Ein Bud ist seitdem nur eine kleingeschriebene String-ID, aufgelöst über `BudRegistry` aus `buds/<id>.yml`. Drei Card-Interaktionsklassen auf eine generische `CardBudInteraction` mit `BudId`-Codec-Feld reduziert. `/bud create <id>` mit Tab-Completion statt fester `--veri`/`--gronkh`/`--keyleth`-Flags.

## Stolpersteine

- **Spielstand-Kompatibilität.** `PlayerBudComponent` speicherte `Set<BudType>`. Kein Migrationsskript nötig, weil `EnumCodec` intern ohnehin als String serialisiert — Codec-Key blieb `"BudTypes"`, beim Decode normalisiert `BudRegistry::normalize` die alten großgeschriebenen Werte.
- **Das Null-Safety-Aufräumen war die eigentliche Arbeit.** In den vermeintlich kosmetischen JDT-Warnungen steckten zwei echte Bugs: `WorldTracker` reichte ein potenziell `null`es `Weather.getId()` in einen `@Nonnull`-Konstruktor durch, und `BudIdArgumentType` hatte eine LSP-verletzende Parameter-Annotation im Override.
- **Der Post-Edit-Hook ist kein Ersatz für das Problems-Panel.** Der Sprachserver liefert nur Diagnostics für die editierte Region, keinen Full-File-Sweep. Zwei Stellen wurden dadurch übersehen.

## Was geholfen hat

`javap -v` gegen `reference/server`, um die *tatsächlichen* Parameter-Annotationen überschriebener SDK-Methoden zu lesen statt sie zu raten. Ein Sanity-Check mit einem bewusst kaputten Symbol, um zu prüfen, ob der Linter die Datei überhaupt neu bewertet.

## Erkenntnisse

- Null-Warnungen an der **Wurzel** fixen, nicht an jeder Call-Site.
- `javax.annotation.Nonnull` ist nicht `TYPE_USE` — bei Records propagiert die Annotation aus dem Header nicht in den Compact Constructor.
- Sauberer Schnitt schlägt Adapter-Interface: die Interfaces wurden gelöscht statt als Kompatibilitätsschicht stehenzubleiben.
