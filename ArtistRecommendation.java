import org.jsoup.nodes.Document;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import java.util.*;
import java.util.regex.*;
import java.io.IOException;

public class ArtistRecommendation {

    public ArrayList<String> getArtists() throws IOException {
        ArrayList<String> html = new ArrayList<>();
        //Set up decades to limit "search" radius
        String[] decades = {"1990", "2000", "2010", "2019"};
        
        for(String year: decades) {
        	html.add("https://top40weekly.com/" + year + "-all-charts/");
        }
    
        //Fetch only the necessary data to clean up the ugly mess
        ArrayList<String> sendToClean = new ArrayList<>();
        for(String page : html) {
            sendToClean = htmlCleanup(page);
        }
        return sendToClean;
    }

    //Takes htmls from above, cleans up the mess, and stores the artists' names
    public ArrayList<String> htmlCleanup(String url) throws IOException {
        final Document document = Jsoup.connect(url).get();
        Elements songs = document.select("div.x-text");
        String[] first = songs.toString().split("(?=<p>)");
        ArrayList<String> second = new ArrayList<>();
        
        for(int i = 0; i < first.length; i++) {
            second.addAll(Arrays.asList(first[i].split("(?=</p)")));
        }
        
        //Setting up regex to remove <p>
        String regex = "[<][p][>]\\d";
        Pattern pattern = Pattern.compile(regex);
        //Removing <p>
        ArrayList<String> pClean = new ArrayList<>();
        for(String line: second) {
        	Matcher match = pattern.matcher(line);
        	if(match.find()) {
        		pClean.add(line);
        	}
        }
        
        //Removing all html breaks/markers 
        ArrayList<String> breakClean = new ArrayList<>();
        for(int i = 0; i < pClean.size(); i++) {
            String line = pClean.get(i).replace("–", "(");
            String [] artists = line.split("<br>");
            breakClean.addAll(Arrays.asList(artists));
        }
        
        ArrayList<String> nameClean = new ArrayList<>();
        for(int i = 0; i < breakClean.size(); i++) {
            String[] artists = breakClean.get(i).split("<p>");
            nameClean.addAll(Arrays.asList(artists));
        }
        
        //Setting up delimiter to properly get artists' names
        String delimiter = "[(•(] (.*?) [(]";
        pattern = Pattern.compile(delimiter);
        
        //Getting the artists' names
        ArrayList<String> artistList = new ArrayList<String>();
        for(String s : nameClean) {
            Matcher matcher = pattern.matcher(s);
            if(matcher.find()) {
                artistList.add(matcher.group(1));
            }
        }
        return artistList;
    }

   
    //Using hashtable with linkedlists
    public Hashtable<String, LinkedList<String>> createHash(ArrayList<String> artistList){
        //Setting up a hash table to store an artist and a linked list for their respective features
        Hashtable<String, LinkedList<String>> list = new Hashtable<>();
        //There are 3 different iterations of how artists are credited as features:
        // "&amp;" // "Featuring" // "featuring"
        for(String artist: artistList) {        	
        	if(artist.contains("&amp;") || artist.contains("Featuring") || artist.contains("featuring")) {
        		
                if(artist.contains("&amp;")) {
                    String[] temp = artist.split("&amp;");
                    list = listHelper(list, temp);
                } else if(artist.contains("Featuring")) {
                    String[] temp = artist.split("Featuring");
                    list = listHelper(list, temp);
                } else if(artist.contains("featuring")) {
                    String[] temp = artist.split("featuring");
                    list = listHelper(list, temp);
                }
                
            } else {
                if(!list.containsKey(artist)) {
                    list.put(artist, new LinkedList<String>());
                }
            }
        }
        return list;
    }

    public Hashtable<String, LinkedList<String>> listHelper(Hashtable<String, LinkedList<String>> list, String[] temp) {
        ArrayList<String> collect = new ArrayList<>();
        
        for(String names: temp) {
            String cleaned = names.strip();
            collect.add(cleaned);
        }
        //Adding each name
        for(String artist: collect) {
            if(!list.containsKey(artist)) {
                String other = "";
                for(int i = 0; i < collect.size(); i++) {
                    if(!collect.get(i).equals(artist)) {
                        other = collect.get(i);
                    }
                }
                list.put(artist, new LinkedList<String>(Arrays.asList(other)));
            } else {
                //If artist is already in
                String otherArtist = "";
                for(int i = 0; i < collect.size(); i++) {
                    if(!collect.get(i).equals(artist)) {
                        otherArtist = collect.get(i);
                    }
                }
                if(!list.get(artist).contains(otherArtist)) {
                    LinkedList<String> holdList = list.get(artist);
                    holdList.add(otherArtist);
                    list.put(artist, holdList);
                }
            }
        }
        return list;
    }
    
    
    public ArrayList<String> artistRecommendations(String name, Hashtable<String, LinkedList<String>> list){
        ArrayList<String> features = new ArrayList<String>();
        boolean mod = true;
        int count = 0;
        
        if(list.containsKey(name)) {
            features.addAll(list.get(name));
            while(mod) {
                mod = false;
                String feat = " ";
                
                try {
                    feat = features.get(count);
                } catch (IndexOutOfBoundsException e) {
                    mod = false;
                    break;
                }
                if(list.containsKey(feat)) {
                    ArrayList<String> hold = new ArrayList<String>(list.get(feat));
                    mod = true;
                    for(String item: hold) {
                        if(!features.contains(item) && !item.equals(name)) {
                            features.add(item);
                            mod = true;
                        }
                    }
                }
                count++;
            }
        } else {
            System.out.println("Artist not found :(\n");
        }
        return features;
    }
    
    //Main function
    public static void main(String[] args) throws IOException {
        ArtistRecommendation recommender = new ArtistRecommendation();
        
        //Take artist name via user input
        Scanner scan = new Scanner(System.in);
        System.out.println("Enter a name of an artist and we'll give you other artists to listen to!\n");
        System.out.println("ARTIST NAME: ");
        String artist = scan.nextLine();
        
        //Setting up artist recommendations -- takes a bit of time to gather
        System.out.println("Loading...");
        ArrayList<String> listOfArtists = recommender.getArtists();
        Hashtable<String, LinkedList<String>> artistList = recommender.createHash(listOfArtists);
        ArrayList<String> recommendations = recommender.artistRecommendations(artist, artistList);
        System.out.print(recommendations);
    }
}
