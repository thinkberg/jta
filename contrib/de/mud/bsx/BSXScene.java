package de.mud.bsx;

import java.awt.Point;
import java.awt.Image;
import java.awt.Graphics;
import java.awt.Color;

import java.awt.image.ImageObserver;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * Scene object for BSX Scenes.
 <ul>
 <li>keeps track on objects with their positions
 <li>renders itself in an offscreen image for speed reasons
 </ul>
*/
class BSXScene extends BSXObject
{
    /** the eight BSX depth layers */
    protected final Hashtable[] layers = new Hashtable[8];
    /** scene allready rendered */
    protected boolean rendered = false;
    /** the offscreen image to render on */
    protected Image visual;

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
	if (-1!=(layer = layerOfObject(id)))
	    {
		removeObject(id,layer);
	    }
	layers[y].put(id,new Point(x,y));
    }
    /**
     * query the layer of the specified object
     @param id object in this scene
     @return -1 object not in this scene, 0..7 layer of the object
    */
    public int layerOfObject( String id )
    {
	for (int layer = 0;layer<layers.length;layer++)
	    if (layers[layer].containsKey(id))
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
	layers[layer].remove(id);
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
	    return (Point)layers[layer].get( id );
	return null;
    }
    /**
     * removes all objects from all layers
     */
    public void clean()
    {
	for(int layer=0;layer<=7;layer++)
	    layers[layer].clear();
    }
    /**
     * Constructor for BSXScene
     @param id Identifier of this scene
     @param img offscreenimage to render the data on
     @param data description of this scene
    */
    public BSXScene( String id, Image img, int[][] data )
    {
	super(id, data);
	this.visual=img;
	for (int layer=0;layer<layers.length;layer++)
	    layers[layer]=new Hashtable();
    }
    /**
     * querys objects on a specific layer in this scene
     @param layer layer to look on
     @return Enumeration with objects on that layer
    */ 
    public Enumeration objects( int layer )
    {
	return layers[layer].keys();
    }
    /**
     * draws the scene on a graphics object
     @param g graphics object to draw on
     @param io ImageObserver to be notified
    */
    public void fill( Graphics g, ImageObserver io )
    {
	if (!rendered)
	    {
		Graphics gg=visual.getGraphics();
		for (int polys=0;polys<data.length;polys++)
		    {
			Color col=bsxColors[data[polys][0]];
			gg.setColor(col);
			for (int points=0;points<poly[2*polys].length;points++)
			    {
				int px,py;
				px=data[polys][2*points+1]*2;
				py=data[polys][2*points+2];
				poly[2*polys+0][points]=px;
				poly[2*polys+1][points]=256-py;
			    }
			gg.fillPolygon(poly[2*polys], 
				       poly[2*polys+1], 
				       poly[2*polys].length );
		    }
		rendered=true;
	    }
	g.drawImage(visual, 0, 0, io);
    }
    /**
     * change the data of this scene
     @param data new data to be used
     */ 
    public void setData( int[][] data )
    {
	super.setData(data);
	rendered=false;
    }
    /**
     * flush offscreenimage to keep memory allocation small
     */
    public void flush()
    {
	visual.flush();
    }
}
