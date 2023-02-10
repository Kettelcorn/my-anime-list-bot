package org.example;

import javax.security.auth.login.LoginException;


import Listeners.AnimeListeners;
import Listeners.CustomsListeners;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;

public class Main {

    // create mal bot using token
    public static void main(String[] args) throws LoginException {
        JDA jda = JDABuilder.createDefault(System.getenv("ANIME_DISCORD")).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(new AnimeListeners());
        runCustomBuilder();
    }

    // create custom team builder bot using token and set up slash command
    public static void runCustomBuilder() {
        JDA jda = JDABuilder
                .createDefault(System.getenv("CUSTOM_DISCORD")).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(new CustomsListeners());

        // slash command for bot
        jda.upsertCommand("custom-builder", "creates ARAM team").setGuildOnly(true).queue();
    }
}




