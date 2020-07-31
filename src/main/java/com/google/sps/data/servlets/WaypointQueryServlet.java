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
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import org.json.JSONObject;  
import org.json.JSONArray;  
import org.apache.commons.math3.util.Precision;

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
import java.util.List;
import java.util.Calendar;
import java.text.DecimalFormat;

// Imports the Google Cloud client library
import com.google.cloud.language.v1.Document;
import com.google.cloud.language.v1.Document.Type;
import com.google.cloud.language.v1.LanguageServiceClient;
import com.google.cloud.language.v1.AnalyzeSyntaxRequest;
import com.google.cloud.language.v1.AnalyzeSyntaxResponse;
import com.google.cloud.language.v1.EncodingType;
import com.google.cloud.language.v1.Token;

/** Servlet that handles the user's query by parsing out
  * the waypoint queries and their matching coordinates in 
  * the database.
  */
@WebServlet("/query")
public class WaypointQueryServlet extends HttpServlet {
  private ArrayList<List<Coordinate>> waypoints = new ArrayList<List<Coordinate>>();
  // The maximum number of coordinates will be an optional input by the user, with 5 as the default
  private static int maxNumberCoordinates = 5;

  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // Return last stored waypoints
    response.setContentType("application/json");
    String json = new Gson().toJson(waypoints);
    response.getWriter().println(json);

