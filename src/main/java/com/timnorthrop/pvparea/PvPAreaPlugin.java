package com.timnorthrop.pvparea;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;

public class PvPAreaPlugin extends JavaPlugin {
    private final Set<PvPArea> areaSet = new HashSet<>();
    private final DeathListener areasListener = new DeathListener(areaSet);

    World world;
    PersistentDataContainer pdc;

    @Override
    public void onEnable() {
        getLogger().info("PvPArea enabled!");
        Keys.init(this);

        world = Bukkit.getWorlds().stream()
                .filter(w -> w.getEnvironment() == World.Environment.NORMAL)
                .findFirst()
                .orElseThrow();
        pdc = world.getPersistentDataContainer();
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
                    PvPArea area = new PvPArea(xMin, xMax, zMin, zMax);

                    if (!area.overlapsAnyArea(areaSet)) {
                        areaSet.add(area);
                    } else {
                        getLogger().info("Area overlaps existing areas. Skipping...");
                    }
                } catch (RuntimeException e) {
                    getLogger().info("Invalid area. Skipping...");
                }
            }
        }

        getServer().getPluginManager().registerEvents(areasListener, this);
        registerCommands();
    }

    private void registerCommands() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("pvparea");

        root.then(Commands.literal("list")
                .executes(ctx -> {
                    CommandSender sender = ctx.getSource().getSender();
                    if (areaSet.isEmpty()) {
                        sender.sendMessage("There are no PvP areas currently active.");
                        return Command.SINGLE_SUCCESS;
                    }

                    StringBuilder response;
                    if (areaSet.size() == 1) {
                        response = new StringBuilder("There is " + areaSet.size() + " PvP area currently active.\n");
                    } else {
                        response = new StringBuilder("There are " + areaSet.size() + " PvP areas currently active.\n");
                    }

                    int i = 0;
                    for (PvPArea a : areaSet) {
                        response.append("area").append(i + 1).append(": ").append(a.toString());
                        if (i != areaSet.size() - 1) {
                            response.append(", ");
                        }
                        i++;
                    }

                    sender.sendMessage(response.toString());
                    return Command.SINGLE_SUCCESS;
                })
        );
        root.then(Commands.literal("add")
                .requires(sender -> sender.getSender().isOp())
                .then(Commands.argument("x-min", IntegerArgumentType.integer())
                        .then(Commands.argument("x-max", IntegerArgumentType.integer())
                                .then(Commands.argument("z-min", IntegerArgumentType.integer())
                                        .then(Commands.argument("z-max", IntegerArgumentType.integer())
                                                .executes(ctx -> {
                                                    PvPArea newArea;
                                                    CommandSender sender = ctx.getSource().getSender();

                                                    try {
                                                        newArea = new PvPArea(
                                                                IntegerArgumentType.getInteger(ctx, "x-min"),
                                                                IntegerArgumentType.getInteger(ctx, "x-max"),
                                                                IntegerArgumentType.getInteger(ctx, "z-min"),
                                                                IntegerArgumentType.getInteger(ctx, "z-max")
                                                        );
                                                    } catch (RuntimeException e) {
                                                        sender.sendRichMessage("<red>Coordinate minimums must be less " +
                                                                "than maximums.</red>");
                                                        return Command.SINGLE_SUCCESS;
                                                    }

                                                    if (!newArea.overlapsAnyArea(areaSet)) {
                                                        areaSet.add(newArea);
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
                                                                    0, newIntsArray.length - 4);

                                                            newIntsArray[newIntsArray.length - 4] = newArea.getXMin();
                                                            newIntsArray[newIntsArray.length - 3] = newArea.getXMax();
                                                            newIntsArray[newIntsArray.length - 2] = newArea.getZMin();
                                                            newIntsArray[newIntsArray.length - 1] = newArea.getZMax();

                                                            pdc.set(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY,
                                                                    newIntsArray);
                                                        }

                                                        areasListener.setAreaSet(areaSet);
                                                        sender.sendRichMessage("Added new PvP area at " + newArea);
                                                    } else {
                                                        sender.sendRichMessage("<red>Area overlaps existing areas. " +
                                                                "Try different values.</red>");
                                                    }
                                                    return Command.SINGLE_SUCCESS;
                                                })
                                        )
                                )
                        )
                )
        );
        root.then(Commands.literal("clear-all")
                .requires(sender -> sender.getSender().isOp())
                .executes(ctx -> {
                    pdc.set(Keys.PVP_AREA, PersistentDataType.INTEGER_ARRAY, new int[]{});
                    areaSet.clear();

                    CommandSender sender = ctx.getSource().getSender();
                    sender.sendMessage("All PvP areas have been cleared.");
                    return Command.SINGLE_SUCCESS;
                })
        );

        LiteralCommandNode<CommandSourceStack> builtRoot = root.build();
        this.getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS,
                commands -> commands.registrar().register(builtRoot));
    }

    @Override
    public void onDisable() {
        getLogger().info("PvPArea disabled!");
    }
}
