package Listeners;

import java.awt.*;
import java.sql.*;
import java.util.*;
import java.util.List;

import dev.katsute.mal4j.MyAnimeList;
import dev.katsute.mal4j.anime.Anime;
import dev.katsute.mal4j.anime.AnimeListStatus;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildReadyEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import org.jetbrains.annotations.NotNull;

public class AnimeListeners extends ListenerAdapter {

    private List<Anime> search;
    private MyAnimeList mal;
    private List<List<AnimeListStatus>> animeListStatus;
    private Guild guild;


    public AnimeListeners(){
        search = new ArrayList<>();
        animeListStatus = new ArrayList<>();
    }


    // sets up slash commands
    @Override
    public void onGuildReady(GuildReadyEvent event) {
        List<CommandData> commandData = new ArrayList<>();
        OptionData option1 = new OptionData(OptionType.STRING, "user", "enter username");
        OptionData option2 = new OptionData(OptionType.STRING, "anime", "enter anime");

        commandData.add(Commands.slash("anime-update", "update user info")
                .addOptions(option1));
        commandData.add(Commands.slash("anime-remove", "remove user info")
                .addOptions(option1));
        commandData.add(Commands.slash("anime-search", "search for anime")
                .addOptions(option2));

        event.getGuild().updateCommands().addCommands(commandData).queue();
    }


    // chooses between mal-search and mal update slash commands
    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        // search for an anime
        if (event.getName().equals("anime-search")) {
            guild = event.getGuild();
            // execute mal query for show
            event.deferReply().setEphemeral(true).queue();
            mal = MyAnimeList.withClientID(System.getenv("MAL_KEY"));
            String show = event.getOption("anime").getAsString();
            search = mal.getAnime().withQuery(show).search();

            // create string select dropdown menu
            StringSelectMenu.Builder builder = StringSelectMenu.create("select-anime");
            for (Anime anime : search) {
                builder.addOption(anime.getTitle(), anime.getTitle());
            }
            event.getHook().sendMessage("Select the correct show: ").setEphemeral(true).addActionRow(builder.build()).queue();
        }

