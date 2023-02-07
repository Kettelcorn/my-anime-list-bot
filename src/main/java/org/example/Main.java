package org.example;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.Duration;
import java.util.*;

import Listeners.MyListener;
import Listeners.MyListeners;
import dev.katsute.mal4j.MyAnimeList;
import dev.katsute.mal4j.anime.AnimeListStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

    // create bot using token and sets slash commands
    public static void main(String[] args) throws LoginException {
        String token = "MTA3MDkyNjI0MDU2OTA5ODMzMA.Griorb.1czQ4_1eygWo3CCo0LNRFF2khu-oNn36geuJxY";
        JDA jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(new MyListeners());
        runCustomBuilder();
    }

    public static void runCustomBuilder() {
        String token = "MTA2NjUxNTM4NTI2MTc3MjgwMA.GkJ7WE.4R0FKyqw6lFSZT7wQxOHPGMnaTbQyypkZzGNJk";
        JDA jda = JDABuilder
                .createDefault("MTA2NjUxNTM4NTI2MTc3MjgwMA.GkJ7WE.4R0FKyqw6lFSZT7wQxOHPGMnaTbQyypkZzGNJk").enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .build();
        jda.addEventListener(new MyListener());

        // slash command for bot
        jda.upsertCommand("custom-builder", "creates ARAM team").setGuildOnly(true).queue();
    }
}




