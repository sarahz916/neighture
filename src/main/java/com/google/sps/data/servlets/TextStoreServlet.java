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
    name = "TextStore",
    description = "TextStore: accesses text input",
    urlPatterns = "/text-store"
)
public class TextStoreServlet extends HttpServlet {
     
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
     // Show all the generated routes stored. 
    Query query = new Query("Route").addSort("timestamp", SortDirection.DESCENDING);

    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    PreparedQuery results = datastore.prepare(query);
    ArrayList<String> Inputs = new ArrayList<>();
    for (Entity entity : results.asIterable()) {
       Inputs.add((String) entity.getProperty("text"));
    }
    Gson gson = new Gson();

    response.setContentType("application/json");
    response.getWriter().println(gson.toJson(Inputs));
  }

}

