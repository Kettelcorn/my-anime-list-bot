package Listeners;

import java.awt.*;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.time.Duration;
import java.util.*;
import java.util.List;

import dev.katsute.mal4j.MyAnimeList;
import dev.katsute.mal4j.anime.Anime;
import dev.katsute.mal4j.anime.AnimeListStatus;
import dev.katsute.mal4j.query.AnimeListUpdate;
import dev.katsute.mal4j.query.UserAnimeListQuery;
import dev.katsute.mal4j.query.UserMangaListQuery;
import dev.katsute.mal4j.user.User;
import dev.katsute.mal4j.user.UserAnimeStatistics;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.example.Main;
import org.jetbrains.annotations.NotNull;

public class MyListeners extends ListenerAdapter {

    private List<Anime> search;
    private List<Anime> shows;
    private MyAnimeList mal;
    private List<List<AnimeListStatus>> animeListStatus;
    private List<String> currentUser;
    private Statement statement;



    public MyListeners(){
        search = new ArrayList<>();
        currentUser = new ArrayList<>();
        animeListStatus = new ArrayList<>();
    }

    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        OptionData option1 = new OptionData(OptionType.STRING, "user", "enter username");
        OptionData option2 = new OptionData(OptionType.STRING, "anime", "enter anime");
        commandData.add(Commands.slash("anime-search", "search for anime")
                .addOptions(option2));
        commandData.add(Commands.slash("anime-update", "update user info")
                .addOptions(option1));
        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    // chooses between mal-search and mal update slash commands
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("anime-search")) {
            event.deferReply().setEphemeral(true).queue();
            mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            String show = event.getOption("anime").getAsString();
            search = mal.getAnime().withQuery(show).search();
            StringSelectMenu.Builder builder = StringSelectMenu.create("select-anime");
            for (Anime anime : search) {
                builder.addOption(anime.getTitle(), anime.getTitle());
            }
            event.getHook().sendMessage("Select the correct show: ").setEphemeral(true).addActionRow(builder.build()).queue();
        }

        if (event.getName().equals("anime-update")) {
            MyAnimeList mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            String user = event.getOption("user").getAsString();
            event.deferReply().addContent("Updating " + user + "'s info, this may take a while")
                    .setEphemeral(true).queue();
            if (currentUser.contains(user)) {
                animeListStatus.remove(currentUser.indexOf(user));
                currentUser.remove(user);
            }
            animeListStatus.add(mal
                    .getUserAnimeListing(user)
                    .withStatus("completed").withLimit(500).search());
            currentUser.add(user);
            event.getHook().sendMessage("Finished updating " + user + "'s info!").setEphemeral(true).queue();

        }
    }

        @Override
        public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event){
            if (event.getComponentId().equals("select-anime")) {
                event.deferReply().setEphemeral(true).queue();
                Anime selectedShow = null;
                for (Anime anime : search) {
                    if (event.getValues().get(0).equals(anime.getTitle())) {
                        selectedShow = anime;
                    }
                }
                System.out.println(selectedShow.getID().toString());
                double scoreTotal = 0.0;
                int totalWatched = 0;
                String hasWatched = "";
                for (List<AnimeListStatus> user : animeListStatus) {
                    for (AnimeListStatus anime : user) {
                        if (anime.getAnime().getID().equals(selectedShow.getID())) {
                            if (anime.getScore().intValue() == 0) {
                                hasWatched += currentUser.get(animeListStatus.indexOf(user));
                            } else {
                                hasWatched += currentUser.get(animeListStatus.indexOf(user)) + ": " +
                                        anime.getScore().toString() + "\n";
                                scoreTotal += anime.getScore().intValue();
                                totalWatched++;
                            }
                        }
                    }
                }

                int id = selectedShow.getID().intValue();
                String url = "https://myanimelist.net/anime/" + id;
                EmbedBuilder embedBuilder = new EmbedBuilder();
                embedBuilder.setTitle(selectedShow.getTitle());
                embedBuilder.setColor(Color.CYAN);
                embedBuilder.setImage(selectedShow.getMainPicture().getLargeURL());
                embedBuilder.addField(url, "Episodes: " + selectedShow.getEpisodes() + "\n\n" + "__Completed__" + "\n" +
                        hasWatched + "\n" + "Average Server Score: " + Math.round(100.0 * scoreTotal / totalWatched) / 100.0, false);
                embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                        event.getMember().getUser().getAvatarUrl());
                event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
                event.getHook().sendMessage("Request complete!").setEphemeral(true).queue();
            }
        }
}
