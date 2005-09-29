/*
 * This file is part of "JTA - Telnet/SSH for the JAVA(tm) platform".
 *
 * (c) Matthias L. Jugel, Marcus Meißner 1996-2005. All Rights Reserved.
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
package de.mud.bsx;

import java.awt.Point;
import java.awt.Graphics;
import java.awt.Color;

import java.util.Vector;
import java.util.Enumeration;

/**
 * Scene object for BSX Scenes.
 <ul>
 <li>keeps track on objects with their positions
 <li>renders its data on a given Graphics object
 </ul>
 @author  Thomas Kriegelstein (tk4@rb.mud.de)
 @version 1.1
*/
public class BSXScene extends BSXObject
{
    /** the eight BSX depth layers */
    protected final Vector[] layers = new Vector[8];
    /** positions of the contained objects */
    protected final Vector[] positions = new Vector[8];

    /**
     * checks if specified object is within this scene
     @param id object to be checked
     @return true if object is here, otherwise false
    */
    public boolean containsObject( String id )
    {
	return (-1==layerOfObject(id)?false:true);
    }
    /**
     * adds an object to this scene
     @param id object to be added
     @param x x-position of object in scene
     @param y y-position of object in scene
    */
    public void addObject( String id, int x, int y)
    {
	int layer;
	if (-1!=(layer = layerOfObject(id))) {
		removeObject(id,layer);
	}
	layers[y].addElement(id);
	positions[y].addElement(new Point(x,y));
    }
    /**
     * query the layer of the specified object
     @param id object in this scene
     @return -1 object not in this scene, 0..7 layer of the object
    */
    public int layerOfObject( String id )
    {
	for (int layer = 0;layer<layers.length;layer++)
		if (layers[layer].contains(id))
			return layer;
	return -1;
    }
    /**
     * removes the specified object from the scene
     @param id object to be removed
     @param layer number of the layer the object is supposed to be
    */
    public void removeObject( String id , int layer )
    {
	int idx;
	idx=layers[layer].indexOf(id);
	layers[layer].removeElementAt(idx);
	positions[layer].removeElementAt(idx);
    }
    /**
     * removes the specified object from the scene
     @param id object to be removed
    */
    public void removeObject( String id )
    {
	int layer;
	if (-1!=(layer=layerOfObject(id)))
		removeObject(id, layer);
    }
    /**
     * querys the location of an object
     @param id object to be found
     @return null object is not in this scene, location otherwise
    */
    public Point locateObject( String id )
    {
	int layer;
	layer = layerOfObject( id );
	if (layer!=-1)
		return (Point)positions[layer].elementAt( layers[layer].indexOf(id) );
	return null;
    }
    /**
     * removes all objects from all layers
     */
    public void clean()
    {
	for(int layer=0;layer<=7;layer++) {
		layers[layer].removeAllElements();
		positions[layer].removeAllElements();
	}
    }
    /**
     * Constructor for BSXScene
     @param id Identifier of this scene
     @param img offscreenimage to render the data on
     @param data description of this scene
    */
    public BSXScene( String id, int[][] data )
    {
	super(id, data);
	for (int layer=0;layer<layers.length;layer++) {
		layers[layer]=new Vector();
		positions[layer]=new Vector();
	}
    }
    /**
     * querys objects on a specific layer in this scene
     @param layer layer to look on
     @return Enumeration with objects on that layer
    */ 
    public Enumeration objects( int layer )
    {
	return layers[layer].elements();
    }
    /**
     * draws the scene on a graphics object
     @param g graphics object to draw on
     @param io ImageObserver to be notified
    */
    public void fill( Graphics g )
    {
	for (int polys=0;polys<data.length;polys++) {
		Color col=bsxColors[data[polys][0]];
		g.setColor(col);
		for (int points=0;points<poly[2*polys].length;points++) {
			int px,py;
			px=data[polys][2*points+1]*2;
			py=data[polys][2*points+2];
			poly[2*polys+0][points]=px;
			poly[2*polys+1][points]=256-py;
		}
		g.fillPolygon(poly[2*polys], 
			      poly[2*polys+1], 
			      poly[2*polys].length );
	}
    }
}

