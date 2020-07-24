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
  public static final Coordinate DAISY = new Coordinate(-87.629454, 41.848653, "daisy");
  public static final Coordinate CLOVER = new Coordinate(-87.635604, 41.855967, "clover");
  public static final Coordinate BELLFLOWER = new Coordinate(-87.64748, 41.843539, "bellflower");
  public static final String EXPECTED_MULTIPLE = "[{\"x\":-87.629454,\"y\":41.848653,\"label\":\"daisy\"},{\"x\":-87.635604,\"y\":41.855967,\"label\":\"clover\"},{\"x\":-87.64748,\"y\":41.843539,\"label\":\"bellflower\"}]\n";
  public static final String EXPECTED_ONE = "[{\"x\":-87.629454,\"y\":41.848653,\"label\":\"daisy\"}]\n";
  public static final String EXPECTED_EMPTY = "[]\n";
  private WaypointQueryServlet servlet;
  
  @Mock (name = "waypoints")
  ArrayList<Coordinate> waypointMock;
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
    waypointMock = new ArrayList<Coordinate>();
  }

  @Test // Empty input
  public void testServletPostEmpty() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("");
    PowerMockito.doReturn(null).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");
    assertEquals(waypointMock, new ArrayList<Coordinate>());
  }

  @Test // Post a query with one waypoint description
  public void testServletPostOne() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("daisy");
    PowerMockito.doReturn(DAISY).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with multiple waypoint descriptions
  public void testServletPostMultiple() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("daisy;clover;bellflower");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY, CLOVER, BELLFLOWER)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
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
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY,null)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with waypoint descriptions separated by a lot of spacing
  public void testServletPostLotsOfSpacing() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("   daisy;    clover   ");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY,CLOVER)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
    comparison.add(DAISY);
    comparison.add(CLOVER);
    assertEquals(waypointMock, comparison);
  }

  @Test // Post a query with a waypoint description with different letter cases
  public void testServletPostDifferentCases() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);
    when(request.getParameter("text-input")).thenReturn("DAisY");
    ((PowerMockitoStubber) PowerMockito.doReturn(DAISY)).when(WaypointQueryServlet.class, "sendGET", anyString());
    PowerMockito.doNothing().when(WaypointQueryServlet.class, "storeInputAndWaypoints", anyString(), eq(waypointMock));

    when(response.getWriter()).thenReturn(writer);
    servlet.doPost(request, response);
    verify(request, atLeast(1)).getParameter("text-input");

    // Create answer to compare against
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
    comparison.add(DAISY);
    assertEquals(waypointMock, comparison);
  }

  //TODO: test one, multiple, bad input, lots of spacing, uppercase/not all lowercase

  @After
  public void after() {
    writer.flush();
  }
}