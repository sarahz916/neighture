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
import java.util.*;


/** Servlet that handles the user's query by parsing out
  * the waypoint queries and their matching coordinates in 
  * the database.
  */
@WebServlet("/query")
public class WaypointQueryServlet extends HttpServlet {
  //ThreadSafe suggestion: make it a map with session as the key. Put it into DataStore actually as retrieve?
  
  //TODO FORCE START NEW SESSION
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession currentSession = request.getSession();
    String currSessionID = currentSession.getId();
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
    //for (Entity entity : results.asIterable()) {
      //waypointsJSONstring = (String) entity.getProperty("waypoints");
      //break;
    //}
    
    // Return last stored waypoints
    response.setContentType("application/json");
    //String json = new Gson().toJson(waypoints);
    response.getWriter().println(waypointsJSONstring);

  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession currentSession = request.getSession();
    String currSessionID = currentSession.getId();
    ArrayList<ArrayList<Coordinate>> waypoints = new ArrayList<ArrayList<Coordinate>>();
    String input = request.getParameter("text-input");
    // Parse out feature requests from input
    String[] waypointQueries = input.split(";");
    for (String waypointQuery : waypointQueries) {
      waypointQuery = waypointQuery.toLowerCase().trim();
      ArrayList<Coordinate> potentialCoordinates = new ArrayList<Coordinate>();
      String[] featureQueries = waypointQuery.split(",");

      for (int i = 0; i < featureQueries.length; i++) {
        String feature = featureQueries[i].toLowerCase().trim();
        // Make call to database
        ArrayList<Coordinate> locations = sendGET(feature, waypointQuery);
        if (i == 0) {
          potentialCoordinates.addAll(locations);
        } else {
          potentialCoordinates.retainAll(locations);
        }
      }
      waypoints.add(potentialCoordinates);
    }   
    // Store input text and waypoint in datastore.
    storeInputAndWaypoints(currSessionID, input, waypoints);
 
    // Redirect back to the index page.
    response.sendRedirect("/index.html");
  }

  /** Sends a request for the input feature to the database
    * Returns the Coordinate matching the input feature 
    */ 
  private static ArrayList<Coordinate> sendGET(String feature, String label) throws IOException {
    ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
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

      // Turn the response into a Coordinate
      String responseString = response.toString();
      System.out.println(responseString);
      JSONArray allWaypoints = new JSONArray(responseString);
      int index = 0;
      while (!allWaypoints.isNull(index)) {
        JSONObject observation = allWaypoints.getJSONObject(index);
        Double x = observation.getDouble("longitude");
        x = Math.round(x * 6000.0)/6000.0;
        Double y = observation.getDouble("latitude");
        y = Math.round(y * 6000.0)/6000.0;
        System.out.println(x);
        System.out.println(y);
        Coordinate featureCoordinate = new Coordinate(x, y, label);
        coordinates.add(featureCoordinate);
        index += 1;
      }
    } else {
      System.out.println("GET request didn't work");
    }
    return coordinates;
  }


  /** Stores input text and waypoints in a RouteEntity in datastore.
    * Returns nothing.
    */ 
  private static void storeInputAndWaypoints(String currSessionID, String textInput, ArrayList<ArrayList<Coordinate>> waypoints){
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
}
