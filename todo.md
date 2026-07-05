Bud Spawn und Despawn durch eigene Medails:

Gerade habe ich eine BudMedailInteraction, da kommt sowas im Log:

[2026/07/05 14:00:18   INFO]           [HytaleGenerator] [BUD] BudMedailInteraction tick0 called with firstRun: true, time: 0.0, type: Primary
[2026/07/05 14:00:18   INFO]           [HytaleGenerator] [BUD] Owning entity: TaschFogster
[2026/07/05 14:01:03   INFO]           [HytaleGenerator] [BUD] BudMedailInteraction tick0 called with firstRun: true, time: 0.0, type: Secondary
[2026/07/05 14:01:03   INFO]           [HytaleGenerator] [BUD] Owning entity: TaschFogster

Ich möchte, dass die Buds nur spawnen, wenn die MedailInteraction Primary ist. Also nur beim ersten Klick. Beim zweiten Klick (Secondary) soll der Bud wieder despawnen.

Wichtig:
Evtl. brauchen wir pro Bud eine eigene MedailInteraction, da ich für jeden Bud eine eigene Medaile machen will.
Also Veri, Gronkh und Keyleth sollen ihre eigene Item MedailInteraction haben, die dann jeweils den eigenen Bud spawnen/despawnen.

Aufgaben:
Medaille -> Card, ich möchte es nun lieber Card nennen als Medaille
- Lege auch für Veri und Gronkh eigenes Card Item an, die dann jeweils den eigenen Bud spawnen/despawnen. Bilder sind schon da (card_veri.png, card_gronkh.png, card_keyleth.png), Model ist bei allen aktuell das selbe
- Lege auch die Interaktionen für Veri und Gronkh an, die dann jeweils den eigenen Bud spawnen/despawnen. Die Interaktionen sollen auch nur beim Primary Klick den Bud spawnen und beim Secondary Klick den Bud despawnen.
- Registriere im Plugin die Interaktionen für Veri und Gronkh, wie es schon bei Keyleth gemacht wurde.