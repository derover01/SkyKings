# SkyKings UI Atlas Source

`skykings-ui-atlas.png` is the compact source atlas for the Minecraft 1.8.9 UI/branding pack.

Grid: 5 columns x 4 rows, 32x32 pixels per tile, row-major order.

1. Home
2. Back
3. Next
4. Locked
5. Ready
6. Completed
7. Premium
8. Coins
9. SkyKings Star
10. Battle Pass
11. Quests
12. Kits
13. Crates
14. Jackpot
15. Shop
16. Trade
17. Clan
18. Duel
19. Event
20. SkyKings logo (`pack.png`)

The build script compiles `scripts/tools/ResourcePackAtlasBuilder.java` with Java 8 and generates the legacy 1.8.x item texture filenames in the staged ZIP. Do not reorder tiles without updating that builder and `ResourcePackIcon` together.
