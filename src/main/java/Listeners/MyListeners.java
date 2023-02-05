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
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
    private Map<String, List<AnimeListStatus>> animeListStatus;
    private Statement statement;



    public MyListeners(Map<String, List<AnimeListStatus>> animeListStatus){
        search = new ArrayList<>();
        this.animeListStatus = animeListStatus;
    }

    // chooses between mal-search and mal update slash commands
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("mal-search")) {
            mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            TextInput show = TextInput.create("Search", "Search", TextInputStyle.SHORT)
                    .setPlaceholder("Enter show here").build();
            Modal modal = Modal.create("modmail", "MyAnimeListBot").addActionRow(show).build();
            event.replyModal(modal).queue();

            // access to MySQL database
        }
        if (event.getName().equals("mal-update")) {
            event.reply("Updating user info, this may take a few minutes...").setEphemeral(true).queue();
            animeListStatus = Main.updateMal();
            MessageChannel messageChannel = event.getMessageChannel();
            messageChannel.sendMessage("Update complete!").queue();
        }
    }


        @Override
        public void onModalInteraction(@NotNull ModalInteractionEvent event){
            if (event.getModalId().equals("modmail")) {
                String show = event.getValue("Search").getAsString();
                search = mal.getAnime().withQuery(show).search();


                StringSelectMenu.Builder builder = StringSelectMenu.create("select-anime");
                for (Anime anime : search) {
                    builder.addOption(anime.getTitle(), anime.getTitle());
                }
                event.reply("Select the correct show: ").setEphemeral(true).addActionRow(builder.build()).queue();
            }
        }
        @Override
        public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event){
            if (event.getComponentId().equals("select-anime")) {
                Anime selectedShow = null;
                for (Anime anime : search) {
                    if (event.getValues().get(0).equals(anime.getTitle())) {
                        selectedShow = anime;
                    }
                }

                double scoreTotal = 0.0;
                int totalWatched = 0;
                String hasWatched = "";
                for (Map.Entry<String, List<AnimeListStatus>> entry : animeListStatus.entrySet()) {
                    for (AnimeListStatus show : entry.getValue()) {
                        if (show.getAnime().getID().equals(selectedShow.getID())) {
                            hasWatched += entry.getKey() + ": " +
                                    show.getScore().toString() + "\n";
                            scoreTotal += show.getScore().intValue();
                            totalWatched++;
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
                        hasWatched + "\n\n" + "Average Score: " + Math.round(100.0 * scoreTotal / totalWatched) / 100.0, false);
                embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                        event.getMember().getUser().getAvatarUrl());
                event.reply("Building info...").setEphemeral(true).queue();
                event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            }
        }
}
