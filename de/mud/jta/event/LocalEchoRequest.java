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
package de.mud.jta.event;

import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;
import de.mud.jta.event.LocalEchoListener;

/**
 * Notification of the local echo property. The terminal should echo all
 * typed in characters locally of this is true.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class LocalEchoRequest implements PluginMessage {
  protected boolean xecho = false;

  /** Create a new local echo request with the specified value. */
  public LocalEchoRequest(boolean echo) {
    xecho = echo;
  }

  /**
   * Notify all listeners about the status of local echo.
   * @param pl the list of plugin message listeners
   * @return always null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof LocalEchoListener)
      ((LocalEchoListener)pl).setLocalEcho(xecho);
    return null;
  }
}
