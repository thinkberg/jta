/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
 *
 * Please visit http://javatelnet.org/ for updates and contact.
 *
 * --LICENSE NOTICE--
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 * --LICENSE NOTICE--
 *
 */
package de.mud.jta;


import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;

/**
 * Help display for JTA.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class Help {

  public static JEditorPane helpText = new JEditorPane();

  public static void show(Component parent, String url) {
    BufferedReader reader = null;
    System.err.println("Help: " + url);

    try {
      helpText.setPage(Help.class.getResource(url));
    } catch (IOException e) {
      System.err.println("unable to load help");
      helpText.setText("The Java Telnet Applet/Application\r\n(c) 1996-2002 Matthias L. Jugel, Marcus Meiﬂner\r\n\r\n");
    }
    helpText.setEditable(false);

    JScrollPane scrollPane =
            new JScrollPane(helpText,
                            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setSize(800, 600);

    final JFrame frame = new JFrame("HELP");
    frame.getContentPane().setLayout(new BorderLayout());
    frame.getContentPane().add(BorderLayout.CENTER, scrollPane);
    JPanel panel = new JPanel();
    JButton close = new JButton("Close Help");
    panel.add(close);
    close.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        frame.setVisible(false);
      }
    });
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e) {
        frame.setVisible(false);
      }
    });
    frame.getContentPane().add(BorderLayout.SOUTH, close);
    frame.setSize(800, 600);
    frame.setVisible(true);
  }

}
