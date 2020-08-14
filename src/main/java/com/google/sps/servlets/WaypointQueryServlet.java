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
import java.util.regex.Matcher;
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
  private static final Pattern WAYPOINT_PATTERN = Pattern.compile("([^\"]\\w*|\".+?\")\\s*"); // Splitting on whitespace and punctuation unless there are double quotes
  private static final ImmutableMap<String, Integer> NUMBER_MAP = ImmutableMap.<String, Integer>builder()
    .put("one", 1).put("two", 2).put("three", 3).put("four", 4).put("five", 5).put("six", 6).put("seven", 7)
    .put("eight", 8).put("nine", 9).put("ten", 10).build(); 
  private static final int MAX_AMOUNT_ALLOWED = 10;
  private final ArrayList<String> FIELDS_MODIFIED = new ArrayList<String>( 
       Arrays.asList("queryFetched", "textFetched", "statusfetch"));
  private static final Double LOOP_BOUNDING_BOX_WIDTH = 0.07246376811; // 5 miles
  private static final Double ONE_WAY_BOUNDING_BOX_WIDTH = 0.0036231884; // 0.25 miles
  private static final String NOUN_SINGULAR_OR_MASS = "NN";
  private static final String NOUN_PLURAL = "NNS";
  private static final String PRONOUN = "PRP";

    
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    SessionDataStore sessionDataStore = new SessionDataStore(request);
    int statusCode = Integer.parseInt(sessionDataStore.queryOnlyifFirstFetch("statusfetch", "Route", "statusCode"));
    if (statusCode == HttpServletResponse.SC_OK) {
      String valueJSONString = sessionDataStore.queryOnlyifFirstFetch("queryFetched", "Route", "waypoints");
      response.setContentType("application/json");
      System.out.println(valueJSONString);
      response.getWriter().println(valueJSONString);   
    } else {
      response.sendError(statusCode);
    }
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    String input = request.getParameter("text-input");
    SessionDataStore sessionDataStore = new SessionDataStore(request);
    Coordinate midpoint = getPoint(sessionDataStore, "midpoint");
    Coordinate start = getPoint(sessionDataStore, "start");
    Coordinate end = getPoint(sessionDataStore, "end");
    ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();
    int statusCode = HttpServletResponse.SC_OK;
    try {
      waypoints = getLocations(input, start, end);
    } catch (IllegalArgumentException e) { // User puts down a number that's out of range
      System.out.println("ILLEGAL ARGUMENT");
      statusCode = HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE;
    } catch (DataFormatException e ) { // User enters malformed input
      statusCode = HttpServletResponse.SC_BAD_REQUEST;
    } catch (Exception e) {
      throw new ServletException(e);
    }
    String waypointsJSONstring = new Gson().toJson(waypoints);
    System.out.println(waypointsJSONstring);
    // Store input text and waypoint in datastore.
    sessionDataStore.storeProperty("Route", "waypoints", waypointsJSONstring);
    sessionDataStore.storeProperty("Route", "text", input);
    sessionDataStore.storeProperty("Route", "statusCode", String.valueOf(statusCode));
    sessionDataStore.setSessionAttributes(FIELDS_MODIFIED);
    // Redirect back to the create-route page.
    response.sendRedirect("/create-route.html");
  }


  /** Using the input text, fetches waypoints from the database to be 
    * used by the frontend. Returns possible waypoints. 
    */
  public ArrayList<List<Coordinate>> getLocations(String input, Coordinate start, Coordinate end) throws IllegalArgumentException, Exception {
    // Parse out feature requests from input
    ArrayList<WaypointDescription> waypointRequests = parseInput(input);
    ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();    

    for (WaypointDescription waypointDescription : waypointRequests) {
      int maxNumberCoordinates = waypointDescription.getMaxAmount();
      String query = waypointDescription.getQuery();
      String feature = waypointDescription.getFeature();
      // Make call to database
      ArrayList<Coordinate> locations = fetchFromDatabase(query, feature, start, end);
      if (locations.isEmpty()) { // Not in the database
        continue;
      } else {
        System.out.println(feature);
        List<Coordinate> locationsList = locations.subList(0, Math.min(maxNumberCoordinates, locations.size()));
        waypoints.add(locationsList);
      }
    }   
    return waypoints;
  }

  /** Parses out the features/waypoints from an input query
    * For each arraylist of strings, the first element is the general waypoint query
    */
  public ArrayList<WaypointDescription> parseInput(String input) throws IllegalArgumentException, Exception {
    input = Normalizer.normalize(input, Normalizer.Form.NFKD);
    ArrayList<WaypointDescription> allWaypoints = new ArrayList<WaypointDescription>();
    String waypointQuery = input.toLowerCase().trim(); // determine -- keep lowercase or allow uppercase?
    // Separate on commas and spaces
    //String[] featureQueries = waypointQuery.split("[,\\s]+"); 
    List<String> list = new ArrayList<String>();
    Matcher m = WAYPOINT_PATTERN.matcher(waypointQuery);
    while (m.find()) {
      String queryString = m.group(1).replaceAll("\\p{Punct}", "");
      if (!queryString.isEmpty()) {
        list.add(queryString);
      }
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
          allWaypoints.add(waypoint);
          waypoint = new WaypointDescription(maxAmount);
        } else {
          waypoint.setMaxAmount(maxAmount);
        }
      } else {
        // Parsing for nouns/not pronouns
        if (primaryTags[i].equals(NOUN_SINGULAR_OR_MASS) || primaryTags[i].equals(NOUN_PLURAL)) { // Doesn't look for proper nouns right now
          waypoint.addFeature(feature);
          allWaypoints.add(waypoint);
          waypoint = new WaypointDescription();
        } else if (bigTags.length == 2 && !primaryTags[i].equals(PRONOUN)) { 
          // Second chance: as long as it's not a pronoun, see if the word can still be a noun
          String[] secondaryTags = bigTags[1];
          if (secondaryTags[i].equals(NOUN_SINGULAR_OR_MASS) || secondaryTags[i].equals(NOUN_PLURAL)) {
            waypoint.addFeature(feature);
            allWaypoints.add(waypoint);
            waypoint = new WaypointDescription();
          }
        }
      }
    }
    if (waypoint.hasFeature()) {
      allWaypoints.add(waypoint);
    }
    return allWaypoints;
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
    return INT_PATTERN.matcher(word).matches() || NUMBER_MAP.containsKey(word)
      || word.equals("all") || word.equals("every");
  }

  /** Assuming that the passed in string is a number, converts it to an int
    */
  public int wordToInt(String word) throws Exception {
    if (INT_PATTERN.matcher(word).matches()) {
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
  public ArrayList<Coordinate> fetchFromDatabase(String query, String label, Coordinate start, Coordinate end) throws IOException {
    String startDate = getStartDate();
    String[] boundaries = getBoundingBox(start, end);
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
  public static String[] getBoundingBox(Coordinate start, Coordinate end) {
    String[] boundaries = new String[4];
    if (start.equals(end)) { // loop
      boundaries[0] = String.valueOf(start.getX() - LOOP_BOUNDING_BOX_WIDTH);
      boundaries[1] = String.valueOf(start.getX() + LOOP_BOUNDING_BOX_WIDTH);
      boundaries[2] = String.valueOf(start.getY() - LOOP_BOUNDING_BOX_WIDTH);
      boundaries[3] = String.valueOf(start.getY() + LOOP_BOUNDING_BOX_WIDTH);
    } else { // one-way
      if (start.getX() > end.getX()) {
        boundaries[0] = String.valueOf(end.getX() - ONE_WAY_BOUNDING_BOX_WIDTH);
        boundaries[1] = String.valueOf(start.getX() + ONE_WAY_BOUNDING_BOX_WIDTH);
      } else {
        boundaries[0] = String.valueOf(start.getX() - ONE_WAY_BOUNDING_BOX_WIDTH);
        boundaries[1] = String.valueOf(end.getX() + ONE_WAY_BOUNDING_BOX_WIDTH);
      }
      if (start.getY() > end.getY()) {
        boundaries[2] = String.valueOf(end.getY() - ONE_WAY_BOUNDING_BOX_WIDTH);
        boundaries[3] = String.valueOf(start.getY() + ONE_WAY_BOUNDING_BOX_WIDTH);
      } else {
        boundaries[2] = String.valueOf(start.getY() - ONE_WAY_BOUNDING_BOX_WIDTH);
        boundaries[3] = String.valueOf(end.getY() + ONE_WAY_BOUNDING_BOX_WIDTH);
      }
    }
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
    query += "&quality_grade=research";
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
      String species = observation.getString("species_guess");
      Coordinate featureCoordinate = new Coordinate(x, y, label, species);
      coordinates.add(featureCoordinate);
      index += 1;
    }
    return coordinates;
  }

  /** Fetches point (start, midpoint, end) from sessionDataStore. 
    */
 private Coordinate getPoint(SessionDataStore sessionDataStore, String pointDescription){
    JSONObject jsonObject = new JSONObject(sessionDataStore.fetchSessionEntity("StartEnd", pointDescription));
    Double x = jsonObject.getDouble("x");
    Double y = jsonObject.getDouble("y");
    Coordinate point = new Coordinate(x, y, pointDescription, "");
    return point;
 }
}
