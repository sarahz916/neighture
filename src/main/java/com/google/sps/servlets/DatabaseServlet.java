package com.example.appengine.users;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** Servlet that searches for a given name in a database, 
  * parsing out and returning the coordinates
  */
@WebServlet("/database")
public class DatabaseServlet extends HttpServlet {
  // TODO: Add map of points to coordinates
  @Override
  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    // TODO: return the coordinates that match the request
  }
}