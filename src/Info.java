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
            URL url = new URL("http://www.espn.com/mlb/team/_/name" + teams.get(teamName));
            URLConnection conn = url.openConnection();
            InputStream istream = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(istream));

            int i = 0;
            String line;
            String html = "";
            while ((line = reader.readLine()) != null) {
                html += line;
            }
            Document d = Jsoup.parse(html);
            Element schedule = d.getElementsByClass("club-schedule").get(0);
            Elements games = schedule.select("a");
            if (games.get(1).select("div").hasClass("time live")) {
                liveGame(games.get(1).attr("href"), teamName);
            } else {
                oldResults(teamName);
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
            Document doc = Jsoup.connect(url).get();
            Element matchup = doc.getElementsByClass("matchup").get(0);
            HashMap<String, String> teamsToScores = new HashMap<String, String>();
            Elements teams = matchup.select("h3");
            for (Element team : teams) {
                teamsToScores.put(team.select("a").text().toLowerCase(), team.select("span").text());
            }
            String message = generateLiveMessage(teamsToScores, teamName);
            Element inning = doc.getElementsByClass("game-state").get(0);
            message += " in the " + inning.text() + ".";
            System.out.println(message);
        } catch (IOException e) {
            System.out.println("Something went wrong while connecting to live game");
        }
    }

    /** Gets results for a team if they are not currently playing */
    private static void oldResults(String teamName) {
        return;
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
