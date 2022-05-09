package com.mcmiddleearth.entities.command;

import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.effect.Explosion;
import com.mcmiddleearth.entities.entities.RealPlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.Particle;

import static com.mojang.brigadier.arguments.IntegerArgumentType.integer;
import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class ExplosionCommand extends McmeEntitiesCommandHandler {

    public ExplosionCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
                .requires(sender -> (sender instanceof RealPlayer)
                        && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
                .then(HelpfulRequiredArgumentBuilder.argument("radius", integer())
                    .then(HelpfulRequiredArgumentBuilder.argument("damage", integer())
                        .executes(context -> spawnExplosion(context.getSource(),
                                             context.getArgument("radius", Integer.class),
                                             context.getArgument("damage", Integer.class),
                                             null, null, null))
                        .then(HelpfulRequiredArgumentBuilder.argument("velocity", integer())
                            .then(HelpfulRequiredArgumentBuilder.argument("knockback", integer())
                                .then(HelpfulRequiredArgumentBuilder.argument("particle", word())
                                    .executes(context -> spawnExplosion(context.getSource(),
                                            context.getArgument("radius", Integer.class),
                                            context.getArgument("damage", Integer.class),
                                            context.getArgument("velocity", Integer.class),
                                            context.getArgument("knockback", Integer.class),
                                            context.getArgument("particle", String.class))))))));
        return commandNodeBuilder;
    }

    private int spawnExplosion(McmeCommandSender sender, int radius, int damage, Integer velocity,
                               Integer knockback, String particleName) {
        Explosion explosion = new Explosion(((RealPlayer)sender).getLocation(),radius,damage);
        if(velocity!=null) explosion.setVelocity(velocity/10.0);
        if(knockback!=null) explosion.setKnockback(knockback/10.0);
        if(particleName!=null) {
            try {
                Particle particle = Particle.valueOf(particleName.toUpperCase());
                explosion.setParticle(particle);
            } catch (IllegalArgumentException ex) {
                sender.sendMessage("Could not parse particle name!");
            }
        }
        explosion.addUnaffected(((RealPlayer)sender));
        explosion.setDamager(((RealPlayer)sender));
        sender.sendMessage("Creating explosion...");
        explosion.explode();
        return 0;
    }

}
