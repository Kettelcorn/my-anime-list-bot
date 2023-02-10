package Listeners;

import java.awt.*;
import java.net.URL;
import java.sql.*;
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
    private GuildReadyEvent event;


    public MyListeners(){
        search = new ArrayList<>();
        currentUser = new ArrayList<>();
        animeListStatus = new ArrayList<>();
    }

    // sets up slash commands
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        this.event = event;
        List<CommandData> commandData = new ArrayList<>();
        OptionData option1 = new OptionData(OptionType.STRING, "user", "enter username");
        OptionData option2 = new OptionData(OptionType.STRING, "anime", "enter anime");
        commandData.add(Commands.slash("anime-update", "update user info")
                .addOptions(option1));
        commandData.add(Commands.slash("anime-remove", "remove user info")
                .addOptions(option1));
        commandData.add(Commands.slash("anime-search", "search for anime")
                .addOptions(option2));
        ;
        event.getGuild().updateCommands().addCommands(commandData).queue();
    }

    // chooses between mal-search and mal update slash commands
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {

        // search for an anime
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

        // update user info in database
        if (event.getName().equals("anime-update")) {
            MyAnimeList mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            String user = event.getOption("user").getAsString();
            event.deferReply().addContent("Updating " + user + "'s info, this may take a while")
                    .setEphemeral(true).queue();
            List<AnimeListStatus> animes = mal.getUserAnimeListing(user).withStatus("completed").withLimit(500).search();
            try{
                Connection connection = DriverManager
                        .getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70",
                        "b376f2add348e8", "6f63cbc1");
                String sql = "SELECT * FROM users";
                Statement stmt = connection.createStatement();
                ResultSet result = stmt.executeQuery(sql);

                String selectedUser = "";
                while (result.next()) {
                    String name = result.getString("username");
                    if (name.equals(user)) {
                        selectedUser = name;
                    }
                }
                if (selectedUser.equals("")) {
                    selectedUser = user;
                    PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users (username) VALUES (?)");
                    preparedStatement.setString(1, selectedUser);
                    preparedStatement.executeUpdate();
                }

                PreparedStatement ps = connection.prepareStatement("SELECT userID FROM users WHERE username = ?");
                ps.setString(1, selectedUser);
                ResultSet rs = ps.executeQuery();

                int userID = -1;
                while (rs.next()) {
                    userID = rs.getInt("userID");
                }

                PreparedStatement ps2 = connection.prepareStatement("DELETE FROM showinfo WHERE userID = ?");
                ps2.setInt(1, userID);
                ps2.executeUpdate();

                animeListStatus.add(animes);
                for (List<AnimeListStatus> animeList : animeListStatus) {
                    for (AnimeListStatus anime : animeList) {
                        PreparedStatement preparedStatement2 = connection
                                .prepareStatement("INSERT INTO showinfo (userID, showID, showScore, showStatus) " +
                                        "VALUES (?, ?, ?, ?)");
                        preparedStatement2.setInt(1, userID);
                        preparedStatement2.setInt(2, anime.getAnime().getID().intValue());
                        preparedStatement2.setInt(3, anime.getScore().intValue());
                        preparedStatement2.setString(4,anime.getStatus().toString());
                        preparedStatement2.executeUpdate();
                    }
                }

                animeListStatus.clear();

                String guilds = "";
                PreparedStatement preparedStatement4 = connection.prepareStatement("SELECT guild FROM users WHERE username = ?");
                preparedStatement4.setString(1, selectedUser);
                ResultSet resultSet = preparedStatement4.executeQuery();

                while (resultSet.next()) {
                    guilds = resultSet.getString("guild");
                    if (guilds == null) {
                        guilds = "";
                    }
                    if (!guilds.contains(event.getGuild().getId().toString())) {
                        guilds += event.getGuild().getId().toString() + ",";
                    }
                }

                PreparedStatement preparedStatement5 = connection.prepareStatement("UPDATE users SET guild = ? WHERE userID = ?");
                preparedStatement5.setString(1, guilds);
                preparedStatement5.setInt(2, userID);
                preparedStatement5.executeUpdate();

            } catch (SQLException e) {
                System.out.println("Error connecting to SQLite database");
                e.printStackTrace();
            }
            event.getHook().sendMessage("Finished updating " + user + "'s info!").setEphemeral(true).queue();
        }

        // remove user from database in specific guild
        if (event.getName().equals("anime-remove")) {
            try {
                Connection connection = DriverManager
                        .getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70",
                                "b376f2add348e8", "6f63cbc1");

                String sql = "SELECT * FROM users";
                Statement stmt = connection.createStatement();
                ResultSet result = stmt.executeQuery(sql);

                String selectedUser = "";
                while (result.next()) {
                    String name = result.getString("user");
                    if (name.equals(event.getOption("user").getAsString())) {
                        selectedUser = name;
                        break;
                    }
                }
                if (selectedUser.equals("")) {
                    event.reply("Invalid name, user is not in our database!").setEphemeral(true).queue();
                } else {
                    PreparedStatement preparedStatement = connection.prepareStatement("SELECT guilds FROM users WHERE user = ?");
                    preparedStatement.setString(1, selectedUser);
                    ResultSet resultSet = preparedStatement.executeQuery();

                    String guilds = "";
                    while (resultSet.next()) {
                        guilds = resultSet.getString("guilds");
                        if (guilds.contains(event.getGuild().getId().toString())) {
                            guilds = guilds.replace(event.getGuild().getId().toString() + ",", "");
                        }
                    }

                    PreparedStatement preparedStatement1 = connection.prepareStatement("UPDATE users SET guilds = ? WHERE user = ?");
                    preparedStatement1.setString(1, guilds);
                    preparedStatement1.setString(2, selectedUser);
                    preparedStatement1.executeUpdate();

                    if (guilds.equals("")) {
                        PreparedStatement preparedStatement2 = connection.prepareStatement("DELETE FROM users WHERE user = ?");
                        preparedStatement2.setString(1, selectedUser);
                        preparedStatement2.executeUpdate();
                    }
                    event.reply("User deleted from our database!").setEphemeral(true).queue();
                }
            } catch (SQLException e) {
                System.out.println("Error connecting to SQLite database");
                e.printStackTrace();
            }
        }
    }

        // when user selects an anime, find the correct show and send to be build
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
                String guild = event.getGuild().getId().toString();
                executeEmbedSelect(createEmbed(selectedShow, guild), event);
            }
        }

        // extracts show out of url
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String guild = event.getGuild().getId().toString();
        if (event.getMessage().getContentRaw().contains("https://myanimelist.net/anime/")) {
            String url = event.getMessage().getContentRaw();
            event.getMessage().delete().queue();

            int startIndex = "https://myanimelist.net/anime/".length();
            int endIndex = url.indexOf("/", startIndex);
            String numberString = url.substring(startIndex, endIndex);
            int number = Integer.parseInt(numberString);
            mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            Anime selectedShow = mal.getAnime(number);

            executeEmbedMessage(createEmbed(selectedShow, guild), event);
        }
    }

    //creates an embeded message with the given show
        public EmbedBuilder createEmbed(Anime selectedShow, String guild) {
            double scoreTotal = 0.0;
            int totalWatched = 0;
            String hasWatched = "";
            String completed = "";
            List<String> showList = new ArrayList<>();
            List<String> scoreList = new ArrayList<>();
            try {
                Connection connection = DriverManager
                        .getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70",
                                "b376f2add348e8", "6f63cbc1");
                PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM showinfo WHERE showID = ?");
                preparedStatement.setInt(1, selectedShow.getID().intValue());
                ResultSet result = preparedStatement.executeQuery();

                while (result.next()) {
                        int userID = result.getInt("userID");
                        PreparedStatement preparedStatement2 = connection.prepareStatement("SELECT * FROM users WHERE userID = ?");
                        preparedStatement2.setInt(1, userID);
                        ResultSet resultSet = preparedStatement2.executeQuery();

                        while (resultSet.next()) {
                            String guilds = resultSet.getString("guild");
                            String user = resultSet.getString("username");
                            System.out.println("outside if");
                            if (guilds.contains(event.getGuild().getId().toString())) {
                                String status = result.getString("showStatus");
                                int score = result.getInt("showScore");

                                completed = "\n\n" + "__Completed__" + "\n";
                                if (score == 0) {
                                    hasWatched += user + "\n";
                                } else {
                                    hasWatched += user + ": " +
                                                score + "\n";
                                    scoreTotal += score;
                                    totalWatched++;
                                }
                                System.out.println("Added " + user);
                            }

                        }
                }
            } catch (SQLException e) {
                System.out.println("Error connecting to SQLite database");
                e.printStackTrace();
            }

            String serverScore = "\n" + "Average Server Score: " +
                    Math.round(100.0 * scoreTotal / totalWatched) / 100.0 + "";
            if (totalWatched == 0) {
                serverScore = "";
            }
            String episodes = selectedShow.getEpisodes() + "";
            if (episodes.equals("0")) {
                episodes = "Not finished yet";
            }

            int id = selectedShow.getID().intValue();
            String url = "https://myanimelist.net/anime/" + id;

            EmbedBuilder embedBuilder = new EmbedBuilder();
            embedBuilder.setTitle(selectedShow.getTitle());
            embedBuilder.setColor(Color.CYAN);
            embedBuilder.setImage(selectedShow.getMainPicture().getLargeURL());
            embedBuilder.addField(url, "Episodes: " + episodes + completed +
                    hasWatched + serverScore, false);
            return embedBuilder;
        }

        //sends embeded message from string select
        public void executeEmbedSelect(EmbedBuilder embedBuilder, StringSelectInteractionEvent event) {
            embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                    event.getMember().getUser().getAvatarUrl());
            event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
            event.getHook().sendMessage("Request complete!").setEphemeral(true).queue();
        }

        //sends embeded message from url message
    public void executeEmbedMessage(EmbedBuilder embedBuilder ,MessageReceivedEvent event) {
        embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                event.getMember().getUser().getAvatarUrl());
        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }
}
