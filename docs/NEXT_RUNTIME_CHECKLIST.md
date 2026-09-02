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
- [ ] `/is delete` vollständig bestätigen.
- [ ] `/is create` erneut → Insel wird erstellt, Startertruhe enthält **keine** neuen Starterressourcen.
- [ ] Server neu starten und erneut delete/create → weiterhin kein Starterloot.
- [ ] Dasselbe über das `/is` GUI testen, nicht nur per Command.
- [ ] Bestandsinsel aus älterem Stand löschen → Spieler wird vor dem Löschen als bereits versorgt persistiert.
- [ ] `plugins/SkyKings-Core/island-starter-claims.txt` bleibt über Neustarts erhalten.

## 5. Giveaway / Verlosung

- [ ] `/verlosen` mit Item in der Hand: globale 3–2–1-Ziehung läuft.
- [ ] Gewinner erhält exakt den vorgesehenen Stack.
- [ ] Gewinner-/Jackpot-Sound ist hörbar.
- [ ] Inventar voll: Gewinn wird sicher gedroppt statt verloren.
- [ ] Jeder Gewinner erzeugt `GIVEAWAY_WIN` im zentralen Audit-Log.
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

Mit Käufer + Verkäufer testen.

- [ ] Shop erstellen/konfigurieren, Stock einzahlen.
- [ ] Kauf mit ausreichend Coins + Platz: Stock − Menge, Käufer Coins − Preis, Käufer Item + Menge, Seller Pending Revenue korrekt.
- [ ] 5-%-Fee korrekt.
- [ ] Nicht genug Coins: weder Stock noch Items verändern.
- [ ] Inventar voll: weder Coins noch Stock gehen verloren.
- [ ] Einnahmen claimen: Pending Revenue wird exakt einmal ausgezahlt.
- [ ] Stock withdraw/add und Restart: persistenter Bestand korrekt.
- [ ] Fremder Spieler kann Shop nicht verwalten.

## 10. Commands / Navigation

- [ ] `/commands` → Economy & Handel zeigt **Casino**, **Jackpot** und **PlayerShop**.
- [ ] Casino-Karte öffnet `/casino`.
- [ ] Jackpot-Karte öffnet `/jackpot`.
- [ ] PlayerShop-Hinweise zeigen die korrekten Commands.
- [ ] Navigation Back/Home bleibt funktionsfähig.

## 11. Multiplayer / Race Conditions

Mindestens zwei Clients gleichzeitig.

- [ ] Gleichzeitiger Voucher-Rechtsklick auf denselben verbleibenden Claim erzeugt insgesamt maximal einen Reward.
- [ ] Gleichzeitige PlayerShop-Käufe beim letzten Stock können zusammen nicht mehr Stock verkaufen als vorhanden.
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
