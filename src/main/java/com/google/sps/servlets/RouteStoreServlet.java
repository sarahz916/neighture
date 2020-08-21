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
import com.google.sps.Coordinate;
import com.google.sps.SessionDataStore;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.util.*;
import com.google.gson.Gson;
import com.google.sps.data.StoredRoute;
import com.google.sps.data.SortedStoredRoute;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;



/** Servlet returns stored route data based off of user input start/end locations if possible*/
@WebServlet(
    name = "RouteStore",
    description = "RouteStore: stores text input and associated waypoints",
    urlPatterns = "/route-store"
)
public class RouteStoreServlet extends HttpServlet {
  private final Integer NUM_RESULTS = 6;
  private final Double CUTOFF_IN_MILES = 15.0;
  private static final Double MILES_TO_COORDINATES = 69.0;
  private final Double DISTANCE_CUTOFF_COORD = CUTOFF_IN_MILES / MILES_TO_COORDINATES; 
     
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    ArrayList<StoredRoute> routes;
    try{    
        SessionDataStore sessionDataStore = new SessionDataStore(request);
        Coordinate midpoint = sessionDataStore.getPoint("midpoint");
        routes = getOrderedStoredRoutes(midpoint.toGeoPt());
    }catch(Exception e){  
        //If there is no midpoint or someother error just return StoredRoutes in no particular order.
        routes = getUnorderedStoredRoutes();
    }
    Gson gson = new Gson();

    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(routes));
  }

/** Returns sum of squared latitude difference and squared longitude differnce between two GeoPt */
  private float getDistance(GeoPt one, GeoPt two){
      float lat_dif = one.getLatitude() - two.getLatitude();
      float lng_dif = one.getLongitude() - two.getLongitude();
      return (float) Math.sqrt(lat_dif*lat_dif + lng_dif*lng_dif);
  }

/** Returns ArrayList of StoredRoutes in no particular order. */
  private ArrayList<StoredRoute> getUnorderedStoredRoutes(){
    Query query = new Query("StoredRoute");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    ArrayList<StoredRoute> routes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      Object textObject = entity.getProperty("text");
      Object waypointsJsonObject = entity.getProperty("actual-route");
      String text;
      String waypointsJson;
      try{
        Text textText = (Text) textObject;
        Text waypointsJsonText = (Text) waypointsJsonObject;
        text = textText.getValue();
        waypointsJson = waypointsJsonText.getValue();
      } catch(ClassCastException e){ //some old routes in datastore are stored as strings
        text = (String) textObject;
        waypointsJson = (String) waypointsJsonObject;
      }
      if(routes.size() <= NUM_RESULTS){ //only want to return NUM_RESULTS routes
        if (waypointsJson != null){
            StoredRoute route = new StoredRoute(id, text, waypointsJson);
            routes.add(route);
        }
      }
      else{
          break;
      }
    }
    return routes;
  }

/** Returns ArrayList of StoredRoutes with closest routes to midpoint first. */
  private ArrayList<StoredRoute> getOrderedStoredRoutes(GeoPt midpoint){
    PriorityQueue<SortedStoredRoute> priorityQueue = new PriorityQueue<>();
    Query query = new Query("StoredRoute");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    ArrayList<StoredRoute> routes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      Object textObject = entity.getProperty("text");
      Object waypointsJsonObject = entity.getProperty("actual-route");
      String text;
      String waypointsJson;
      try{
        Text textText = (Text) textObject;
        Text waypointsJsonText = (Text) waypointsJsonObject;
        text = textText.getValue();
        waypointsJson = waypointsJsonText.getValue();
      } catch(ClassCastException e){ //some old routes in datastore are stored as strings
        text = (String) textObject;
        waypointsJson = (String) waypointsJsonObject;
      }
      GeoPt center;
      center = (GeoPt) entity.getProperty("center-of-mass");
      //have function to calculate distance from midpoint
      float distance = getDistance(center, midpoint);
      if ((waypointsJson != null) && (distance < DISTANCE_CUTOFF_COORD.floatValue())){
        StoredRoute route = new StoredRoute(id, text, waypointsJson);
        SortedStoredRoute sortRoute = new SortedStoredRoute(distance, route);
        priorityQueue.add(sortRoute);
      }
    }
    while(routes.size() <= NUM_RESULTS){
        SortedStoredRoute sortRoute = priorityQueue.poll();
        if (sortRoute == null) break;
        routes.add(sortRoute.getRoute());
    }
    return routes;
  }

}

