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
import java.util.Enumeration;

/** Servlet that returns checkbox data */
@WebServlet("/chosen-waypoints")
public class ChosenWaypointsServlet extends HttpServlet {
    private ArrayList<Coordinate> waypoints = new ArrayList<Coordinate>();
    
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Set response content type
    response.setContentType("application/json");

    Enumeration paramNames = request.getParameterNames();

    while(paramNames.hasMoreElements()) {
    String responseString = (String)paramNames.nextElement();
    JSONObject jsonObject = new JSONObject(responseString);
    System.out.println(jsonObject);
    Double x = jsonObject.getDouble("x");
    Double y = jsonObject.getDouble("y");
    String feature = jsonObject.getString("label");
    Coordinate featureCoordinate = new Coordinate(x, y, feature);
    waypoints.add(featureCoordinate);
    }
    String json = new Gson().toJson(waypoints);
    response.getWriter().println(json);

    // After the map is made, we can get rid of the old waypoints
    //waypoints.clear();
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doGet(request, response);
    }

}

