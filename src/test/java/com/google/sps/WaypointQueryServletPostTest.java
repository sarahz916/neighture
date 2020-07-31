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
import java.util.ArrayList;
import java.util.Arrays;

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

@RunWith(PowerMockRunner.class)
@PrepareForTest(WaypointQueryServlet.class)
public class WaypointQueryServletPostTest {
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
  private WaypointQueryServlet servlet;
  
  @Mock (name = "waypoints")
  ArrayList<ArrayList<Coordinate>> waypointMock;
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  StringWriter stringWriter;
  @Mock
  PrintWriter writer;

  @Before
  public void before() throws Exception {
    request = mock(HttpServletRequest.class);       
    response = mock(HttpServletResponse.class);  
    servlet = PowerMockito.spy(new WaypointQueryServlet());
    PowerMockito.mockStatic(WaypointQueryServlet.class);

    // Propagate private variable with data
    waypointMock = new ArrayList<ArrayList<Coordinate>>();
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);

    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
    when(response.getWriter()).thenReturn(writer);

    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "getStartDate");
  }

  /* UNIT TESTS */
  @Test // Empty input
  public void testServletPostEmpty() throws Exception {
    when(request.getParameter("text-input")).thenReturn("");
    PowerMockito.doReturn(NOTHING).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with one waypoint description
  public void testServletPostOne() throws Exception {
    when(request.getParameter("text-input")).thenReturn("daisy");
    PowerMockito.doReturn(DAISY).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with multiple waypoint descriptions
  public void testServletPostMultiple() throws Exception {
    when(request.getParameter("text-input")).thenReturn("daisy;clover;bellflower");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY, CLOVER, BELLFLOWER)).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    comparison.add(CLOVER);
    comparison.add(BELLFLOWER);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with a waypoint description that isn't in the database
  public void testServletPostBadInput() throws Exception {
    when(request.getParameter("text-input")).thenReturn("daisy;wrong");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY, NOTHING)).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with waypoint descriptions separated by a lot of spacing
  public void testServletPostLotsOfSpacing() throws Exception {
    when(request.getParameter("text-input")).thenReturn("   daisy;    clover   ");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY,CLOVER)).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    comparison.add(CLOVER);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with a waypoint description with different letter cases
  public void testServletPostDifferentCases() throws Exception {
    when(request.getParameter("text-input")).thenReturn("DAisY");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY)).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with non-semicolon, non-comma punctuation delimiters
  public void testServletPostDifferentPunctuation() throws Exception {
    when(request.getParameter("text-input")).thenReturn("daisy! clover. bellflower");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY, CLOVER, BELLFLOWER)).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    comparison.add(CLOVER);
    comparison.add(BELLFLOWER);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query separating feature queries within a waypoint description with spaces
  public void testServletPostSeparateSpaces() throws Exception {
    when(request.getParameter("text-input")).thenReturn("tree lichen");
    ((PowerMockitoStubber) PowerMockito.doReturn(TREE_LICHEN)).when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(TREE_LICHEN);
    assertEquals(waypointMock, comparison);
  }

  /* INTEGRATION TESTS with fake GET from database */
  @Test // Empty input, GET request from database
  public void testServletPostEmptyIntegration() throws Exception {
    when(request.getParameter("text-input")).thenReturn("");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    PowerMockito.doReturn(NOTHING_BACKEND).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }
  
  @Test // Post a query with one waypoint description, GET request from database
  public void testServletPostOneIntegration() throws Exception {
    when(request.getParameter("text-input")).thenReturn("daisy");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    PowerMockito.doReturn(DAISY_BACKEND).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // One waypoint description with multiple instances, GET request from database
  public void testServletPostOneMultipleInstances() throws Exception {
    when(request.getParameter("text-input")).thenReturn("raspberry");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    PowerMockito.doReturn(RASPBERRY_BACKEND).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(RASPBERRY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Two features that have the exact same coordinates
  public void testServletPostSameCoordinates() throws Exception {
    when(request.getParameter("text-input")).thenReturn("tree,lichen");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(TREE_BACKEND, LICHEN_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
  
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(TREE_LICHEN);
    assertEquals(waypointMock, comparison);
  }

  @Test // Two features that have similar coordinates
  public void testServletPostSimilarCoordinates() throws Exception {
    when(request.getParameter("text-input")).thenReturn("meadowsweet,sunflower");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(MEADOWSWEET_BACKEND, SUNFLOWER_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(MEADOWSWEET_SUNFLOWER);
    assertEquals(waypointMock, comparison);
  }

  @Test // Two features that have different coordinates
  public void testServletPostDifferentCoordinates() throws Exception {
    when(request.getParameter("text-input")).thenReturn("tree,raspberry");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(TREE_BACKEND, RASPBERRY_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }

  @Test // Multiple waypoint descriptions with multiple features
  public void testServletPostMultipleDescriptionsAndFeatures() throws Exception {
    when(request.getParameter("text-input")).thenReturn("tree,lichen; raspberry");
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "fetchFromDatabase", anyString(), anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(TREE_BACKEND, LICHEN_BACKEND, RASPBERRY_BACKEND)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());

    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(TREE_LICHEN);
    comparison.add(RASPBERRY);
    assertEquals(waypointMock, comparison);
  }

  @After
  public void after() {
    writer.flush();
  }
}