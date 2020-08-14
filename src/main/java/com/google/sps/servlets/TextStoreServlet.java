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
import com.google.sps.SessionDataStore;
import java.io.IOException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.util.ArrayList;
import java.util.List;
import com.google.gson.Gson;

/** Servlet that returns text input data */
@WebServlet(
    name = "TextStore",
    description = "TextStore: accesses text input",
    urlPatterns = "/text-store"
)
public class TextStoreServlet extends HttpServlet {

  /** Looks up text input from Route Entites by Session ID in datastore.
  *   Returns text of most recent input-text from session ID. 
  */ 
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
      SessionDataStore sessionDataStore = new SessionDataStore(request);
      String valueJSONString = sessionDataStore.queryOnlyifFirstFetch("textFetched", "Route", "text");
      response.setContentType("application/json");
      response.getWriter().println(valueJSONString);
  }

}
