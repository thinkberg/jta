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
package de.mud.jta;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Help display for JTA.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class HelpFrame extends Frame {

  public TextArea helpText;

  public HelpFrame(String url) {
    addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        HelpFrame.this.setVisible(false);
      }
    });
    setLayout(new BorderLayout());
    helpText = new TextArea("", 30, 80, Scrollbar.VERTICAL | Scrollbar.HORIZONTAL);
    helpText.setEditable(false);

    BufferedReader reader = null;

    try {
      reader = new BufferedReader(new InputStreamReader(HelpFrame.class.getResourceAsStream(url)));
    } catch (Exception e1) {
      // ignore
    }

    try {
      URL helpUrl = new URL(url);
      reader = new BufferedReader(new InputStreamReader(helpUrl.openStream()));
    } catch (java.io.IOException e1) {

    }

    try {
      String line;
      while ((line = reader.readLine()) != null) {
        helpText.append(line + "\n");
      }
    } catch (Exception e) {
      System.err.println("unable to load help");
      helpText.append("The Java Telnet Applet/Application\r\n(c) 1996-2002 Matthias L. Jugel, Marcus Meiﬂner\r\n\r\n");
    }


    add(helpText, BorderLayout.CENTER);

    Button b = new Button("Close Window");
    b.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent
              e) {
        HelpFrame.this.setVisible(false);
      }
    });
    Panel p = new Panel();
    p.add(b);
    add(p, BorderLayout.SOUTH);

    pack();
  }

}
