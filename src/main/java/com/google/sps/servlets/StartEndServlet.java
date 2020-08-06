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


  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      SessionDataStore sessionDataStore = new SessionDataStore(request);
      /*String valueJSONString = sessionDataStore.queryOnlyifFirstFetch(FETCH_FIELD, ENTITY_TYPE, FETCH_PROPERTY);
      response.setContentType("application/json");
      response.getWriter().println(valueJSONString);*/
  }

 @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        //Get StartEnd from request
        
        /*SessionDataStore sessionDataStore = new SessionDataStore(request);
        // Store input text and waypoint in datastore.
        sessionDataStore.storeProperty(ENTITY_TYPE, FETCH_PROPERTY, waypoints);
        sessionDataStore.storeStoredRoute();
        sessionDataStore.setSessionAttributes(FIELDS_MODIFIED);
        // Redirect back to the index page.
        response.sendRedirect("/index.html");*/
}
