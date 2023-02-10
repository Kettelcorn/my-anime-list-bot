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
        String token = "MTA3MDkyNjI0MDU2OTA5ODMzMA.Griorb.1czQ4_1eygWo3CCo0LNRFF2khu-oNn36geuJxY";
        JDA jda = JDABuilder.createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(new AnimeListeners());
        runCustomBuilder();
    }

    // create custom team builder bot using token and set up slash command
    public static void runCustomBuilder() {
        String token = "MTA2NjUxNTM4NTI2MTc3MjgwMA.GkJ7WE.4R0FKyqw6lFSZT7wQxOHPGMnaTbQyypkZzGNJk";
        JDA jda = JDABuilder
                .createDefault(token).enableIntents(GatewayIntent.MESSAGE_CONTENT).build();
        jda.addEventListener(new CustomsListeners());

        // slash command for bot
        jda.upsertCommand("custom-builder", "creates ARAM team").setGuildOnly(true).queue();
    }
}




