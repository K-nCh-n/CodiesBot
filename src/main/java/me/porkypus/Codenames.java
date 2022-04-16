package me.porkypus;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Codenames {

    HashSet<String> wordList, customWords;
    HashSet<User> players, spymaster, red, blue;
    HashMap<String, Integer> scores;
    HashMap<String, ButtonStyle> wordSets, wordsInGame;
    HashMap<User, String> spymasterTeams;
    boolean running, ready;
    String turn;
    int guesses;
    boolean clueSent;

    public Codenames() {
        wordList = new HashSet<>();
        customWords = new HashSet<>();
        wordSets = new HashMap<>();
        wordsInGame = new HashMap<>();
        scores = new HashMap<>();
        spymasterTeams = new HashMap<>();

        players = new HashSet<>();
        spymaster = new HashSet<>();
        red = new HashSet<>();
        blue = new HashSet<>();

        clueSent = false;
        guesses = -1;
    }


    public HashSet<User> getRed() {
        return red;
    }

    public HashSet<User> getBlue() {
        return blue;
    }

    public List<String> getRedList() {
        List<String> names = new ArrayList<>();
        for (User user : red) {
            names.add(user.getName());
        }
        return names;
    }

    public List<String> getBlueList() {
        List<String> names = new ArrayList<>();
        for (User user : blue) {
            names.add(user.getName());
        }
        return names;
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (User user : players) {
            names.add(user.getName());
        }
        return names;
    }

    public int getGuesses() {
        return guesses;
    }

    public void setGuesses(int guesses) {
        this.guesses = guesses;
    }


    public HashSet<User> getSpymasters() {
        return spymaster;
    }

    public HashMap<User, String> getSpymasterTeams() {
        return spymasterTeams;
    }

    public HashMap<String, String> getTeamsSpymaster() {
        HashMap<String, String> result = new HashMap<>();
        for (User user : spymasterTeams.keySet()) {
            result.put(spymasterTeams.get(user), user.getName());
        }
        return result;
    }

    public boolean isClueSent() {
        return clueSent;
    }

    public void setClueSent(boolean clueSent) {
        this.clueSent = clueSent;
    }


    public boolean isNotPlayer(User user) {
        return !red.contains(user) && !blue.contains(user);
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isRunning() {
        return running;
    }

    public HashMap<String, Integer> getScores() {
        return scores;
    }

    public HashSet<User> getPlayers() {
        return players;
    }

    public String getTurn() {
        return turn;
    }

    public void decrementGuesses() {
        guesses--;
    }

    //WordLists {

    /**
     * Updates the wordlist according to the sets chosen
     */
    public void updateWordlist() {
        wordList.clear();
        for (String wordSet : wordSets.keySet()) {
            if (wordSets.get(wordSet).equals(ButtonStyle.SUCCESS)) {
                try {
                    BufferedReader br = new BufferedReader(new FileReader(wordSet + ".txt"));
                    String line;
                    while ((line = br.readLine()) != null) {
                        wordList.add(line);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (wordList.isEmpty()) {
            wordSets.put("Codenames", ButtonStyle.SUCCESS);
            updateWordlist();
        }
    }

    /**
     * Updates the sets chosen
     *
     * @param style Selection of the set,
     *              Chosen: ButtonStyle.SUCCESS
     *              Not Chosen: ButtonStyle.SECONDARY
     */
    public void editWordset(String wordset, ButtonStyle style) {
        wordSets.put(wordset, style);
    }

    public void setCustomWords(String message) {
        customWords.clear();
        try {
            String[] words = message.replaceAll("[^a-zA-Z,]", " ").split(",");
            for (String word : words) {
                word = word.trim();
                customWords.add(word);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public HashSet<String> getCustomWords() {
        return customWords;
    }

    public HashSet<String> getWordList() {
        updateWordlist();
        return wordList;
    }

    /**
     * Resets the game,
     * Clears players and word sets chosen
     */
    public void resetGame() {
        running = true;
        players.clear();

        wordSets = new HashMap<>();
        wordSets.put("Codenames", ButtonStyle.SUCCESS);
        wordSets.put("Undercover", ButtonStyle.SECONDARY);
        wordSets.put("Duet", ButtonStyle.SECONDARY);
        wordList.clear();
        updateWordlist();
    }

    /**
     * Add a user to the player list
     *
     * @param user User to add
     */
    public void addPlayer(User user) {
        players.add(user);
    }

    /**
     * Restores scores and words in game
     */
    public void initialiseGame() {
        ready = false;
        turn = "DANGER";
        scores.put("DANGER", 9);
        scores.put("PRIMARY", 8);

        wordsInGame.clear();
        updateWordlist();
        setCustomWords(customWords.toString());
    }

    /**
     * Randomises the words to be used in game
     *
     * @return Hashtable of words in game and their buttonStyles
     */
    public HashMap<String, ButtonStyle> start() {
        initialiseGame();
        int i = 0;
        while (i < 25) {
            String word = getRandomElementFromSet(wordList);
            if (i < 8) {
                wordsInGame.put(word, ButtonStyle.PRIMARY);
            } else if (i < 17) {
                wordsInGame.put(word, ButtonStyle.DANGER);
            } else if (i == 24) {
                wordsInGame.put(word, ButtonStyle.UNKNOWN);
            } else {
                wordsInGame.put(word, ButtonStyle.SECONDARY);
            }
            i++;
        }
        return wordsInGame;
    }

    public void stop() {
        running = false;
    }


    /**
     * Decrements the score of the team that last played
     *
     * @param team DANGER = Red
     *             PRIMARY = Blue
     * @return Whether game has ended
     */
    public boolean decrementScore(String team) {
        int newScore = -1;
        if (!team.equals("SECONDARY")) {
            newScore = scores.get(team) - 1;
            scores.put(team, newScore);
        }
        return newScore == 0;
    }

    /**
     * Alternates between teams Red and Blue
     */
    public void changeTurn() {
        turn = turn.equals("PRIMARY") ? "DANGER" : "PRIMARY";
        setClueSent(false);
    }

    /**
     * @param buttonId ID of the pressed button
     * @return Whether the pressed button corresponds to a button in the true layout
     */
    public boolean checkPlayerButton(String buttonId) {
        String word = buttonId.replaceFirst("playerButton", "");
        return wordsInGame.containsKey(word);
    }

    public ButtonStyle getButtonStyle(String buttonId) {
        String word = buttonId.replaceFirst("playerButton", "");
        return wordsInGame.get(word);
    }

    /**
     * Randomises the players and decides which team each player is on
     */
    public void randomisePlayers() {
        spymaster.clear();
        red.clear();
        blue.clear();
        int numPlayers = players.size();
        for (int i = 0; i < numPlayers; i++) {
            User player = getRandomElementFromSet(players);
            if (spymaster.size() < 2) {
                if (spymaster.isEmpty()) {
                    spymasterTeams.put(player, "DANGER");
                } else {
                    spymasterTeams.put(player, "PRIMARY");
                }
                spymaster.add(player);
            } else if (red.size() < blue.size()) {
                red.add(player);
            } else if (red.size() > blue.size()) {
                blue.add(player);
            } else {
                Random random = new Random();
                if (random.nextBoolean()) {
                    red.add(player);
                } else {
                    blue.add(player);
                }
            }
        }
        ready = true;
    }

    static <E> E getRandomElementFromSet(Set<E> set) {
        Random random = new Random();
        int randNum = random.nextInt(set.size());
        Iterator<E> iterator = set.iterator();
        for (int i = 0; i < randNum; i++) {
            iterator.next();
        }
        E element = iterator.next();
        set.remove(element);
        return element;
    }
}
