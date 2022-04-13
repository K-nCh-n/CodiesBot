package me.porkypus;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

public class Codenames {
    List<String> wordList;
    List<User> players, spymaster, red ,blue;
    Hashtable<String,Integer> scores;
    Hashtable<String,ButtonStyle> wordSets, wordsInGame;
    boolean running, ready;
    String turn;

    public Codenames(){
        wordList = new ArrayList<>();
        wordSets = new Hashtable<>();
        wordsInGame = new Hashtable<>();

        players = new ArrayList<>();
        spymaster = new ArrayList<>();
        red = new ArrayList<>();
        blue = new ArrayList<>();
    }

    //WordLists {

    public List<User> getSpymasters() {
        return spymaster;
    }

    /**
     * Updates the wordlist according to the sets chosen
     */
        public void chooseWordSets(){
            for (String wordSet: wordSets.keySet()) {
                if (wordSets.get(wordSet).equals(ButtonStyle.SUCCESS)){
                    try {
                        BufferedReader br = new BufferedReader(new FileReader(wordSet + ".txt"));
                        String line;
                        while((line = br.readLine()) != null) {
                            wordList.add(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

    /**
     * Updates the sets chosen
     * @param style Selection of the set,
     *              Chosen: ButtonStyle.SUCCESS
     *              Not Chosen: ButtonStyle.SECONDARY
     */
        public void editWordset(String wordset, ButtonStyle style){
            wordSets.put(wordset,style);
        }

        public void addCustomWords(String message){
            try {
                String[] words = message.split(",");
                for (String word : words) {
                    word =  word.trim();
                    wordList.add(word);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    //}

    /**
     * Resets the game,
     * Clears players and word sets chosen
     */
    public void resetGame(){
        running = true;
        players.clear();

        wordSets = new Hashtable<>();
        wordSets.put("Codenames", ButtonStyle.SUCCESS);
        wordSets.put("Undercover", ButtonStyle.SECONDARY);
        wordSets.put("Duet", ButtonStyle.SECONDARY);
        wordList.clear();
    }

    /**
     * Add a user to the player list
     * @param user User to add
     */
    public void addPlayer(User user){players.add(user);}

    /**
     * Restores scores and words in game
     */
    public void initialiseGame(){
        ready = false;
        turn = "DANGER";
        scores.put("DANGER" , 9);
        scores.put("PRIMARY" , 8);

        wordsInGame.clear();
        chooseWordSets();
    }

    /**
     * Randomises the words to be used in game
     * @return Hashtable of words in game and their buttonStyles
     */
    public Hashtable<String, ButtonStyle> start(){
        initialiseGame();
        Collections.shuffle(wordList);
        for (int i = 0; i < 25; i++) {
            String word = wordList.get(i);
            if (i < 8) {
                wordsInGame.put(word, ButtonStyle.PRIMARY);
            } else if (i < 17) {
                wordsInGame.put(word, ButtonStyle.DANGER);
            } else {
                wordsInGame.put(word, ButtonStyle.SECONDARY);
            }
        }
        return wordsInGame;
    }

    public void stop(){
        running = false;
    }

    public boolean isRunning(){
        return running;
    }

    /**
     * Decrements the score of the team that last played
     * @param team DANGER = Red
     *             PRIMARY = Blue
     * @return Whether game has ended
     */
    public boolean decrementScore(String team){
        int newScore = -1;
        if (!team.equals("SECONDARY")){
            newScore = scores.get(team)-1;
            scores.put(team, newScore);
        }
        return newScore == 0;
    }

    public Hashtable<String, Integer> getScores() {
        return scores;
    }

    public List<User> getPlayers() {
        return players;
    }

    public List<User> getGuessers() {
        List<User> guessers = new ArrayList<>();
        guessers.addAll(red);
        guessers.addAll(blue);
        return guessers;
    }

    public String getTurn() {
        return turn;
    }

    public void changeTurn(){
        turn = turn.equals("PRIMARY") ? "DANGER" : "PRIMARY";
    }

    public boolean checkPlayerButton(String buttonId){
        String word = buttonId.replaceFirst("playerButton", "");
        return wordsInGame.contains(word);
    }

    public ButtonStyle getButtonStyle(String buttonId){
        String word = buttonId.replaceFirst("playerButton", "");
        return wordsInGame.get(word);
    }

    public String randomisePlayers() {
        Collections.shuffle(players);
        spymaster.clear();
        red.clear();
        blue.clear();
        for (int i = 0; i < players.size(); i++) {
            if (i < 2) {
                spymaster.add(players.get(i));
            } else if (i < ((players.size() - 2) / 2) + 2) {
                red.add(players.get(i));
            } else {
                blue.add(players.get(i));
            }
        }
        ready = true;
        return "Spymasters: " + spymaster + "\nRed: " + red + "\nBlue: " + blue;
    }

    public boolean isReady() {
        return ready;
    }
}
