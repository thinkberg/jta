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

import de.mud.jta.PluginListener;

/**
 * This interface should be used by plugins who would like to be notified
 * about the return of the focus from another plugin.
 * <P>
 * Implemented after a suggestion by Dave &lt;david@mirrabooka.com&gt;
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public interface ReturnFocusListener extends PluginListener {
  /** Called if the end of return focus message is sent. */
  public void returnFocus();
}
