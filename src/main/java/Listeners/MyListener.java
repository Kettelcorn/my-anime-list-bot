package Listeners;


import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;


public class MyListener extends ListenerAdapter {
    private List<String> members;
    private List<Member> channelPeople;
    private SlashCommandInteractionEvent events;
    private int players;

    private StringSelectInteractionEvent event;
    public MyListener() {
        members = new ArrayList<>();
        players = 0;
        channelPeople = new ArrayList<>();
    }
  
    // when the user types a slash command
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        
        // checks if user is in an audio channel
        if (!event.getMember().getVoiceState().inAudioChannel()) {
            event.reply("You must be in a voice channel to use this bot").setEphemeral(true).queue();
        } else {
            events = event;
            channelPeople = event.getMember().getVoiceState().getChannel().getMembers();
            
            // checks if enough people to play a game
            if (channelPeople.size() < 4) {
                event.reply("Not enough people in voice channel to make game").setEphemeral(true).queue();
            }
            if (event.getName().equals("custom-builder")) {
                selectGameSize("Select the game size");
            }
        }
    }

    
    // when user selects a string option on a drop-down menu
    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        this.event = event;
        
        // if user choose team size
        if (event.getComponentId().equals("chose-game-size")) {
            switch (event.getValues().get(0)) {
                case "2 vs 2":
                    players = 4;
                    break;
                case "3 vs 3":
                    players = 6;
                    break;
                case "4 vs 4":
                    players = 8;
                    break;
                case "5 vs 5":
                    players = 10;
                    break;
            }
            if (players > channelPeople.size()) {
                event.reply("Invalid team size").setEphemeral(true).queue();
            } else {
                chooseOption(event);
            }
        }
        
        // if user chooses a member to exclude
        if (event.getComponentId().equals("select-members")) {
            List<Member> newList = new ArrayList<Member>();
            for (Member person : channelPeople) {
                if (!person.getUser().getName().equals(event.getValues().get(0))) {
                    newList.add(person);
                }
            }
            channelPeople = newList;
            chooseOption(event);
        }
    }

    
    // logic for choosing to either exclude player or build teams
    public void chooseOption(StringSelectInteractionEvent event) {
        if (channelPeople.size() > players) {
            selectName(event);
        } else {
            createTeams();
        }
    }

    
    // creates drop down menu of different game size options
    public void selectGameSize(String message) {
        events.reply(message).setEphemeral(true)
                .addActionRow(
                        StringSelectMenu.create("chose-game-size")
                                .addOption("2 vs 2", "2 vs 2")
                                .addOption("3 vs 3", "3 vs 3")
                                .addOption("4 vs 4", "4 vs 4")
                                .addOption("5 vs 5", "5 vs 5")
                                .build())
                .queue();
    }
    
    
    // creates a drop-down menu with dynamic options based on who is in the voice channel
    // or who has been excluded by the user
    public void selectName(StringSelectInteractionEvent event) {
        StringSelectMenu.Builder builder = StringSelectMenu.create("select-members");
        for (Member loser : channelPeople) {
            builder.addOption(loser.getUser().getName(), loser.getUser().getName());
        }

        event.reply("Chose who is not playing").setEphemeral(true).addActionRow(builder.build()).queue();
    }

    
    // takes list of people, randomizes order, and sends message to discord displaying results
    public void createTeams() {
        event.reply("Setting up teams").setEphemeral(true).queue();
        List<Member> temp = new ArrayList<>(channelPeople);
        Collections.shuffle(temp);
        String team1 = "";
        String team2 = "";
        for (int i = 0; i < temp.size() / 2; i++) {
            team1 += temp.get(i).getUser().getName() + "\n";
            team2 += temp.get(temp.size() - 1 - i).getUser().getName() + "\n";

        }

        //embeds message and sends result to discord message
        EmbedBuilder embedBuilder = new EmbedBuilder();
        embedBuilder.setTitle("Custom teams");
        embedBuilder.setColor(Color.CYAN);
        embedBuilder.addField("__Team 1:__", team1 + "\n\n", false);
        embedBuilder.addField("__Team 2:__", team2, true);
        embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                event.getMember().getUser().getAvatarUrl());
        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}


