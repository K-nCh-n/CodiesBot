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
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;

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
                .setChunkingFilter(ChunkingFilter.ALL)
                .setMemberCachePolicy(MemberCachePolicy.ALL)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
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
                case "cw":
                    String wordString = command.replaceFirst("cw", "").toLowerCase();
                    if(wordString.isBlank()){
                        channel.sendMessage("```Custom Words: \n" + game.getCustomWords() + "```").queue();
                    } else {
                        game.setCustomWords(wordString);
                    }
                    break;

                case "ccw":
                    game.setCustomWords("");

                case "wl":
                    HashSet<String> wordlist = game.getWordList();
                    channel.sendMessage("```Wordlist: ```").queue();
                    ArrayList<String> outMessages = new ArrayList<>();
                    String outMessage = "```";
                    int i = 0;
                    for (String word : wordlist) {
                        outMessage = outMessage + " " + word;
                        if (outMessage.length() > 1990) {
                            outMessages.add(outMessage + "```");
                            outMessage = "```";
                            i++;
                        }
                    }
                    for (String s:outMessages) {
                        channel.sendMessage(s).queue();
                    }
            }
        }
    }

    @Override
    public void onSlashCommandInteraction(SlashCommandInteractionEvent event) {
        MessageChannel channel = event.getChannel();
        String command = event.getName();

        switch (command){
            case "codies":
                if (game.isRunning()) {
                    event.reply("An instance of codies has already been started").queue();
                } else {
                    List<ActionRow> actionRows =  resetGame(channel);
                    game.initialiseGame();
                    event.reply("```Lobby: ```").addActionRows(actionRows).queue();
                }
                break;

            case "stop":
                game.stop();
                event.reply("Game was stopped").queue();
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
        ButtonStyle trueStyle = game.getButtonStyle(buttonId);

        Guild guild = event.getGuild();
        assert guild != null;
        TextChannel spymastersChannel = guild.getTextChannelsByName("spymasters", true).get(0);

        //Button to join
        if (buttonId.equals("join")) {
            game.addPlayer(user);
            event.editMessage("```Lobby: " + game.getNames() + "```").complete();
        }

        //Button to start game
        else if (buttonId.equals("start")) {
            startGame(guild, spymastersChannel, event.getChannel());
            Button passButton;
            if (game.getTurn().equals("DANGER")){
                passButton = Button.danger("pass", "Pass");
            } else {
                passButton = Button.primary("pass", "Pass");
            }
            event.editComponents().setActionRow(passButton).queue();
        }

        //Buttons to choose word sets
        else if (buttonId.startsWith("wordset")){
            ButtonStyle newStyle = style.equals(ButtonStyle.SECONDARY)? ButtonStyle.SUCCESS : ButtonStyle.SECONDARY;
            game.editWordset(button.getLabel(), newStyle);
            event.editButton(button.withStyle(newStyle)).complete();
        }

        //Pass Button
        else if (buttonId.equals("pass")) {
            if (!checkPlayerTurn(user)) {
                event.deferEdit().queue();
            } else {
                game.changeTurn();
                event.editButton(button.withStyle(ButtonStyle.valueOf(game.getTurn()))).complete();
            }
        }

        //Button to randomise
        else if (buttonId.equals("random")) {
            if (game.getPlayers().size() < 4) {
                channel.sendMessage("```More than 3 players are required```").queue();
                event.deferEdit().complete();
                return;
            }
            String outMessage = game.randomisePlayers();
            for (Member spymaster : guild.getMembersWithRoles(guild.getRolesByName("spymaster", true).get(0))) {
                guild.removeRoleFromMember(spymaster, guild.getRolesByName("spymaster", true).get(0)).complete();
            }
            event.editMessage(outMessage).complete();
        }

        //Bomb
        else if(trueStyle == null) {
            List<ActionRow> spymasterActionRows = getSpymasterActionRows();
            String oldMessage = event.getMessage().toString();
            event.editMessage(oldMessage)
                    .setActionRows(spymasterActionRows)
                    .queue();
            game.stop();
        }

        //Game Buttons
        else if(game.checkPlayerButton(buttonId)) {
            if (!checkPlayerTurn(user)) {
                event.deferEdit().queue();
            } else {
               //Change the button colour
               event.editButton(button.asDisabled().withStyle(trueStyle)).complete();

               //Update score
               HashMap<String, Integer> scores = game.getScores();
               int redRemaining = scores.get("DANGER");
               int blueRemaining = scores.get("PRIMARY");
               boolean gameEnd = game.decrementScore(trueStyle.toString());

               //Check if end game or change turn
               String turn = game.getTurn();
               if (gameEnd){
                   String losingTeam = redRemaining == 0 ? "Blue team" : "Red team";
                   channel.sendMessage("```" + losingTeam + " got rekt```").queue();
               }else if (!trueStyle.toString().equals(turn) || styleString.equals("SECONDARY")){
                   game.changeTurn();
               }
               botMessage.editMessage("```Red: " + redRemaining + " Blue: " + blueRemaining + " Turn: " + (game.getTurn().equals("DANGER") ? "RED" : "BLUE")  + "```").complete();
            }
            }
        else{
            event.deferEdit().queue();
        }
    }

    /**
     * Setups and starts game
     */
    public void startGame(Guild guild, TextChannel spymastersChannel, MessageChannel playersChannel){
        if(!game.isReady()){
            playersChannel.sendMessage("```You didn't set the teams you uncultured swine```").queue();
        }
        else {
            //Set permissions for spymaster channel
            for (User spymaster : game.getSpymasters()) {
                guild.addRoleToMember(Objects.requireNonNull(guild.getMember(spymaster)), guild.getRolesByName("spymaster", true).get(0)).complete();
            }

            //Generate words
            HashMap<String,ButtonStyle> wordsInGame = game.start();
            spymasterButtonList.clear();
            playerButtonList.clear();

            //Create Lists of Buttons for spymaster and shuffle
            for(String word: wordsInGame.keySet()){
                ButtonStyle style = wordsInGame.get(word);
                Button button;
                if (!style.equals(ButtonStyle.UNKNOWN)) {
                    button = Button.of(style,  word, word);
                } else {
                    Emote emote = spymastersChannel.getGuild().getEmotesByName("malding_shawty", true).get(0);
                    button = Button.secondary("word", word).withEmoji(Emoji.fromEmote(emote));
                }
                spymasterButtonList.add(button.asDisabled());
            }
            Collections.shuffle(spymasterButtonList);
            //Create List of buttons for players
            for (Button button : spymasterButtonList) {
                playerButtonList.add(Button.success("playerButton" + button.getId(), button.getLabel()));
            }

            //Create Action Rows
            List<ActionRow> spymasterActionRows = getSpymasterActionRows();
            List<ActionRow> playerActionRows = getPlayerActionRows();

            //Send Messages in corresponding Channels
            spymastersChannel.sendMessage("True Layout")
                    .setActionRows(spymasterActionRows)
                    .queue();

            playersChannel.sendMessage("```Red: 9 Blue: 8 Turn: RED```")
                    .setActionRows(playerActionRows)
                    .queue();
        }
    }

    /**
     * Creates a new instance of the game
     * @param channel Channel to send initial message
     */
    public List<ActionRow> resetGame(MessageChannel channel){
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

        return actionRows;
    }

    public boolean checkPlayerTurn(User user){
        String turn = game.getTurn();
        return (game.getRed().contains(user) && turn.equals("DANGER")) ||
                (game.getBlue().contains(user) && turn.equals("PRIMARY"));
    }

    public List<ActionRow> getSpymasterActionRows(){
        List<ActionRow> spymasterActionRows = new ArrayList<>();
        spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(0, 5)));
        spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(5, 10)));
        spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(10, 15)));
        spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(15, 20)));
        spymasterActionRows.add(ActionRow.of(spymasterButtonList.subList(20, 25)));
        return spymasterActionRows;
    }

    public List<ActionRow> getPlayerActionRows(){
        List<ActionRow> playerActionRows = new ArrayList<>();
        playerActionRows.add(ActionRow.of(playerButtonList.subList(0, 5)));
        playerActionRows.add(ActionRow.of(playerButtonList.subList(5, 10)));
        playerActionRows.add(ActionRow.of(playerButtonList.subList(10, 15)));
        playerActionRows.add(ActionRow.of(playerButtonList.subList(15, 20)));
        playerActionRows.add(ActionRow.of(playerButtonList.subList(20, 25)));
        return playerActionRows;
    }
}