/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Righs Reserved.
 *
 * The software is licensed under the terms and conditions in the
 * license agreement included in the software distribution package.
 *
 * You should have received a copy of the license along with this
 * software; see the file license.txt. If not, navigate to the 
 * URL http://javatelnet.org/ and view the "License Agreement".
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
