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
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

/** Servlet that returns text input data */
@WebServlet(
    name = "TextStore",
    description = "TextStore: accesses text input",
    urlPatterns = "/text-store"
)
public class TextStoreServlet extends HttpServlet {

  /** Looks up text input from Route Entites by Session ID in datastore.
  *   Returns text of most recent input-text from session ID. 
  */ 
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    HttpSession currentSession = request.getSession();
    String currSessionID = currentSession.getId();
    String textinput = getTextInputforSession(currSessionID);
    response.setContentType("text/plain");
    response.getWriter().println(textinput);
  }
 
 
  /** Looks up text input from Route Entites by Session ID in datastore.
  *   Returns text of most recent input-text from session ID. 
  */ 
  private String getTextInputforSession(String currSessionID){
    //Retrieve text-input for that session.
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
    String text = (String) MostRecentStore.getProperty("text");
    return text;
  }

}
