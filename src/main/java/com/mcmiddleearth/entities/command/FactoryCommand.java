package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.ai.goal.GoalType;
import com.mcmiddleearth.entities.ai.goal.head.*;
import com.mcmiddleearth.entities.api.McmeEntityType;
import com.mcmiddleearth.entities.api.MovementType;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.api.VirtualEntityGoalFactory;
import com.mcmiddleearth.entities.command.argument.FactoryPropertyArgument;
import com.mcmiddleearth.entities.entities.McmeEntity;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.entities.composite.bones.SpeechBalloonLayout;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Horse;
import org.bukkit.util.Vector;

import java.util.UUID;
import java.util.logging.Logger;

import static com.mojang.brigadier.arguments.StringArgumentType.greedyString;

public class FactoryCommand extends McmeEntitiesCommandHandler {

    public FactoryCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
                .requires(sender -> (sender instanceof RealPlayer)
                        && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
                .executes(context -> setFactoryValue(context.getSource(),"show",""))
                .then(HelpfulRequiredArgumentBuilder.argument("property", new FactoryPropertyArgument())
                        .then(HelpfulRequiredArgumentBuilder.argument("value", greedyString())
                                .executes(context -> setFactoryValue(context.getSource(),
                                                                     context.getArgument("property", String.class),
                                                                     context.getArgument("value", String.class)))));
        return commandNodeBuilder;
    }

