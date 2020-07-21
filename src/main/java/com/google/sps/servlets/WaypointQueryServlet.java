package com.example.appengine.users;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Servlet that handles the user's query by parsing out
  * the waypoint queries and their matching coordinates in 
  * the database.
  */
@WebServlet("/query")
public class WaypointQueryServlet extends HttpServlet {
  @Override
  public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
    String text = getParameter(request, "text-input", "");
    // TODO: Parse out input and find in database
    // Database will probably have to wait until everything is set up
    // or I'll just call another servlet with a GET function
  }
}