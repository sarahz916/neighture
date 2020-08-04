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
import org.json.JSONObject;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;  

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;  
import java.util.*;

/** Servlet that scans for which checkboxes are checked and returns the selected
  * waypoints as Coordinates. Returns a JSON String of ArrayList<Coordinates>
  */ 
@WebServlet("/chosen-waypoints")
public class ChosenWaypointsServlet extends HttpServlet {
    
    /** Goes through datastore to find most recent Direction Entity associated with SessionID.
    *   Returns the waypoints ArrayList<Coordinates> as a JSON String in response. 
    */ 
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String currSessionID = getSession(request);
   
    String waypointsJSONstring = getQueryResultsforSession(currSessionID);
    String waypointsJSONstring = "[]";
    System.out.println("Exception occurred");
    // Return last stored waypoints
    response.setContentType("application/json");
    response.getWriter().println(waypointsJSONstring);
    }

    /** Scans the checkbox form for checked coordinates and appends that to waypoints. 
    *   Waypoints if stored in Datastore as a Direction Entity associated with session ID.
    */ 
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //TODO (zous): should we throw exceptions/error if there are no checked checkboxes
        ArrayList<Coordinate> waypoints = getWaypointsfromRequest(request);
        // Store input text and waypoint in datastore.
        String currSessionID = getSession(request);
        storeInputAndWaypoints(currSessionID, waypoints);
        // Redirect back to the index page.
        response.sendRedirect("/index.html");
    }

    private static String getSession(HttpServletRequest request){
        HttpSession currentSession = request.getSession();
        String currSessionID = currentSession.getId();
        return currSessionID;
    }

    /** Scans the checkbox form for checked coordinates and appends that to waypoints. 
    *   Returns waypoints as ArrayList<Coordinates>
    */ 
    private ArrayList<Coordinate> getWaypointsfromRequest(HttpServletRequest request){
        Enumeration paramNames = request.getParameterNames();
        ArrayList<Coordinate> waypoints = new ArrayList<Coordinate>();
        while(paramNames.hasMoreElements()) {
            // Name of checkbox is JSON String of Coordinate Object
            String responseString = (String)paramNames.nextElement();
            JSONObject jsonObject = new JSONObject(responseString);
            Double x = jsonObject.getDouble("x");
            Double y = jsonObject.getDouble("y");
            String feature = jsonObject.getString("label");
            Coordinate featureCoordinate = new Coordinate(x, y, feature);
            waypoints.add(featureCoordinate);
        }
        return waypoints;
    }

     /** Stores input text and waypoints in a DirectionEntity in datastore.
    * Returns nothing.
    */ 
    private static void storeInputAndWaypoints(String currSessionID, ArrayList<Coordinate> waypoints){
        Entity DirectionEntity = new Entity("Direction");
        DirectionEntity.setProperty("session-id", currSessionID);
        String json = new Gson().toJson(waypoints);
        long timestamp = System.currentTimeMillis();
        DirectionEntity.setProperty("timestamp", timestamp);
        // Store as a json string because Coordinates are unsupported.
        DirectionEntity.setProperty("waypoints", json);
        // Store Direction.
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        datastore.put(DirectionEntity);
    }

    /** Filters through  Direction Entities to find the one for currSessionID.
    * Returns the choosen waypoints as a JSON String.
    */     
    private String getQueryResultsforSession(String currSessionID){
        //Retrieve Waypoints for that session.
        Filter sesionFilter =
        new FilterPredicate("session-id", FilterOperator.EQUAL, currSessionID);
        // sort by most recent query for session ID
        Query query = 
                new Query("Direction")
                    .setFilter(sesionFilter)
                    .addSort("timestamp", SortDirection.DESCENDING);
        DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
        List results = datastore.prepare(query).asList(FetchOptions.Builder.withLimit(1));
        Entity MostRecentStore = (Entity) results.get(0);
        String waypointsJSONstring = (String) MostRecentStore.getProperty("waypoints");
        return waypointsJSONstring;
    }

}