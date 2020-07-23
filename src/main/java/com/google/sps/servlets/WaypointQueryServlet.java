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
import com.google.gson.Gson;
import org.json.JSONObject;  

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;  
import java.util.ArrayList;

/** Servlet that handles the user's query by parsing out
  * the waypoint queries and their matching coordinates in 
  * the database.
  */
@WebServlet("/query")
public class WaypointQueryServlet extends HttpServlet {
  private ArrayList<Coordinate> waypoints = new ArrayList<Coordinate>();

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Return last stored waypoints
    response.setContentType("application/json");
    String json = new Gson().toJson(waypoints);
    System.out.println(json);
    response.getWriter().println(json);
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Since we got a new query, we can get rid of the old waypoints
    waypoints.clear();

    String input = request.getParameter("text-input");
    System.out.println(input);
    // Parse out feature requests from input
    String[] featureQueries = input.split(";[ ]?");
    for (String feature : featureQueries) {
      System.out.println(feature.toLowerCase());
      // Make call to database
      Coordinate location = sendGET(feature.toLowerCase());
      if (location != null) {
        waypoints.add(location);
      }
    }

    // Redirect back to the index page.
    response.sendRedirect("/index.html");
  }

  /** Sends a request for the input feature to the database
    * Returns the Coordinate matching the input feature 
    */ 
  private static Coordinate sendGET(String feature) throws IOException {
    //URL obj = new URL("http://localhost:8080/database?q=" + feature);
    URL obj = new URL("https://neighborhood-nature.appspot.com/database?q=" + feature);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		con.setRequestProperty("User-Agent", "Mozilla/5.0");
		int responseCode = con.getResponseCode();
		System.out.println("GET Response Code :: " + responseCode);
		if (responseCode == HttpURLConnection.HTTP_OK) { // success
			BufferedReader in = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String inputLine;
			StringBuffer response = new StringBuffer();


        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

			// print result
			System.out.println(response.toString());
      String responseString = response.toString();
      if (responseString.equals("{}")) {
        return null;
      }

      // Turn the response into a Coordinate
      JSONObject jsonObject = new JSONObject(responseString);
      Double x = jsonObject.getDouble("longitude");
      Double y = jsonObject.getDouble("latitude");
      Coordinate featureCoordinate = new Coordinate(x, y, feature);
      return featureCoordinate;
		} else {
			System.out.println("GET request didn't work");
      return null;
		}
  }
}
