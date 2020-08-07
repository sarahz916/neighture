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
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;
import com.google.sps.data.StoredRoute;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;



/** Servlet that returns comment data */
@WebServlet(
    name = "RouteStore",
    description = "RouteStore: stores text input and associated waypoints",
    urlPatterns = "/route-store"
)
public class RouteStoreServlet extends HttpServlet {
     
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
     // Show all the generated routes stored. 

    //TODO(zous): Put a limit on how many StoredRoutes sent to the dropdown menu in generatedroutes.html
    Query query = new Query("StoredRoute");

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    ArrayList<StoredRoute> routes = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
      long id = entity.getKey().getId();
      String text = (String) entity.getProperty("text");
      String waypointsJson = (String) entity.getProperty("actual-route");
      if (waypointsJson != null){
        StoredRoute route = new StoredRoute(id, text, waypointsJson);
        routes.add(route);
      }
    }
    Gson gson = new Gson();

    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(routes));
  }

}

