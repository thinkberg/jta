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
package de.mud.jta.event;

import de.mud.jta.PluginMessage;
import de.mud.jta.PluginListener;
import de.mud.jta.event.SoundListener;

import java.net.URL;

/**
 * Play a sound.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class SoundRequest implements PluginMessage {
  protected URL audioClip;

  public SoundRequest(URL audioClip) {
    this.audioClip = audioClip;
  }

  /**
   * Notify all listeners that they may play the sound.
   * @param pl the list of plugin message listeners
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof SoundListener) {
      ((SoundListener)pl).playSound(audioClip);
    }
    return null;
  }
}
