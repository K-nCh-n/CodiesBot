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


public class DiscordBot extends ListenerAdapter {

    boolean running = false;
    int turn = 0; // 0 is red turn and 1 is blue turn

    static List<String> wordList = new ArrayList<>();
    static List<Button> spymasterButtonList = new ArrayList<>();
    static List<Button> playerButtonList = new ArrayList<>();

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

        jda.upsertCommand("codies", "Type /codies to get started").queue();

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

        if (msg.getContentRaw().equals("!spymaster")) {
            msg.delete().queue();
            if (!running) {
                event.getChannel().sendMessage("Please start a game to join a team").queue();
            } else {
                if (spymaster.size() < 2) {
                    if (spymaster.contains(event.getAuthor())) {
                        event.getChannel().sendMessage(event.getAuthor().getName() + " is already a spymaster").queue();
                    } else {
                        spymaster.add(event.getAuthor());
                        event.getChannel().sendMessage( "```" + event.getAuthor().getName() + " has been added to the spymasters```").queue();
                    }
                } else {
                    event.getChannel().sendMessage("There can only be two spymasters").queue();
                }
            }
        }

        if (msg.getContentRaw().equals("!red")) {
            msg.delete().queue();
            if (!running) {
                event.getChannel().sendMessage("Please start a game to join a team").queue();
            } else {
                if (red.contains(event.getAuthor())) {
                    event.getChannel().sendMessage(event.getAuthor().getName() + " is already on the red team").queue();
                } else {
                    red.add(event.getAuthor());
                    event.getChannel().sendMessage( "```" + event.getAuthor().getName() + " has been added to the blue team```").queue();
                }

            }
        }

        if (msg.getContentRaw().equals("!blue")) {
            msg.delete().queue();
            if (!running) {
                event.getChannel().sendMessage("Please start a game to join a team").queue();
            } else {
                if (blue.contains(event.getAuthor())) {
                    event.getChannel().sendMessage(event.getAuthor().getName() + " is already on the blue team").queue();
                } else {
                    blue.add(event.getAuthor());
                    event.getChannel().sendMessage( "```" + event.getAuthor().getName() + " has been added to the blue team```").queue();
                }

            }
        }

    }


    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        if (event.getName().equals("codies")) {

            running = true;
            redRemaining = 9;
            blueRemaining = 8;

            spymasterButtonList = new ArrayList<>();
            playerButtonList = new ArrayList<>();

            Guild guild = event.getGuild();
            /*
            assert guild != null;
            guild.createTextChannel("spymasters")
                    .addPermissionOverride(guild.getPublicRole(), null, EnumSet.of(Permission.VIEW_CHANNEL))
                    .queue();

             */

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

            assert guild != null;
            TextChannel textChannel = guild.getTextChannelsByName("spymasters", true).get(0);

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

        if (event.getName().equals("exit")){
            running = false;
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        event.editButton(event.getButton().asDisabled()).queue();

        for (Button button : spymasterButtonList) {
            if ((button.getId() + "x").equals(event.getButton().getId())) {
                if (button.getStyle().equals(ButtonStyle.DANGER)) {
                    redRemaining--;
                    event.getMessage().editMessage("```Red: " + redRemaining + " Blue: " + blueRemaining + "```").queue();
                    if (redRemaining == 0) {
                        event.getChannel().sendMessage("The Red Team has won!").queue();
                        running = false;
                    }
                }

                if (button.getStyle().equals(ButtonStyle.PRIMARY)) {
                    blueRemaining--;
                    event.getMessage().editMessage("```Red: " + redRemaining + " Blue: " + blueRemaining + "```").queue();
                    if (blueRemaining == 0) {
                        event.getChannel().sendMessage("The Blue Team has won!").queue();
                        running = false;
                    }
                }
            }
        }



    }
}