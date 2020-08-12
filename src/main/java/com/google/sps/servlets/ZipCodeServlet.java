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
import com.google.sps.Coordinate;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.util.*;
import com.google.maps.GeoApiContext;
import com.google.maps.model.GeocodingResult;
import com.google.maps.GeocodingApi;
import com.google.maps.GaeRequestHandler;
import com.google.maps.errors.ApiException;
import java.lang.InterruptedException;

/** Servlet creates bounding box for searching generated routes off of zipcode input. 
  */ 
@WebServlet("/zip-code")
public class ZipCodeServlet extends HttpServlet{
    private static final Double BOUNDING_BOX_WIDTH = 0.07246376811; // 5 miles
    private final String API_KEY = "AIzaSyBaBCxBuGqZx0IGQ4lb9eKrICwU8Rduz3c";

    /** Sets zip code bounds in session attribute. */ 
    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();
        String zip = (String) session.getAttribute("zip-code");
        // need try catch here
        try{
            setBounds(request, zip);

        }catch(Exception e){}
        response.getWriter().println(zip);
    }
    
    /** Formats and stores zipcode in session attributes. */ 
    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Integer value = Integer.parseInt(request.getParameter("zip-code"));
        //fill in zeros on string 
        String zipcode = String.format("%05d", value);
        HttpSession session = request.getSession();
        session.setAttribute("zip-code", zipcode);
        response.sendRedirect("/generated-routes.html");
    }

      /** Gets bounds of zipcode and sets bounds in session attributes. */ 
    private void setBounds(HttpServletRequest request, String zipcode) throws IOException {
        GeoApiContext context = new GeoApiContext.Builder(new GaeRequestHandler.Builder())
            .apiKey(API_KEY)
            .build();
        GeocodingResult[] results;
        try {
            results =  GeocodingApi.geocode(context,
                zipcode).await();
        } catch(ApiException | InterruptedException ex){
            return;
        }
        Double max_x = results[0].geometry.bounds.northeast.lng;
        Double max_y = results[0].geometry.bounds.northeast.lat;
        Double min_x = results[0].geometry.bounds.southwest.lng;
        Double min_y = results[0].geometry.bounds.southwest.lat;
        HttpSession session = request.getSession();
        session.setAttribute("max_x", max_x);
        session.setAttribute("min_x", min_x);
        session.setAttribute("max_y", max_y);
        session.setAttribute("min_y", min_y);
    }
}