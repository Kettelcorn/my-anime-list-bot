package Listeners;

import java.util.*;
import dev.katsute.mal4j.MyAnimeList;
import dev.katsute.mal4j.anime.Anime;
import dev.katsute.mal4j.anime.AnimeListStatus;
import dev.katsute.mal4j.query.UserMangaListQuery;
import dev.katsute.mal4j.user.User;
import dev.katsute.mal4j.user.UserAnimeStatistics;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

public class MyListeners extends ListenerAdapter {

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.getName().equals("mal-bot")) {

            TextInput show = TextInput.create("Search", "Search", TextInputStyle.SHORT)
                    .setPlaceholder("Enter show here").build();
            Modal modal = Modal.create("modmail", "MyAnimeListBot").addActionRow(show).build();
            event.replyModal(modal).queue();
        }
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (event.getModalId().equals("modmail")) {
            String show = event.getValue("Search").getAsString();
            MyAnimeList mal = MyAnimeList.withClientID("ed63f8418f1cdf0c626aae8618705f15");
            List<Anime> search = mal.getAnime().withQuery(show).search();
            StringSelectMenu.Builder builder = StringSelectMenu.create("select-anime");
            for (Anime anime: search) {
                builder.addOption(anime.getTitle(), anime.getTitle());
            }
            event.reply("Select the correct show: ").setEphemeral(true).addActionRow(builder.build()).queue();
        }

    }

}
