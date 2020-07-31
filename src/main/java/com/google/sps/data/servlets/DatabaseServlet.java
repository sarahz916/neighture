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
    myMap.put("clover", "[{\"latitude\": 41.855967, \"longitude\": -87.635604, \"common_name\": {\"name\": \"clover\"}}]");
    myMap.put("daisy", "[{\"latitude\": 41.848653, \"longitude\": -87.629454, \"common_name\": {\"name\": \"daisy\"}}]");
    myMap.put("bellflower", "[{\"latitude\": 41.843539, \"longitude\": -87.647480, \"common_name\": {\"name\": \"bellflower\"}}]");
    myMap.put("tulip", "[{\"latitude\": 41.855223, \"longitude\": -87.631930, \"common_name\": {\"name\": \"tulip\"}}]");
    myMap.put("mushroom", "[{\"latitude\": 41.898864, \"longitude\": -87.622965, \"common_name\": {\"name\": \"mushroom\"}}, {\"latitude\": 41.898912, \"longitude\": -87.642910, \"common_name\": {\"name\": \"mushroom\"}}]");
    myMap.put("meadowsweet", "[{\"latitude\": 41.897427, \"longitude\": -87.619934, \"common_name\": {\"name\": \"park\"}}]");
    myMap.put("sunflower", "[{\"latitude\": 41.897521, \"longitude\": -87.619934, \"common_name\": {\"name\": \"sunflower\"}}]");
    myMap.put("tree", "[{\"latitude\": 41.897219, \"longitude\": -87.622235, \"common_name\": {\"name\": \"tree\"}}]");
    myMap.put("lichen", "[{\"latitude\": 41.897219, \"longitude\": -87.622235, \"common_name\": {\"name\": \"lichen\"}}]");
    myMap.put("raspberry", "[{\"latitude\": 41.897946, \"longitude\": -87.622112, \"common_name\": {\"name\": \"raspberry\"}}, {\"latitude\": 41.896968, \"longitude\": -87.624580, \"common_name\": {\"name\": \"raspberry\"}}, {\"latitude\": 41.888454, \"longitude\": -87.623920, \"common_name\": {\"name\": \"raspberry\"}}]");
    return myMap;
  }
}
