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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import java.io.*;
import javax.servlet.http.*;
import org.apache.commons.io.FileUtils;

import org.junit.Test;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class DatabaseServletTest {
    public static final String EXPECTED_EXIST = "{\"latitude\": 41.848653, \"longitude\": -87.629454,  \"common_name\": {\"name\": \"daisy\"}}\n";
    public static final String EXPECTED_NOT_EXIST = "{}\n";
    @Test
    public void testServletFeatureInDatabase() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);       
        HttpServletResponse response = mock(HttpServletResponse.class);    

        when(request.getParameter("q")).thenReturn("daisy");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);
        System.out.println(stringWriter.toString());

        new DatabaseServlet().doGet(request, response);

        verify(request, atLeast(1)).getParameter("q"); // only if you want to verify q was called
        writer.flush(); // it may not have been flushed yet...
        assertEquals(stringWriter.toString(), EXPECTED_EXIST);
    }

    @Test
    public void testServletFeatureNotInDatabase() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);       
        HttpServletResponse response = mock(HttpServletResponse.class);    

        when(request.getParameter("q")).thenReturn("nothing");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(writer);

        new DatabaseServlet().doGet(request, response);

        verify(request, atLeast(1)).getParameter("q"); // only if you want to verify q was called
        writer.flush(); // it may not have been flushed yet...
        assertEquals(stringWriter.toString(), EXPECTED_NOT_EXIST);
    }
}