/*
 * This file is part of "The Java Telnet Application".
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * "The Java Telnet Application" is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this software; see the file COPYING.  If not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * Example UploadServlet for usage with the Capture plugin.
 *
 * @author Matthias L. Jugel
 * @version $Id$
 */
public class UploadServlet extends HttpServlet {


  /**
   * Accept POST requests only.
   * Retrieve the data from text/x-www-urlencoded request.
   */
  public void doPost(HttpServletRequest req, HttpServletResponse res)
          throws ServletException, IOException {


    res.setContentType("text/html");
    PrintWriter out = res.getWriter();
    out.println("<HTML>");
    out.println("<HEAD><TITLE>JTA Example Upload</TITLE></HEAD>");
    out.println("<BODY>");
    out.println("<H1>JTA Example Upload</H1>");

    out.println("<H3>Request Parameters:</H3><PRE>");

    Enumeration enum = req.getParameterNames();
    while (enum.hasMoreElements()) {
      String name = (String) enum.nextElement();
      String values[] = req.getParameterValues(name);
      if (values != null) {
        for (int i = 0; i < values.length; i++) {
          out.println(name + " (" + i + "): " + values[i]);
        }
      }
    }

    // place your storage code here to store the data in a file
    // or data base

    out.println("</PRE>");
    out.println("</BODY></HTML>");
  }
}