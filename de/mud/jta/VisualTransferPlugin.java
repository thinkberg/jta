/*
 * This file is part of "The Java Telnet Application".
 *
 * (c) Matthias L. Jugel, Marcus Meiﬂner 1996-2002. All Rights Reserved.
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

import java.awt.datatransfer.Clipboard;

/**
 * A visual plugin that also allows to copy and paste data.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public interface VisualTransferPlugin extends VisualPlugin {
  /**
   * Copy currently selected text into the clipboard.
   * @param clipboard the clipboard
   */
  public void copy(Clipboard clipboard);

  /**
   * Paste text from clipboard to the plugin.
   * @param clipboard the clipboard
   */
  public void paste(Clipboard clipboard);
}
