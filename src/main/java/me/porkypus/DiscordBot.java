package me.porkypus;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import javax.security.auth.login.LoginException;
import java.util.*;


public class DiscordBot extends ListenerAdapter {

    Codenames game = new Codenames();
    List<Button> spymasterButtonList, playerButtonList;
    String prefix = "!";

    public static void main(String[] args) throws LoginException {

        JDA jda = JDABuilder.createDefault(System.getenv("DISCORD_TOKEN"))
                .addEventListeners(new DiscordBot())
                .setActivity(Activity.playing("Type codies to get started"))
                .build();

        jda.upsertCommand("join", "Type /join").queue();
        jda.upsertCommand("codies", "Type /codies").queue();
        jda.upsertCommand("start", "Type /start").queue();
        jda.upsertCommand("stop", "Type /stop").queue();
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        User sender = event.getAuthor();
        if (sender.isBot()) {
            return;
        }
        Message userMessage = event.getMessage();
        String raw = userMessage.getContentRaw();
        MessageChannel channel = event.getChannel();

        if (raw.startsWith(prefix)) {
            String command = raw.replaceFirst(prefix, "").toLowerCase();
            switch (command){
                case "codies":
                    if (game.isRunning()) {
                        channel.sendMessage("An instance of codies has already been started").queue();
                    } else {
                        resetGame(channel);
                    }
                    break;

                case "join":
                    joinGame(sender, channel);
                    break;

                case "start":
                    Guild guild = event.getGuild();
                    TextChannel spymastersChannel = guild.getTextChannelsByName("spymasters", true).get(0);
                    startGame(spymastersChannel, event.getChannel());
                    break;

                case "stop":
                    game.stop();
                    channel.sendMessage("Game was stopped").queue();
                    break;

                case "cw":
                    String wordString = command.replaceFirst("cw", "").toLowerCase();
                    game.addCustomWords(wordString);
                    break;
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        User user = event.getUser();
        MessageChannel channel = event.getChannel();
        String command = event.getName();
        switch (command){
            case "codies":
                if (game.isRunning()) {
                    channel.sendMessage("An instance of codies has already been started").queue();
                } else {
                    resetGame(channel);
                }
                break;

            case "join":
                joinGame(user, channel);
                break;

            case "start":
                Guild guild = event.getGuild();
                assert guild != null;
                TextChannel spymastersChannel = guild.getTextChannelsByName("spymasters", true).get(0);
                startGame(spymastersChannel, event.getChannel());
                break;

            case "stop":
                game.stop();
                channel.sendMessage("Game was stopped").queue();
                break;
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        MessageChannel channel = event.getChannel();
        Message botMessage = event.getMessage();
        User user = event.getUser();

        String buttonId = event.getComponentId();
        Button button = event.getButton();
        ButtonStyle style = button.getStyle();
        String styleString = style.toString();

        //Button to join
        if (buttonId.equals("join")) {
            joinGame(user, channel);
            event.deferEdit().queue();
        }

        //Button to start game
        else if (buttonId.equals("start")) {
            Guild guild = event.getGuild();
            assert guild != null;
            TextChannel spymastersChannel = guild.getTextChannelsByName("spymasters", true).get(0);
            startGame(spymastersChannel, event.getChannel());
        }

        //Buttons to choose word sets
        else if (buttonId.startsWith("wordset")){
            ButtonStyle newStyle = style.equals(ButtonStyle.SECONDARY)? ButtonStyle.SUCCESS : ButtonStyle.SECONDARY;
            game.editWordset(button.getLabel(), newStyle);
            event.editButton(button.withStyle(newStyle)).complete();
        }

        //Button to randomise
        else if (buttonId.equals("random")) {
            if (game.getPlayers().size() < 4) {
                channel.sendMessage("```More than 3 players are required```").queue();
                event.deferEdit().complete();
                return;
            }
            String outMessage = game.randomisePlayers();
            botMessage.editMessage(outMessage).complete();
        }

        //Game Buttons
        else if(game.checkPlayerButton(buttonId)) {
            //Check if player
            if (!game.getGuessers().contains(user)){
                event.deferEdit().queue();
            } else {
                //Change the button colour
                ButtonStyle trueStyle = game.getButtonStyle(buttonId);
                event.editButton(button.asDisabled().withStyle(trueStyle)).complete();

                //Update score
                boolean gameEnd = game.decrementScore(styleString);
                Hashtable<String, Integer> scores = game.getScores();
                int redRemaining = scores.get("DANGER");
                int blueRemaining = scores.get("PRIMARY");
                botMessage.editMessage("```Red: " + redRemaining + " Blue: " + blueRemaining + "```").complete();

                //Check if end game or change turn
                String turn = game.getTurn();
                if (gameEnd){
                    String winningTeam = redRemaining == 0 ? "Blue team" : "Red team";
                    channel.sendMessage(winningTeam + "got rekt" ).queue();
                } else if (!styleString.equals(turn) || styleString.equals("SECONDARY")){
                    game.changeTurn();
                }
            }
        }

        //wtf was that button
        else{
            event.deferEdit().queue();
        }
    }

    /**
     * Setups and starts game
     */
    public void startGame(TextChannel spymastersChannel, MessageChannel playersChannel){
        if(!game.isReady()){
            playersChannel.sendMessage("Please set the teams before starting the game").queue();
        }
        else {
            //Generate words
            Hashtable<String,ButtonStyle> wordsInGame = game.start();

            //Create Lists of Buttons for spymaster and shuffle
            for(String word: wordsInGame.keySet()){
                ButtonStyle style = wordsInGame.get(word);
                Button button = Button.of(style,  word, word);
                spymasterButtonList.add(button.asDisabled());
            }
            Collections.shuffle(spymasterButtonList);
            //Create List of buttons for players
            for (Button button : spymasterButtonList) {
                playerButtonList.add(Button.success("playerButton" + button.getId(), button.getLabel()));
            }

            //Create Action Rows
            List<ActionRow> spymasterActionRows = new ArrayList<>();
            spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(0, 5)));
            spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(5, 10)));
            spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(10, 15)));
            spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(15, 20)));
            spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(20, 25)));

            List<ActionRow> playerActionRows = new ArrayList<>();
            playerActionRows.add(ActionRow.of(playerButtonList.subList(0, 5)));
            playerActionRows.add(ActionRow.of(playerButtonList.subList(5, 10)));
            playerActionRows.add(ActionRow.of(playerButtonList.subList(10, 15)));
            playerActionRows.add(ActionRow.of(playerButtonList.subList(15, 20)));
            playerActionRows.add(ActionRow.of(playerButtonList.subList(20, 25)));

            //Send Messages in corresponding Channels
            spymastersChannel.sendMessage("True Layout")
                    .setActionRows(spymasterActionRows)
                    .queue();

            playersChannel.sendMessage("Score:")
                    .setActionRows(playerActionRows)
                    .queue();
        }
    }

    /**
     * Adds user to player list
     * @param user User to add
     * @param channel Channel to send error message
     */
    public void joinGame(User user, MessageChannel channel){
        List<User> players = game.getPlayers();
        if (players.contains(user)) {
            channel.sendMessage("Join only once idiot").queue();
        } else {
            game.addPlayer(user);
        }
    }

    /**
     * Creates a new instance of the game
     * @param channel Channel to send initial message
     */
    public void resetGame(MessageChannel channel){
        game.resetGame();
        spymasterButtonList =  new ArrayList<>();
        playerButtonList = new ArrayList<>();
        List<ActionRow> actionRows = new ArrayList<>();
        actionRows.add(ActionRow.of(
                (Button.success("wordsetCodenames", "Codenames")),
                (Button.secondary("wordsetUndercover", "Undercover")),
                (Button.secondary("wordsetDuet", "Duet"))
        ));
        actionRows.add(ActionRow.of(
                (Button.primary("join", "Join Game")),
                (Button.success("random", "Randomise Teams")),
                (Button.success("start", "Start Game"))
        ));
        channel.sendMessage("Teams:")
                .setActionRows(actionRows).queue();
    }
}