        // update user info in database
        if (event.getName().equals("anime-update")) {
            guild = event.getGuild();
            // get users anime listing
            MyAnimeList mal = MyAnimeList.withClientID(System.getenv("MAL_KEY"));
            String user = event.getOption("user").getAsString();
            event.deferReply().addContent("Updating " + user + "'s info, this may take a while")
                    .setEphemeral(true).queue();
            List<AnimeListStatus> animes = mal.getUserAnimeListing(user).withStatus("completed").withLimit(500).search();

            // update database with user's anime list info
            try{
                Connection connection = DriverManager
                        .getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70",
                        "b376f2add348e8", System.getenv("DB_PASSWORD"));

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
                ResultSet rs = selectOption(connection, "userID", "users", selectedUser);
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
                        preparedStatement2.setInt(3, anime.getScore());
                        preparedStatement2.setString(4,anime.getStatus().toString());
                        preparedStatement2.executeUpdate();
                    }
                }
                animeListStatus.clear();

                // adds current guild to list of guilds for user
                String guilds = "";
                ResultSet resultSet = selectOption(connection, "guild", "users", selectedUser);
                while (resultSet.next()) {
                    guilds = resultSet.getString("guild");
                    if (guilds == null) {
                        guilds = "";
                    }
                    if (!guilds.contains(event.getGuild().getId())) {
                        guilds += event.getGuild().getId() + ",";
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
            guild = event.getGuild();
            try {
                Connection connection = DriverManager
                        .getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70",
                                "b376f2add348e8", System.getenv("DB_PASSWORD"));

                String sql = "SELECT * FROM users";
                Statement stmt = connection.createStatement();
                ResultSet result = stmt.executeQuery(sql);
                String selectedUser = "";
                while (result.next()) {
                    String name = result.getString("username");
                    if (name.equals(event.getOption("user").getAsString())) {
                        selectedUser = name;
                        break;
                    }
                }

                if (selectedUser.equals("")) {
                    event.reply("Invalid name, user is not in our database!").setEphemeral(true).queue();
                } else {
                    ResultSet resultSet = selectOption(connection, "guild", "users", selectedUser);
                    String guilds = "";
                    while (resultSet.next()) {
                        guilds = resultSet.getString("guild");
                        if (guilds.contains(event.getGuild().getId().toString())) {
                            guilds = guilds.replace(event.getGuild().getId() + ",", "");
                        }
                    }

                    PreparedStatement preparedStatement1 = connection.prepareStatement("UPDATE users SET guild = ? WHERE username = ?");
                    preparedStatement1.setString(1, guilds);
                    preparedStatement1.setString(2, selectedUser);
                    preparedStatement1.executeUpdate();

                    if (guilds.equals("")) {
                        PreparedStatement preparedStatement2 = connection.prepareStatement("DELETE FROM users WHERE username = ?");
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


    // when user selects an anime, find the correct show and send to be built
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
            executeEmbedSelect(createEmbed(selectedShow), event);
        }
    }


    // extracts show ID out of url
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getMessage().getContentRaw().contains("https://myanimelist.net/anime/")) {
            String url = event.getMessage().getContentRaw();
            event.getMessage().delete().queue();

            int startIndex = "https://myanimelist.net/anime/".length();
            int endIndex = url.indexOf("/", startIndex);
            String numberString = url.substring(startIndex, endIndex);
            int number = Integer.parseInt(numberString);

            mal = MyAnimeList.withClientID(System.getenv("MAL_KEY"));
            Anime selectedShow = mal.getAnime(number);
            executeEmbedMessage(createEmbed(selectedShow), event);
        }
    }


    //creates an embed message with the given show
    public EmbedBuilder createEmbed(Anime selectedShow) {
        if (selectedShow == null) {
            throw new IllegalArgumentException("Invalid show, does not exist in database");
        }

        // gets user information on who has watched given show
        double scoreTotal = 0.0;
        int totalWatched = 0;
        String hasWatched = "";
        String completed = "";

        try {
            Connection connection = DriverManager
                    .getConnection("jdbc:mysql://us-cdbr-east-06.cleardb.net:3306/heroku_1e6b905fd709b70",
                            "b376f2add348e8", System.getenv("DB_PASSWORD"));
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
                        List<String> guildList = Arrays.asList(guilds.split(","));
                        String user = resultSet.getString("username");
                        String guildID = guild.getId();
                        System.out.println("outside if");
                        for (String id : guildList) {
                            if (id.equals(guild.getId())) {
                                int score = result.getInt("showScore");

                                // creates string to present users who have watched show
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
            }
        } catch (SQLException e) {
            System.out.println("Error connecting to SQLite database");
            e.printStackTrace();
        }

        // constructs string to present information
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


    //sends embed message from string select
    public void executeEmbedSelect(EmbedBuilder embedBuilder, StringSelectInteractionEvent event) {
        embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                event.getMember().getUser().getAvatarUrl());
        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
        event.getHook().sendMessage("Request complete!").setEphemeral(true).queue();
    }


    //sends embed message from url message
    public void executeEmbedMessage(EmbedBuilder embedBuilder ,MessageReceivedEvent event) {
        embedBuilder.setFooter("Request made by " + event.getMember().getUser().getName(),
                event.getMember().getUser().getAvatarUrl());
        event.getChannel().sendMessageEmbeds(embedBuilder.build()).queue();
    }


    // helper method for redundant sql query
    public ResultSet selectOption(Connection connection, String column, String table, String value) {
        ResultSet resultSet = null;
        try {
            PreparedStatement ps = connection.prepareStatement("SELECT " + column + " FROM " + table + " WHERE username = ?");
            ps.setString(1, value);
            resultSet = ps.executeQuery();
        } catch (SQLException e) {
            System.out.println("Error connecting to SQLite database");
            e.printStackTrace();
        }
        return resultSet;
    }
}
