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
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Calendar.Builder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.*;
import javax.servlet.http.*;
import org.apache.commons.io.FileUtils;
import org.springframework.test.util.ReflectionTestUtils;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.api.mockito.expectation.PowerMockitoStubber;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/** Tests each public function of WaypointQueryServlet aside from those relating to the datastore
  */
@RunWith(PowerMockRunner.class)
@PrepareForTest(WaypointQueryServlet.class)
public class WaypointQueryServletTest {
  public static final ArrayList<Coordinate> MUSHROOM = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62944, 41.84864, "mushroom", "red mushroom")));
  public static final ArrayList<Coordinate> CLOVER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.63566666666667, 41.856, "clover", "four-leaf clover")));
  public static final ArrayList<Coordinate> BELLFLOWER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.6475, 41.8435, "bellflower", "yellow bellflower")));
  public static final ArrayList<Coordinate> RASPBERRY = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62212, 41.89796, "raspberry", "big raspberry"), new Coordinate(-87.62456, 41.89696, "raspberry", "big raspberry"), new Coordinate(-87.62392, 41.88844, "raspberry", "big raspberry")));
  public static final ArrayList<Coordinate> TREE = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62224, 41.8972, "tree", "pine tree")));
  public static final ArrayList<Coordinate> NOTHING = new ArrayList<Coordinate>();
  public static final String MUSHROOM_BACKEND = "[{\"latitude\": 41.848653, \"longitude\": -87.629454, \"species_guess\": \"red mushroom\", \"common_name\": {\"name\": \"mushroom\"}}]";
  public static final String RASPBERRY_BACKEND = "[{\"latitude\": 41.897946, \"longitude\": -87.622112, \"species_guess\": \"big raspberry\", \"common_name\": {\"name\": \"raspberry\"}}, "
    + "{\"latitude\": 41.896968, \"longitude\": -87.624580, \"species_guess\": \"big raspberry\", \"common_name\": {\"name\": \"raspberry\"}}, "
    + "{\"latitude\": 41.888454, \"longitude\": -87.623920, \"species_guess\": \"big raspberry\", \"common_name\": {\"name\": \"raspberry\"}}]";
  public static final String TREE_BACKEND = "[{\"latitude\": 41.897219, \"longitude\": -87.622235, \"species_guess\": \"pine tree\", \"common_name\": {\"name\": \"tree\"}}]";
  public static final String NOTHING_BACKEND = "[]";
  public static final String COMPARISON_DATE = "2019-08-01";
  public static final String MULT_FEATURES_ONE_WAYPOINT_QUERY = "mushroom,cLover     raSpbeRRy   ";
  public static final String NUMBER_BEGINNING_QUERY = "2 mushroom,cLover,raspberry";
  public static final String NUMBER_MIDDLE_QUERY = "tree,2 mushroom, clover, raspberry";
  public static final String ONE_FEATURE_MULT_WAYPOINT_QUERY = "mushroom;+.raspberry!!?\ntree";
  public static final String MULT_FEATURES_MULT_WAYPOINT_QUERY = "mushroom,  raspberry; tree";
  public static final int MAX_AMOUNT = 2;
  public static final WaypointDescription MUSHROOM_DESC = new WaypointDescription("mushroom");
  public static final WaypointDescription MUSHROOM_WITH_NUMBER = new WaypointDescription(MAX_AMOUNT, "mushroom");
  public static final WaypointDescription CLOVER_DESC = new WaypointDescription("clover");
  public static final WaypointDescription RASPBERRY_DESC = new WaypointDescription("raspberry");
  public static final WaypointDescription TREE_DESC = new WaypointDescription("tree");
  public static final WaypointDescription GREAT_BLUE_HERON = new WaypointDescription("great blue heron");
  public static final ArrayList<String> TREE_WORD = new ArrayList<String>(Arrays.asList("tree"));
  public static final ArrayList<String> LICHEN_WORD = new ArrayList<String>(Arrays.asList("lichen"));
  public static final ArrayList<String> MUSHROOM_WORD = new ArrayList<String>(Arrays.asList("mushroom"));
  public static final ArrayList<String> CLOVER_WORD = new ArrayList<String>(Arrays.asList("clover"));
  public static final ArrayList<String> RASPBERRY_WORD = new ArrayList<String>(Arrays.asList("raspberry"));
  private static final Coordinate START = new Coordinate(-87.62940, 41.84865, "start", "");
  private static final Coordinate END = new Coordinate(-87.62942, 41.84861, "end", "");
  private static final String[] LOOP_COMPARISON = {"-87.70186376811", "-87.55693623189", "41.77618623189", "41.92111376811"};
  private static final String[] ONE_WAY_COMPARISON = {"-87.6330431884", "-87.6257768116", "41.8449868116", "41.8522731884"};
  private WaypointQueryServlet servlet;

  @Before
  public void before() throws Exception { 
    servlet = PowerMockito.spy(new WaypointQueryServlet());
  }

  /* Testing getStartDate */
  @Test 
  public void testGetStartDate() throws Exception {
    PowerMockito.mockStatic(Calendar.class);
    Calendar calendarMock = new Calendar.Builder().setDate(2020, Calendar.AUGUST, 1).build();
    when(Calendar.getInstance()).thenReturn(calendarMock);
    String date = WaypointQueryServlet.getStartDate();
    assertEquals(date, COMPARISON_DATE);
  }

  /* Testing getBoundingBox loop */
  @Test 
  public void testGetBoundingBoxLoop() throws Exception {
    String[] bounds = servlet.getBoundingBox(START, START);
    assertEquals(bounds, LOOP_COMPARISON);
  }

  /* Testing getBoundingBox one-way */
  @Test 
  public void testGetBoundingBoxOneWay() throws Exception {
    String[] bounds = servlet.getBoundingBox(START, END);
    assertEquals(bounds, ONE_WAY_COMPARISON);
  }

  /* Testing jsonToCoordinates */
  @Test 
  public void testJsonToCoordinates() throws Exception {
    ArrayList<Coordinate> coordinateResult = WaypointQueryServlet.jsonToCoordinates(MUSHROOM_BACKEND, "mushroom");
    assertEquals(coordinateResult, MUSHROOM);
  }

  /* Testing fetchFromDatabase when result exists; mock sendGET */
  @Test 
  public void testFetchFromDatabaseResult() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(TREE_BACKEND);
    ArrayList<Coordinate> coordinateResult = servlet.fetchFromDatabase("tree", "tree,lichen", START, START);
    assertEquals(coordinateResult, TREE);
  }

  /* Testing fetchFromDatabase when result doesn't exist; mock sendGET */
  @Test 
  public void testFetchFromDatabaseNoResult() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(NOTHING_BACKEND);
    ArrayList<Coordinate> coordinateResult = servlet.fetchFromDatabase("trash", "trash", START, START);
    assertEquals(coordinateResult, NOTHING);
  }

  /* Testing isInt for a numeral */
  @Test
  public void testIsIntNumeral() throws Exception {
    boolean wordIsInt = servlet.isInt("2");
    assertTrue(wordIsInt);
  }

  /* Testing isInt for a written-out number */
  @Test
  public void testIsIntWritten() throws Exception {
    boolean wordIsInt = servlet.isInt("two");
    assertTrue(wordIsInt);
  } 

  /* Testing isInt on a non-number */
  @Test
  public void testIsIntNot() throws Exception {
    boolean wordIsInt = servlet.isInt("mushroom");
    assertFalse(wordIsInt);
  } 

  /* Testing wordToInt for a numeral */
  @Test
  public void testWordToIntNumeral() throws Exception {
    int newInt = servlet.wordToInt("2");
    assertEquals(newInt, MAX_AMOUNT);
  }

  /* Testing wordToInt for a written-out number */
  @Test
  public void testWordToIntWritten() throws Exception {
    int newInt = servlet.wordToInt("two");
    assertEquals(newInt, MAX_AMOUNT);
  } 

  /* Testing parseInput, different cases */
  @Test 
  public void testParseWaypointQueryCasing() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput(MULT_FEATURES_ONE_WAYPOINT_QUERY);
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(MUSHROOM_DESC);
    comparison.add(CLOVER_DESC);
    comparison.add(RASPBERRY_DESC);
    assertEquals(features, comparison);
  }

  /* Testing parseInput, spaces and commas */
  @Test 
  public void testParseWaypointQuerySplit() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput(MULT_FEATURES_ONE_WAYPOINT_QUERY.toLowerCase());
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(MUSHROOM_DESC);
    comparison.add(CLOVER_DESC);
    comparison.add(RASPBERRY_DESC);
    assertEquals(features, comparison);
  }

  /* Testing parseInput where the query contains a number at the beginning */
  @Test 
  public void testParseWaypointQueryNumberBeginning() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput(NUMBER_BEGINNING_QUERY);
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(MUSHROOM_WITH_NUMBER);
    comparison.add(CLOVER_DESC);
    comparison.add(RASPBERRY_DESC);
    assertEquals(features, comparison);
  }

  /* Testing parseInput where the query contains a number in the middle */
  @Test 
  public void testParseWaypointQueryNumberMiddle() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput(NUMBER_MIDDLE_QUERY);
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(TREE_DESC);
    comparison.add(MUSHROOM_WITH_NUMBER);
    comparison.add(CLOVER_DESC);
    comparison.add(RASPBERRY_DESC);
    assertEquals(features, comparison);
  }

  /* Testing parseInput with an adjective */
  @Test 
  public void testParseWaypointQueryAdjective() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput("amazing tree");
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(TREE_DESC);
    assertEquals(features, comparison);
  }  

  /* Testing parseInput with a full sentence and just one feature */
  @Test 
  public void testParseWaypointQueryFullSentence() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput("i want to see a tree");
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(TREE_DESC);
    assertEquals(features, comparison);
  }  

  /* Testing parseInput, all punctuation, one feature per waypoint */
  @Test 
  public void testProcessInputTextPunctuation() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput(ONE_FEATURE_MULT_WAYPOINT_QUERY);
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(MUSHROOM_DESC);
    //comparison.add(CLOVER_DESC);
    comparison.add(RASPBERRY_DESC);
    comparison.add(TREE_DESC);
    for (WaypointDescription feature : features) {
      System.out.println(feature.getFeature());
      //System.out.println(feature.getMaxAmount());
    }
    assertEquals(features, comparison);
  }

  /* Testing parseInput, multiple features and multiple waypoints */
  @Test 
  public void testProcessInputTextMultipleFeaturesWaypoints() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput(MULT_FEATURES_MULT_WAYPOINT_QUERY);
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(MUSHROOM_DESC);
    //comparison.add(CLOVER_DESC);
    comparison.add(RASPBERRY_DESC);
    comparison.add(TREE_DESC);
    assertEquals(features, comparison);
  }

  /* Testing parseInput with quotes */
  @Test 
  public void testParseWaypointQueryQuotes() throws Exception {
    ArrayList<WaypointDescription> features = servlet.parseInput("tree \"great blue heron\"");
    ArrayList<WaypointDescription> comparison = new ArrayList<WaypointDescription>();
    comparison.add(TREE_DESC);
    comparison.add(GREAT_BLUE_HERON);
    assertEquals(features, comparison);
  }

  /* Testing getLocations on empty input, stubbing database call */
  @Test
  public void testGetLocationsEmpty() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(NOTHING_BACKEND);
    ArrayList<List<Coordinate>> locations = servlet.getLocations("", START, START);
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    assertEquals(locations, comparison);
  }

  /* Testing getLocations on one input, stubbing database call */
  @Test
  public void testGetLocationsOne() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(RASPBERRY_BACKEND);
    ArrayList<List<Coordinate>> locations = servlet.getLocations("raspberry", START, START);
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(RASPBERRY);
    assertEquals(locations, comparison);
  }

  /* Testing getLocations on an input where we limit the number, stubbing database call */
  @Test
  public void testGetLocationsNumber() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(RASPBERRY_BACKEND);
    ArrayList<List<Coordinate>> locations = servlet.getLocations("1 raspberry", START, START);
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(Arrays.asList(RASPBERRY.get(0)));
    assertEquals(locations, comparison);
  }
}