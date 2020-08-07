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

import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

/** Servlet that returns text input data */
@WebServlet(
    name = "StartEnd",
    description = "StartEnd: records start end location of user",
    urlPatterns = "/start-end"
)
public class StartEndServlet extends HttpServlet {
    private final String ENTITY_TYPE =  "StartEnd";
    private ArrayList<String> results = new ArrayList<String>();


  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      SessionDataStore sessionDataStore = new SessionDataStore(request);
      String start = "\"" + sessionDataStore.fetchSessionEntity(ENTITY_TYPE, "start") + "\"";
      String end = "\"" + sessionDataStore.fetchSessionEntity(ENTITY_TYPE, "end") + "\"";
      String StartEndJson = "{ \"start\":" + start + ", \"end\":" + end + "}";
      response.setContentType("application/json");
      response.getWriter().println(StartEndJson);
  }

 @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //Get StartEnd from request
        String start = request.getParameter("startloc-input");
        String end = request.getParameter("endloc-input");
        results.add(start);
        results.add(end);
        SessionDataStore sessionDataStore = new SessionDataStore(request);
        // Store start and end in datastore with ID.
        sessionDataStore.storeProperty(ENTITY_TYPE, "start", start);
        sessionDataStore.storeProperty(ENTITY_TYPE, "end", end);
        // Redirect back to the index page.
        response.sendRedirect("/index.html");
    }
}
