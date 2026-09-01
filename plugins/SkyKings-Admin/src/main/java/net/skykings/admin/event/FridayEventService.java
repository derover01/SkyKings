package net.skykings.admin.event;

import net.skykings.admin.warp.WarpService;
import net.skykings.core.api.SkyKingsCoreAPI;
import net.skykings.core.sound.SoundFeedback;
import net.skykings.crates.CrateItemCodec;
import net.skykings.crates.CrateRegistry;
import net.skykings.crates.SkyKingsCrates;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Orchestriert das woechentliche SkyKings-Freitags-Community-Event:
 * Intro -> automatische Verlosung -> manuelle Hand-Item-Verlosungen -> Drop-Event am Warp "Event".
 */
public final class FridayEventService implements CommandExecutor {

    private enum Phase { IDLE, AUTO_DRAW, MANUAL_DRAWS, DROP_COUNTDOWN, DROP_RUNNING }

    private static final String EVENT_WARP = "Event";
    private static final int DROP_COUNTDOWN_SECONDS = 15;
    private static final int DROP_COUNT = 42;

    private final JavaPlugin plugin;
    private final SkyKingsCoreAPI core;
    private final WarpService warps;
    private final Random random = new Random();
    private final List<BukkitTask> tasks = new ArrayList<BukkitTask>();

    private Phase phase = Phase.IDLE;
    private boolean manualDrawRunning;
    private long generation;

    public FridayEventService(JavaPlugin plugin, SkyKingsCoreAPI core, WarpService warps) {
        this.plugin = plugin;
        this.core = core;
        this.warps = warps;
    }

