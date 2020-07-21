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
import com.google.sps.data.Comment;

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
  // TODO: Add map of points to coordinates
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: return the coordinates that match the request
    Map<String, Coordinate> locations = createMap();
    String feature = request.getParameter("feature");
    if (locations.containsKey(feature)) {
      //do something
    }
  }

  private static Map<String, Coordinate> createMap() {
    Map<String, Coordinate> myMap = new HashMap<String, Coordinate>();
    myMap.put("clover", {x: 3, y: 2, label: "clover"});
    myMap.put("daisy", {x: 1, y: 3, label: "daisy"});
    myMap.put("bellflower", {x: 7, y: 8, label: "bellflower"});
    return myMap;
  }
}