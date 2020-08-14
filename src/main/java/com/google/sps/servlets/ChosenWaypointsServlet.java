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
import com.google.sps.SessionDataStore;
import com.google.gson.Gson;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;

/** Servlet that scans for which checkboxes are checked and returns the selected
  * waypoints as Coordinates. Returns a JSON String of ArrayList<Coordinates>
  */ 
@WebServlet("/chosen-waypoints")
public class ChosenWaypointsServlet extends HttpServlet {
    private final ArrayList<String> FIELDS_MODIFIED = new ArrayList<String>( 
            Arrays.asList("chosenFetched", "textFetched"));
    

    /** Goes through datastore to find most recent Direction Entity associated with SessionID.
    *   Returns the waypoints ArrayList<Coordinates> as a JSON String in response. 
    */ 
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      SessionDataStore sessionDataStore = new SessionDataStore(request);
      String waypointsJSONstring = sessionDataStore.queryOnlyifFirstFetch("chosenFetched", "Route",  "actual-route");
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
        SessionDataStore sessionDataStore = new SessionDataStore(request);
        // Store input text and waypoint in datastore.
        sessionDataStore.storeProperty("Route", "actual-route", (String) new Gson().toJson(waypoints));
        Coordinate centerOfmass = getCenterofMass(waypoints);
        sessionDataStore.storeIndexedProperty("Route", "center-of-mass", centerOfmass);
        sessionDataStore.storeStoredRoute();
        sessionDataStore.setSessionAttributes(FIELDS_MODIFIED);
        // Redirect back to the index page.
        response.sendRedirect("/create-route.html");
    }

    /** Scans the checkbox form for checked coordinates and appends that to waypoints. 
    *   Returns waypoints as ArrayList<Coordinates>
    */ 
    public ArrayList<Coordinate> getWaypointsfromRequest(HttpServletRequest request){
        Enumeration paramNames = request.getParameterNames();
        ArrayList<Coordinate> waypoints = new ArrayList<Coordinate>();
        while(paramNames.hasMoreElements()) {
            // Name of checkbox is JSON String of Coordinate Object
            String responseString = (String)paramNames.nextElement();
            JSONObject jsonObject = new JSONObject(responseString);
            Double x = jsonObject.getDouble("x");
            Double y = jsonObject.getDouble("y");
            String feature = jsonObject.getString("label");
            String species = jsonObject.getString("species");
            Coordinate featureCoordinate = new Coordinate(x, y, feature, species);
            waypoints.add(featureCoordinate);
        }
        return waypoints;
    }
    /** Averages latitude and longititude of all waypoints and returns Coordinate of center. 
    */ 
    private Coordinate getCenterofMass(ArrayList<Coordinate> waypoints){
        int num_pts = waypoints.size(); 
        Double avg_x = 0.0;
        Double avg_y = 0.0;
        for (Coordinate pt: waypoints){
            avg_x += pt.getX();
            avg_y += pt.getY();
        }
        avg_x = avg_x / num_pts;
        avg_y = avg_y / num_pts;
        Coordinate center =  new Coordinate(avg_x, avg_y, "midpoint", "");
        return center;
    }

    
}