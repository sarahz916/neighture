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

package com.example.appengine.users;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;

/** Servlet that searches for a given name in a database, 
  * returning the coordinates as part of a JSON
  */
@WebServlet("/database")
public class DatabaseServlet extends HttpServlet {
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    Map<String, String> locations = createMap();
    String feature = request.getParameter("q");
    if (locations.containsKey(feature)) { // Feature is in the database
      // Return all waypoints with feature
      response.setContentType("application/json");
      response.getWriter().println(locations.get(feature));
    } else {
      response.setContentType("application/json");
      response.getWriter().println("[]");
    }
  }

  /** Returns a hard-coded map of features to waypoints 
    */
  private static Map<String, String> createMap() {
    Map<String, String> myMap = new HashMap<String, String>();
    myMap.put("clover", "{\"latitude\": 40.847881, \"longitude\": -87.653633, \"common_name\": {\"name\": \"clover\"}}");
    myMap.put("daisy", "{\"latitude\": 40.826222, \"longitude\": -87.646227,  \"common_name\": {\"name\": \"daisy\"}}");
    myMap.put("bellflower", "{\"latitude\": 40.855942, \"longitude\": -87.643636,  \"common_name\": {\"name\": \"bellflower\"}}");
    return myMap;
  }
}