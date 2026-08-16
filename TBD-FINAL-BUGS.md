Problem: Beim öffnen der Farming oder Lumbering Station, ist ein komisches Verhalten:
In den ersten Sekunden nach dem öffnen, werden Items aus meinem Invetar automatisch verschoben, als wäre ein Key gedrückt. Ich habe keine Tasten gedrückt, und es passiert auch nur in den ersten Sekunden nach dem öffnen der Station. Danach ist alles normal.

Problem: Initaler "Turn on"-Bug (FUEL):
GGF Zusammenhang mit Problem darüber: Wenn man die Station öffnet, und dann das erste Mal Futter drin hat und bei Fuel auf "Turn on" klickt, verschwindet sofort das Futter, und die Station ist aber direkt wieder aus mit akustischem Signal.

Problem: Zahlenwerte für Field Radius:
Zurzeit erlauben wir Zahlenwerte, das kann aber ungünstig sein. Wir sollten Größen erlauben: SMALL, MEDIUM, LARGE
Farming Station: SMALL = 4, MEDIUM = 5, LARGE = 6
Lumbering Station: SMALL = 5, MEDIUM = 7, LARGE = 9 (4 Bäume, 8 Bäume, 12 Bäume)

Problem: Animationen:
Keyleth: Das aktuelle REST macht kein sitzen. SIT lässt hingegen Keyleth in den Boden versinken, da sie sogesehen dann auf ezwas sitzt.
Beides ungünstig.
Gronkh: Aktuell ohne Arbeit sollte er liegen, aber er steht nur herum.

Problem: Direkte LLM-Interaktion mit F:
Aktuell kann man direkt mit F eine LLM Reaktion vom Bud erhalten. Das Problem: Diese kommt nicht sofort sondern versetzt. Für eien direkte Interkation ungünstig, sollte wie ein State Change z.B. sofort die LLM Reaktion erfolgen.

Nice to have: Anvisieren bei F-Interaktion:
Wäre cool wenn der Bud seinen Kopf zum Spieler dreht wenn man mit F-Interaktion den Bud anspricht. Aktuell schaut er immer in die Richtung in die er gerade schaut, egal ob der Spieler links oder rechts steht.

Problem: Missing animation 'Interact' (WARN, kosmetisch, bekannte Lücke)
Gronkh_Bud.json hat kein AnimationSets.Interact (erbt nur von Trork), Veri_Bud.json hat dieselbe Lücke — nur Keyleth_Bud.json definiert explizit einen Interact-Clip. Keine Lumbering-Regression, sondern eine bereits vorher bestehende, kosmetische Lücke, die laut TODO ohnehin erst in Phase "Kosmetische Politur" angegangen wird. Kein Handlungsbedarf jetzt.

Problem: Store is currently processing! (SEVERE, unser Code, real)
findNextWorkAssignment ruft am Anfang unbedingt currentGameTime(world) auf (Zeile 277), das intern world.getEntityStore().getStore().getResource(...) macht — ein EntityStore-Store-Zugriff aus dem ChunkStore-Tick von WorkstationFuelTickSystem heraus. Wenn der EntityStore gerade selbst tickt, wirft das die IllegalStateException. Passiert bei jedem Tick jeder gebundenen Workstation (für den WATER_REFRESH-Check), nicht nur bei Lumbering — vorbestehender Bug aus der Farming-Runde, keine Regression durch die 8-Punkte-Änderung. Der catch (RuntimeException) fängt es ab, aber die gesamte Prioritätskette (Till/Plant/Water/Fertilize/Harvest) wird für diesen Tick übersprungen, nicht nur der Water-Refresh-Teil — bei häufigem Auftreten könnte das Gronkh sichtbar stocken lassen. Kein Blocker fürs Wachstumstesten, aber ein echter Fix wäre sinnvoll (z. B. now nur lazy holen, wenn waterRefreshWinner-Zweig tatsächlich gebraucht wird, oder Game-Time anders cachen statt live pro Tick aus dem EntityStore zu ziehen).

Problem: Keine Fuel SLot Animation wie bei den anderen Hytale Stations:
Ohne diese ANimation sieht man nicht, dass es aktiv läuft + es zeigt auch in etwa die Dauer an, wie lange es noch läuft.

Problem: Feldradius ist nicht ebenerdig und Bud hat Probleme dadurch:
Ich habe es nun auch mal auf eienm Feld probiert, wo ich Blockhöhen drin habe (also -1, 0, +1) und da hat der Bud Probleme, die Felder richtig zu bearbeiten.
Gedanke: Wenn wir merken, dass die Aktion nicht ausgeführt werden konnte, den Bud direkt auf den Block schicken?

Code:
Wir haben Lumbering State in FarmWorkAction.java, das ist bad Code seperation. Wir sollten das in LumberingWorkAction.java auslagern.
Generell: Speziale Aktionen wie Lumbering oder Farming sollten in eigenen Klassen sein, die von einer gemeinsamen WorkAction-Klasse erben.
Gemeinsame Aktionen wie Till, Plant, Water, Fertilize, Harvest sollten in der gemeinsamen WorkAction-Klasse sein

Configs:
Wir haben in der WorkConfig zu viele spezifische Configs, die nur für Lumbering oder Farming gelten. Wir sollten diese ggf. in eigene Configs auslagern, die von einer gemeinsamen WorkConfig-Klasse erben.

Cards/Items Verschwunden nach Server neustart:
Problem: Ich habe aus der Station die Karte bzw. Items rausgenommen, und nach einem Serverneustart waren diese aus meinem Inventar verschwunden. Ich habe die Station nicht verlassen, sondern nur den Server neugestartet. Die Items waren weg. Es ist immer wieder passiert.