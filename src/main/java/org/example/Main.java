package org.example;

import javax.security.auth.login.LoginException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import Listeners.MyListeners;
import dev.katsute.mal4j.MyAnimeList;
import dev.katsute.mal4j.anime.AnimeListStatus;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {


    public static void main(String[] args) throws LoginException {
        List<List<AnimeListStatus>> animeListStatus = new ArrayList<>();
        List<String> users = new ArrayList<>();
        String token = "MTA3MDkyNjI0MDU2OTA5ODMzMA.Griorb.1czQ4_1eygWo3CCo0LNRFF2khu-oNn36geuJxY";
        try {
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mal-users", "root", "Wavedash420$");

            Statement statement = connection.createStatement();

            ResultSet resultSet = statement.executeQuery("select * from users");


            while (resultSet.next()) {
                System.out.println(resultSet.getInt("idusers"));
                System.out.println(resultSet.getString("user"));
                MyAnimeList mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
                users.add(resultSet.getString("user"));
                animeListStatus.add(mal.getUserAnimeListing(resultSet.getString("user")).withStatus("completed").withLimit(500).search());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        JDA jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(new MyListeners(animeListStatus, users));
        jda.upsertCommand("mal-bot", "wakes up mal bot").setGuildOnly(true).queue();
    }
}