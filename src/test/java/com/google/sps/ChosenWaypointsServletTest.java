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
import java.util.*;
import com.google.gson.Gson;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.*;
import javax.servlet.http.*;
import org.apache.commons.io.FileUtils;
import org.springframework.test.util.ReflectionTestUtils;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

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

//Want to call Post method and then check datastore 
//Then delete the entries placed into the datastore

@RunWith(PowerMockRunner.class)
@PrepareForTest(ChosenWaypointsServlet.class)
public class ChosenWaypointsServletTest {
  public static final Coordinate DAISY = new Coordinate(-87.629454, 41.848653, "daisy");
  public static final Coordinate CLOVER = new Coordinate(-87.635604, 41.855967, "clover");
  public static final Coordinate BELLFLOWER = new Coordinate(-87.64748, 41.843539, "bellflower");
  public static final String EXPECTED_MULTIPLE = "[{\"x\":-87.629454,\"y\":41.848653,\"label\":\"daisy\"},{\"x\":-87.635604,\"y\":41.855967,\"label\":\"clover\"},{\"x\":-87.64748,\"y\":41.843539,\"label\":\"bellflower\"}]\n";
  public static final String EXPECTED_ONE = "[{\"x\":-87.629454,\"y\":41.848653,\"label\":\"daisy\"}]\n";
  public static final String EXPECTED_EMPTY = "[]\n";
  //Create Strings of Selected Coordinates as JSONS
  public static final String JSON_STRING_DAISY = new Gson().toJson(DAISY);
  public static final String JSON_STRING_CLOVER = new Gson().toJson(CLOVER);
  public static final String JSON_STRING_BELLFLOWER = new Gson().toJson(BELLFLOWER);
  
  private ChosenWaypointsServlet servlet;
  

  private final LocalServiceTestHelper helper =
      new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
  
  @Mock (name = "waypoints")
  ArrayList<Coordinate> waypointMock;
  @Mock (name = "datastore")
  HashMap<String, String> mockDataStore;
  @Mock
  HttpServletRequest request;
  @Mock
  HttpServletResponse response;
  @Mock
  HttpSession session;
  @Mock 
  ArrayList<String> chosenWaypointsMock;
  @Mock
  StringWriter stringWriter;
  @Mock
  PrintWriter writer;

  @Before
  public void before() throws Exception  {
    request = mock(HttpServletRequest.class);       
    response = mock(HttpServletResponse.class); 
    session = mock(HttpSession.class); 
    servlet = new ChosenWaypointsServlet();
    PowerMockito.mockStatic(ChosenWaypointsServlet.class);
    // Propagate private variable with data
    waypointMock = new ArrayList<Coordinate>();
    chosenWaypointsMock = new ArrayList<String>();
    mockDataStore = new HashMap<String, String>();

    stringWriter = new StringWriter();
    writer = new PrintWriter(stringWriter);
  }

  /*getWaypointsfromRequestTests*/

  /** Test one waypoint. 
  */  
  @Test 
  public void testOne(){
    chosenWaypointsMock.add(JSON_STRING_DAISY);
    when(request.getParameterNames()).thenReturn(Collections.enumeration(chosenWaypointsMock));
    ArrayList<Coordinate> actual = servlet.getWaypointsfromRequest(request);
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
    comparison.add(DAISY);
    //String comparisonString = new Gson().toJson(comparison);
    assertEquals(comparison, actual);
  }

  /** Test multiple chosen waypoints.
  */  
  @Test 
  public void testMultiple(){
    chosenWaypointsMock.add(JSON_STRING_DAISY);
    chosenWaypointsMock.add(JSON_STRING_BELLFLOWER);
    chosenWaypointsMock.add(JSON_STRING_CLOVER);
    when(request.getParameterNames()).thenReturn(Collections.enumeration(chosenWaypointsMock));
    ArrayList<Coordinate> actual = servlet.getWaypointsfromRequest(request);
    ArrayList<Coordinate> comparison = new ArrayList<Coordinate>();
    comparison.add(DAISY);
    comparison.add(BELLFLOWER);
    comparison.add(CLOVER);
    //String comparisonString = new Gson().toJson(comparison);
    assertEquals(comparison, actual);
  }
}
