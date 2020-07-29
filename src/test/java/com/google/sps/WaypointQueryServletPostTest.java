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
  public static final ArrayList<Coordinate> DAISY = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.6295, 41.84866666666667, "daisy")));
  public static final ArrayList<Coordinate> CLOVER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.63566666666667, 41.856, "clover")));
  public static final ArrayList<Coordinate> BELLFLOWER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.6475, 41.8435, "bellflower")));
  public static final ArrayList<Coordinate> RASPBERRY = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62216666666667, 41.898, "raspberry"), new Coordinate(-87.6245, 41.897, "raspberry"), new Coordinate(-87.624, 41.8885, "raspberry")));
  public static final ArrayList<Coordinate> TREE_LICHEN = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62216666666667, 41.897166666666664, "tree,lichen")));
  public static final ArrayList<Coordinate> MEADOWSWEET_SUNFLOWER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.62, 41.8975, "meadowsweet,sunflower")));
  public static final ArrayList<Coordinate> NOTHING = new ArrayList<Coordinate>();
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
  public void before() {
    request = mock(HttpServletRequest.class);       
    response = mock(HttpServletResponse.class);  
    servlet = PowerMockito.spy(new WaypointQueryServlet());
    PowerMockito.mockStatic(WaypointQueryServlet.class);

    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);

    // Propagate private variable with data
    waypointMock = new ArrayList<ArrayList<Coordinate>>();
  }

  /* UNIT TESTS */
  @Test // Empty input
  public void testServletPostEmpty() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doReturn(NOTHING).when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with one waypoint description
  public void testServletPostOne() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("daisy");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doReturn(DAISY).when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with multiple waypoint descriptions
  public void testServletPostMultiple() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("daisy;clover;bellflower");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY, CLOVER, BELLFLOWER)).when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
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
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("daisy;wrong");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY, NOTHING)).when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
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
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("   daisy;    clover   ");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY,CLOVER)).when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
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
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("DAisY");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY)).when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  /* INTEGRATION TESTS with real GET from database */
  @Test // Empty input, real GET request from database
  public void testServletPostEmptyIntegration() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }
  
  @Test // Post a query with one waypoint description, real GET request from database
  public void testServletPostOneIntegration() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("daisy");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // One waypoint description with multiple instances, real GET request from database
  public void testServletPostOneMultipleInstances() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("raspberry");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(RASPBERRY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Two features that have the exact same coordinates
  public void testServletPostSameCoordinates() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("tree,lichen");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(TREE_LICHEN);
    assertEquals(waypointMock, comparison);
  }

  @Test // Two features that have similar coordinates
  public void testServletPostSimilarCoordinates() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("meadowsweet,sunflower");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(MEADOWSWEET_SUNFLOWER);
    assertEquals(waypointMock, comparison);
  }

  @Test // Two features that have different coordinates
  public void testServletPostDifferentCoordinates() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("tree,raspberry");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(NOTHING);
    assertEquals(waypointMock, comparison);
  }

  @Test // Multiple waypoint descriptions with multiple features
  public void testServletPostMultipleDescriptionsAndFeatures() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("tree,lichen; meadowsweet,sunflower");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "analyzeSyntaxText", anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "sendGET", anyString(), anyString());
    PowerMockito.doCallRealMethod().when(WaypointQueryServlet.class, "jsonToCoordinates", anyString(), anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    ArrayList<ArrayList<Coordinate>> comparison = new ArrayList<ArrayList<Coordinate>>();
    comparison.add(TREE_LICHEN);
    comparison.add(MEADOWSWEET_SUNFLOWER);
    assertEquals(waypointMock, comparison);
  }

  @After
  public void after() {
    writer.flush();
  }
}