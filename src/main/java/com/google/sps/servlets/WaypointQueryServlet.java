// Copyright 2019 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.sps;
import com.google.sps.Coordinate;
import com.google.sps.WaypointDescription;
import com.google.sps.SessionDataStore;
import com.google.gson.Gson;
import com.google.common.collect.ImmutableMap;
import org.json.JSONObject;  
import org.json.JSONArray;  
import org.apache.commons.math3.util.Precision;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;  
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.text.Normalizer;

import java.io.FileInputStream; 
import java.io.InputStream;  
import opennlp.tools.postag.POSModel; 
import opennlp.tools.postag.POSSample; 
import opennlp.tools.postag.POSTaggerME; 

/** Servlet that handles the user's query by parsing out
  * the waypoint queries and their matching coordinates in 
  * the database.
  */
@WebServlet("/query")
public class WaypointQueryServlet extends HttpServlet {
  private static final Pattern PATTERN = Pattern.compile("^\\d+$"); // Improves performance by avoiding compile of pattern in every method call
  private static final ImmutableMap<String, Integer> NUMBER_MAP = ImmutableMap.<String, Integer>builder()
    .put("one", 1).put("two", 2).put("three", 3).put("four", 4).put("five", 5).put("six", 6).put("seven", 7)
    .put("eight", 8).put("nine", 9).put("ten", 10).put("eleven", 11).put("twelve", 12).put("fifteen", 15).put("twenty", 20).build(); 
  private final ArrayList<String> FIELDS_MODIFIED = new ArrayList<String>( 
       Arrays.asList("queryFetched", "textFetched"));
  private final String FETCH_FIELD = "queryFetched";
  private final String FETCH_PROPERTY = "waypoints";
  private final String ENTITY_TYPE = "Route";
  private static final String NOUN_SINGULAR_OR_MASS = "NN";
  private static final String NOUN_PLURAL = "NNS";
  private static final String PRONOUN = "PRP";
    
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    SessionDataStore sessionDataStore = new SessionDataStore(request);
    String valueJSONString = sessionDataStore.queryOnlyifFirstFetch(FETCH_FIELD, ENTITY_TYPE, FETCH_PROPERTY);
    response.setContentType("application/json");
    response.getWriter().println(valueJSONString);    
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String input = request.getParameter("text-input");
    ArrayList<List<Coordinate>> waypoints;
    try {
      waypoints = getLocations(input);
    } catch (Exception e) {
      throw new ServletException(e);
    }
    String waypointsJSONstring = new Gson().toJson(waypoints);
    SessionDataStore sessionDataStore = new SessionDataStore(request);
    // Store input text and waypoint in datastore.
    sessionDataStore.storeProperty(ENTITY_TYPE, "waypoints", waypointsJSONstring);
    sessionDataStore.storeProperty(ENTITY_TYPE, "text", input);
    sessionDataStore.setSessionAttributes(FIELDS_MODIFIED);
    // Redirect back to the index page.
    response.sendRedirect("/index.html");
  }


  /** Using the input text, fetches waypoints from the database to be 
    * used by the frontend. Returns possible waypoints. 
    */
  public ArrayList<List<Coordinate>> getLocations(String input) throws Exception {
    // Parse out feature requests from input
    ArrayList<WaypointDescription> waypointRequests = processInputText(input);
    ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();    

    for (WaypointDescription waypointDescription : waypointRequests) {
      int maxNumberCoordinates = waypointDescription.getMaxAmount();
      String waypointLabel = waypointDescription.getLabel();
      LinkedHashSet<String> featureRequests = waypointDescription.getFeatures();
      boolean firstFeatureFound = false;
      ArrayList<Coordinate> potentialCoordinates = new ArrayList<Coordinate>();
      // first is the arraylist index of the first feature in the database
      for (String featureRequest : featureRequests) {
        // Make call to database
        ArrayList<Coordinate> locations = fetchFromDatabase(featureRequest, waypointLabel);
        if (!locations.isEmpty()) { // The feature is in the database
          if (firstFeatureFound) {
            potentialCoordinates.retainAll(locations);
          } else {
            potentialCoordinates.addAll(locations);
            firstFeatureFound = true;
          }
        }
      }

      List<Coordinate> locations = potentialCoordinates.subList(0, Math.min(maxNumberCoordinates, potentialCoordinates.size()));
      waypoints.add(locations);
    }   
    return waypoints;
  }

  /** Parses the input string to separate out all the features
    * For each arraylist of strings, the first element is the general waypoint query
    */
  public ArrayList<WaypointDescription> processInputText(String input) throws Exception {
    input = Normalizer.normalize(input, Normalizer.Form.NFKD);
    ArrayList<WaypointDescription> allWaypoints = new ArrayList<WaypointDescription>();
    // Separate on newlines and punctuation: semicolon, period, question mark, exclamation mark, plus sign
    String[] waypointQueries = input.split("[;.?!+\\n]+");
    for (String waypointQuery : waypointQueries) {
      ArrayList<WaypointDescription> waypointsFromQuery = parseWaypointQuery(waypointQuery);
      allWaypoints.addAll(waypointsFromQuery);
    }
    return allWaypoints;
  }

  /** Parses out the features from a waypoint query
    */
  public ArrayList<WaypointDescription> parseWaypointQuery(String waypointQuery) throws Exception {
    waypointQuery = waypointQuery.toLowerCase().trim(); // determine -- keep lowercase or allow uppercase?
    ArrayList<WaypointDescription> waypointsFromQuery = new ArrayList<WaypointDescription>();
    // Separate on commas and spaces
    String[] featureQueries = waypointQuery.split("[,\\s]+"); // TODO: only include numbers (adjectives?) and nouns
    String[][] bigTags = getTags(featureQueries);
    String[] primaryTags = bigTags[0];
    WaypointDescription waypoint = new WaypointDescription();
    for (int i = 0; i < featureQueries.length; i++) {
      String feature = featureQueries[i]; 
      if (isInt(feature)) {
        int maxAmount = wordToInt(feature);
        if (waypoint.hasFeatures()) { 
          // Start a new waypoint description
          waypoint.createLabel();
          waypointsFromQuery.add(waypoint);
          waypoint = new WaypointDescription(maxAmount);
        } else {
          waypoint.setMaxAmount(maxAmount);
        }
      } else {
        // Parsing for nouns/not pronouns
        if (primaryTags[i].equals(NOUN_SINGULAR_OR_MASS) || primaryTags[i].equals(NOUN_PLURAL)) { // Doesn't look for proper nouns right now
          waypoint.addFeature(feature);
        } else if (bigTags.length == 2 && !primaryTags[i].equals(PRONOUN)) { 
          // Second chance: as long as it's not a pronoun, see if the word can still be a noun
          String[] secondaryTags = bigTags[1];
          if (secondaryTags[i].equals(NOUN_SINGULAR_OR_MASS) || secondaryTags[i].equals(NOUN_PLURAL)) {
            waypoint.addFeature(feature);
          }
        }
      }
    }
    if (waypoint.hasFeatures()) {
      waypoint.createLabel();
      waypointsFromQuery.add(waypoint);
    }
    return waypointsFromQuery;
  }

  /** Gets the part of speech tags for a sentence of tokens
    */
  public String[][] getTags(String[] tokens) throws Exception {
    //Loading Parts of speech-maxent model      
    //InputStream inputStream = new FileInputStream("src/main/webapp/WEB-INF/en-pos-maxent.bin") ;
    InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("en-pos-maxent.bin");
    POSModel model = new POSModel(inputStream); 
      
    //Instantiating POSTaggerME class 
    POSTaggerME tagger = new POSTaggerME(model); 

    //Generating tags 
    String[][] bigTags = tagger.tag(2, tokens);
    return bigTags;
  }

  /** Determines if the passed in string is a number 
    */
  public boolean isInt(String word) {
    return PATTERN.matcher(word).matches() || NUMBER_MAP.containsKey(word)
      || word.equals("all") || word.equals("every");
  }

  /** Assuming that the passed in string is a number, converts it to an int
    */
  public int wordToInt(String word) throws Exception {
    if (PATTERN.matcher(word).matches()) {
      return Integer.parseInt(word);
    } else if (NUMBER_MAP.containsKey(word)) {
      return NUMBER_MAP.get(word);
    } else if (word.equals("all") || word.equals("every")) {
      return Integer.MAX_VALUE;
    }
    throw new Exception("Word is not an integer!");
  }

  /** Sends a request for the input feature to the database
    * Returns the Coordinate matching the input feature 
    */ 
  public static ArrayList<Coordinate> fetchFromDatabase(String feature, String label) throws IOException {
    String startDate = getStartDate();
    String json = sendGET(feature);
    if (json != null) {
      return jsonToCoordinates(json, label);
    }
    return new ArrayList<Coordinate>();
  }

  /** Returns the date a year ago in string form
    */
  public static String getStartDate() {
    Calendar previousYear = Calendar.getInstance();
    previousYear.add(Calendar.YEAR, -1);
    DecimalFormat formatter = new DecimalFormat("00"); // To allow leading 0s
    String yearAsString = formatter.format(previousYear.get(Calendar.YEAR));
    String monthAsString = formatter.format(previousYear.get(Calendar.MONTH) + 1); // The months are 0-indexed
    String dayAsString = formatter.format(previousYear.get(Calendar.DAY_OF_MONTH));
    String dateString = yearAsString + "-" + monthAsString + "-" + dayAsString;
    return dateString;
  }

  /** Sends a request for the input feature to the database and returns 
    * a JSON of the features
    */ 
  public static String sendGET(String feature) throws IOException {
    //URL obj = new URL("https://neighborhood-nature.appspot.com/database?q=" + feature);
    URL obj = new URL("http://localhost:8080/database?q=" + feature);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", "Mozilla/5.0");
    int responseCode = con.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) { // success
      BufferedReader in = new BufferedReader(new InputStreamReader(
              con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer(); 

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      return response.toString();
    } else {
      System.out.println("GET request didn't work");
      return null;
    }
  }

  /** Turns a JSON string into an arraylist of coordinates
    */
  public static ArrayList<Coordinate> jsonToCoordinates(String json, String label) {
    ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
    JSONArray allWaypoints = new JSONArray(json);
    int index = 0;
    while (!allWaypoints.isNull(index)) {
      JSONObject observation = allWaypoints.getJSONObject(index);
      Double x = observation.getDouble("longitude");
      x = Math.round(x * 25000.0)/25000.0;
      Double y = observation.getDouble("latitude");
      y = Math.round(y * 25000.0)/25000.0;
      Coordinate featureCoordinate = new Coordinate(x, y, label);
      coordinates.add(featureCoordinate);
      index += 1;
    }
    return coordinates;
  }

}
