# SkyKings – Next Runtime Checklist

Stand: 2026-09-02

Diese Liste ist das finale Ingame-/Windows-Gate nach dem automatisierten Build. Sie wird erst auf einem echten Paper/Spigot-1.8.9-Testserver mit mindestens zwei Clients vollständig abgehakt.

## 0. Deploy / Start

- [ ] Neueste `main`-Artefakte deployen.
- [ ] Server komplett stoppen und neu starten; kein `/reload`.
- [ ] Konsole auf `SEVERE`, Stacktraces, fehlende Commands und Service-Fehler prüfen.
- [ ] `/skcheck` ausführen und alle Pflichtsysteme auf `[OK]` prüfen.
- [ ] `SkyPvP`, `SkyIslands`, `SkyPlots`, `SkyCommunityEvent` geladen.

## 1. Universelle Item-Stackability

Für jede normale, handelbare Belohnung denselben Gegenstand **separat** 2–5 Mal erzeugen/erhalten.

- [ ] Identischer Typ/Wert/Tier stackt automatisch zusammen.
- [ ] Unterschiedlicher Typ/Wert/Tier stackt nicht fälschlich zusammen.
- [ ] Kein sichtbarer Serial-/UUID-/Debug-Text in Name oder Lore.
- [ ] Friday Blade / Armor / Bow aus getrennten Drops besitzen identische Meta, sofern Material/Enchants identisch sind.
- [ ] Normale Vanilla-Rewards bleiben vanilla-stackbar.
- [ ] Zwei separat gekaufte PlayerShop-Haendler-Eier stacken zusammen.
- [ ] Voll belegtes Inventar + vorhandener nicht voller Haendler-Ei-Stack: `/playershop kaufen` darf das neue Ei in diesen Stack legen.
- [ ] Altes Haendler-Ei mit Legacy-UUID-Marker bleibt weiterhin als gueltiges Shop-Ei verwendbar.

## 2. Crates

- [ ] Gleiche Crate mehrfach separat ausgeben: ein gemeinsamer Stack entsteht.
- [ ] Verschiedene Crate-Tiers bleiben getrennt.
- [ ] Rechtsklick auf einen Stack verbraucht exakt **eine** Crate.
- [ ] Reward wird exakt einmal vergeben.
- [ ] Shift + Rechtsklick öffnet für Exile+ den vorgesehenen Stack/alle zulässigen Crates.
- [ ] Spieler unter Exile erhalten keinen unerlaubten Open-All-Pfad.
- [ ] Rapid-Clicks erzeugen keine zusätzlichen Rewards.
- [ ] Legacy-Crates aus altem Bestand bleiben lesbar/einlösbar.

## 3. Gutscheine / Voucher

Neue v2-Gutscheine: gleicher Typ + gleiches Ziel = identische Meta und damit stackbar. Die maximale Claim-Anzahl kommt aus der serverseitigen Issued-Registry.

- [ ] Zwei identische Gutscheine separat erzeugen: sie stacken.
- [ ] Unterschiedliche Voucher-Typen/Ziele stacken nicht.
- [ ] Ein Rechtsklick verbraucht exakt ein Exemplar.
- [ ] Zwei legitim ausgegebene Exemplare können insgesamt exakt zweimal eingelöst werden.
- [ ] Kopierter/duplizierter Stack kann die serverseitig registrierte Claim-Grenze **nicht** erhöhen.
- [ ] 20–30 schnelle Rechtsklicks auf den letzten Claim ergeben maximal einen Reward.
- [ ] Server direkt nach erfolgreicher Einlösung neu starten: derselbe verbrauchte Claim bleibt gesperrt.
- [ ] Rang-, Rankup-, Kit-, Rechte-, Prefix-, Coin- und GiveAll-Gutscheine einzeln prüfen.
- [ ] Confirm-GUI bei Rang/Rechte: Ablehnen verbraucht nichts; Annehmen verbraucht exakt ein Exemplar.
- [ ] Inventar-voll-Fall beim Kit: kein Claim-/Itemverlust durch fehlenden Platz.
- [ ] `issued-items.txt` und `redeemed-vouchers.txt` nach Restart weiterhin konsistent.
- [ ] **Stats-Reset-Voucher: N/A** – dieser Voucher-Typ existiert im aktuellen Codebestand nicht.

