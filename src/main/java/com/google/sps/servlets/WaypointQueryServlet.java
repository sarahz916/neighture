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
import com.google.gson.Gson;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;
import org.json.JSONObject;  
import org.json.JSONArray;  
import org.apache.commons.math3.util.Precision;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;  
import java.util.ArrayList;
import java.util.List;
import java.util.Calendar;
import java.util.regex.Pattern;
import java.text.DecimalFormat;
import java.text.Normalizer;

// Imports the Google Cloud client library
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.Token;

/** Servlet that handles the user's query by parsing out
  * the waypoint queries and their matching coordinates in 
  * the database.
  */
@WebServlet("/query")
public class WaypointQueryServlet extends HttpServlet {
  // The maximum number of coordinates will be an optional input by the user, with 5 as the default
  public static int DEFAULT_MAX_NUMBER_COORDINATES = 5;
  private int maxNumberCoordinates = DEFAULT_MAX_NUMBER_COORDINATES;
  private static final Pattern PATTERN = Pattern.compile("^\\d+$"); // Improves performance by avoiding compile of pattern in every method call

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession currentSession = request.getSession();
    String currSessionID = currentSession.getId();
    boolean fetched = markFetch(request);
    if (!fetched){
        String waypointsJSONstring = getQueryResultsforSession(currSessionID);
        // Return last stored waypoints
        response.setContentType("application/json");
        response.getWriter().println(waypointsJSONstring);
    }
    else{
        response.setContentType("application/json");
        response.getWriter().println("[]");
    }
    
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String currSessionID = getSessionID(request);
    setSessionAttributes(request);
    String input = request.getParameter("text-input");
    ArrayList<List<Coordinate>> waypoints = getLocations(input);
    // Store input text and waypoint in datastore so same session ID can retrieve later.
    storeInputAndWaypoints(currSessionID, input, waypoints);
    // Redirect back to the index page.
    response.sendRedirect("/index.html");
  }

    /** Uses the Session ID to retrieve waypoints from datastore
    * Returns waypoints in JSON String format
    */
  private String getQueryResultsforSession(String currSessionID){
    //Retrieve Waypoints for that session.
    Filter sesionFilter =
    new FilterPredicate("session-id", FilterOperator.EQUAL, currSessionID);
    // sort by most recent query for session ID
    Query query = 
            new Query("Route")
                .setFilter(sesionFilter)
                .addSort("timestamp", SortDirection.DESCENDING);
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    List results = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
    Entity MostRecentStore = (Entity) results.get(0);
    String waypointsJSONstring = (String) MostRecentStore.getProperty("waypoints");
    return waypointsJSONstring;
  }

  /** Using the input text, fetches waypoints from the database to be 
    * used by the frontend. Returns possible waypoints. 
    */
  private ArrayList<List<Coordinate>> getLocations(String input) throws IOException {
    // Parse out feature requests from input
    ArrayList<ArrayList<String>> featureRequests = processInputText(input);
    ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();    

    for (ArrayList<String> waypointQueries : featureRequests) {
      String waypointQuery = waypointQueries.get(0);
      ArrayList<Coordinate> potentialCoordinates = new ArrayList<Coordinate>();
      // first is the arraylist index of the first feature in the database
      for (int first = 1, i = 1; i < waypointQueries.size(); i++, first++) {
        String featureRequest = waypointQueries.get(i);
        // Check if feature request is a number
        if (PATTERN.matcher(featureRequest).matches()) {
          maxNumberCoordinates = Integer.parseInt(featureRequest);
        } else if (featureRequest.equals("all") || featureRequest.equals("every")) {
          maxNumberCoordinates = Integer.MAX_VALUE;
        } else {
          // Make call to database
          ArrayList<Coordinate> locations = fetchFromDatabase(featureRequest, waypointQuery);
          if (!locations.isEmpty()) { // The feature is in the database
            if (i == first) {
              potentialCoordinates.addAll(locations);
              first = 0; // We don't need to worry about it anymore since at least one feature was found!
            } else {
              potentialCoordinates.retainAll(locations);
            }
          }
        }
      }
      List<Coordinate> locations = potentialCoordinates.subList(0, Math.min(maxNumberCoordinates, potentialCoordinates.size()));
      maxNumberCoordinates = DEFAULT_MAX_NUMBER_COORDINATES;
      waypoints.add(locations);
    }   
    return waypoints;
  }

  /** Parses the input string to separate out all the features
    * For each arraylist of strings, the first element is the general waypoint query
    */
  private static ArrayList<ArrayList<String>> processInputText(String input) {
    input = Normalizer.normalize(input, Normalizer.Form.NFKD);
    ArrayList<ArrayList<String>> allFeatures = new ArrayList<ArrayList<String>>();
    // Separate on newlines and punctuation: semicolon, period, question mark, exclamation mark, plus sign
    String[] waypointQueries = input.split("[;.?!+\\n]+");
    for (String waypointQuery : waypointQueries) {
      waypointQuery = waypointQuery.toLowerCase().trim();
      ArrayList<String> featuresOneWaypoint = new ArrayList<String>();
      featuresOneWaypoint.add(waypointQuery);
      // Separate on commas and spaces
      String[] featureQueries = waypointQuery.split("[,\\s]+");
      for (int i = 0; i < featureQueries.length; i++) {
        String feature = featureQueries[i].toLowerCase().trim();
        featuresOneWaypoint.add(feature);
      }
      allFeatures.add(featuresOneWaypoint);
    }
    return allFeatures;
  }

  /** Sends a request for the input feature to the database
    * Returns the Coordinate matching the input feature 
    */ 
  private static ArrayList<Coordinate> fetchFromDatabase(String feature, String label) throws IOException {
    String startDate = getStartDate();
    String json = sendGET(feature);
    if (json != null) {
      return jsonToCoordinates(json, label);
    }
    return new ArrayList<Coordinate>();
  }

  /** Returns the date a year ago in string form
    * Note: not used in tests (mocked out)
    */
  private static String getStartDate() {
    Calendar previousYear = Calendar.getInstance();
    previousYear.add(Calendar.YEAR, -1);
    DecimalFormat formatter = new DecimalFormat("00"); // To allow leading 0s
    String yearAsString = formatter.format(previousYear.get(Calendar.YEAR));
    String monthAsString = formatter.format(previousYear.get(Calendar.MONTH));
    String dayAsString = formatter.format(previousYear.get(Calendar.DATE));
    String dateString = yearAsString + "-" + monthAsString + "-" + dayAsString;
    return dateString;
  }

  /** Sends a request for the input feature to the database and returns 
    * a JSON of the features
    */ 
  private static String sendGET(String feature) throws IOException {
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
  private static ArrayList<Coordinate> jsonToCoordinates(String json, String label) {
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

  /** Stores input text and waypoints in a RouteEntity in datastore.
    * Returns nothing.
    */ 
  private static void storeInputAndWaypoints(String currSessionID, String textInput, ArrayList<List<Coordinate>> waypoints){
    Entity RouteEntity = new Entity("Route");
    RouteEntity.setProperty("session-id", currSessionID);
    RouteEntity.setProperty("text", textInput);
    String json = new Gson().toJson(waypoints);
    long timestamp = System.currentTimeMillis();
    RouteEntity.setProperty("timestamp", timestamp);
    // Store as a json string because Coordinates are unsupported.
    RouteEntity.setProperty("waypoints", json);
    // Store Route.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(RouteEntity);
  }


  /** Returns session ID of request. 
  */ 
  private static String getSessionID(HttpServletRequest request){
    HttpSession currentSession = request.getSession();
    String currSessionID = currentSession.getId();
    return currSessionID;
  }

   /** If session has already fetched from servlet will return true.
   *   Else if it's first time to fetch from servlet will return false.
  */ 
  private static boolean markFetch(HttpServletRequest request){
    HttpSession currentSession = request.getSession();
    if (!(boolean) currentSession.getAttribute("queryFetched")){
        currentSession.setAttribute("queryFetched", true);
        return false;
    }
    else{
        return true;
    }
  }
   /** Changes queryFetched sessionAttribute to false.
  */ 

  private static void setSessionAttributes(HttpServletRequest request){
      HttpSession currentSession = request.getSession();
      currentSession.setAttribute("queryFetched", false);
      currentSession.setAttribute("chosenFetched", true);
      currentSession.setAttribute("textFetched", true);
  }

}
