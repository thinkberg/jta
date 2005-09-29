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

import java.awt.Color;
import java.awt.Graphics;

/**
 * Basic object for BSX Graphic.
 <ul>
 <li>renders its data on a given Graphics object
 </ul>
 @author  Thomas Kriegelstein (tk4@rb.mud.de)
 @version 1.0
 */
public class BSXObject
{
    /** the data to be rendered */
    protected int[][] data;
    /** the translated polygons */
    protected int[][] poly;
    /** name of this object */
    final String id;
    /** BSX Colors */
    public final static Color[] bsxColors = new Color[]
	{ new Color(  0,   0,  0),
	  new Color(  0,   0, 255),
	  new Color( 34, 139,  34),
	  new Color(135, 206, 235),
	  new Color(205,  92,  92),
	  new Color(255, 105, 180),
	  new Color(165,  42,  42),
	  new Color(211, 211, 211),
	  new Color(105, 105, 105),
	  new Color(  0, 191, 255),
	  new Color(  0, 255,   0),
	  new Color(  0, 255, 255),
	  new Color(255,  99,  71),
	  new Color(255,   0, 255),
	  new Color(255, 255,   0),
	  new Color(255, 255, 255) };
    /**
     * Constructor for BSXObject.
     @param id identifier of this object
     @param data field containing gfx information
     <ul>
     <li>data.length - number of polygons
     <li>(data[i].length-1)/2 - number of edges
     <li>data[i][0] - color index
     <li>data[i][j*2+1] - x coord
     <li>data[i][j*2+2] - y coord
     </ul>
    */
    public BSXObject( String id, int[][] data )
    {
		this.id = id;
		setData(data);
    }
    /**
     * draw image on specified graphics
     @param g draw there
     @param x BSX_XPOS
     @param y BSX_YPOS
     @param obs ImageObserver to be notified
    */
    public void draw( Graphics g, int x, int y )
    {
	for (int polys=0;polys<data.length;polys++)
	    {
		Color col=bsxColors[data[polys][0]];
		g.setColor(col);
		for (int points=0;points<poly[2*polys].length;points++)
		    {
			int px,py;
			px = data[polys][2*points+1];
			py = data[polys][2*points+2];
			px = px-127;
			px = px+x*16;
			py = py-127;
			py = py+4*y;
			poly[2*polys+0][points]=px*2;
			poly[2*polys+1][points]=255-py;
		    }
		g.fillPolygon(poly[2*polys], 
			      poly[2*polys+1], 
			      poly[2*polys].length );
	    }
    }
    /**
     * change th data of this object
     @param data new data to be used
    */ 
    public void setData(int[][] data)
    {
	this.data=data;
	poly=new int[data.length*2][];
	for (int num=0;num<data.length;num++)
	    {
		poly[2*num+0]=new int[(data[num].length-1)/2];
		poly[2*num+1]=new int[(data[num].length-1)/2];
	    }
    }
    public boolean equals(Object obj)
    {
	return (obj instanceof BSXObject)&&((BSXObject)obj).id.equals(this.id);
    }
    public int hashCode()
    {
	return id.hashCode();
    }
    /**
     * keep ressoure allocation small
     */
    public void flush()
    {
    }
}
