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

import java.net.HttpURLConnection;
import java.net.URL; 
import com.google.cloud.language.v1.Document;
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

/** Servlet that returns comment data */
@WebServlet(
    name = "RouteStore",
    description = "RouteStore: stores text input and associated waypoints",
    urlPatterns = "/route-store"
)
public class RouteStoreServlet extends HttpServlet {
     
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
     // Let's first just return the json object {text input: waypoints}
     
     // not sure how to query the datastore
    Query query = new Query("Route").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);

    Gson gson = new Gson();
    response.setContentType("application/json");
    response.getWriter().println(request.getRequestURL());
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    
    // Get the text input that came with the query
    String thisUrl = request.getRequestURI();
    URL queryservlet = new URL("");

    // Get the waypoints that came wtih the query
    //String text = getParameter(request, "text-input", "");
    // how do we want waypoints stored (by coordinates or names?) see how Waypoint Query communicates with route-script.js
    Entity RouteEntity = new Entity("Route");
    //RouteEntity.setProperty("text", text);
    //RouteEntity.setProperty("waypoints", waypoints);

    // Store Route.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(RouteEntity);

  }
}

