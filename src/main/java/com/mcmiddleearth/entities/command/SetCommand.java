package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.goal.GoalType;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.command.argument.AttributeTypeArgument;
import com.mcmiddleearth.entities.command.argument.GoalTypeArgument;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.VirtualEntity;
import com.mcmiddleearth.entities.entities.simple.SimpleHorse;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class SetCommand extends McmeEntitiesCommandHandler {

    public SetCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
                .requires(sender -> (sender instanceof RealPlayer)
                        && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
                .then(HelpfulLiteralBuilder.literal("goal")
                    .then(HelpfulRequiredArgumentBuilder.argument("type", new GoalTypeArgument())
                        .executes(context -> setGoal(context.getSource(), context.getArgument("type", String.class), false))
                        .then(HelpfulLiteralBuilder.literal("loop")
                            .executes(context -> setGoal(context.getSource(), context.getArgument("type", String.class), true)))))
                .then(HelpfulLiteralBuilder.literal("displayname")
                    .then(HelpfulRequiredArgumentBuilder.argument("displayname", word())
                        .executes(context -> setDisplayName(context.getSource(), context.getArgument("displayname", String.class)))))
                .then(HelpfulLiteralBuilder.literal("item")
                    .then(HelpfulRequiredArgumentBuilder.argument("slot", word())
                        .then(HelpfulRequiredArgumentBuilder.argument("item", word())
                            .executes(context -> setItem(context.getSource(), context.getArgument("slot",String.class),
                                                                              context.getArgument("item",String.class))))))
                .then(HelpfulLiteralBuilder.literal("attribute")
                    .then(HelpfulRequiredArgumentBuilder.argument("type", new AttributeTypeArgument())
                        .then(HelpfulRequiredArgumentBuilder.argument("value", word())
                            .executes(context -> setAttribute(context.getSource(),context.getArgument("type",String.class),
                                                                                  context.getArgument("value", String.class))))));
        return commandNodeBuilder;
    }

    public int setItem(McmeCommandSender sender, String slotName, String itemMaterial) {
        McmeEntity entity = ((BukkitCommandSender)sender).getSelectedEntities().stream().findFirst().orElse(null);
        if(entity instanceof VirtualEntity) {
            if(slotName.equalsIgnoreCase("saddle")) {
                if(entity instanceof SimpleHorse) {
                    if(itemMaterial.equalsIgnoreCase("saddle")) {
                        ((SimpleHorse)entity).setSaddled(true);
                        sender.sendMessage("Entity saddled.");
                    } else {
                        ((SimpleHorse)entity).setSaddled(false);
                        sender.sendMessage("Entity unsaddled.");
                    }
                } else {
                    sender.sendMessage("Not implemented for custom entities.");
                }
            } else {
                EquipmentSlot slot = EquipmentSlot.HAND;
                try {
                    slot = EquipmentSlot.valueOf(slotName.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Can't parse equipment slot. Using main hand.");
                }
                Material material = Material.LEATHER_CHESTPLATE;
                try {
                    material = Material.valueOf(itemMaterial.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Can't parse item material. Using leather chest plate.");
                }
                ItemStack item = new ItemStack(material);
                ((VirtualEntity) entity).setEquipment(slot, item);
                sender.sendMessage(slot.name().toLowerCase() + " item set to " + material.name().toLowerCase() + ".");
            }
        } else {
            sender.sendMessage("You need to select an entity first!");
        }
        return 0;
    }

    private int setAttribute(McmeCommandSender sender, String type, String valueString) {
        try {
            double value = Double.parseDouble(valueString);
            McmeEntity entity = ((BukkitCommandSender)sender).getSelectedEntities().stream().findFirst().orElse(null);
            if(entity instanceof VirtualEntity) {
                Attribute attributeType = Attribute.valueOf(type.toUpperCase());
                AttributeInstance attribute = ((VirtualEntity)entity).getAttribute(attributeType);
//Logger.getGlobal().info("attribute instance set command: "+attribute);
                if(attribute == null) {
                    ((VirtualEntity)entity).registerAttribute(attributeType);
                    attribute = ((VirtualEntity)entity).getAttribute(attributeType);
                }
                if(attribute!=null) {
                    attribute.setBaseValue(value);
                    sender.sendMessage("Attribute '"+type+"' set to "+value);
                } else {
                    sender.sendMessage("Attribute not found!");
                }
            } else {
                sender.sendMessage("You need to select an entity first!");
            }
        } catch(NumberFormatException ex) {
            sender.sendMessage("Attribute value must be  a decimal number!");
        } catch(IllegalArgumentException ex) {
            sender.sendMessage("Invalid attribute type!");
        }
        return 0;
    }

    private int setDisplayName(McmeCommandSender source, String displayname) {
        McmeEntity entity = ((BukkitCommandSender)source).getSelectedEntities().stream().findFirst().orElse(null);
        if(entity instanceof VirtualEntity) {
            ((VirtualEntity)entity).setDisplayName(displayname);
            source.sendMessage("Set display name to: "+displayname);
        } else {
            source.sendMessage("You need to select an entity first!");
        }
        return 0;
    }


    private int setGoal(McmeCommandSender sender, String type, boolean loop) {
        try {
            RealPlayer player = (RealPlayer) sender;
            McmeEntity mcmeEntity = player.getSelectedEntities().stream().findFirst().orElse(null);
            if(!(mcmeEntity instanceof VirtualEntity)) {
                if(mcmeEntity == null) {
                    sender.sendMessage("You need to select at least one entity to apply the goal to.");
                } else {
                    sender.sendMessage("Goals can be applied at virtual entities only.");
                }
                return 0;
            }
            VirtualEntity entity = (VirtualEntity) mcmeEntity;
            GoalType goalType = GoalType.valueOf(type.toUpperCase());
            VirtualEntityGoalFactory factory = new VirtualEntityGoalFactory(goalType)
                    .withLoop(loop)
                    .withTargetEntity(player.getSelectedTargetEntity())
                    .withTargetLocation(player.getSelectedPoints().stream().findFirst().orElse(null))
                    .withCheckpoints(player.getSelectedPoints().toArray(new Location[0]));
            entity.setGoal(factory.build(entity));
        } catch (IllegalArgumentException ex) {
            sender.sendMessage("Invalid goal type!");
        } catch (InvalidLocationException e) {
            sender.sendMessage("Invalid location. All location must be same world!");
        } catch (InvalidDataException e) {
            sender.sendMessage(e.getMessage());
        }
        return 0;
        /*
        World world = ((RealPlayer) sender).getBukkitPlayer().getLocation().getWorld();
        Location[] checkpoints = new Location[]{new Location(world, -10, 20, 3),
                                                new Location(world, 10, 20, 3),
                                                new Location(world, 10, 20, 13),
                                                new Location(world, -10, 20, 13)};
        VirtualEntity entity = (VirtualEntity) ((RealPlayer) sender).getSelectedEntities().iterator().next();
        Goal goal = new GoalLocationTargetFollowCheckpoints(GoalType.FOLLOW_CHECKPOINTS, entity,
                                              new WalkingPathfinder(entity),checkpoints,true);
        entity.setGoal(goal);
        return 0;*/
    }

}