## 4. Island Starterloot Anti-Farm

- [ ] Spieler ohne bisherige Insel: `/is create` → Startertruhe enthält einmalig Starterressourcen.
- [ ] Beim ersten Create erscheint **nicht** faelschlich der Hinweis, dass Starterressourcen bereits verbraucht wurden.
- [ ] `/is delete` vollständig bestätigen.
- [ ] `/is create` erneut → Insel wird erstellt, Startertruhe enthält **keine** neuen Starterressourcen.
- [ ] Beim zweiten Create erscheint der Hinweis, dass Starterressourcen nur einmal ausgegeben werden.
- [ ] Server neu starten und erneut delete/create → weiterhin kein Starterloot.
- [ ] Dasselbe über das `/is` GUI testen, nicht nur per Command.
- [ ] Bestandsinsel aus älterem Stand löschen → Spieler wird vor dem Löschen als bereits versorgt persistiert.
- [ ] `plugins/SkyKings-Core/island-starter-claims.txt` bleibt über Neustarts erhalten.
- [ ] Admin-Recovery: Insel vollständig löschen → `/is resetstarter <Spieler>` → `/is create` gibt wieder genau **einmal** Starterloot aus.
- [ ] `/is resetstarter <Spieler>` wird verweigert, solange der Zielspieler noch eine Insel besitzt.
- [ ] Nach dem Recovery-Create erneut delete/create ohne weiteren Reset → Startertruhe bleibt wieder leer.

## 5. Giveaway / Verlosung

- [ ] `/verlosen` mit Item in der Hand: globale 3–2–1-Ziehung läuft.
- [ ] Gewinner erhält exakt den vorgesehenen Stack.
- [ ] Gewinner-/Jackpot-Sound ist hörbar.
- [ ] Inventar voll: Gewinn wird sicher gedroppt statt verloren.
- [ ] Jeder Gewinner erzeugt `GIVEAWAY_WIN` im zentralen Audit-Log; Actor ist der auslösende Admin, nicht eine UUID im Amount-Feld.
- [ ] Keine zweite Verlosung kann parallel gestartet werden.

## 6. Freitags-Event

Kompletten Flow **natürlich** durchlaufen lassen; nicht per Stop abkürzen.

- [ ] `/freitag` startet Auto-Phase.
- [ ] 10 automatische Ziehungen laufen; Rewards werden exakt einmal vergeben.
- [ ] Auto-Gewinner werden mit `GIVEAWAY_WIN` auditiert.
- [ ] Wechsel in Staff-Verlosungen erfolgt.
- [ ] Manuelle `/verlosen`-Gewinne: 3–2–1, Reward, Audit.
- [ ] `/verlosen fertig` startet Drop-Countdown.
- [ ] Drop-Event erzeugt vorgesehene Loot-Menge ohne Serial-Müll.
- [ ] **Natürlicher Abschluss:** Abschlussmeldung + finale Firework-Serie erscheint nach Ende des Drop-Events.
- [ ] `/freitag stop` während einer Phase beendet Tasks sauber; alte Tasks feuern nach Neustart des Events nicht weiter.

## 7. Casino

- [ ] `/casino` öffnet die Void-Crown-Oberfläche.
- [ ] Gültiger Einsatz wird exakt einmal abgebucht.
- [ ] Gewinnfall zahlt exakt den vorgesehenen Payout.
- [ ] Verlustfall erzeugt keine Rückzahlung.
- [ ] Zu wenig Coins / ungültiger Einsatz: keine Abbuchung.
- [ ] Schnelle Mehrfachklicks umgehen Rate-Limit/Balance-Prüfung nicht.
- [ ] Audit-/Ledger-Einträge stimmen mit Kontobewegungen überein.

