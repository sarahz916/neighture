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
import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.*;  

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;  
import java.util.*;

/** Servlet that scans for which checkboxes are checked and returns the selected
  * waypoints as Coordinates. Returns a JSON String of ArrayList<Coordinates>
  */ 
@WebServlet("/gen-route")
public class GenRouteServlet extends HttpServlet {
    private ArrayList<String> DropDownvalue = new ArrayList<String>();

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String value = request.getParameter("routes-drop-down");
        DropDownvalue.add(value);
        // Return last stored waypoints
        response.sendRedirect("/generated-routes.html");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("application/json");
    if(!DropDownvalue.isEmpty()){
        response.getWriter().println(DropDownvalue.get(0));
        DropDownvalue.clear();
    }
    else{
        response.getWriter().println("[]");
    }
    }

}