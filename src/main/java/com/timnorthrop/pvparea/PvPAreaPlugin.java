package com.timnorthrop.pvparea;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.command.brigadier.argument.ArgumentTypes;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PvPAreaPlugin extends JavaPlugin {
    private final Map<Long, Set<PvPArea>> areaMap = new HashMap<>();

    private List<PersistentDataContainer> pdcs;

    public Map<Long, Set<PvPArea>> getAreaMap() {
        return this.areaMap;
    }

    @Override
    public void onEnable() {
        Keys.init(this);

        List<World> worlds = Bukkit.getWorlds();
        pdcs = new ArrayList<>();
        for (World w : worlds) {
            PersistentDataContainer pdc = w.getPersistentDataContainer();
            pdcs.add(pdc);
            int[] areasInts = pdc.get(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY);

            if (areasInts == null || areasInts.length == 0) {
                getLogger().info("No areas found in PDC. Proceeding with 0 PvPAreas...");
            } else if (areasInts.length % 4 != 0) {
                getLogger().info("PDC integer array contains an incompatible number of ints.");
            } else {
                for (int i = 0; i < areasInts.length; i += 4) {
                    int xMin = areasInts[i];
                    int xMax = areasInts[i + 1];
                    int zMin = areasInts[i + 2];
                    int zMax = areasInts[i + 3];

                    try {
                        PvPArea area = new PvPArea(xMin, xMax, zMin, zMax, w);

                        if (!area.overlapsAnyArea(areaMap)) {
                            addAreaToMap(area.getChunkKeys(), area, areaMap);
                        } else {
                            getLogger().info("Area overlaps existing area(s). Skipping...");
                        }
                    } catch (RuntimeException e) {
                        getLogger().info("Invalid area. Skipping...");
                    }
                }
            }
        }

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        registerCommands();

        getLogger().info("PvPArea enabled!");
    }

    private void registerCommands() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("pvparea");

        root.then(Commands.literal("list")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (areaMap.isEmpty()) {
                        sender.sendMessage("There are no PvP areas currently active.");
                        return Command.SINGLE_SUCCESS;
                    }

                    Set<String> allAreas = new HashSet<>();
                    for (Set<PvPArea> set : areaMap.values()) {
                        for (PvPArea area : set) {
                            allAreas.add(area.toString());
                        }
                    }

                    StringBuilder list = new StringBuilder();
                    int i = 1;
                    for (String s : allAreas) {
                        list.append("area").append(i).append(": ").append(s);
                        if (i == allAreas.size()) {
                            list.append(", ");
                        }
                        i++;
                    }

                    sender.sendRichMessage("There <isare> " + allAreas.size() +
                                    " PvP <areas> currently active.<newline><list>",
                            Placeholder.component("isare", Component.text(allAreas.size() == 1 ? "is" : "are")),
                            Placeholder.component("areas", Component.text(allAreas.size() == 1 ? "area" : "areas")),
                            Placeholder.component("list", Component.text(list.toString())));

                    return Command.SINGLE_SUCCESS;
                })
        );
        root.then(Commands.literal("add")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.argument("x-min", IntegerArgumentType.integer())
                        .then(Commands.argument("x-max", IntegerArgumentType.integer())
                                .then(Commands.argument("z-min", IntegerArgumentType.integer())
                                        .then(Commands.argument("z-max", IntegerArgumentType.integer())
                                                .then(Commands.argument("world", ArgumentTypes.world())
                                                .executes(ctx -> {
                                                    PvPArea newArea;
                                                    CommandSender sender = ctx.getSource().getSender();
                                                    final World world = ctx.getArgument("world", World.class);

                                                    try {
                                                        newArea = new PvPArea(
                                                                IntegerArgumentType.getInteger(ctx, "x-min"),
                                                                IntegerArgumentType.getInteger(ctx, "x-max"),
                                                                IntegerArgumentType.getInteger(ctx, "z-min"),
                                                                IntegerArgumentType.getInteger(ctx, "z-max"),
                                                                world
                                                        );
                                                    } catch (IllegalArgumentException e) {
                                                        sender.sendRichMessage("<red>Coordinate minimums must be " +
                                                                "less than maximums.</red>");
                                                        return Command.SINGLE_SUCCESS;
                                                    }

                                                    PersistentDataContainer pdc = world.getPersistentDataContainer();

                                                    if (!newArea.overlapsAnyArea(areaMap)) {
                                                        addAreaToMap(newArea.getChunkKeys(), newArea, areaMap);
                                                        int[] areasInts = pdc.get(Keys.PVP_AREA,
                                                                PersistentDataType.INTEGER_ARRAY);
                                                        if (areasInts == null) {
                                                            pdc.set(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY,
                                                                    new int[] {
                                                                            newArea.getXMin(),
                                                                            newArea.getXMax(),
                                                                            newArea.getZMin(),
                                                                            newArea.getZMax()
                                                                    }
                                                            );
                                                        } else {
                                                            int[] newIntsArray = new int[areasInts.length + 4];
                                                            System.arraycopy(areasInts, 0, newIntsArray,
                                                                    0, areasInts.length);

                                                            newIntsArray[newIntsArray.length - 4] = newArea.getXMin();
                                                            newIntsArray[newIntsArray.length - 3] = newArea.getXMax();
                                                            newIntsArray[newIntsArray.length - 2] = newArea.getZMin();
                                                            newIntsArray[newIntsArray.length - 1] = newArea.getZMax();

                                                            pdc.set(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY,
                                                                    newIntsArray);
                                                        }

                                                        sender.sendRichMessage("Added new PvP area at " + newArea);
                                                    } else {
                                                        sender.sendRichMessage("<red>Area overlaps existing areas. " +
                                                                "Try different values.</red>");
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                }))
                                        )
                                )
                        )
                )
        );
        root.then(Commands.literal("clear-all")
                .requires(sender -> sender.getSender().isOp())
                .executes(ctx -> {
                    for (PersistentDataContainer pdc : pdcs) {
                        pdc.set(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY, new int[]{});
                    }
                    areaMap.clear();

                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage("All PvP areas have been cleared.");
                    return Command.SINGLE_SUCCESS;
                })
        );

        LiteralCommandNode<CommandSourceStack> builtRoot = root.build();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                commands -> commands.registrar().register(builtRoot));
    }

    private void addAreaToMap(Set<Long> chunkKeys, PvPArea area, Map<Long, Set<PvPArea>> areaMap) {
        for (long ck : chunkKeys) {
            areaMap.putIfAbsent(ck, new HashSet<>());
            areaMap.get(ck).add(area);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("PvPArea disabled!");
    }
}
