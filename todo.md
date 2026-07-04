JobState-System:
- Ist das aktuelle Queue System ausreichend?
- Benötigen wir ein JobState-System, PRO Spieler?
- Hier wäre die Idee, dass je nach Prio Chat Message Anfragen abarbeitet werden
- Auch mit Aufräummechanismus, eine Anfrage die länger als eine Minute nicht beantwortet wurde, wird verworfen

Prorisierungssystem:
- Müssen wir hier was ändern?
- Eines ist wichtig: Direkte Spieleranfragen sollten immer höchste Prio haben

Memory-System:
- Passt das so aktuell?
- Wir sollten bei direkte Dialogen (Wie Spieler fragt NPC) die Memorys auf jeden Fall mit in den Prompt packen, ist das aktuell schon so?

Spieler/NPC Dialoge:
- Sollen ja irgendwie auch möglich sein, d.h. NPCs reagieren untereinander und auf dem Spieler
- Es sollte aber nie der selbe NPC auf seine letzte Nachricht reagieren... 
- D.h. in der Memory/History muss auch angegeben werden, wer die letzte Nachricht geschrieben hat, damit der NPC nicht auf sich selbst reagiert
- Was haben wir davon bereits?

Auch wichtig:
Häufigkeit naufeinander folgender Chat Messages redzuieren, der Spieler muss auch noch in Ruhe lesen können was rein kommt - reicht es hier aus einfach ein Config Parameter zu erhöhen?

Wichtigstes feature für 1.8.0 ist ja die Memory/History Funktion. Das muss mega gut funktionieren!