    @Override
    public synchronized boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName();
        if ("freitag".equalsIgnoreCase(name)) return handleFriday(sender, args);
        if ("verlosen".equalsIgnoreCase(name)) return handleManualDraw(sender, args);
        return true;
    }

    private boolean handleFriday(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skykings.admin.friday")) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length > 0 && "stop".equalsIgnoreCase(args[0])) {
            stop(true);
            sender.sendMessage(ChatColor.YELLOW + "Freitags-Event wurde beendet.");
            return true;
        }
        if (args.length > 0 && "status".equalsIgnoreCase(args[0])) {
            sender.sendMessage(ChatColor.GOLD + "Freitags-Event: " + ChatColor.WHITE + phase.name()
                    + ChatColor.GRAY + (manualDrawRunning ? " (Verlosung laeuft)" : ""));
            return true;
        }
        if (phase != Phase.IDLE) {
            sender.sendMessage(ChatColor.RED + "Das Freitags-Event laeuft bereits. Phase: " + phase.name());
            return true;
        }
        Location event = warps.get(EVENT_WARP);
        if (event == null || event.getWorld() == null) {
            sender.sendMessage(ChatColor.RED + "Der Warp 'Event' fehlt oder seine Welt ist nicht geladen. Erst /setwarp Event setzen.");
            return true;
        }
        start(event);
        return true;
    }

    private boolean handleManualDraw(CommandSender sender, String[] args) {
        if (!sender.hasPermission("skykings.admin.friday")) {
            sender.sendMessage(ChatColor.RED + "Dafuer hast du keine Berechtigung.");
            return true;
        }
        if (args.length > 0 && "fertig".equalsIgnoreCase(args[0])) {
            if (phase != Phase.MANUAL_DRAWS) {
                sender.sendMessage(ChatColor.RED + "Die manuelle Verlosungsphase ist gerade nicht aktiv.");
                return true;
            }
            if (manualDrawRunning) {
                sender.sendMessage(ChatColor.YELLOW + "Warte, bis die aktuelle Verlosung gezogen wurde.");
                return true;
            }
            beginDropCountdown();
            return true;
        }
        if (phase != Phase.MANUAL_DRAWS) {
            sender.sendMessage(ChatColor.RED + "Starte zuerst /freitag und warte auf die manuelle Verlosungsphase.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "/verlosen ohne 'fertig' muss ingame mit einem Item in der Hand benutzt werden.");
            return true;
        }
        if (manualDrawRunning) {
            sender.sendMessage(ChatColor.YELLOW + "Es wird gerade schon ein Gewinn gezogen.");
            return true;
        }
        Player admin = (Player) sender;
        ItemStack hand = admin.getItemInHand();
        if (hand == null || hand.getType() == Material.AIR || hand.getAmount() <= 0) {
            admin.sendMessage(ChatColor.RED + "Halte den kompletten Gewinn-Stack in der Hand und nutze /verlosen.");
            SoundFeedback.error(admin);
            return true;
        }
        startManualDraw(hand.clone());
        return true;
    }

    private void start(final Location eventLocation) {
        cancelTasks();
        generation++;
        final long token = generation;
        phase = Phase.AUTO_DRAW;
        manualDrawRunning = false;

        broadcastBlank();
        Bukkit.broadcastMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "✦ FREITAGS-EVENT ✦");
        Bukkit.broadcastMessage(ChatColor.WHITE + "Der SkyKings Community-Abend beginnt jetzt!");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Erst Auto-Verlosung, danach Staff-Gewinne und zum Finale ein Drop-Event.");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Event-Area: " + ChatColor.WHITE + "/warp Event");
        broadcastBlank();
        broadcastSound(Sound.ENDERDRAGON_GROWL, 0.75F, 1.25F);
        fireworks(eventLocation, 4);

        schedule(40L, token, new Runnable() {
            @Override public void run() {
                Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "AUTO-VERLOSUNG"
                        + ChatColor.GRAY + " • Ein Online-Spieler wird automatisch gezogen...");
                broadcastSound(Sound.NOTE_PLING, 0.8F, 1.05F);
            }
        });
        schedule(100L, token, new Runnable() {
            @Override public void run() { finishAutomaticDraw(eventLocation); }
        });
    }

    private synchronized void finishAutomaticDraw(Location eventLocation) {
        if (phase != Phase.AUTO_DRAW) return;
        List<Player> players = onlinePlayers();
        if (players.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Auto-Verlosung uebersprungen: Kein Spieler online.");
        } else {
            Player winner = players.get(random.nextInt(players.size()));
            int reward = random.nextInt(4);
            String rewardText;
            if (reward == 0) {
                core.getEconomyService().deposit(winner.getUniqueId(), 500000L, "FRIDAY_EVENT", "Auto-Verlosung");
                rewardText = "500.000 Coins";
            } else if (reward == 1) {
                core.getEconomyService().deposit(winner.getUniqueId(), 1000000L, "FRIDAY_EVENT", "Auto-Verlosung");
                rewardText = "1.000.000 Coins";
            } else if (reward == 2) {
                core.getNetherstarService().deposit(winner.getUniqueId(), 10L, "FRIDAY_EVENT", "Auto-Verlosung");
                rewardText = "10 SkyKings Sterne";
            } else {
                ItemStack crate = createRandomCrate(2);
                if (crate != null) {
                    giveOrDrop(winner, crate);
                    rewardText = "2 zufaellige Crates";
                } else {
                    core.getEconomyService().deposit(winner.getUniqueId(), 750000L, "FRIDAY_EVENT", "Crate-Fallback");
                    rewardText = "750.000 Coins";
                }
            }
            Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + winner.getName()
                    + ChatColor.YELLOW + " gewinnt " + ChatColor.WHITE + rewardText + ChatColor.YELLOW + "!");
            broadcastSound(Sound.LEVEL_UP, 0.9F, 1.35F);
            fireworks(winner.getLocation(), 3);
        }

        phase = Phase.MANUAL_DRAWS;
        Bukkit.broadcastMessage(ChatColor.DARK_GRAY + "----------------------------------------");
        Bukkit.broadcastMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "STAFF-VERLOSUNGEN");
        Bukkit.broadcastMessage(ChatColor.WHITE + "Jetzt folgen ausgewaehlte Item-Gewinne.");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Danach startet das grosse Drop-Event bei " + ChatColor.YELLOW + "/warp Event" + ChatColor.GRAY + ".");
        broadcastSound(Sound.NOTE_PLING, 0.7F, 1.45F);
        fireworks(eventLocation, 2);
    }

    private synchronized void startManualDraw(final ItemStack prize) {
        manualDrawRunning = true;
        final String prizeName = describe(prize);
        final long token = generation;
        Bukkit.broadcastMessage(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "VERLOSUNG"
                + ChatColor.GRAY + " • " + ChatColor.WHITE + prizeName);
        Bukkit.broadcastMessage(ChatColor.GRAY + "Alle aktuell Online-Spieler sind automatisch dabei.");
        broadcastSound(Sound.NOTE_PLING, 0.65F, 1.0F);

        for (int i = 3; i >= 1; i--) {
            final int seconds = i;
            schedule((4 - i) * 20L, token, new Runnable() {
                @Override public void run() {
                    Bukkit.broadcastMessage(ChatColor.YELLOW + "Ziehung in " + ChatColor.WHITE + seconds + ChatColor.YELLOW + "...");
                    broadcastSound(Sound.NOTE_PLING, 0.7F, 1.0F + (3 - seconds) * 0.18F);
                }
            });
        }
        schedule(70L, token, new Runnable() {
            @Override public void run() { finishManualDraw(prize, prizeName); }
        });
    }

    private synchronized void finishManualDraw(ItemStack prize, String prizeName) {
        if (phase != Phase.MANUAL_DRAWS || !manualDrawRunning) return;
        List<Player> players = onlinePlayers();
        if (players.isEmpty()) {
            Bukkit.broadcastMessage(ChatColor.YELLOW + "Keine Online-Spieler fuer diese Verlosung.");
        } else {
            Player winner = players.get(random.nextInt(players.size()));
            giveOrDrop(winner, prize.clone());
            Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + winner.getName()
                    + ChatColor.YELLOW + " gewinnt " + ChatColor.WHITE + prizeName + ChatColor.YELLOW + "!");
            broadcastSound(Sound.LEVEL_UP, 0.85F, 1.4F);
            fireworks(winner.getLocation(), 2);
        }
        manualDrawRunning = false;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("skykings.admin.friday")) {
                player.sendMessage(ChatColor.GRAY + "Naechster Gewinn: Item in die Hand + " + ChatColor.AQUA + "/verlosen"
                        + ChatColor.GRAY + " • Ende: " + ChatColor.AQUA + "/verlosen fertig");
            }
        }
    }

    private synchronized void beginDropCountdown() {
        Location event = warps.get(EVENT_WARP);
        if (event == null || event.getWorld() == null) {
            phase = Phase.MANUAL_DRAWS;
            for (Player player : Bukkit.getOnlinePlayers()) if (player.hasPermission("skykings.admin.friday")) {
                player.sendMessage(ChatColor.RED + "Warp Event ist nicht geladen. Drop-Phase wurde nicht gestartet.");
            }
            return;
        }
        phase = Phase.DROP_COUNTDOWN;
        final long token = generation;
        broadcastBlank();
        Bukkit.broadcastMessage(ChatColor.RED.toString() + ChatColor.BOLD + "DROP-EVENT STARTET!");
        Bukkit.broadcastMessage(ChatColor.WHITE + "Crates, OP-PvP-Items, God Apples, Gear und mehr fallen gleich vom Himmel.");
        Bukkit.broadcastMessage(ChatColor.YELLOW.toString() + ChatColor.BOLD + "JETZT: /warp Event");
        Bukkit.broadcastMessage(ChatColor.GRAY + "Start in " + DROP_COUNTDOWN_SECONDS + " Sekunden.");
        broadcastBlank();
        broadcastSound(Sound.ENDERDRAGON_GROWL, 0.7F, 1.4F);
        fireworks(event, 5);

        int[] marks = new int[] {10, 5, 3, 2, 1};
        for (final int mark : marks) {
            long delay = (DROP_COUNTDOWN_SECONDS - mark) * 20L;
            schedule(delay, token, new Runnable() {
                @Override public void run() {
                    Bukkit.broadcastMessage(ChatColor.RED + "Drop-Event in " + ChatColor.WHITE + mark + ChatColor.RED + "s!");
                    broadcastSound(Sound.NOTE_PLING, 0.8F, 1.0F + (DROP_COUNTDOWN_SECONDS - mark) * 0.025F);
                }
            });
        }
        schedule(DROP_COUNTDOWN_SECONDS * 20L, token, new Runnable() {
            @Override public void run() { startDropEvent(); }
        });
    }

    private synchronized void startDropEvent() {
        final Location center = warps.get(EVENT_WARP);
        if (center == null || center.getWorld() == null) {
            stop(false);
            return;
        }
        phase = Phase.DROP_RUNNING;
        final long token = generation;
        Bukkit.broadcastMessage(ChatColor.GOLD.toString() + ChatColor.BOLD + "✦ DROP-EVENT LAEUFT ✦");
        Bukkit.broadcastMessage(ChatColor.YELLOW + "Sammelt, was ihr kriegen koennt!");
        broadcastSound(Sound.WITHER_SPAWN, 0.85F, 1.25F);
        fireworks(center, 7);

        BukkitTask dropTask = new BukkitRunnable() {
            private int dropped;
            @Override public void run() {
                synchronized (FridayEventService.this) {
                    if (phase != Phase.DROP_RUNNING || token != generation) { cancel(); return; }
                }
                if (dropped >= DROP_COUNT) {
                    cancel();
                    finishDropEvent(center);
                    return;
                }
                ItemStack loot = createDropLoot();
                if (loot != null) dropPhysical(center, loot);
                if (dropped % 6 == 0) {
                    broadcastSound(Sound.ORB_PICKUP, 0.35F, 1.1F + random.nextFloat() * 0.5F);
                    firework(randomized(center, 5.0D, 1.0D));
                }
                dropped++;
            }
        }.runTaskTimer(plugin, 0L, 8L);
        tasks.add(dropTask);
    }

    private synchronized void finishDropEvent(Location center) {
        if (phase != Phase.DROP_RUNNING) return;
        phase = Phase.IDLE;
        Bukkit.broadcastMessage(ChatColor.AQUA.toString() + ChatColor.BOLD + "Freitags-Event beendet!"
                + ChatColor.WHITE + " Danke fuers Mitmachen ♥");
        broadcastSound(Sound.LEVEL_UP, 0.85F, 1.55F);
        fireworks(center, 6);
        tasks.clear();
    }

    private ItemStack createDropLoot() {
        int roll = random.nextInt(100);
        if (roll < 24) {
            ItemStack crate = createRandomCrate(roll < 4 ? 2 : 1);
            if (crate != null) return crate;
        }
        if (roll < 39) return eventSword();
        if (roll < 51) return new ItemStack(Material.GOLDEN_APPLE, 4 + random.nextInt(9), (short) 1);
        if (roll < 62) return new ItemStack(Material.ENDER_PEARL, 8 + random.nextInt(9));
        if (roll < 72) return new ItemStack(Material.DIAMOND_BLOCK, 8 + random.nextInt(17));
        if (roll < 81) return eventArmor();
        if (roll < 89) return eventBow();
        if (roll < 95) return new ItemStack(Material.EXP_BOTTLE, 32 + random.nextInt(33));
        return new ItemStack(Material.POTION, 2 + random.nextInt(3), (short) 16421);
    }

    private ItemStack eventSword() {
        ItemStack item = new ItemStack(Material.DIAMOND_SWORD);
        item.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 5);
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        item.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 2);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.AQUA.toString() + ChatColor.BOLD + "Friday Blade");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack eventArmor() {
        Material[] pieces = { Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS };
        ItemStack item = new ItemStack(pieces[random.nextInt(pieces.length)]);
        item.addUnsafeEnchantment(Enchantment.PROTECTION_ENVIRONMENTAL, 4);
        item.addUnsafeEnchantment(Enchantment.DURABILITY, 3);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.LIGHT_PURPLE.toString() + ChatColor.BOLD + "Friday Armor");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack eventBow() {
        ItemStack item = new ItemStack(Material.BOW);
        item.addUnsafeEnchantment(Enchantment.ARROW_DAMAGE, 5);
        item.addUnsafeEnchantment(Enchantment.ARROW_KNOCKBACK, 2);
        item.addUnsafeEnchantment(Enchantment.ARROW_FIRE, 1);
        item.addUnsafeEnchantment(Enchantment.ARROW_INFINITE, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ChatColor.GOLD.toString() + ChatColor.BOLD + "Friday Bow");
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createRandomCrate(int amount) {
        Plugin raw = Bukkit.getPluginManager().getPlugin("SkyKings-Crates");
        if (!(raw instanceof SkyKingsCrates) || !raw.isEnabled()) return null;
        SkyKingsCrates crates = (SkyKingsCrates) raw;
        CrateRegistry registry = crates.getCrateRegistry();
        CrateItemCodec codec = crates.getCrateItemCodec();
        if (registry == null || codec == null) return null;
        Collection<CrateRegistry.CrateDefinition> all = registry.getAll();
        if (all.isEmpty()) return null;
        List<CrateRegistry.CrateDefinition> choices = new ArrayList<CrateRegistry.CrateDefinition>(all);
        CrateRegistry.CrateDefinition selected = choices.get(random.nextInt(choices.size()));
        try {
            return codec.create(selected, amount);
        } catch (RuntimeException ex) {
            plugin.getLogger().warning("Friday-Crate konnte nicht sicher ausgegeben werden: " + ex.getMessage());
            return null;
        }
    }

    private void dropPhysical(Location center, ItemStack stack) {
        Location spawn = randomized(center, 8.0D, 7.0D + random.nextDouble() * 3.0D);
        Item item = center.getWorld().dropItem(spawn, stack);
        item.setPickupDelay(10);
        item.setVelocity(new Vector((random.nextDouble() - 0.5D) * 0.28D,
                -0.05D - random.nextDouble() * 0.08D,
                (random.nextDouble() - 0.5D) * 0.28D));
    }

    private Location randomized(Location center, double radius, double yOffset) {
        double angle = random.nextDouble() * Math.PI * 2D;
        double distance = Math.sqrt(random.nextDouble()) * radius;
        return center.clone().add(Math.cos(angle) * distance, yOffset, Math.sin(angle) * distance);
    }

    private List<Player> onlinePlayers() {
        List<Player> players = new ArrayList<Player>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOnline()) players.add(player);
        }
        Collections.shuffle(players, random);
        return players;
    }

    private void giveOrDrop(Player player, ItemStack stack) {
        Map<Integer, ItemStack> left = player.getInventory().addItem(stack);
        for (ItemStack value : left.values()) player.getWorld().dropItemNaturally(player.getLocation(), value);
    }

    private String describe(ItemStack stack) {
        if (stack == null) return "Unbekannter Gewinn";
        ItemMeta meta = stack.getItemMeta();
        String name = meta != null && meta.hasDisplayName() ? meta.getDisplayName()
                : stack.getType().name().toLowerCase().replace('_', ' ');
        return ChatColor.WHITE + String.valueOf(stack.getAmount()) + "x " + name;
    }

    private void broadcastSound(Sound sound, float volume, float pitch) {
        for (Player player : Bukkit.getOnlinePlayers()) player.playSound(player.getLocation(), sound, volume, pitch);
    }

    private void fireworks(Location location, int amount) {
        for (int i = 0; i < amount; i++) {
            final Location at = randomized(location, 5.5D, 0.5D + random.nextDouble() * 2.5D);
            schedule(i * 5L, generation, new Runnable() {
                @Override public void run() { firework(at); }
            });
        }
    }

    private void firework(Location location) {
        if (location == null || location.getWorld() == null) return;
        Firework firework = location.getWorld().spawn(location, Firework.class);
        FireworkMeta meta = firework.getFireworkMeta();
        FireworkEffect.Type[] types = { FireworkEffect.Type.BALL_LARGE, FireworkEffect.Type.STAR, FireworkEffect.Type.BURST };
        meta.addEffect(FireworkEffect.builder()
                .with(types[random.nextInt(types.length)])
                .withColor(Color.fromRGB(38, 210, 220), Color.fromRGB(255, 205, 55))
                .withFade(Color.WHITE)
                .trail(true)
                .flicker(true)
                .build());
        meta.setPower(1);
        firework.setFireworkMeta(meta);
    }

    private void schedule(long delay, final long token, final Runnable action) {
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
            @Override public void run() {
                synchronized (FridayEventService.this) {
                    if (token != generation || phase == Phase.IDLE) return;
                }
                action.run();
            }
        }, Math.max(0L, delay));
        tasks.add(task);
    }

    private void broadcastBlank() { Bukkit.broadcastMessage(" "); }

    public synchronized void shutdown() { stop(false); }

    private synchronized void stop(boolean announce) {
        if (phase == Phase.IDLE && !announce) return;
        generation++;
        cancelTasks();
        phase = Phase.IDLE;
        manualDrawRunning = false;
        if (announce) {
            Bukkit.broadcastMessage(ChatColor.RED + "Das Freitags-Event wurde von Staff beendet.");
            broadcastSound(Sound.NOTE_BASS, 0.6F, 0.8F);
        }
    }

    private void cancelTasks() {
        for (BukkitTask task : tasks) if (task != null) task.cancel();
        tasks.clear();
    }
}
