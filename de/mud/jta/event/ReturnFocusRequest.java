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
import de.mud.jta.event.ReturnFocusListener;

/**
 * Notify listeners that the focus is to be returned to whoever wants it.
 * <P>
 * Implemented after a suggestion by Dave &lt;david@mirrabooka.com&gt;
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class ReturnFocusRequest implements PluginMessage {
  /** Create a new return focus request.*/
  public ReturnFocusRequest() { }

  /**
   * Notify all listeners about return focus message.
   * @param pl the list of plugin message listeners
   * @return always null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof ReturnFocusListener)
      ((ReturnFocusListener)pl).returnFocus();
    return null;
  }
}
