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
  public static final ArrayList<Coordinate> DAISY = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62944, 41.84864, "daisy")));
  public static final ArrayList<Coordinate> CLOVER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.63566666666667, 41.856, "clover")));
  public static final ArrayList<Coordinate> BELLFLOWER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.6475, 41.8435, "bellflower")));
  public static final ArrayList<Coordinate> RASPBERRY = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62212, 41.89796, "raspberry"), new Coordinate(-87.62456, 41.89696, "raspberry"), new Coordinate(-87.62392, 41.88844, "raspberry")));
  public static final ArrayList<Coordinate> TREE_LICHEN = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62224, 41.8972, "tree,lichen")));
  public static final ArrayList<Coordinate> MEADOWSWEET_SUNFLOWER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.61992, 41.89752, "meadowsweet,sunflower")));
  public static final ArrayList<Coordinate> NOTHING = new ArrayList<Coordinate>();
  public static final String DAISY_BACKEND = "[{\"latitude\": 41.848653, \"longitude\": -87.629454, \"common_name\": {\"name\": \"daisy\"}}]";
  public static final String RASPBERRY_BACKEND = "[{\"latitude\": 41.897946, \"longitude\": -87.622112, \"common_name\": {\"name\": \"raspberry\"}}, {\"latitude\": 41.896968, \"longitude\": -87.624580, \"common_name\": {\"name\": \"raspberry\"}}, {\"latitude\": 41.888454, \"longitude\": -87.623920, \"common_name\": {\"name\": \"raspberry\"}}]";
  public static final String TREE_BACKEND = "[{\"latitude\": 41.897219, \"longitude\": -87.622235, \"common_name\": {\"name\": \"tree\"}}]";
  public static final String LICHEN_BACKEND = "[{\"latitude\": 41.897219, \"longitude\": -87.622235, \"common_name\": {\"name\": \"lichen\"}}]";
  public static final String MEADOWSWEET_BACKEND = "[{\"latitude\": 41.897523, \"longitude\": -87.619934, \"common_name\": {\"name\": \"meadowsweet\"}}]";
  public static final String SUNFLOWER_BACKEND = "[{\"latitude\": 41.897521, \"longitude\": -87.619934, \"common_name\": {\"name\": \"sunflower\"}}]";
  public static final String NOTHING_BACKEND = "[]";
  public static final String COMPARISON_DATE = "2019-08-01";
  public static final String MULT_FEATURES_ONE_WAYPOINT_LABEL = "daisy,cLover     raSpbeRRy   ";
  public static final String ONE_FEATURE_MULT_WAYPOINT_LABEL = "daisy;+clover.raspberry!!?\ntree";
  public static final String MULT_FEATURES_MULT_WAYPOINT_LABEL = "daisy,clover; raspberry tree";
  public static final ArrayList<String> DAISY_CLOVER_RASPBERRY = new ArrayList<String>(Arrays.asList("daisy", "clover", "raspberry"));
  public static final ArrayList<String> TREE_QUERY = new ArrayList<String>(Arrays.asList("tree"));
  public static final ArrayList<String> LICHEN_QUERY = new ArrayList<String>(Arrays.asList("lichen"));
  private WaypointQueryServlet servlet;
  
  @Mock
  int maxNumberCoordinatesMock;

  @Before
  public void before() throws Exception { 
    servlet = PowerMockito.spy(new WaypointQueryServlet());
    //servlet = mock(WaypointQueryServlet.class, CALLS_REAL_METHODS);
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

  /* Testing jsonToCoordinates */
  @Test 
  public void testJsonToCoordinates() throws Exception {
    ArrayList<Coordinate> coordinateResult = WaypointQueryServlet.jsonToCoordinates(DAISY_BACKEND, "daisy");
    assertEquals(coordinateResult, DAISY);
  }

  /* Testing fetchFromDatabase when result exists; mock sendGET */
  @Test 
  public void testFetchFromDatabaseResult() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(TREE_BACKEND);
    ArrayList<Coordinate> coordinateResult = WaypointQueryServlet.fetchFromDatabase("tree", "tree,lichen");
    assertEquals(coordinateResult, TREE_LICHEN);
  }

  /* Testing fetchFromDatabase when result doesn't exist; mock sendGET */
  @Test 
  public void testFetchFromDatabaseNoResult() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(NOTHING_BACKEND);
    ArrayList<Coordinate> coordinateResult = WaypointQueryServlet.fetchFromDatabase("trash", "trash");
    assertEquals(coordinateResult, NOTHING);
  }

  /* Testing determineIfInt */
  // ADD

  /* Testing parseWaypointQuery, different cases */
  @Test 
  public void testParseWaypointQueryCasing() throws Exception {
    ArrayList<String> features = WaypointQueryServlet.parseWaypointQuery(MULT_FEATURES_ONE_WAYPOINT_LABEL);
    assertEquals(features, DAISY_CLOVER_RASPBERRY);
  }

  /* Testing parseWaypointQuery, spaces and commas */
  @Test 
  public void testParseWaypointQuerySplit() throws Exception {
    ArrayList<String> features = WaypointQueryServlet.parseWaypointQuery(MULT_FEATURES_ONE_WAYPOINT_LABEL.toLowerCase());
    assertEquals(features, DAISY_CLOVER_RASPBERRY);
  }

  /* Testing processInputText, all punctuation, one feature per waypoint */
  @Test 
  public void testProcessInputTextPunctuation() throws Exception {
    ArrayList<ArrayList<String>> features = WaypointQueryServlet.processInputText(ONE_FEATURE_MULT_WAYPOINT_LABEL);
    ArrayList<ArrayList<String>> comparison = new ArrayList<ArrayList<String>>();
    comparison.add(new ArrayList<String>(Arrays.asList("daisy")));
    comparison.add(new ArrayList<String>(Arrays.asList("clover")));
    comparison.add(new ArrayList<String>(Arrays.asList("raspberry")));
    comparison.add(new ArrayList<String>(Arrays.asList("tree")));
    assertEquals(features, comparison);
  }

  /* Testing processInputText, multiple features and multiple waypoints */
  @Test 
  public void testProcessInputTextMultipleFeaturesWaypoints() throws Exception {
    ArrayList<ArrayList<String>> features = WaypointQueryServlet.processInputText(MULT_FEATURES_MULT_WAYPOINT_LABEL);
    ArrayList<ArrayList<String>> comparison = new ArrayList<ArrayList<String>>();
    comparison.add(new ArrayList<String>(Arrays.asList("daisy", "clover")));
    comparison.add(new ArrayList<String>(Arrays.asList("raspberry", "tree")));
    assertEquals(features, comparison);
  }

  /* Testing getLocations on empty input, stubbing database call */
  @Test
  public void testGetLocationsEmpty() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(NOTHING_BACKEND);
    // String anyString = anyString();
    // when(servlet.sendGET(anyString)).thenReturn(NOTHING_BACKEND);
    ArrayList<List<Coordinate>> locations = servlet.getLocations("");
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(Arrays.asList());
    assertEquals(locations, comparison);
  }

  /* Testing getLocations on one input, stubbing database call */
  @Test
  public void testGetLocationsOne() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(RASPBERRY_BACKEND);
    ArrayList<List<Coordinate>> locations = servlet.getLocations("raspberry");
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(RASPBERRY);
    assertEquals(locations, comparison);
  }

  /* Testing getLocations on an input where we limit the number, stubbing database call */
  @Test
  public void testGetLocationsNumber() throws Exception {
    PowerMockito.stub(PowerMockito.method(WaypointQueryServlet.class, "sendGET")).toReturn(RASPBERRY_BACKEND);
    ArrayList<List<Coordinate>> locations = servlet.getLocations("1 raspberry");
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(Arrays.asList(RASPBERRY.get(0)));
    assertEquals(locations, comparison);
  }

  /* Testing getLocations with two features that have the exact same coordinates, stubbing database call */
  @Test // Two features that have the exact same coordinates
  public void testServletPostSameCoordinates() throws Exception {
    PowerMockito.mockStatic(WaypointQueryServlet.class);
    maxNumberCoordinatesMock = 5;
    ReflectionTestUtils.setField(servlet, "maxNumberCoordinates", maxNumberCoordinatesMock);
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "processInputText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "parseWaypointQuery", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "determineIfInt", anyString(), anyInt());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(TREE_BACKEND, LICHEN_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    ArrayList<List<Coordinate>> locations = servlet.getLocations("tree, lichen");
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(TREE_LICHEN);
    assertEquals(locations, comparison);
  }

  /* Testing getLocations with two features that have similar coordinates, stubbing database call */
  @Test
  public void testServletPostSimilarCoordinates() throws Exception {
    PowerMockito.mockStatic(WaypointQueryServlet.class);
    maxNumberCoordinatesMock = 5;
    ReflectionTestUtils.setField(servlet, "maxNumberCoordinates", maxNumberCoordinatesMock);
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "processInputText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "parseWaypointQuery", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "determineIfInt", anyString(), anyInt());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(MEADOWSWEET_BACKEND, SUNFLOWER_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    ArrayList<List<Coordinate>> locations = servlet.getLocations("meadowsweet, sunflower");
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(MEADOWSWEET_SUNFLOWER);
    assertEquals(locations, comparison);
  }

  /* Testing getLocations with two features that have different coordinates, stubbing database call */
  @Test
  public void testServletPostDifferentCoordinates() throws Exception {
    PowerMockito.mockStatic(WaypointQueryServlet.class);
    maxNumberCoordinatesMock = 5;
    ReflectionTestUtils.setField(servlet, "maxNumberCoordinates", maxNumberCoordinatesMock);
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "processInputText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "parseWaypointQuery", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "determineIfInt", anyString(), anyInt());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(TREE_BACKEND, RASPBERRY_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    ArrayList<List<Coordinate>> locations = servlet.getLocations("tree, raspberry");
    ArrayList<List<Coordinate>> comparison = new ArrayList<List<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(locations, comparison);
  }
}