    // After the map is made, we can get rid of the old waypoints
    waypoints.clear();
  }

  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String input = request.getParameter("text-input");
    getLocations(input);
 
    // Redirect back to the index page.
    response.sendRedirect("/index.html");
  }

  /** Using the input text, fetches waypoints from the database to be 
    * used by the frontend
    */
  private void getLocations(String input) throws IOException {
    //analyzeSyntaxText(input);
    // Parse out feature requests from input
    ArrayList<ArrayList<String>> featureRequests = processInputText(input);
    for (ArrayList<String> featureRequest : featureRequests) {
      String waypointQuery = featureRequest.get(0);
      ArrayList<Coordinate> potentialCoordinates = new ArrayList<Coordinate>();
      for (int i = 1; i < featureRequest.size(); i++) {
        String feature = featureRequest.get(i);
        // Make call to database
        ArrayList<Coordinate> locations = fetchFromDatabase(feature, waypointQuery);
        if (i == 1) {
          potentialCoordinates.addAll(locations);
        } else if (!locations.isEmpty()) {
          potentialCoordinates.retainAll(locations);
        }
      }
      List<Coordinate> locations = potentialCoordinates.subList(0, Math.min(maxNumberCoordinates, potentialCoordinates.size()));
      waypoints.add(locations);
    }   
    // Store input text and waypoint in datastore.
    storeInputAndWaypoints(input, waypoints);
  }

  /** Parses the input string to separate out all the features
    * For each arraylist of strings, the first element is the general waypoint query
    */
  private static ArrayList<ArrayList<String>> processInputText(String input) {
    ArrayList<ArrayList<String>> allFeatures = new ArrayList<ArrayList<String>>();
    String[] waypointQueries = input.split("[;.?!+]+");
    for (String waypointQuery : waypointQueries) {
      waypointQuery = waypointQuery.toLowerCase().trim();
      ArrayList<String> featuresOneWaypoint = new ArrayList<String>();
      featuresOneWaypoint.add(waypointQuery);
      String[] featureQueries = waypointQuery.split("[, ]+");
      for (int i = 0; i < featureQueries.length; i++) {
        String feature = featureQueries[i].toLowerCase().trim();
        featuresOneWaypoint.add(feature);
      }
      allFeatures.add(featuresOneWaypoint);
    }
    return allFeatures;
  }

  /** from the string {@code text}. */
  public static List<Token> analyzeSyntaxText(String text) throws IOException {
    // [START language_syntax_text]
    // Instantiate the Language client com.google.cloud.language.v1.LanguageServiceClient
    try (LanguageServiceClient language = LanguageServiceClient.create()) {
      Document doc = Document.newBuilder().setContent(text).setType(Type.PLAIN_TEXT).build();
      AnalyzeSyntaxRequest request =
          AnalyzeSyntaxRequest.newBuilder()
              .setDocument(doc)
              .setEncodingType(EncodingType.UTF16)
              .build();
      // analyze the syntax in the given text
      AnalyzeSyntaxResponse response = language.analyzeSyntax(request);
      // print the response
      for (Token token : response.getTokensList()) {
        System.out.printf("\tText: %s\n", token.getText().getContent());
        System.out.printf("\tBeginOffset: %d\n", token.getText().getBeginOffset());
        System.out.printf("Lemma: %s\n", token.getLemma());
        System.out.printf("PartOfSpeechTag: %s\n", token.getPartOfSpeech().getTag());
        System.out.printf("\tAspect: %s\n", token.getPartOfSpeech().getAspect());
        System.out.printf("\tCase: %s\n", token.getPartOfSpeech().getCase());
        System.out.printf("\tForm: %s\n", token.getPartOfSpeech().getForm());
        System.out.printf("\tGender: %s\n", token.getPartOfSpeech().getGender());
        System.out.printf("\tMood: %s\n", token.getPartOfSpeech().getMood());
        System.out.printf("\tNumber: %s\n", token.getPartOfSpeech().getNumber());
        System.out.printf("\tPerson: %s\n", token.getPartOfSpeech().getPerson());
        System.out.printf("\tProper: %s\n", token.getPartOfSpeech().getProper());
        System.out.printf("\tReciprocity: %s\n", token.getPartOfSpeech().getReciprocity());
        System.out.printf("\tTense: %s\n", token.getPartOfSpeech().getTense());
        System.out.printf("\tVoice: %s\n", token.getPartOfSpeech().getVoice());
        System.out.println("DependencyEdge");
        System.out.printf("\tHeadTokenIndex: %d\n", token.getDependencyEdge().getHeadTokenIndex());
        System.out.printf("\tLabel: %s\n\n", token.getDependencyEdge().getLabel());
      }
      return response.getTokensList();
    } catch (IOException e) {
      System.out.println("Something went wrong");
      System.out.println(e);
      return new ArrayList<Token>();
    }
    // [END language_syntax_text]
  }

  /** Sends a request for the input feature to the database
    * Returns the Coordinate matching the input feature 
    */ 
  private static ArrayList<Coordinate> fetchFromDatabase(String feature, String label) throws IOException {
    String startDate = getStartDate();
    String json = sendGET(feature);
    if (json != null) {
      return jsonToCoordinates(json, label);
    }
    return new ArrayList<Coordinate>();
  }

  /** Returns the date a year ago in string form
    * Note: not used in tests (mocked out)
    */
  private static String getStartDate() {
    Calendar previousYear = Calendar.getInstance();
    previousYear.add(Calendar.YEAR, -1);
    DecimalFormat formatter = new DecimalFormat("00"); // To allow leading 0s
    String yearAsString = formatter.format(previousYear.get(Calendar.YEAR));
    String monthAsString = formatter.format(previousYear.get(Calendar.MONTH));
    String dayAsString = formatter.format(previousYear.get(Calendar.DATE));
    String dateString = yearAsString + "-" + monthAsString + "-" + dayAsString;
    return dateString;
  }

  /** Sends a request for the input feature to the database and returns 
    * a JSON of the features
    */ 
  private static String sendGET(String feature) throws IOException {
    //URL obj = new URL("https://neighborhood-nature.appspot.com/database?q=" + feature);
    URL obj = new URL("http://localhost:8080/database?q=" + feature);
    HttpURLConnection con = (HttpURLConnection) obj.openConnection();
    con.setRequestMethod("GET");
    con.setRequestProperty("User-Agent", "Mozilla/5.0");
    int responseCode = con.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) { // success
      BufferedReader in = new BufferedReader(new InputStreamReader(
              con.getInputStream()));
      String inputLine;
      StringBuffer response = new StringBuffer(); 

      while ((inputLine = in.readLine()) != null) {
        response.append(inputLine);
      }
      in.close();
      return response.toString();
    } else {
      System.out.println("GET request didn't work");
      return null;
    }
  }

  /** Turns a JSON string into an arraylist of coordinates
    */
  private static ArrayList<Coordinate> jsonToCoordinates(String json, String label) {
    ArrayList<Coordinate> coordinates = new ArrayList<Coordinate>();
    JSONArray allWaypoints = new JSONArray(json);
    int index = 0;
    while (!allWaypoints.isNull(index)) {
      JSONObject observation = allWaypoints.getJSONObject(index);
      Double x = observation.getDouble("longitude");
      x = Math.round(x * 25000.0)/25000.0;
      Double y = observation.getDouble("latitude");
      y = Math.round(y * 25000.0)/25000.0;
      Coordinate featureCoordinate = new Coordinate(x, y, label);
      coordinates.add(featureCoordinate);
      index += 1;
    }
    return coordinates;
  }

  /** Stores input text and waypoints in a RouteEntity in datastore.
    * Returns nothing.
    */ 
  private static void storeInputAndWaypoints(String textInput, ArrayList<List<Coordinate>> waypoints){
    Entity RouteEntity = new Entity("Route");
    RouteEntity.setProperty("text", textInput);
    String json = new Gson().toJson(waypoints);
    long timestamp = System.currentTimeMillis();
    RouteEntity.setProperty("timestamp", timestamp);
    // Store as a json string because Coordinates are unsupported.
    RouteEntity.setProperty("waypoints", json);
    // Store Route.
    DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
    datastore.put(RouteEntity);
  }
}
