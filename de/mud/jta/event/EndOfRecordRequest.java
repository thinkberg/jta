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
import de.mud.jta.event.EndOfRecordListener;

/**
 * Notification of the end of record event
 * <P>
 * <B>Maintainer:</B> Marcus Meissner
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class EndOfRecordRequest implements PluginMessage {
  /** Create a new local echo request with the specified value. */
  public EndOfRecordRequest() { }

  /**
   * Notify all listeners about the end of record message
   * @param pl the list of plugin message listeners
   * @return always null
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof EndOfRecordListener)
      ((EndOfRecordListener)pl).EndOfRecord();
    return null;
  }
}
