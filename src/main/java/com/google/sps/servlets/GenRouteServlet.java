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
@WebServlet("/gen-route")
public class GenRouteServlet extends HttpServlet {
    private final ArrayList<String> FIELDS_MODIFIED = new ArrayList<String>( 
            Arrays.asList("genRouteFetch"));
    private final String FETCH_FIELD = "genRouteFetch";
    private final String FETCH_PROPERTY = "gen-route";
    private final String ENTITY_TYPE = "genRoute";
    
    /** Retrieves past generated route user selected on generatedroutes.html
    */ 
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      SessionDataStore sessionDataStore = new SessionDataStore(request);
      String waypointsJSONstring = sessionDataStore.queryOnlyifFirstFetch(FETCH_FIELD, ENTITY_TYPE, FETCH_PROPERTY);
      response.setContentType("application/json");
      response.getWriter().println(waypointsJSONstring);
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String value = request.getParameter("routes-drop-down");
        SessionDataStore sessionDataStore = new SessionDataStore(request);
        // Store input text and waypoint in datastore.
        sessionDataStore.storeProperty(ENTITY_TYPE, FETCH_PROPERTY, value);
        sessionDataStore.setSessionAttributes(FIELDS_MODIFIED);
        // Return last stored waypoints
        response.sendRedirect("/generated-routes.html");
    }


}