# Regression Test Checklist (Plugin)

Diese Checkliste ist für manuelle Regressionstests vor Release/Deployment.

## Testlauf-Metadaten

- Plugin-Version:
- Server-Version:
- Ergebnis gesamt: ☐ Pass ☐ Fail

---

## 0) Setup (pro Testlauf)

- [ ] Server frisch gestartet
- [ ] Definierte Testwelt geladen
- [ ] Debug-Logging aktiv
- [ ] Modus A vorbereitet: LLM aktiv (gültiger Key/Endpoint)
- [ ] Modus B vorbereitet: LLM aus / ungültig / Timeout (Fallback erzwingen)

---

## 1) Debug

### Creation
- [ ] `all`
- [ ] `veri`
- [ ] `gronkh`
- [ ] `keyleth`

### Deletion
- [ ] `all`
- [ ] `veri`
- [ ] `gronkh`
- [ ] `keyleth`

### Sonstiges
- [ ] `reset`
- [ ] `state change`
- [ ] `debug x` (invalid command): sauberer Fehler, kein Crash

---

## 2) Reactions

### Block
- [ ] `break`
- [ ] `place`

### Discover
- [ ] Trigger erkannt und Reaktion korrekt

### World
- [ ] World-Event erkannt und Reaktion korrekt

### Weather
- [ ] Wetterwechsel erkannt und Reaktion korrekt

### Item
- [ ] `ore`
- [ ] `plant`

### Crafting
- [ ] `use`
- [ ] `craft`

### Mood
- [ ] `creation`

### Teleport
- [ ] Bud-Verhalten konsistent (kein Desync)

---

## 3) Chat

- [ ] `veri`
- [ ] `gronkh`
- [ ] `keyleth`
- [ ] `gronkh, keyleth`
- [ ] `random`

---

## 4) LLM + Fallback Matrix (Pflicht)

> Die folgenden Punkte für zentrale Flows prüfen (Debug, Reactions, Chat).

### A) LLM aktiv
- [ ] Antwort kommt rechtzeitig
- [ ] Antwort ist inhaltlich passend
- [ ] Keine Fehler im Log

### B) LLM nicht verfügbar (Fallback)
- [ ] Timeout → Fallback greift
- [ ] 4xx/5xx → Fallback greift
- [ ] Invalid/leer Antwortpayload → Fallback greift
- [ ] Kein Crash, Spielfluss bleibt stabil

### C) Robustheit
- [ ] Rate-Limit-Szenario abgefangen (Retry/Backoff oder Fallback)
- [ ] Fehler sind nachvollziehbar geloggt
- [ ] Verhalten ist deterministisch genug für reproduzierbare Tests

---

## 5) Abschlusskriterien

- [ ] Keine Crashes/Hard Errors während des gesamten Laufs
- [ ] Zustände aller Buds bleiben konsistent
- [ ] Pflichtfälle in Modus A und Modus B bestanden
- [ ] Findings dokumentiert

---

## Findings / Notizen

- 
- 
- 

## Follow-ups

- [ ] Bug-Ticket erstellt
- [ ] Re-Test geplant
- [ ] Changelog aktualisiert
