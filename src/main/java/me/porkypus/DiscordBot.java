package me.porkypus;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;


public class DiscordBot extends ListenerAdapter {

    boolean running = false;
    int turn = 0; // 0 is red turn and 1 is blue turn
    int guesses = 1;

    static List<String> wordList = new ArrayList<>();
    static List<Button> spymasterButtonList = new ArrayList<>();
    static List<Button> playerButtonList = new ArrayList<>();

    static List<User> pool = new ArrayList<>();
    static List<User> spymaster = new ArrayList<>();
    static List<User> red = new ArrayList<>();
    static List<User> blue = new ArrayList<>();

    int redRemaining = 9;
    int blueRemaining = 8;

    public static void main(String[] args) throws LoginException {

        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"))
                .addEventListeners(new DiscordBot())
                .setActivity(Activity.playing("Type codies to get started"))
                .build();

        jda.upsertCommand("codies", "Type /codies").queue();
        jda.upsertCommand("start", "Type /start").queue();

        try {
            BufferedReader br = new BufferedReader(new FileReader("words.txt"));
            String line;
            while((line = br.readLine()) != null) {
                wordList.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) {
            return;
        }
        Message msg = event.getMessage();

        if (msg.getContentRaw().equals("!join")) {
            msg.delete().queue();
            if (pool.contains(event.getAuthor())) {
                event.getChannel().sendMessage("Join only once idiot").queue();
            } else {
                    pool.add(event.getAuthor());
            }
        }
    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("codies")) {
            if (running) {
                event.reply("An instance of codies has already been started").queue();
            }
            running = true;
            pool = new ArrayList<>();
            event.reply("Spymasters: " + spymaster + "\nRed: " + red + "\nBlue: " + blue)
                    .addActionRow(Button.primary("random", "Randomise Teams"))
                    .queue();
        }

        if (event.getName().equals("start")) {
            if (!running) {
                event.reply("```Please initiate teams using /codies first").queue();
            } else {
                redRemaining = 9;
                blueRemaining = 8;

                spymasterButtonList = new ArrayList<>();
                playerButtonList = new ArrayList<>();

                Guild guild = event.getGuild();
                assert guild != null;
                TextChannel textChannel = guild.getTextChannelsByName("spymasters", true).get(0);

                Collections.shuffle(wordList);

                for (int i = 0; i < 25; i++) {
                    if (i < 8) {
                        spymasterButtonList.add(Button.primary(wordList.get(i), wordList.get(i)).asDisabled());
                    } else if (i < 17) {
                        spymasterButtonList.add(Button.danger(wordList.get(i), wordList.get(i)).asDisabled());
                    } else {
                        spymasterButtonList.add(Button.secondary(wordList.get(i), wordList.get(i)).asDisabled());
                    }
                }

                Collections.shuffle(spymasterButtonList);

                for (Button button : spymasterButtonList) {
                    playerButtonList.add(Button.success(button.getId() + "x", button.getLabel()));
                }


                List<ActionRow> actionRows = new ArrayList<>();
                actionRows.add(ActionRow.of(spymasterButtonList.subList(0, 5)));
                actionRows.add(ActionRow.of(spymasterButtonList.subList(5, 10)));
                actionRows.add(ActionRow.of(spymasterButtonList.subList(10, 15)));
                actionRows.add(ActionRow.of(spymasterButtonList.subList(15, 20)));
                actionRows.add(ActionRow.of(spymasterButtonList.subList(20, 25)));

                textChannel.sendMessage("True Layout")
                        .setActionRows(actionRows)
                        .queue();

                event.reply("```Red: " + redRemaining + " Blue: " + blueRemaining + "```")
                        .addActionRow(
                                playerButtonList.subList(0, 5))
                        .addActionRow(
                                playerButtonList.subList(5, 10))
                        .addActionRow(
                                playerButtonList.subList(10, 15))
                        .addActionRow(
                                playerButtonList.subList(15, 20))
                        .addActionRow(
                                playerButtonList.subList(20, 25))
                        .queue();
            }
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {

        if (Objects.equals(event.getButton().getId(), "random")) {
            if (pool.size() < 4) {
                event.getChannel().sendMessage("```More than 3 players are required```").queue();
            }
            Collections.shuffle(pool);
            for (int i = 0; i < pool.size();i++) {
                if (i < 2) {
                    spymaster.add(pool.get(i));
                }
                else if (i < ((pool.size() - 2) / 2) + 2) {
                    red.add(pool.get(i));
                }
                else {
                    blue.add(pool.get(i));
                }
            }
            event.getMessage().editMessage("Spymasters: " + spymaster + "\nRed: " + red + "\nBlue: " + blue).queue();
            event.editButton(event.getButton().asDisabled()).queue();
        }

        for (Button button : spymasterButtonList) {
            if ((button.getId() + "x").equals(event.getButton().getId())) {
                if (turn == 0 && !red.contains(event.getUser())) {
                    event.deferEdit().queue();
                    return;
                }
                if (turn == 1 && !blue.contains(event.getUser())) {
                    event.deferEdit().queue();
                    return;
                }

                if (button.getStyle().equals(ButtonStyle.DANGER)) {
                    event.editButton(event.getButton().asDisabled().withStyle(ButtonStyle.DANGER)).queue();
                    redRemaining--;
                    event.getMessage().editMessage("```Red: " + redRemaining + " Blue: " + blueRemaining + "```").queue();
                    if (redRemaining == 0) {
                        event.reply("The Red Team has won!").queue();
                        running = false;
                        return;
                    }
                }

                if (button.getStyle().equals(ButtonStyle.PRIMARY)) {
                    event.editButton(event.getButton().asDisabled().withStyle(ButtonStyle.PRIMARY)).queue();
                    blueRemaining--;
                    event.getMessage().editMessage("```Red: " + redRemaining + " Blue: " + blueRemaining + "```").queue();
                    if (blueRemaining == 0) {
                        event.reply("The Blue Team has won!").queue();
                        running = false;
                        return;
                    }
                }

                if (button.getStyle().equals(ButtonStyle.SECONDARY)) {
                    event.editButton(event.getButton().asDisabled().withStyle(ButtonStyle.SECONDARY)).queue();
                }
            }
        }
        guesses--;
        if (guesses == 0) {
            if (turn == 0) turn = 1;
            else turn = 0;
            guesses = 1;
        }
    }
}