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
import de.mud.jta.PluginConfig;
import de.mud.jta.event.ConfigurationListener;


/**
 * Configuration request message. Subclassing this message can be used to
 * make the configuration more specific and efficient.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author Matthias L. Jugel, Marcus Meiﬂner
 */
public class ConfigurationRequest implements PluginMessage {
  PluginConfig config;

  public ConfigurationRequest(PluginConfig config) {
    this.config = config;
  }

  /**
   * Notify all listeners of a configuration event.
   * @param pl the list of plugin message listeners
   */
  public Object firePluginMessage(PluginListener pl) {
    if(pl instanceof ConfigurationListener) {
      ((ConfigurationListener)pl).setConfiguration(config);
    }
    return null;
  }
}
