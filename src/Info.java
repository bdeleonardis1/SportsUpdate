/**
 * Created by bdele on 3/6/2017.
 */

/* Website reading and file IO */
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

/* Jsoup parsing */
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/* Other utilities */
import java.util.HashMap;

public class Info {
    private static HashMap<String, String> teams = new HashMap<>();
    public static void main(String[] args) throws InterruptedException{
        teams.put("mets", "/nym/new-york-mets");
        teams.put("yankees", "/nyy/new-york-yankees");

        //updates every 30 seconds
        /*while (true) {
            getTeamInfo("yankees");
            Thread.sleep(30000);
        }*/

        //updates once
        getTeamInfo("mets");
    }

    /** Determines whether a game is live or not and then calls the appropriate method */
    private static void getTeamInfo(String teamName) {
        try {
            /** Just practicing getting data from a web page without using jsoup */
            URL url = new URL("http://www.espn.com/mlb/team/_/name" + teams.get(teamName));
            URLConnection conn = url.openConnection();
            InputStream istream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream));

            int i = 0;
            String line;
            StringBuilder html = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                html.append(line);
            }
            Document d = Jsoup.parse(html.toString());


            Element schedule = d.getElementsByClass("club-schedule").get(0);   // selects the schedule on the left sidebar of the team's page
            Elements games = schedule.select("a");                              // gets each game from the sidebar
            if (games.get(1).select("div").hasClass("time live")) {    // checks if their is a live game. Will probably move this code around a little
                liveGame(games.get(1).attr("href"), teamName);               // gets the page of the current game
            } else {
                oldResults(teamName, games.get(1), games.get(2));
            }
        } catch (MalformedURLException e) {
            System.out.println("ESPN has changed the location of their teams page!");
        } catch (IOException e) {
            System.out.println("There was a problem connecting to the scoreboard page!");
        }
    }

    /** Outputs the information about a live game by calling generateLiveMessage*/
    private static void liveGame(String url, String teamName) {
        try {
            Document doc = Jsoup.connect(url).get();                                 // uses Jsoup to load the current page
            Element matchup = doc.getElementsByClass("matchup").get(0);     // just selects the matchup area
            HashMap<String, String> teamsToScores = new HashMap<>();
            Elements teams = matchup.select("h3");                           // the h3 tag has both the team name and their current score
            for (Element team : teams) {
                teamsToScores.put(team.select("a").text().toLowerCase(), team.select("span").text()); // maps team name to score
            }
            String message = generateLiveMessage(teamsToScores, teamName);
            Element inning = doc.getElementsByClass("game-state").get(0);     // gets the string rep of the inning
            message += " in the " + inning.text() + ".";
            System.out.println(message);
        } catch (IOException e) {
            System.out.println("Something went wrong while connecting to live game");
        }
    }

    /** Gets results for a team if they are not currently playing */
    private static void oldResults(String teamName, Element nextGame, Element lastGame) {
        String message = "The " + teamName + " are not currently playing. Their next game is on ";

        /* Strips info for next game */
        message += nextGame.getElementsByClass("game-date").get(0).text();
        message += " at " + nextGame.getElementsByClass("time").get(0).text();
        String nextInfo = nextGame.getElementsByClass("game-info").get(0).text();
        if (nextInfo.substring(0, 3).equals("vs")) {
            message += " at home vs the ";
        } else {
            message += " away against the ";
        }

        /* Strips info for the last game */
        message += nextInfo.substring(nextInfo.indexOf(" ") + 1) + ". The " + teamName;
        String lastResult = lastGame.getElementsByClass("game-result").get(0).text();
        if (lastResult.equals("L")) {
            message += " lost ";
        } else {
            message += " won ";
        }
        String lastTeam = lastGame.getElementsByClass("game-info").get(0).text();
        message += "their last game against the " + lastTeam.substring(lastTeam.indexOf(" ") + 1) + " ";
        String score = lastGame.getElementsByClass("score").get(0).text();
        if (lastResult.equals("L")) {
            int index = score.indexOf("-");
            message += score.substring(index + 1) + "-" + score.substring(0, index);
        } else {
            message += score;
        }
        message += ".";
        
        System.out.println(message);
    }

    /** Returns the message about who is winning in a live game */
    private static String generateLiveMessage(HashMap<String, String> teamsToScore, String team) {
        int myTeamScore = Integer.parseInt(teamsToScore.get(team));
        int otherTeamScore = 0;
        String otherTeam = "";
        for (String key : teamsToScore.keySet()) {
            if (!key.toLowerCase().equals(team)) {
                otherTeam = key;
                otherTeamScore = Integer.parseInt(teamsToScore.get(key));
            }
        }
        String message = "The " + capitalizeFirst(team);
        if (myTeamScore > otherTeamScore) {
            message += " are beating the ";
        } else if (myTeamScore < otherTeamScore) {
            message += " are losing to the ";
        } else {
            message += " are tied with the ";
        }

        message += capitalizeFirst(otherTeam) + " " + myTeamScore + "-" + otherTeamScore;

        return message;
    }


    /** Little helper method to make team names look a little nicer */
    private static String capitalizeFirst(String word) {
        return (word.charAt(0) + "").toUpperCase() + word.substring(1);
    }
}
