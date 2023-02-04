package Listeners;

import java.awt.*;
import java.net.URL;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Connection;
import java.util.*;
import java.util.List;

import dev.katsute.mal4j.MyAnimeList;
import dev.katsute.mal4j.anime.Anime;
import dev.katsute.mal4j.anime.AnimeListStatus;
import dev.katsute.mal4j.query.UserAnimeListQuery;
import dev.katsute.mal4j.query.UserMangaListQuery;
import dev.katsute.mal4j.user.User;
import dev.katsute.mal4j.user.UserAnimeStatistics;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectInteraction;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

public class MyListeners extends ListenerAdapter {

    private List<Anime> search;
    private List<Anime> shows;
    private MyAnimeList mal;
    private List<AnimeListStatus> animeListStatus;



    public MyListeners(){
        search = new ArrayList<>();
        animeListStatus = new ArrayList<>();
    }
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("mal-bot")) {
            mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            TextInput show = TextInput.create("Search", "Search", TextInputStyle.SHORT)
                    .setPlaceholder("Enter show here").build();
            Modal modal = Modal.create("modmail", "MyAnimeListBot").addActionRow(show).build();
            event.replyModal(modal).queue();
            animeListStatus = mal.getUserAnimeListing("Kettelcorn").withStatus("completed").withLimit(500).search();

            // access to MySQL database
            try {
                Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/mal-users", "root", "Wavedash420$");

                Statement statement = connection.createStatement();

                int result = statement.executeUpdate("INSERT INTO users (idusers, user) VALUES (1, 'Plardwich')");

                ResultSet resultSet = statement.executeQuery("select * from users");

                while (resultSet.next()) {
                    System.out.println(resultSet.getInt("idusers"));
                    System.out.println(resultSet.getString("user"));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("modmail")) {
            String show = event.getValue("Search").getAsString();
            search = mal.getAnime().withQuery(show).search();


            StringSelectMenu.Builder builder = StringSelectMenu.create("select-anime");
            for (Anime anime: search) {
                builder.addOption(anime.getTitle(), anime.getTitle());
            }
            event.reply("Select the correct show: ").setEphemeral(true).addActionRow(builder.build()).queue();
        }
    }
    @Override
    public void onStringSelectInteraction(@NotNull StringSelectInteractionEvent event) {
        if (event.getComponentId().equals("select-anime")) {
            Anime selectedShow = null;
            for (Anime anime : search) {
                if (event.getValues().get(0).equals(anime.getTitle())) {
                    selectedShow = anime;
                }
            }
            String hasWatched = "not";
            for (AnimeListStatus listStatus : animeListStatus) {
                if (listStatus.getAnime().getID().equals(selectedShow.getID())) {
                    hasWatched = "";
                }
            }

            int id = selectedShow.getID().intValue();
            String url = "https://myanimelist.net/anime/" + id;
            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(selectedShow.getTitle());
            embedBuilder.setColor(Color.CYAN);
            embedBuilder.setImage(selectedShow.getMainPicture().getLargeURL());
            embedBuilder.addField(url, "Episodes: " + selectedShow.getEpisodes() + "\n" +
                      "Kettelcorn has " + hasWatched + " watched this show", false);
            embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                    event.getMember().getUser().getAvatarUrl());
            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        }
    }

}
