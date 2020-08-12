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
import java.util.Calendar;
import java.util.regex.Pattern;
import java.util.zip.DataFormatException;
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
  private static final Pattern INT_PATTERN = Pattern.compile("^\\d+$"); // Improves performance by avoiding compile of pattern in every method call
  private static final Pattern WAYPOINT_PATTERN = Pattern.compile("([^\"]\\S*|\".+?\")[\\s\\p{Punct}&&[\"]]*"); // Splitting on whitespace and punctuation unless there are double quotes
  private static final ImmutableMap<String, Integer> NUMBER_MAP = ImmutableMap.<String, Integer>builder()
    .put("one", 1).put("two", 2).put("three", 3).put("four", 4).put("five", 5).put("six", 6).put("seven", 7)
    .put("eight", 8).put("nine", 9).put("ten", 10).build(); 
  private static final int MAX_AMOUNT_ALLOWED = 10;
  private final ArrayList<String> FIELDS_MODIFIED = new ArrayList<String>( 
       Arrays.asList("queryFetched", "textFetched"));
  private final String FETCH_FIELD = "queryFetched";
  private final String FETCH_PROPERTY = "waypoints";
  private final String ENTITY_TYPE = "Route";
  private Coordinate midpoint;
  private static final Double BOUNDING_BOX_WIDTH = 0.07246376811; // 5 miles
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
    SessionDataStore sessionDataStore = new SessionDataStore(request);
    midpoint = getMidpoint(sessionDataStore);
    ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();
    try {
      waypoints = getLocations(input);
    } catch (IllegalArgumentException e) { // User puts down a number that's out of range
      System.out.println("ILLEGAL ARGUMENT");
      response.setStatus(HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE);
    } catch (DataFormatException e ) { // User enters malformed input
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
    } catch (Exception e) {
      throw new ServletException(e);
    }
    String waypointsJSONstring = new Gson().toJson(waypoints);
    // Store input text and waypoint in datastore.
    sessionDataStore.storeProperty(ENTITY_TYPE, "waypoints", waypointsJSONstring);
    sessionDataStore.storeProperty(ENTITY_TYPE, "text", input);
    sessionDataStore.setSessionAttributes(FIELDS_MODIFIED);
    // Redirect back to the create-route page.
    response.sendRedirect("/create-route.html");
  }


  /** Using the input text, fetches waypoints from the database to be 
    * used by the frontend. Returns possible waypoints. 
    */
  public ArrayList<List<Coordinate>> getLocations(String input) throws IllegalArgumentException, Exception {
    // Parse out feature requests from input
    ArrayList<WaypointDescription> waypointRequests = processInputText(input);
    ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();    

    for (WaypointDescription waypointDescription : waypointRequests) {
      int maxNumberCoordinates = waypointDescription.getMaxAmount();
      String query = waypointDescription.getQuery();
      String feature = waypointDescription.getFeature();
      // Make call to database
      ArrayList<Coordinate> locations = fetchFromDatabase(query, feature);
      if (locations.isEmpty()) { // Not in the database
        continue;
      } else {
        List<Coordinate> locationsList = locations.subList(0, Math.min(maxNumberCoordinates, locations.size()));
        waypoints.add(locationsList);
      }
    }   
    return waypoints;
  }

  /** Parses the input string to separate out all the features
    * For each arraylist of strings, the first element is the general waypoint query
    */
  public ArrayList<WaypointDescription> processInputText(String input) throws IllegalArgumentException, Exception {
    input = Normalizer.normalize(input, Normalizer.Form.NFKD);
    ArrayList<WaypointDescription> allWaypoints = new ArrayList<WaypointDescription>();
    // Separate on newlines and punctuation: semicolon, period, question mark, exclamation mark, plus sign
    // String[] waypointQueries = input.split("[;.?!+\\n]+");
    // for (String waypointQuery : waypointQueries) {
    //   ArrayList<WaypointDescription> waypointsFromQuery = parseWaypointQuery(waypointQuery);
    //   allWaypoints.addAll(waypointsFromQuery);
    // }
    ArrayList<WaypointDescription> waypointsFromQuery = parseWaypointQuery(input);
    return allWaypoints;
  }

  /** Parses out the features from a waypoint query
    */
  public ArrayList<WaypointDescription> parseWaypointQuery(String waypointQuery) throws IllegalArgumentException, Exception {
    waypointQuery = waypointQuery.toLowerCase().trim(); // determine -- keep lowercase or allow uppercase?
    ArrayList<WaypointDescription> waypointsFromQuery = new ArrayList<WaypointDescription>();
    // Separate on commas and spaces
    //String[] featureQueries = waypointQuery.split("[,\\s]+"); 
    List<String> list = new ArrayList<>
    Matcher m = Pattern.compile("([^\"]\\S*|\".+?\")\\s*").matcher(str);
    while (m.find()) {
      list.add(m.group(1).replace("\"", ""));
    }
    for (String el : list) {
      System.out.println(el);
    }
    String[] featureQueries = list.toArray(new String[0]);
    String[][] bigTags = getTags(featureQueries);
    String[] primaryTags = bigTags[0];
    WaypointDescription waypoint = new WaypointDescription();
    for (int i = 0; i < featureQueries.length; i++) {
      String feature = featureQueries[i]; 
      if (feature.isEmpty()) {
        continue;
      }
      if (isInt(feature)) {
        int maxAmount = wordToInt(feature);
        if (maxAmount > 10 || maxAmount < 1) {
          throw new IllegalArgumentException("Number out of range! If you enter a number, please let it be from 1 to 10");
        }
        if (waypoint.hasFeature()) { 
          // Start a new waypoint description
          waypointsFromQuery.add(waypoint);
          waypoint = new WaypointDescription(maxAmount);
        } else {
          waypoint.setMaxAmount(maxAmount);
        }
      } else {
        // Parsing for nouns/not pronouns
        // System.out.println(feature);
        // System.out.println(primaryTags[i]);
        // if (bigTags.length == 2) {
        //   System.out.println(bigTags[1][i]);
        // }
        if (primaryTags[i].equals(NOUN_SINGULAR_OR_MASS) || primaryTags[i].equals(NOUN_PLURAL)) { // Doesn't look for proper nouns right now
          waypoint.addFeature(feature);
          waypointsFromQuery.add(waypoint);
          waypoint = new WaypointDescription();
        } else if (bigTags.length == 2 && !primaryTags[i].equals(PRONOUN)) { 
          // Second chance: as long as it's not a pronoun, see if the word can still be a noun
          String[] secondaryTags = bigTags[1];
          if (secondaryTags[i].equals(NOUN_SINGULAR_OR_MASS) || secondaryTags[i].equals(NOUN_PLURAL)) {
            waypoint.addFeature(feature);
            waypointsFromQuery.add(waypoint);
            waypoint = new WaypointDescription();
          }
        }
      }
    }
    if (waypoint.hasFeature()) {
      waypointsFromQuery.add(waypoint);
    }
    return waypointsFromQuery;
  }

  /** Gets the part of speech tags for a sentence of tokens
    */
  public String[][] getTags(String[] tokens) throws Exception {
    //Loading Parts of speech-maxent model      
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
      return MAX_AMOUNT_ALLOWED;
    }
    throw new Exception("Word is not an integer!");
  }

  /** Sends a request for the input feature to the database
    * Returns the Coordinate matching the input feature 
    */ 
  public ArrayList<Coordinate> fetchFromDatabase(String query, String label) throws IOException {
    String startDate = getStartDate();
    String[] boundaries = getBoundingBox();
    String json = sendGET(query, startDate, boundaries);
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

  /** Uses the midpoint coordinate to find the coordinates for the bounding box surrounding 
    * the midpoint by BOUNDING_BOX_WIDTH on each side
    * Returns list in order of bound: west, east, south, north
    */
  public String[] getBoundingBox() {
    String[] boundaries = new String[4];
    boundaries[0] = String.valueOf(midpoint.getX() - BOUNDING_BOX_WIDTH);
    boundaries[1] = String.valueOf(midpoint.getX() + BOUNDING_BOX_WIDTH);
    boundaries[2] = String.valueOf(midpoint.getY() - BOUNDING_BOX_WIDTH);
    boundaries[3] = String.valueOf(midpoint.getY() + BOUNDING_BOX_WIDTH);
    return boundaries;
  }

  /** Sends a request for the input feature to the database and returns 
    * a JSON of the features
    */ 
  public static String sendGET(String feature, String startDate, String[] bounds) throws IOException {
    // Create query
    String query = "https://www.inaturalist.org/observations.json?q=" + feature;
    query += "&d1=" + startDate;
    query += "&swlng=" + bounds[0] + "&nelng=" + bounds[1] + "&swlat=" + bounds[2] + "&nelat=" + bounds[3];
    URL obj = new URL(query); 
    
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

  /** Fetches midpoint from sessionDataStore. 
    */
 private Coordinate getMidpoint(SessionDataStore sessionDataStore){
    JSONObject jsonObject = new JSONObject(sessionDataStore.fetchSessionEntity("StartEnd", "midpoint"));
    Double x = jsonObject.getDouble("x");
    Double y = jsonObject.getDouble("y");
    midpoint = new Coordinate(x, y, "midpoint");
    return midpoint;
 }
}
