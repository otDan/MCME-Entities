package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mojang.brigadier.context.CommandContext;
import org.bukkit.Location;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class SelectCommand extends McmeEntitiesCommandHandler {

    public SelectCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
                .requires(sender -> (sender instanceof RealPlayer)
                        && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
                .then(HelpfulLiteralBuilder.literal("entity")
                    .executes(context -> showSelection(context.getSource()))
                    .then(HelpfulLiteralBuilder.literal("target")
                        .executes(context -> setSelectTargetEntity(context.getSource()))
                        .then(HelpfulLiteralBuilder.literal("@p")
                            .executes(context -> {
                                ((BukkitCommandSender)context.getSource()).setSelectedTargetEntity((RealPlayer)context.getSource());
                                context.getSource().sendMessage("Saved you as target entity!");
                                return 0;
                            }))
                        .then(HelpfulRequiredArgumentBuilder.argument("name",word())
                            .executes(context -> selectTargetEntityByName(context.getSource(),context.getArgument("name",String.class)))))
                    .then(HelpfulLiteralBuilder.literal("clear")
                        .executes(context -> clearSelection(context.getSource()))))
                .then(HelpfulLiteralBuilder.literal("location")
                    .executes(context -> showSelectedLocations(context.getSource()))
                    .then(HelpfulLiteralBuilder.literal("add")
                        .executes(context -> addSelectedLocation(context.getSource(),null))
                        .then(HelpfulRequiredArgumentBuilder.argument("location", greedyString())
                            .executes(context -> addSelectedLocation(context.getSource(),context.getArgument("location",String.class)))))
                    .then(HelpfulLiteralBuilder.literal("clear")
                        .executes(context -> clearSelectedLocations(context.getSource()))));
        return commandNodeBuilder;
    }

    private int selectTargetEntityByName(McmeCommandSender sender, String name) {
        McmeEntity entity = EntitiesPlugin.getEntityServer().getEntity(name);
        if(entity != null) {
            ((BukkitCommandSender)sender).setSelectedTargetEntity(entity);
            sender.sendMessage("Target entity set: "+name);
        } else {
            sender.sendMessage("No entity found by name: "+name);
        }
        return 0;
    }

    private int showSelection(McmeCommandSender sender) {
        sender.sendMessage("Selected Entities:");
        ((BukkitCommandSender)sender).getSelectedEntities().forEach(entity
                -> sender.sendMessage(entity.getEntityId()+" "+entity.getName()+" "
                +entity.getLocation().getBlockX()+" "+entity.getLocation().getBlockY()+" "+entity.getLocation().getBlockZ()));
        return 0;
    }

    private int clearSelection(McmeCommandSender sender) {
        ((BukkitCommandSender)sender).clearSelectedEntities();
        sender.sendMessage("Entity selection cleared");
        return 0;
    }

    private int setSelectTargetEntity(McmeCommandSender sender) {
        McmeEntity entity = ((BukkitCommandSender)sender).getSelectedEntities().stream().findFirst().orElse(null);
        if(entity != null) {
            ((BukkitCommandSender) sender).setSelectedTargetEntity(entity);
            sender.sendMessage("Saved as target entity:  " + entity.getName() + " "
                    + entity.getLocation().getBlockX() + " " + entity.getLocation().getBlockY() + " " + entity.getLocation().getBlockZ());
        } else {
            sender.sendMessage("You need to select an entity first.");
        }
        return 0;
    }

    private int clearSelectedLocations(McmeCommandSender sender) {
        RealPlayer player = (RealPlayer) sender;
        player.getSelectedPoints().clear();
        sender.sendMessage("Location selection cleared.");
        return 0;
    }

    private int addSelectedLocation(McmeCommandSender sender, String location) {
        RealPlayer player = (RealPlayer) sender;
        if(location == null) {
            player.getSelectedPoints().add(player.getLocation());
            sender.sendMessage("Added your position to your list of selected locations.");
        } else {
            try {
                Location loc = parseLocation(player.getBukkitPlayer(), location);
                player.getSelectedPoints().add(loc);
                sender.sendMessage("Added ("+ loc.getBlockX()+" "+loc.getBlockY()+" "+loc.getBlockZ()
                                                                    +") to your list of selected locations.");
            } catch(IllegalArgumentException ex) {
                sender.sendMessage("Invalid input! Can't parse location.");
            }
        }
        return 0;
    }

    private int showSelectedLocations(McmeCommandSender sender) {
        sender.sendMessage("Selected Locations:");
        ((BukkitCommandSender)sender).getSelectedPoints().forEach(location -> sender.sendMessage(
                location.getBlockX()+" "+location.getBlockY()+" "+location.getBlockZ()));
        return 0;

    }

}
