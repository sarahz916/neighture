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

package com.google.sps.servlets;
import com.google.sps.SessionDataStore;
import com.google.sps.Coordinate;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import java.net.HttpURLConnection;
import java.net.URL;  
import com.google.maps.GeoApiContext;
import com.google.maps.model.GeocodingResult;
import com.google.maps.GeocodingApi;
import com.google.maps.GaeRequestHandler;
import com.google.maps.errors.ApiException;
import java.lang.InterruptedException;
import org.json.JSONObject;
import org.json.JSONException;

/** Servlet that returns text input data */
@WebServlet(
    name = "StartEnd",
    description = "StartEnd: records start end location of user",
    urlPatterns = "/start-end"
)
public class StartEndServlet extends HttpServlet {
    private final String API_KEY = "AIzaSyBaBCxBuGqZx0IGQ4lb9eKrICwU8Rduz3c";
    private static final Double MILES_TO_COORDINATES = 69.0;
    private static final Double DEFAULT_RADIUS = 5.0;
    private static final String DEFAULT_COORDINATE_STRING = String.valueOf(DEFAULT_RADIUS / MILES_TO_COORDINATES);

  /** Fetches start and end data associated with Session ID */
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      SessionDataStore sessionDataStore = new SessionDataStore(request);
      try{
          JSONObject start = new JSONObject(sessionDataStore.fetchSessionEntity("StartEnd", "start"));
          JSONObject end = new JSONObject(sessionDataStore.fetchSessionEntity("StartEnd", "end"));
          JSONObject midpoint = new JSONObject(sessionDataStore.fetchSessionEntity("StartEnd", "midpoint"));
          JSONObject startendmid = new JSONObject();
          startendmid.put("start", start);
          startendmid.put("end", end);
          startendmid.put("midpoint", midpoint);
          response.setContentType("application/json");
          response.getWriter().println(startendmid.toString());
      }catch(JSONException e){
          response.getWriter().println("[]");
      }
  }

 /** Stores start, end and midpoint locations as JSON Strings of Coordinates with Session ID */
 @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try {
            SessionDataStore sessionDataStore = new SessionDataStore(request);
            //Get StartEnd from request
            String start = request.getParameter("startloc-input");
            String end;
            // Check if this is a loop or one-way route and get the end accordingly.
            if (request.getParameterMap().containsKey("endloc-input")) {
                end = request.getParameter("endloc-input");
            } else {
                end = start;
                Double radiusInMiles = getRadius(request);
                if (radiusInMiles == -1.0) {
                  response.setContentType("text/html");
                  response.getWriter().println("Invalid number; please enter a nonnegative number. "
                   + "Your radius will be set to " + String.valueOf(DEFAULT_RADIUS) + " miles.");
                  sessionDataStore.storeProperty(ENTITY_TYPE, "radius", DEFAULT_COORDINATE_STRING);
                } else if (radiusInMiles == 0.0) {
                  sessionDataStore.storeProperty(ENTITY_TYPE, "radius", DEFAULT_COORDINATE_STRING);
                } else {
                  Double radiusInCoordinates = radiusInMiles / MILES_TO_COORDINATES;
                  sessionDataStore.storeProperty(ENTITY_TYPE, "radius", String.valueOf(radiusInCoordinates));
                }
            }
            //Request GeoCoding API for coordinates
            Coordinate startCoord = getGeoLocation(start);
            Coordinate endCoord = getGeoLocation(end);
            //Get midpoint Coordinate
            Coordinate midCoord = getMidpoint(startCoord, endCoord);
            // Store start and end in datastore with ID.
            SessionDataStore sessionDataStore = new SessionDataStore(request);
            sessionDataStore.storeProperty("StartEnd", "start", new Gson().toJson(startCoord));
            sessionDataStore.storeProperty("StartEnd", "end", new Gson().toJson(endCoord));
            sessionDataStore.storeProperty("StartEnd", "midpoint", new Gson().toJson(midCoord));
            // Redirect back to the create-route page.
            response.sendRedirect("/create-route.html");
        } catch(Exception e){
            response.sendRedirect("/error-page.html");
        }
    }

  /** Gets geoLocation via GeoEncoding API and returns Coordinate of place */ 
    private Coordinate getGeoLocation(String placeQuery) throws IOException {
        GeoApiContext context = new GeoApiContext.Builder(new GaeRequestHandler.Builder())
            .apiKey(API_KEY)
            .build();
        GeocodingResult[] results;
        try {
            results =  GeocodingApi.geocode(context,
                placeQuery).await();
        } catch(ApiException | InterruptedException ex){
            return null;
        }
        Double y = results[0].geometry.location.lat;
        Double x = results[0].geometry.location.lng;
        Coordinate startEndCoordinate  = new Coordinate(x, y, placeQuery, "");
        return startEndCoordinate;
    }

  /**Returns Coordinate of midpoint between start and end Coordinates */ 
    private Coordinate getMidpoint(Coordinate start, Coordinate end){
        Double y = (start.getY() + end.getY())/2;
        Double x = (start.getX() + end.getX())/2;
        return new Coordinate(x, y, "midpoint", "");
    }

  /** Returns the radius of the loop entered by the user, -1 if the input was invalid, 0 for no input. */
  private Double getRadius(HttpServletRequest request) {
    // Get the input from the form.
    String radiusString = request.getParameter("radius");
    if (radiusString.isEmpty()) {
      return 0.0;
    }

    // Convert the input to a double.
    Double radius;
    try {
      radius = Double.parseDouble(radiusString);
    } catch (NumberFormatException e) {
      System.err.println("Could not convert to double: " + radiusString);
      return -1.0;
    }

    // Check that the input is greater than 0.
    if (radius <=0) {
      System.err.println("Player choice is out of range: " + radiusString);
      return -1.0;
    }
    return radius;
  }
}