## 8. Jackpot

- [ ] `/jackpot` öffnet GUI; Quick Entries funktionieren.
- [ ] Einsatz wird erst nach erfolgreicher Validierung abgebucht.
- [ ] Persistenzfehler-Simulation, falls Testumgebung möglich: Einsatz wird erstattet.
- [ ] Mindestens zwei Spieler einzahlen; Chance/Pot stimmen proportional.
- [ ] Ziehung zahlt 95 % des Pots an genau einen Gewinner, 5 % Sink.
- [ ] Gewinner hört `LEVEL_UP`.
- [ ] Restart mit normalem Round-State: Entries bleiben erhalten.
- [ ] Ein bewusst erzeugter `PENDING`-Settlement-State wird nach Restart **nicht** automatisch doppelt ausgezahlt; `/skcheck` zeigt `REVIEW_REQUIRED`.

## 9. PlayerShop

Mit Käufer + Verkäufer testen. Käufer benutzt **das echte Vanilla-Villager-Handelsfenster**; die 3×9-Oberfläche ist nur der Besitzer-Editor.

### Besitzer / Einrichtung

- [ ] `/playershop kaufen` → Haendler-Ei erhalten und auf eigener Insel/eigenem Plot platzieren.
- [ ] Shift + Rechtsklick auf eigenen Villager öffnet Besitzer-Menü.
- [ ] `Angebote bearbeiten` öffnet den 3×9-Editor.
- [ ] Bis zu 9 Angebots-Spalten gleichzeitig konfigurieren.
- [ ] Pro Spalte obere + mittlere Reihe mit demselben Item/Data befüllen; dadurch sind z. B. 2×64 Items in einem Trade möglich.
- [ ] Unterschiedliches Item/Data in zweiter Reihe derselben Spalte wird abgelehnt.
- [ ] Preisänderungen (+1/-1, Shift ±10, Mittelklick +100, Q Reset) werden gespeichert.
- [ ] Stock withdraw/add und Restart: persistenter Bestand und Preise korrekt.
- [ ] Fremder Spieler kann Shop nicht verwalten.

### Echtes Villager-Handelsfenster / Kauf

- [ ] Normaler Rechtsklick als Käufer öffnet **das echte Villager-Handelsfenster**, kein separates 3×9-Käufer-GUI.
- [ ] Mehrere konfigurierte Angebote lassen sich mit den Vanilla-Pfeilen/Trade-Auswahl durchschalten.
- [ ] Trade-Vorschau zeigt Item, Gesamtmenge/zweiten Stack und Coin-Preis verständlich an.
- [ ] Virtueller `SkyKings Coins`-Netherstern ist nicht entnehmbar und landet niemals im Spielerinventar.
- [ ] Shift-Klick aus dem unteren Spielerinventar kann keine Items in die Merchant-Eingabeslots verschieben.
- [ ] Drag über die Merchant-Slots wird blockiert.
- [ ] ESC/Inventar schließen gibt **keinen** virtuellen Netherstern oder Preview-Gegenstand zurück.
- [ ] Kauf mit ausreichend Coins + Platz: Angebot wird exakt einmal verkauft, Käufer Coins − Preis, Käufer Itemmenge korrekt, Seller Pending Revenue korrekt.
- [ ] 2×64-Angebot kaufen: exakt beide gespeicherten Stacks werden einmal zugestellt.
- [ ] 5-%-Fee korrekt, auch bei Preisen, die nicht glatt durch 100 teilbar sind.
- [ ] Nicht genug Coins: weder Stock noch Items verändern.
- [ ] Inventar voll: weder Coins noch Stock gehen verloren.
- [ ] Ausverkauftes/zwischenzeitlich geändertes Angebot kann aus einem bereits offenen Trade-Fenster nicht doppelt gekauft werden.
- [ ] Nach erfolgreichem Kauf aktualisiert/öffnet sich der Villager-Trade mit dem neuesten Shopzustand erneut.

