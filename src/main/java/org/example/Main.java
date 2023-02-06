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
        // connect to database
        /*try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70", "b376f2add348e8", "6f63cbc1");
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select * from users");

            // retrieve data from MAL and add to map
            while (resultSet.next()) {
                System.out.println(resultSet.getInt("idusers"));
                System.out.println(resultSet.getString("user"));
                MyAnimeList mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
                animeListStatus.put(resultSet.getString("user"), mal.
                        getUserAnimeListing(resultSet.getString("user")).
                        withStatus("completed").withLimit(500).search());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }*/



