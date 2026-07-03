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
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;

import static net.kyori.adventure.text.Component.text;

public class PvPAreaPlugin extends JavaPlugin {
    private final Map<AreaChunkKey, Set<PvPArea>> areaMap = new HashMap<>();

    private List<PersistentDataContainer> pdcs;

    public Map<AreaChunkKey, Set<PvPArea>> getAreaMap() {
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
            if (areasInts == null) {
                continue;
            }
            List<Integer> newAreasInts = new ArrayList<>();

            if (areasInts.length % 4 != 0) {
                getLogger().info("PDC integer array for world " + w.getKey() + " contains an incompatible " +
                        "number of ints");
            } else {
                for (int i = 0; i < areasInts.length; i += 4) {
                    int xMin = areasInts[i];
                    int xMax = areasInts[i + 1];
                    int zMin = areasInts[i + 2];
                    int zMax = areasInts[i + 3];

                    try {
                        PvPArea area = new PvPArea(xMin, xMax, zMin, zMax, w);

                        if (area.overlapsNoAreas(areaMap)) {
                            addAreaToMap(area.getChunkKeys(), area, areaMap);
                            newAreasInts.add(xMin);
                            newAreasInts.add(xMax);
                            newAreasInts.add(zMin);
                            newAreasInts.add(zMax);
                            getLogger().info("Detected PvP area at " + area.toString());
                        } else {
                            getLogger().info("Area overlaps existing area(s) - removed from PDC of world " +
                                    w.getKey());
                        }
                    } catch (RuntimeException e) {
                        getLogger().info("Invalid area - removed from PDC of world " + w.getKey());
                    }
                }

                int[] arr = newAreasInts.stream().mapToInt(i -> i).toArray();
                pdc.set(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY, arr);
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

                    Set<PvPArea> allAreas = new HashSet<>();
                    for (Set<PvPArea> set : areaMap.values()) {
                        allAreas.addAll(set);
                    }

                    sender.sendRichMessage("There <isare> " + allAreas.size() + " PvP <areas> currently active.",
                            Placeholder.component("isare", text(allAreas.size() == 1 ? "is" : "are")),
                            Placeholder.component("areas", text(allAreas.size() == 1 ? "area" : "areas")));
                    sender.sendRichMessage("<list>", Placeholder.component("list", getList(allAreas)));
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

                                                    if (newArea.overlapsNoAreas(areaMap)) {
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

    private static Component getList(Set<PvPArea> allAreas) {
        Component list = text("");
        int i = 1;
        for (PvPArea area : allAreas) {
            list = list.append(text("area" + i).color(NamedTextColor.YELLOW))
                    .append(text(": " + area))
                    .append(text(i == allAreas.size() ? "" : ", "));
            i++;
        }
        return list;
    }

    private void addAreaToMap(Set<AreaChunkKey> chunkKeys, PvPArea area, Map<AreaChunkKey, Set<PvPArea>> areaMap) {
        for (AreaChunkKey ck : chunkKeys) {
            areaMap.putIfAbsent(ck, new HashSet<>());
            areaMap.get(ck).add(area);
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("PvPArea disabled!");
    }
}