### Erlös / Entfernen

- [ ] Einnahmen claimen: Pending Revenue wird exakt einmal ausgezahlt.
- [ ] Test mit extrem hohem Coin-Kontostand: Wenn Claim den `long`-Kontostand ueberlaufen wuerde, wird die Auszahlung blockiert und Pending Revenue bleibt unveraendert gespeichert.
- [ ] Sehr hohes bereits angesammeltes Pending Revenue: weiterer Kauf darf vor Abbuchung fail-closed abbrechen statt Revenue zu ueberlaufen.
- [ ] Shop mit leerem Stock + abgeholten Einnahmen entfernen → Villager verschwindet und genau **ein** Haendler-Ei wird zurückgegeben; bei freier Hand direkt in die Hand.
- [ ] Kein Platz fuer das zurückzugebende Haendler-Ei → Remove wird komplett abgebrochen; Shop und Villager bleiben erhalten.
- [ ] Zurückgegebenes Haendler-Ei ist erneut platzierbar und stackt mit aktuellen Haendler-Eiern.

## 10. Commands / Navigation

- [ ] `/commands` → Economy & Handel zeigt **Casino**, **Jackpot** und **PlayerShop**.
- [ ] Casino-Karte öffnet `/casino`.
- [ ] Jackpot-Karte öffnet `/jackpot`.
- [ ] PlayerShop-Hinweise zeigen die korrekten Commands.
- [ ] Navigation Back/Home bleibt funktionsfähig.
- [ ] Admin mit `skykings.admin.coins`: `/addcoins <Spieler> <Anzahl>` erhöht exakt um die angegebene positive Menge.
- [ ] Admin mit `skykings.admin.coins`: `/setcoins <Spieler> <Anzahl>` setzt exakt auf den Wert; `0` funktioniert, negative Werte werden abgelehnt.
- [ ] `/addcoins` und `/setcoins` funktionieren auch bei einem bekannten Offline-Spieler und erzeugen Audit-Einträge.
- [ ] Spieler ohne `skykings.admin.coins` kann beide Coin-Commands nicht verwenden.

## 11. Multiplayer / Race Conditions

Mindestens zwei Clients gleichzeitig.

- [ ] Gleichzeitiger Voucher-Rechtsklick auf denselben verbleibenden Claim erzeugt insgesamt maximal einen Reward.
- [ ] Zwei Käufer öffnen dasselbe PlayerShop-Angebot gleichzeitig; beim letzten Stock kann insgesamt nur **ein** Kauf erfolgreich sein.
- [ ] Verkäufer verändert/entnimmt ein Angebot, während Käufer das Villager-Fenster offen hat → alter Preview-Klick erzeugt keinen Dupe und keine falsche Abbuchung.
- [ ] Käufer disconnectet/alt+F4 während geöffnetem Villager-Trade → keine virtuellen Merchant-Items bleiben erhalten und kein Kauf wird erfunden.
- [ ] Gleichzeitige Casino-/Jackpot-Aktionen erzeugen keine negativen/duplizierten Kontostände.
- [ ] Crate-Rapid-Open erzeugt keine Mehrfachclaims außerhalb des Stackbestands.

## 12. Finales Release-Gate

- [ ] Automatisierter Maven-/CI-Build auf `main` grün.
- [ ] Alle automatisierten Tests grün.
- [ ] `/skcheck` ohne Pflichtfehler.
- [ ] Alle obigen kritischen Runtime-Flows auf echtem 1.8.9-Server getestet.
- [ ] Testserver einmal vollständig neu gestartet und Kernflows erneut kurz geprüft.
- [ ] Erst danach Produktion freigeben.

> Wichtig: Ein grüner Compile-/Unit-Test ersetzt bei Minecraft 1.8.9 keine echte Bukkit-/Client-Runtime-Prüfung. Der letzte Release-Schritt bleibt deshalb bewusst ein manueller Multiplayer-Test auf dem Windows-Testserver.
