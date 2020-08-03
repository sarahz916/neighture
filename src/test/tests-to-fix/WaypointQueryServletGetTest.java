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

@RunWith(JUnit4.class)
public class WaypointQueryServletGetTest {
  public static final ArrayList<Coordinate> DAISY = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.629454, 41.848653, "daisy")));
  public static final ArrayList<Coordinate> CLOVER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.635604, 41.855967, "clover")));
  public static final ArrayList<Coordinate> BELLFLOWER = new ArrayList<Coordinate>(Arrays.asList(new Coordinate(-87.64748, 41.843539, "bellflower")));
  public static final String EXPECTED_MULTIPLE = "[[{\"x\":-87.629454,\"y\":41.848653,\"label\":\"daisy\"}],[{\"x\":-87.635604,\"y\":41.855967,\"label\":\"clover\"}],[{\"x\":-87.64748,\"y\":41.843539,\"label\":\"bellflower\"}]]\n";
  public static final String EXPECTED_ONE = "[[{\"x\":-87.629454,\"y\":41.848653,\"label\":\"daisy\"}]]\n";
  public static final String EXPECTED_EMPTY = "[]\n";
  
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
  @InjectMocks
  WaypointQueryServlet servlet; //the class to test

  @Before
  public void before() {
    request = mock(HttpServletRequest.class);       
    response = mock(HttpServletResponse.class);  
    servlet = mock(WaypointQueryServlet.class);
    servlet = new WaypointQueryServlet();

    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);

    // Propagate private variable with data
    waypointMock = new ArrayList<ArrayList<Coordinate>>();
  }

  @Test
  public void testServletGetMultiple() throws Exception {  
    // Set the private variable values here by reflection.
    waypointMock.add(DAISY);
    waypointMock.add(CLOVER);
    waypointMock.add(BELLFLOWER);
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);

    when(response.getWriter()).thenReturn(writer);
    servlet.doGet(request, response);
    assertEquals(stringWriter.toString(), EXPECTED_MULTIPLE);
  }

  @Test
  public void testServletGetOne() throws Exception {
    // Set the private variable values here by reflection.
    waypointMock.add(DAISY);
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);

    when(response.getWriter()).thenReturn(writer);
    servlet.doGet(request, response);
    assertEquals(stringWriter.toString(), EXPECTED_ONE);
  }

  @Test
  public void testServletGetEmpty() throws Exception {
    // Set the private variable values here by reflection.
    ReflectionTestUtils.setField(servlet, "waypoints", waypointMock);

    when(response.getWriter()).thenReturn(writer);
    servlet.doGet(request, response);
    assertEquals(stringWriter.toString(), EXPECTED_EMPTY);
  }

  //TODO: test one, multiple, bad input, lots of spacing, uppercase/not all lowercase

  @After
  public void after() {
    writer.flush();
  }
}