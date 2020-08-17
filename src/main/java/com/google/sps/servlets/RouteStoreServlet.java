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



/** Servlet returns stored route data based off of user input zipcode */
@WebServlet(
    name = "RouteStore",
    description = "RouteStore: stores text input and associated waypoints",
    urlPatterns = "/route-store"
)
public class RouteStoreServlet extends HttpServlet {
  private final Integer NUM_RESULTS = 10;
     
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession session = request.getSession();
    ArrayList<StoredRoute> routes;
    try{    
        //If there is zipcode attributes, return StoredRoutes with closest Routes to zipcode first.
        //Maybe don't use zip code but use start and end locations
        Double zip_lng = (Double) session.getAttribute("zip_x");
        Double zip_lat = (Double) session.getAttribute("zip_y");
        GeoPt zip = new GeoPt(zip_lat.floatValue(), zip_lng.floatValue());
        routes = getOrderedStoredRoutes(zip);
    }catch(Exception e){  
        //If there is no zipcode or someother error just return StoredRoutes in no particular order.
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
      return (lat_dif*lat_dif + lng_dif*lng_dif);
  }

/** Returns ArrayList of StoredRoutes in no particular order. */
  private ArrayList<StoredRoute> getUnorderedStoredRoutes(){
    Query query = new Query("StoredRoute");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    ArrayList<StoredRoute> routes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String text = (String) entity.getProperty("text");
      String waypointsJson = (String) entity.getProperty("actual-route");
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

/** Returns ArrayList of StoredRoutes with closest routes to zipcode first. */
  private ArrayList<StoredRoute> getOrderedStoredRoutes(GeoPt zip){
    PriorityQueue<SortedStoredRoute> priorityQueue = new PriorityQueue<>();
    Query query = new Query("StoredRoute");
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    ArrayList<StoredRoute> routes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String text = (String) entity.getProperty("text");
      String waypointsJson = (String) entity.getProperty("actual-route");
      GeoPt center;
      try{
        center = (GeoPt) entity.getProperty("center-of-mass");
      }catch(Exception e){ //Some datastore center-of-mass if not as GeoPt, don't include those
          continue;
      }
      //have function to calculate distance from zip code center
      float distancefromZip = getDistance(center, zip);
      if (waypointsJson != null){
        StoredRoute route = new StoredRoute(id, text, waypointsJson);
        SortedStoredRoute sortRoute = new SortedStoredRoute(distancefromZip, route);
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