    private int setFactoryValue(McmeCommandSender sender, String property, String value) {
        BukkitCommandSender player = (BukkitCommandSender) sender;
        VirtualEntityFactory factory = player.getEntityFactory();
        switch (property.toLowerCase()) {
            case "show":
                showFactory(player);
                break;
            case "clear":
                player.setEntityFactory(new VirtualEntityFactory(
                                        new McmeEntityType(McmeEntityType.CustomEntityType.BAKED_ANIMATION),null));
                break;
            case "type":
                McmeEntityType entityType = McmeEntityType.valueOf(value);
                if (entityType != null) {
                    factory.withEntityType(entityType);
                }
                break;
            case "blacklist":
                factory.withBlackList(value.equalsIgnoreCase("true"));
                break;
            case "uniqueid":
                try {
                    factory.withUuid(UUID.fromString(value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse UUID!");
                }
                break;
            case "name":
                factory.withName(value);
                break;
            case "datafile":
                factory.withDataFile(value);
                break;
            case "displayname":
                factory.withDisplayName(value);
                break;
            case "displaynameposition":
                try {
                    factory.withDisplayNamePosition(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse display name position!");
                }
                break;
            case "location":
                try {
                    if(value.equalsIgnoreCase("@p") && (player instanceof RealPlayer)) {
                        factory.withEntityForSpawnLocation((RealPlayer)player);
                    } else {
                        factory.withLocation(parseLocation(((RealPlayer) player).getBukkitPlayer(), value));
                    }
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse location!");
                }
                break;
            case "movementtype":
                try {
                    factory.withMovementType(MovementType.valueOf(value.toUpperCase()));
//Logger.getGlobal().info("Factory: "+factory);
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse movement type");
                }
                break;
            case "goaltype":
                try {
                    getOrCreateGoalFactory(factory).withGoalType(GoalType.valueOf(value.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse goal type");
                }
                break;
            case "loop":
                getOrCreateGoalFactory(factory).withLoop(value.equalsIgnoreCase("true"));
                break;
            case "checkpoints":
                getOrCreateGoalFactory(factory).withCheckpoints(player.getSelectedPoints().toArray(new Location[0]));
                break;
            case "relative_position":
                getOrCreateGoalFactory(factory).withRelativePosition(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                break;
            case "headgoal":
                String[] split = value.split(" ");
                int paramA = 10, paramB = 0, paramC = 0;
                try {
                    if(split.length>1) paramA = Integer.parseInt(split[1]);
                    if(split.length>2) paramB = Integer.parseInt(split[2]);
                    if(split.length>3) paramC = Integer.parseInt(split[3]);
                } catch (NumberFormatException ignore) {}
                VirtualEntityGoalFactory goalFactory = getOrCreateGoalFactory(factory);
                try {
                    HeadGoalType headGoalType = HeadGoalType.valueOf(value.toUpperCase());
                    switch(headGoalType) {
                        case LOOK:
                            goalFactory.getHeadGoals().add(new HeadGoalLook(((RealPlayer)player).getBukkitPlayer().getLocation(),null,paramA));
                            break;
                        case WATCH:
                            goalFactory.getHeadGoals().add(new HeadGoalWatch(player.selectedTargetEntity,null, paramA));
                            break;
                        case WAYPOINT_TARGET:
                            goalFactory.getHeadGoals().add(new HeadGoalWaypointTarget(null, paramA));
                            break;
                        case LOCATION_TARGET:
                            goalFactory.getHeadGoals().add(new HeadGoalLocationTarget(null, paramA));
                            break;
                        case ENTITY_TARGET:
                            goalFactory.getHeadGoals().add(new HeadGoalEntityTarget(null, paramA));
                            break;
                        case STARE:
                            goalFactory.getHeadGoals().add(new HeadGoalStare(paramB,paramC, paramA));
                            break;
                    }
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse head goal type!");
                }
                break;
            case "targetlocation":
                try {
                    getOrCreateGoalFactory(factory).withTargetLocation(parseLocation(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse target location!");
                }
                break;
            case "targetentity":
                McmeEntity target = EntitiesPlugin.getEntityServer().getEntity(value);
                if(target != null) {
                    getOrCreateGoalFactory(factory).withTargetEntity(target);
                } else {
                    sender.sendMessage("Invalid input! Target entity not found!");
                }
                break;
            case "headpitchcenter":
                try {
                    factory.withHeadPitchCenter(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse pitch center!");
                }
                break;
            case "speechballoonlayout":
                try {
                    split = value.split(" ");
                    factory.withSpeechBalloonLayout(new SpeechBalloonLayout(SpeechBalloonLayout.Position.valueOf(split[0]),
                                                                            SpeechBalloonLayout.Width.valueOf(split[1])));
                } catch (IllegalArgumentException | ArrayIndexOutOfBoundsException ex) {
                    sender.sendMessage("Invalid input! Could not parse speech balloon layout!");
                }
                break;
            case "mouth":
                try {
                    factory.withMouth(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse mouth position!");
                }
                break;
            case "manualanimation":
                factory.withManualAnimationControl(value.equalsIgnoreCase("true"));
                break;
            case "headposedelay":
                try {
                    factory.withHeadPoseDelay(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse integer for head pose delay!");
                }
                break;
            case "viewdistance":
                try {
                    factory.withViewDistance(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse integer for viewDistance!");
                }
                break;
            case "maxrotationstep":
                try {
                    factory.withMaxRotationStep(Float.parseFloat(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse float for maxRotationStep!");
                }
                break;
            case "maxrotationstepflight":
                try {
                    factory.withMaxRotationStepFlight(Float.parseFloat(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse float for maxRotationStepFlight!");
                }
                break;
            case "updateinterval":
                try {
                    factory.withUpdateInterval(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse integer for update interval!");
                }
                break;
            case "jumpheight":
                try {
                    factory.withJumpHeight(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse integer for jump height!");
                }
                break;
            case "knockbackbase":
                try {
                    factory.withKnockBackBase(Float.parseFloat(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse float for knockBackBase!");
                }
                break;
            case "knockbackperdamage":
                try {
                    factory.withKnockBackPerDamage(Float.parseFloat(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse float for knockBackPerDamage!");
                }
                break;
            case "saddlepoint":
                try {
                    factory.withSaddlePoint(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse saddle position!");
                }
                break;
            case "sitpoint":
                try {
                    factory.withSitPoint(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse sit point!");
                }
                break;
            case "attackpoint":
                try {
                    factory.withAttackPoint(parseVector(((RealPlayer)player).getBukkitPlayer(), value));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse attack point!");
                }
                break;
            case "attackdelay":
                try {
                    factory.withAttackDelay(Integer.parseInt(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse attack delay!");
                }
                break;
            case "flightlevel":
                goalFactory = getOrCreateGoalFactory(factory);
                try {
                    goalFactory.withFlightLevel(Double.parseDouble(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse double for flight level!");
                }
                break;
            case "attackpitch":
                goalFactory = getOrCreateGoalFactory(factory);
                try {
                    goalFactory.withAttackPitch(Float.parseFloat(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse double for attack pitch!");
                }
                break;
            case "dive":
                goalFactory = getOrCreateGoalFactory(factory);
                try {
                    goalFactory.withDive(Double.parseDouble(value));
                } catch (NumberFormatException ex) {
                    sender.sendMessage("Invalid input! Could not parse double for dive!");
                }
                break;
            case "color":
                try {
                    factory.withHorseColor(Horse.Color.valueOf(value.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse horse color!");
                }
                break;
            case "style":
                try {
                    factory.withHorseStyle(Horse.Style.valueOf(value.toUpperCase()));
                } catch (IllegalArgumentException ex) {
                    sender.sendMessage("Invalid input! Could not parse horse style!");
                }
                break;
            case "saddle":
                factory.withSaddled(value.equalsIgnoreCase("true"));
                break;
            case "dev":
                switch(value) {
                    case "1":
                        factory.withViewDistance(300);
                        factory.withMovementType(MovementType.FLYING);
                        factory.withMaxRotationStepFlight(1);
                        factory.withAttackPoint(new Vector(0,-6,20));
                        getOrCreateGoalFactory(factory).withDive(0.4f);
                        getOrCreateGoalFactory(factory).withFlightLevel(40);
                        getOrCreateGoalFactory(factory).withAttackPitch(32.5f);
                        factory.withAttribute(Attribute.GENERIC_ATTACK_SPEED,0.4);
                        factory.withAttribute(Attribute.GENERIC_FLYING_SPEED,0.6);
        //AttributeInstance attackSpeed = factory.getAttributes().get(Attribute.GENERIC_ATTACK_SPEED);
        //if(attackSpeed!=null) Logger.getGlobal().info("Vfactory command: Attack speed: "+attackSpeed.getBaseValue()+" -> "+attackSpeed.getValue());
                        break;
                    case "2":
                        factory.withViewDistance(300);
                        factory.withMovementType(MovementType.FLYING);
                        factory.withMaxRotationStepFlight(0.7f);
                        factory.withAttackPoint(new Vector(0,-15,40));
                        getOrCreateGoalFactory(factory).withDive(1);
                        getOrCreateGoalFactory(factory).withFlightLevel(70);
                        getOrCreateGoalFactory(factory).withAttackPitch(30);
                        factory.withAttribute(Attribute.GENERIC_ATTACK_SPEED,0.4);
                        factory.withAttribute(Attribute.GENERIC_FLYING_SPEED,0.7);
                        break;
                    default:
                }
                break;
            default:
                sender.sendMessage("Property " + property +" could not be changed.");
                return 0;
        }
        sender.sendMessage(property + " set to " + value + ".");
        return 0;
    }

    private void showFactory(BukkitCommandSender player) {
        VirtualEntityFactory factory = player.getEntityFactory();
        VirtualEntityGoalFactory goalFactory = factory.getGoalFactory();
        player.sendMessage("Entity Factory Settings:");
        player.sendMessage("Entity type: "+factory.getType().name());
        player.sendMessage("Movement type: "+factory.getMovementType().name());
        player.sendMessage("Name: "+factory.getName());
        player.sendMessage("Display name: "+factory.getDisplayName());
        player.sendMessage("Display name position: "+""+factory.getDisplayNamePosition());
        player.sendMessage("Entity type: "+factory.getDataFile());
        player.sendMessage("Use Blacklist: "+""+factory.hasBlackList());
        player.sendMessage("UUID: "+""+factory.getUniqueId());
        player.sendMessage("Entity Location: "+""+factory.getLocation());
        player.sendMessage("Max rotation step: "+""+factory.getMaxRotationStep());
        player.sendMessage("Max rotation step in flight: "+""+factory.getMaxRotationStepFlight());
        player.sendMessage("Update interval in ticks: "+""+factory.getUpdateInterval());
        player.sendMessage("View distance in blocks: "+""+factory.getViewDistance());
        player.sendMessage("Jump height in blocks: "+""+factory.getJumpHeight());
        player.sendMessage("Mouth position: "+""+factory.getMouth());
        player.sendMessage("Entity Location: "+""+factory.getLocation());
        player.sendMessage("Base Knockback: "+""+factory.getKnockBackBase());
        player.sendMessage("Knockback per Damage: "+""+factory.getKnockBackPerDamage());

    }

}
