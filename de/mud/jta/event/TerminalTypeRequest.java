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
package de.mud.jta.event;

import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;
import de.mud.jta.event.TerminalTypeListener;

/**
 * Request message for the current terminal type.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class TerminalTypeRequest implements PluginMessage {
  /**
   * Ask all terminal type listener about the terminal type and return
   * the first answer.
   * @param pl the list of plugin message listeners
   * @return the terminal type or null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof TerminalTypeListener) {
      Object ret = ((TerminalTypeListener)pl).getTerminalType();
      if(ret != null) return ret;
    }
    return null;
  }
}
