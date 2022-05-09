package com.mcmiddleearth.entities.command;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.mcmiddleearth.command.McmeCommandSender;
import com.mcmiddleearth.command.builder.HelpfulLiteralBuilder;
import com.mcmiddleearth.command.builder.HelpfulRequiredArgumentBuilder;
import com.mcmiddleearth.entities.EntitiesPlugin;
import com.mcmiddleearth.entities.Permission;
import com.mcmiddleearth.entities.api.VirtualEntityFactory;
import com.mcmiddleearth.entities.entities.RealPlayer;
import com.mcmiddleearth.entities.exception.InvalidDataException;
import com.mcmiddleearth.entities.exception.InvalidLocationException;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ComponentBuilder;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static com.mojang.brigadier.arguments.StringArgumentType.word;

public class LoadCommand extends McmeEntitiesCommandHandler {

    public LoadCommand(String command) {
        super(command);
    }

    @Override
    protected HelpfulLiteralBuilder createCommandTree(HelpfulLiteralBuilder commandNodeBuilder) {
        commandNodeBuilder
                .requires(sender -> (sender instanceof RealPlayer)
                        && ((RealPlayer) sender).getBukkitPlayer().hasPermission(Permission.USER.getNode()))
                .then(HelpfulRequiredArgumentBuilder.argument("file", word())
                        .executes(context -> loadEntities(context.getSource(), context.getArgument("file", String.class))));
        return commandNodeBuilder;
    }

    private int loadEntities(McmeCommandSender sender, String fileName) {
        File file = new File(EntitiesPlugin.getEntitiesFolder(),fileName+".json");
        Gson gson = EntitiesPlugin.getEntitiesGsonBuilder().create();
        int counter = 0;
        try (JsonReader reader = gson.newJsonReader(new FileReader(file))) {
            reader.beginArray();
            while(reader.hasNext()) {
                VirtualEntityFactory factory = gson.fromJson(reader, VirtualEntityFactory.class);
                EntitiesPlugin.getEntityServer().spawnEntity(factory);
                counter++;
            }
            reader.endArray();
            sender.sendMessage(counter+" entities loaded.");
        } catch (FileNotFoundException e) {
            sender.sendMessage("File not found.");
        } catch (IOException e) {
            sender.sendMessage("File input error.");
        } catch (IllegalArgumentException ex) {
            sender.sendMessage(ex.getMessage());
        } catch (InvalidDataException e) {
            sender.sendMessage("Invalid entity data in file.");
        } catch (InvalidLocationException e) {
            sender.sendMessage("Invalid location data in file.");
        }
        return 0;
    }
}
