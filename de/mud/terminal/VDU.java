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

package de.mud.terminal;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Point;
import java.awt.Insets;
import java.awt.Event;
import java.awt.Label;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Scrollbar;
import java.awt.Image;

/*
import java.awt.Graphics;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.PageFormat;
*/

import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseEvent;
import java.awt.event.FocusListener;
import java.awt.event.FocusEvent;

/**
 * Video Display Unit emulation. This class implements all necessary
 * features of a character display unit, but not the actual terminal emulation.
 * It can be used as the base for terminal emulations of any kind.
 * <P>
 * This is a lightweight component. It will render very badly if used
 * in standard AWT components without overloaded update() method. The
 * update() method must call paint() immediately without clearing the
 * components graphics context or parts of the screen will simply
 * disappear.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author  Matthias L. Jugel, Marcus Meiﬂner
 */
public class VDU extends Component 
  implements MouseListener, MouseMotionListener {
  /** The current version id tag */
  public final static String ID = "$Id$";

  /** Enable debug messages. */
  public final static int debug = 0;

  /** lightweight component definitions */
  private final static long VDU_EVENTS = AWTEvent.KEY_EVENT_MASK 
                                       | AWTEvent.FOCUS_EVENT_MASK
                                       | AWTEvent.ACTION_EVENT_MASK
                                       | AWTEvent.MOUSE_MOTION_EVENT_MASK
                                       | AWTEvent.MOUSE_EVENT_MASK;
  
  private Dimension size;                             /* rows and columns */
  private Insets insets;                            /* size of the border */
  private boolean raised;            /* indicator if the border is raised */

  private char charArray[][];                  /* contains the characters */
  private int charAttributes[][];             /* contains character attrs */
  private int bufSize, maxBufSize;                        /* buffer sizes */

  private int windowBase;                   /* where the start displaying */
  private int screenBase;                      /* the actual screen start */
  private int topMargin;                             /* top scroll margin */
  private int bottomMargin;                       /* bottom scroll margin */

  private Font normalFont;                                 /* normal font */
  private FontMetrics fm;                         /* current font metrics */
  private int charWidth;                       /* current width of a char */
  private int charHeight;                     /* current height of a char */
  private int charDescent;                           /* base line descent */
  private int resizeStrategy;                /* current resizing strategy */

  private int cursorX, cursorY;                /* current cursor position */
  private boolean showcursor = true;		/* show cursor */
  private Point selectBegin, selectEnd;          /* selection coordinates */
  private String selection;                 /* contains the selected text */

  private Scrollbar scrollBar;
  private SoftFont  sf = new SoftFont();

  private boolean update[];        /* contains the lines that need update */
  private boolean colorPrinting = false;	/* print display in color */
  
  private Image backingStore = null;

  /**
   * Create a color representation that is brighter than the standard
   * color but not what we would like to use for bold characters.
   * @param clr the standard color
   * @return the new brighter color
   */
  private Color brighten(Color clr) {
    return new Color(Math.max((int) (clr.getRed() *.85), 0),
		     Math.max((int) (clr.getGreen() * .85), 0),
		     Math.max((int) (clr.getBlue() * .85), 0));
  }

  /** A list of colors used for representation of the display */
  private Color color[] = { brighten(Color.black),
                            brighten(Color.red),
                            brighten(Color.green),
                            brighten(Color.yellow),
                            brighten(Color.blue),
                            brighten(Color.magenta),
                            brighten(Color.cyan),
                            brighten(Color.white),
  };

  public final static int COLOR_0 = 0;
  public final static int COLOR_1 = 1;
  public final static int COLOR_2 = 2;
  public final static int COLOR_3 = 3;
  public final static int COLOR_4 = 4;
  public final static int COLOR_5 = 5;
  public final static int COLOR_6 = 6;
  public final static int COLOR_7 = 7;

  /* definitions of standards for the display unit */
  private static int COLOR_FG_STD  = 7;
  private static int COLOR_BG_STD  = 0;
  private final static int COLOR         = 0x7f8;
  private final static int COLOR_FG      = 0x78;
  private final static int COLOR_BG      = 0x780;

  /** User defineable cursor colors */
  private Color cursorColorFG = null;
  private Color cursorColorBG = null;

  /** Scroll up when inserting a line. */
  public final static boolean SCROLL_UP   = false;
  /** Scroll down when inserting a line. */
  public final static boolean SCROLL_DOWN = true;

  /** Do nothing when the component is resized. */
  public final static int RESIZE_NONE  = 0;
  /** Resize the width and height of the character screen. */
  public final static int RESIZE_SCREEN  = 1;
  /** Resize the font to the new screen size. */
  public final static int RESIZE_FONT  = 2;
  
  /** Make character normal. */ 
  public final static int NORMAL  = 0x00;
  /** Make character bold. */ 
  public final static int BOLD    = 0x01;
  /** Underline character. */ 
  public final static int UNDERLINE  = 0x02;
  /** Invert character. */ 
  public final static int INVERT  = 0x04;

  /** 
   * Create a new video display unit with the passed width and height in
   * characters using a special font and font size. These features can
   * be set independently using the appropriate properties.
   * @param width the length of the character lines
   * @param height the amount of lines on the screen
   * @param font the font to be used (usually Monospaced)
   */
  public VDU(int width, int height, Font font) {
    // lightweight component handling
    enableEvents(VDU_EVENTS);
    
    // set the normal font to use
    setFont(font);
    // set the standard resize strategy
    setResizeStrategy(RESIZE_FONT);
    // set the display screen size
    setScreenSize(width, height);

    setForeground(Color.white);
    setBackground(Color.black);

    cursorColorFG = color[COLOR_FG_STD];
    cursorColorBG = color[COLOR_BG_STD];

    clearSelection();

    addMouseListener(this);
    addMouseMotionListener(this);

    selection = null;
  }
  
  /**
   * Create a display unit with specific size, Font is "Monospaced", size 12.
   * @param width the length of the character lines
   * @param height the amount of lines on the screen
   */
  public VDU(int width, int height) {
    this(width, height, new Font("Monospaced", Font.PLAIN, 11));
  }

  /**
   * Create a display with the font passed and size 80x24.
   * @param font the font to be used (usually Monospaced)
   */
  public VDU(Font font) {
    this(80, 24, font);
  }

  /**
   * Create a display unit with size 80x24 and Font "Monospaced", size 12.
   */
  public VDU() {
    this(80, 24, new Font("Monospaced", Font.PLAIN, 11));
  }

  public void setColorSet(Color[] colorset) {
    System.arraycopy(colorset, 0, color, 0, 8);
    update[0] = true;
    redraw();
  }

  public Color[] getColorSet() {
    return color;
  }

  /**
   * Put a character on the screen with normal font and outline.
   * The character previously on that position will be overwritten.
   * You need to call redraw() to update the screen.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @param ch the character to show on the screen
   * @see #insertChar
   * @see #deleteChar
   * @see #redraw
   */
  public void putChar(int c, int l, char ch) {
    putChar(c, l, ch, NORMAL);
  }

  /**
   * Put a character on the screen with specific font and outline.
   * The character previously on that position will be overwritten.
   * You need to call redraw() to update the screen.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @param ch the character to show on the screen
   * @param attributes the character attributes
   * @see #BOLD
   * @see #UNDERLINE
   * @see #INVERT
   * @see #NORMAL
   * @see #insertChar
   * @see #deleteChar
   * @see #redraw
   */  

  public void putChar(int c, int l, char ch, int attributes) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);
    charArray[screenBase + l][c] = ch;
    charAttributes[screenBase + l][c] = attributes;
    markLine(l, 1);
  }

  /**
   * Get the character at the specified position.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @see #putChar
   */
  public char getChar(int c, int l) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);
    return charArray[screenBase + l][c];
  }

  /**
   * Get the attributes for the specified position.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @see #putChar
   */
  public int getAttributes(int c, int l) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);
    return charAttributes[screenBase + l][c];
  }

  /**
   * Insert a character at a specific position on the screen.
   * All character right to from this position will be moved one to the right.
   * You need to call redraw() to update the screen.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @param ch the character to insert
   * @param attributes the character attributes
   * @see #BOLD
   * @see #UNDERLINE
   * @see #INVERT
   * @see #NORMAL
   * @see #putChar
   * @see #deleteChar
   * @see #redraw
   */
  public void insertChar(int c, int l, char ch, int attributes) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);
    System.arraycopy(charArray[screenBase + l], c, 
         charArray[screenBase + l], c + 1, size.width - c - 1);
    System.arraycopy(charAttributes[screenBase + l], c, 
         charAttributes[screenBase + l], c + 1, size.width - c - 1);
    putChar(c, l, ch, attributes);
  }

  /**
   * Delete a character at a given position on the screen.
   * All characters right to the position will be moved one to the left.
   * You need to call redraw() to update the screen.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @see #putChar
   * @see #insertChar
   * @see #redraw
   */
  public void deleteChar(int c, int l) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);
    if(c < size.width - 1) {
      System.arraycopy(charArray[screenBase + l], c + 1,
           charArray[screenBase + l], c, size.width - c - 1);
      System.arraycopy(charAttributes[screenBase + l], c + 1,
           charAttributes[screenBase + l], c, size.width - c - 1);
    }
    putChar(size.width - 1, l, (char)0);
  }

  /**
   * Put a String at a specific position. Any characters previously on that 
   * position will be overwritten. You need to call redraw() for screen update.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @param s the string to be shown on the screen
   * @see #BOLD
   * @see #UNDERLINE
   * @see #INVERT
   * @see #NORMAL
   * @see #putChar
   * @see #insertLine
   * @see #deleteLine
   * @see #redraw
   */  
  public void putString(int c, int l, String s) {
    putString(c, l, s, NORMAL);
  }
  
  /**
   * Put a String at a specific position giving all characters the same
   * attributes. Any characters previously on that position will be 
   * overwritten. You need to call redraw() to update the screen.
   * @param c x-coordinate (column)
   * @param l y-coordinate (line)
   * @param s the string to be shown on the screen
   * @param attributes character attributes
   * @see #BOLD
   * @see #UNDERLINE
   * @see #INVERT
   * @see #NORMAL
   * @see #putChar
   * @see #insertLine
   * @see #deleteLine
   * @see #redraw
   */
  public void putString(int c, int l, String s, int attributes) {
    for(int i = 0; i < s.length() && c + i < size.width; i++)
      putChar(c + i, l, s.charAt(i), attributes);
  }

  /**
   * Insert a blank line at a specific position.
   * The current line and all previous lines are scrolled one line up. The
   * top line is lost. You need to call redraw() to update the screen.
   * @param l the y-coordinate to insert the line
   * @see #deleteLine
   * @see #redraw
   */
  public void insertLine(int l) {
    insertLine(l, 1, SCROLL_UP);
  }

  /**
   * Insert blank lines at a specific position.
   * You need to call redraw() to update the screen
   * @param l the y-coordinate to insert the line
   * @param n amount of lines to be inserted
   * @see #deleteLine
   * @see #redraw
   */
  public void insertLine(int l, int n) {
    insertLine(l, n, SCROLL_UP);
  }  

  /**
   * Insert a blank line at a specific position. Scroll text according to
   * the argument.
   * You need to call redraw() to update the screen
   * @param l the y-coordinate to insert the line
   * @param scrollDown scroll down
   * @see #deleteLine
   * @see #SCROLL_UP
   * @see #SCROLL_DOWN
   * @see #redraw
   */
  public void insertLine(int l, boolean scrollDown) {
    insertLine(l, 1, scrollDown);
  }  

  /**
   * Insert blank lines at a specific position.
   * The current line and all previous lines are scrolled one line up. The
   * top line is lost. You need to call redraw() to update the screen.
   * @param l the y-coordinate to insert the line
   * @param n number of lines to be inserted
   * @param scrollDown scroll down
   * @see #deleteLine
   * @see #SCROLL_UP
   * @see #SCROLL_DOWN
   * @see #redraw
   */
  public synchronized void insertLine(int l, int n, boolean scrollDown) {
    l = checkBounds(l, 0, size.height - 1);

    char cbuf[][] = null;
    int abuf[][] = null;
    int offset = 0;
    int oldBase = screenBase;

    if (l > bottomMargin) /* We do not scroll below bottom margin (below the scrolling region). */
	return;
    int top = (l < topMargin ? 
               0 : (l > bottomMargin ?
                    (bottomMargin + 1 < size.height ?
                     bottomMargin + 1 : size.height - 1) : topMargin));
    int bottom = (l > bottomMargin ?
                  size.height - 1 : (l < topMargin ? 
                                     (topMargin > 0 ?
                                      topMargin - 1 : 0) : bottomMargin));

    // System.out.println("l is "+l+", top is "+top+", bottom is "+bottom+", bottomargin is "+bottomMargin+", topMargin is "+topMargin);
    
    if(scrollDown) {
      if(n > (bottom - top)) n = (bottom - top);
      cbuf = new char[bottom - l - (n - 1)][size.width];
      abuf = new int[bottom - l - (n - 1)][size.width];
      
      System.arraycopy(charArray, oldBase + l, cbuf, 0, bottom - l - (n - 1));
      System.arraycopy(charAttributes, oldBase + l, 
		       abuf, 0, bottom - l - (n - 1));
      System.arraycopy(cbuf, 0, charArray, oldBase + l + n, 
		       bottom - l - (n - 1));
      System.arraycopy(abuf, 0, charAttributes, oldBase + l + n, 
		       bottom - l - (n - 1));
      cbuf = charArray;
      abuf = charAttributes;
    } else try {
      if(n > (bottom - top) + 1) n = (bottom - top) + 1;
      if(bufSize < maxBufSize) {
        if(bufSize + n > maxBufSize) {
          offset = n - (maxBufSize - bufSize);
          bufSize = maxBufSize;
          screenBase = maxBufSize - size.height - 1;
          windowBase = screenBase;
        } else {
          screenBase += n;
          windowBase += n;
          bufSize += n;
        }
        cbuf = new char[bufSize][size.width];
        abuf = new int[bufSize][size.width];
      } else {
        offset = n;
        cbuf = charArray;
        abuf = charAttributes;
      }
      // copy anything from the top of the buffer (+offset) to the new top
      // up to the screenBase.
      if(oldBase > 0) {
        System.arraycopy(charArray, offset, 
                         cbuf, 0, 
                         oldBase - offset);
        System.arraycopy(charAttributes, offset, 
                         abuf, 0, 
                         oldBase - offset);
      }
      // copy anything from the top of the screen (screenBase) up to the
      // topMargin to the new screen
      if(top > 0) {
        System.arraycopy(charArray, oldBase, 
                         cbuf, screenBase, 
                         top);
        System.arraycopy(charAttributes, oldBase, 
                         abuf, screenBase, 
                         top);
      }
      // copy anything from the topMargin up to the amount of lines inserted
      // to the gap left over between scrollback buffer and screenBase
      if(oldBase > 0) {
	System.arraycopy(charArray, oldBase + top, 
			 cbuf, oldBase - offset,
			 n);
	System.arraycopy(charAttributes, oldBase + top, 
			 abuf, oldBase - offset,
			 n);
      }
      // copy anything from topMargin + n up to the line linserted to the
      // topMargin
      System.arraycopy(charArray, oldBase + top + n,
                       cbuf, screenBase + top,
                       l - top - (n - 1));
      System.arraycopy(charAttributes, oldBase + top + n,
                       abuf, screenBase + top,
                       l - top - (n - 1));
      //
      // copy the all lines next to the inserted to the new buffer
      if(l < size.height - 1) {
        System.arraycopy(charArray, oldBase + l + 1,
                         cbuf, screenBase + l + 1,
                         (size.height - 1) - l);
        System.arraycopy(charAttributes, oldBase + l + 1,
                         abuf, screenBase + l + 1,
                         (size.height - 1) - l);
      }
    } catch(ArrayIndexOutOfBoundsException e) {
      // this should not happen anymore, but I will leave the code
      // here in case something happens anyway. That code above is
      // so complex I always have a hard time understanding what
      // I did, even though there are comments
      System.err.println("*** Error while scrolling up:");
      System.err.println("--- BEGIN STACK TRACE ---");
      e.printStackTrace();
      System.err.println("--- END STACK TRACE ---");
      System.err.println("bufSize="+bufSize+", maxBufSize="+maxBufSize);
      System.err.println("top="+top+", bottom="+bottom);
      System.err.println("n="+n+", l="+l);
      System.err.println("screenBase="+screenBase+", windowBase="+windowBase);
      System.err.println("oldBase="+oldBase);
      System.err.println("size.width="+size.width+", size.height="+size.height);
      System.err.println("abuf.length="+abuf.length+", cbuf.length="+cbuf.length);
      System.err.println("*** done dumping debug information");
    }
    
    for(int i = 0; i < n; i++) {
      cbuf[(screenBase + l) + (scrollDown ? i : -i) ] = new char[size.width];
      abuf[(screenBase + l) + (scrollDown ? i : -i) ] = new int[size.width];
    }

    charArray = cbuf;
    charAttributes = abuf;
    
    if(scrollDown)
      markLine(l, bottom - l + 1);
    else
      markLine(top, l - top + 1);

    if(scrollBar != null)
      scrollBar.setValues(windowBase, size.height, 0, bufSize);
  }
  
  /**
   * Delete a line at a specific position. Subsequent lines will be scrolled 
   * up to fill the space and a blank line is inserted at the end of the 
   * screen.
   * @param l the y-coordinate to insert the line
   * @see #deleteLine
   */
  public void deleteLine(int l) {
    l = checkBounds(l, 0, size.height - 1);

    int bottom = (l>bottomMargin?size.height-1:
		  (l<topMargin?topMargin:bottomMargin+1));
    System.arraycopy(charArray, screenBase + l + 1,
                     charArray, screenBase + l, bottom - l -1 );
    System.arraycopy(charAttributes, screenBase + l + 1,
                     charAttributes, screenBase + l, bottom - l -1);
    charArray[screenBase + bottom - 1] = new char[size.width];
    charAttributes[screenBase + bottom - 1] = new int[size.width];
    markLine(l, bottom - l);
  }


  /**
   * Delete a rectangular portion of the screen.
   * You need to call redraw() to update the screen.
   * @param c x-coordinate (column)
   * @param l y-coordinate (row)
   * @param w with of the area in characters
   * @param h height of the area in characters
   * @see #deleteChar
   * @see #deleteLine
   * @see redraw
   */
  public void deleteArea(int c, int l, int w, int h) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);

    char cbuf[] = new char[w];
    int abuf[] = new int[w];
    
    for(int i = 0; i < h && l + i < size.height; i++)
    {
      System.arraycopy(cbuf, 0, charArray[screenBase + l + i], c, w);
      System.arraycopy(abuf, 0, charAttributes[screenBase + l + i], c, w);
    }
    markLine(l, h);
  }

  /**
   * Puts the cursor at the specified position.
   * @param c column
   * @param l line
   */
  public void setCursorPosition(int c, int l) {
    c = checkBounds(c, 0, size.width - 1);
    l = checkBounds(l, 0, size.height - 1);
    markLine(cursorY, 1);
    // FIXME: those are superflous? -Marcus
    cursorX = (c < size.width ? c : size.width);
    cursorY = (l < size.height ? l : size.height);
    markLine(cursorY, 1);
  }

  public void showCursor(boolean doshow) {
    if (doshow != showcursor)
	markLine(cursorY,1);
    showcursor = doshow;
  }

  /**
   * Get the current cursor position.
   * @see java.awt.Dimension
   */
  public Dimension getCursorPosition() {
    return new Dimension(cursorX, cursorY);
  }

  /**
   * Set the top scroll margin for the screen. If the current bottom margin
   * is smaller it will become the top margin and the line will become the
   * bottom margin.
   * @param l line that is the margin
   */
  public void setTopMargin(int l) {
    if(l > bottomMargin) {
      topMargin = bottomMargin;
      bottomMargin = l;
    }
    else
      topMargin = l;
    if(topMargin < 0) topMargin = 0;
    if(bottomMargin > size.height - 1) bottomMargin = size.height - 1;
  }

  /**
   * Get the top scroll margin.
   */
  public int getTopMargin() {
    return topMargin;
  }

  /**
   * Set the bottom scroll margin for the screen. If the current top margin
   * is bigger it will become the bottom margin and the line will become the
   * top margin.
   * @param l line that is the margin
   */
  public void setBottomMargin(int l) {
    if(l < topMargin) {
      bottomMargin = topMargin;
      topMargin = l;
    }
    else
      bottomMargin = l;
    if(topMargin < 0) topMargin = 0;
    if(bottomMargin > size.height - 1) bottomMargin = size.height - 1;
  }

  /**
   * Get the bottom scroll margin.
   */
  public int getBottomMargin() {
    return bottomMargin;
  }
    
  /**
   * Set scrollback buffer size.
   * @param amount new size of the buffer
   */
  public void setBufferSize(int amount) {
    if(amount < size.height) amount = size.height;
    if(amount < maxBufSize) {
      char cbuf[][] = new char[amount][size.width];
      int abuf[][] = new int[amount][size.width];
      int copyStart = bufSize - amount < 0 ? 0 : bufSize - amount;
      int copyCount = bufSize - amount < 0 ? bufSize : amount;
      if(charArray != null)
        System.arraycopy(charArray, copyStart, cbuf, 0, copyCount);
      if(charAttributes != null)
        System.arraycopy(charAttributes, copyStart, abuf, 0, copyCount);
      charArray = cbuf;
      charAttributes = abuf;
      bufSize = copyCount;
      screenBase = bufSize - size.height;
      windowBase = screenBase;
    }
    maxBufSize = amount;
 
    update[0] = true;
    redraw();
  }

  /**
   * Retrieve current scrollback buffer size.
   * @see #setBufferSize
   */
  public int getBufferSize() {
    return bufSize;
  }

  /**
   * Retrieve maximum buffer Size.
   * @see #getBufferSize
   */
  public int getMaxBufferSize() {
    return maxBufSize;
  }

  /**
   * Set the current window base. This allows to view the scrollback buffer.
   * @param line the line where the screen window starts
   * @see setBufferSize
   * @see getBufferSize
   */
  public void setWindowBase(int line) {
    if(line > screenBase) line = screenBase;
    else if(line < 0) line = 0;
    windowBase = line;
    update[0] = true;
    redraw();
  }

  /**
   * Get the current window base.
   * @see setWindowBase
   */
  public int getWindowBase() {
    return windowBase;
  }

  /**
   * Set the font to be used for rendering the characters on screen.
   * @param font the new font to be used.
   */
  public void setFont(Font font) {
    super.setFont(normalFont = font);
    fm = getFontMetrics(font);
    if(fm != null) {
      charWidth = fm.charWidth('@');
      charHeight = fm.getHeight();
      charDescent = fm.getDescent();
    }
    if(update != null) update[0] = true;
    redraw();
  }
      
  /**
   * Change the size of the screen. This will include adjustment of the 
   * scrollback buffer.
   * @param columns width of the screen
   * @param columns height of the screen
   */
  public void setScreenSize(int width, int height) {
    char cbuf[][];
    int abuf[][];
    int bsize = bufSize;

    if(width < 1 || height < 1) return;
    
    if(debug > 0)
      System.err.println("VDU: screen size ["+width+","+height+"]");

    if(height > maxBufSize) 
      maxBufSize = height;

    if(height > bufSize) {
      bufSize = height;
      screenBase = 0;
      windowBase = 0;
    }

    if(windowBase+height >= bufSize)
      windowBase = bufSize-height;

    if(screenBase+height >= bufSize)
      screenBase = bufSize-height;


    cbuf = new char[bufSize][width];
    abuf = new int[bufSize][width];
    
    if(charArray != null && charAttributes != null)
      for(int i = 0; i < bsize && i < bufSize; i++) {
        System.arraycopy(charArray[i], 0, cbuf[i], 0, 
             width < size.width ? width : size.width);
        System.arraycopy(charAttributes[i], 0, abuf[i], 0, 
             width < size.width ? width : size.width);
      }
    charArray = cbuf;
    charAttributes = abuf;
    size = new Dimension(width, height);
    topMargin = 0;
    bottomMargin = height - 1;
    update = new boolean[height + 1];
    update[0] = true;
    if(resizeStrategy == RESIZE_FONT)
      setBounds(getBounds());
  }

  /**
   * Get the screen size in rows and columns.
   */
  public Dimension getScreenSize() {
    return size;
  }

  /**
   * Set the strategy when window is resized.
   * RESIZE_FONT is default.
   * @param strategy the strategy
   * @see #RESIZE_NONE
   * @see #RESIZE_FONT
   * @see #RESIZE_SCREEN
   */
  public void setResizeStrategy(int strategy) {
    resizeStrategy = strategy;
  }
  
  /**
   * Get amount of rows on the screen.
   */
  public int getRows() { return size.height; }

  /**
   * Get amount of columns on the screen.
   */
  public int getColumns() { return size.width; }

  /**
   * Set the border thickness and the border type.
   * @param thickness border thickness in pixels, zero means no border
   * @param raised a boolean indicating a raised or embossed border
   */
  public void setBorder(int thickness, boolean raised) {
    if(thickness == 0) insets = null;
    else insets = new Insets(thickness+1, thickness+1, 
                             thickness+1, thickness+1);
    this.raised = raised;
  }

  /**
   * Connect a scrollbar to the VDU. This should be done differently
   * using a property change listener.
   * @param scrollBar the scroll bar
   */
  public void setScrollbar(Scrollbar scrollBar) {
    if(scrollBar == null) return;
    this.scrollBar = scrollBar;
    this.scrollBar.setValues(windowBase, size.height, 0, bufSize - size.height);
    this.scrollBar.addAdjustmentListener(new AdjustmentListener() {
      public void adjustmentValueChanged(AdjustmentEvent evt) {
        setWindowBase(evt.getValue());
      }
    });
  }

  /**
   * Mark lines to be updated with redraw().
   * @param l starting line
   * @param n amount of lines to be updated
   * @see #redraw
   */
  public void markLine(int l, int n) {
    l = checkBounds(l, 0, size.height - 1);
    for(int i = 0; (i < n) && (l + i < size.height); i++)
      update[l + i + 1] = true;
  }
  
  /**
   * Redraw marked lines.
   * @see #markLine
   */
  public void redraw() {
    if(backingStore != null) {
      redraw(backingStore.getGraphics());
      repaint();
    }
  }

  protected synchronized void redraw(Graphics g) {
    if(debug > 0) System.err.println("redraw()");

    int xoffset = (super.getSize().width - size.width * charWidth) / 2;
    int yoffset = (super.getSize().height - size.height * charHeight) / 2;

    int selectStartLine = selectBegin.y - windowBase;
    int selectEndLine = selectEnd.y - windowBase;

    Color fg = color[COLOR_FG_STD];
    Color bg = color[COLOR_BG_STD];

    g.setFont(normalFont);

   
   /* for debug only
    if (update[0]) {
        System.err.println("Redrawing all");
    } else {
	for (int l = 1; l < size.height+1; l++) {
	    if (update[l]) {
	    	for (int c = 0; c < size.height-l;c++) {
		    if (!update[c+l]) {
			System.err.println("Redrawing "+(l-1)+" - "+(l+c-2));
			l=l+c;
			break;
		    }
		}
	    }
	}
    }
    */

    for(int l = 0; l < size.height; l++) {
      if(!update[0] && !update[l + 1]) continue;
      update[l + 1] = false;
      if(debug > 2) System.err.println("redraw(): line "+l);
      for(int c = 0; c < size.width; c++) {
        int addr = 0;
        int currAttr = charAttributes[windowBase + l][c];

        fg = getForeground();
        bg = getBackground();

        if((currAttr & COLOR_FG) != 0)
          fg = color[((currAttr & COLOR_FG) >> 3)-1];
        if((currAttr & COLOR_BG) != 0)
          bg = color[((currAttr & COLOR_BG) >> 7)-1].darker();

        if((currAttr & BOLD) != 0)
          if(fg.equals(Color.black))
            fg = Color.gray;
          else {
	    fg = fg.brighter();
	    // bg = bg.brighter(); -- make some programs ugly
	  }

        if((currAttr & INVERT) != 0) { Color swapc = bg; bg=fg;fg=swapc; }

        if (sf.inSoftFont(charArray[windowBase + l][c])) {
          g.setColor(bg);	
          g.fillRect(c * charWidth + xoffset, l * charHeight + yoffset, 
                     charWidth, charHeight);
          g.setColor(fg);	
          sf.drawChar(g,charArray[windowBase + l][c],xoffset+c*charWidth,
                      l*charHeight+yoffset, charWidth, charHeight);
          if((currAttr & UNDERLINE) != 0)
            g.drawLine(c * charWidth + xoffset,
                       (l+1) * charHeight - charDescent / 2 + yoffset,
                       c * charWidth + charWidth + xoffset, 
                       (l+1) * charHeight - charDescent / 2 + yoffset);
          continue;
        }
      
        // determine the maximum of characters we can print in one go
        while(c + addr < size.width && 
              charAttributes[windowBase + l][c + addr] == currAttr &&
              !sf.inSoftFont(charArray[windowBase + l ][c+addr])) {
          if(charArray[windowBase + l][c + addr] < ' ')
            charArray[windowBase + l][c + addr] = ' ';
          addr++;
        }
      
        // clear the part of the screen we want to change (fill rectangle)
        g.setColor(bg);
        g.fillRect(c * charWidth + xoffset, l * charHeight + yoffset,
                   addr * charWidth, charHeight);

        g.setColor(fg);
      
        // draw the characters
        g.drawChars(charArray[windowBase + l], c, addr, 
                    c * charWidth + xoffset, 
                    (l+1) * charHeight - charDescent + yoffset);

        if((currAttr & UNDERLINE) != 0)
          g.drawLine(c * charWidth + xoffset,
                     (l+1) * charHeight - charDescent / 2 + yoffset,
                     c * charWidth + addr * charWidth + xoffset, 
                     (l+1) * charHeight - charDescent / 2 + yoffset);
        
        c += addr - 1;
      }

      // selection code, highlites line or part of it when it was
      // selected previously
      if(l >= selectStartLine && l <= selectEndLine) {
	int selectStartColumn = (l == selectStartLine ? selectBegin.x : 0);
	int selectEndColumn = 
	  (l == selectEndLine ? 
	    (l == selectStartLine ? selectEnd.x - selectStartColumn : 
	                            selectEnd.x) : size.width);
	if(selectStartColumn != selectEndColumn) {
	  if(debug > 0) 
	    System.err.println("select("+selectStartColumn+"-"
	                                +selectEndColumn+")");
          g.setXORMode(bg);
	  g.fillRect(selectStartColumn * charWidth + xoffset,
	             l * charHeight + yoffset,
		     selectEndColumn * charWidth,
		     charHeight);
	  g.setPaintMode();
        }
      }

    }

    // draw cursor
    if(showcursor && (
       screenBase + cursorY >= windowBase && 
       screenBase + cursorY < windowBase + size.height)
    ) {
      g.setColor(cursorColorFG);
      g.setXORMode(cursorColorBG);
      g.fillRect( cursorX * charWidth + xoffset, 
                 (cursorY + screenBase - windowBase) * charHeight + yoffset,
                 charWidth, charHeight);
      g.setPaintMode();
      g.setColor(color[COLOR_FG_STD]);
    }

    // draw border
    if(insets != null) {
      g.setColor(getBackground());
      xoffset--; yoffset--;
      for(int i = insets.top - 1; i >= 0; i--)
        g.draw3DRect(xoffset - i, yoffset - i,
                     charWidth * size.width + 1 + i * 2, 
                     charHeight * size.height + 1 + i * 2,
                     raised);
    }
    update[0] = false;
  }

  /**
   * Update the display. To reduce flashing we have overridden this method.
   */
  /*
  public void update(Graphics g) {
    if(debug > 0) System.err.println("update()");
    paint(g);
  }
  */

  /**
   * Paint the current screen using the backing store image.
   */
  public synchronized void paint(Graphics g) {
    if(backingStore == null) {
      Dimension size = super.getSize();
      backingStore = createImage(size.width, size.height);
      update[0] = true;
      redraw();
    }

    if(debug > 1)
      System.err.println("Clip region: "+g.getClipBounds());

    g.drawImage(backingStore, 0, 0, this);
  }

/*
  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if(pi >= 1) {
      return Printable.NO_SUCH_PAGE;
    }
    paint(g);
    return Printable.PAGE_EXISTS;
  }
*/

  /**
   * Set default for printing black&amp;white or colorized as displayed on
   * screen. 
   * @param name colorPrint true = print in full color, default b&amp;w only
   */
  public void setColorPrinting(boolean colorPrint) {
    colorPrinting = colorPrint;
  }

  public void print(Graphics g) {
    if(debug > 0) System.err.println("DEBUG: print()");
    for(int i = 0; i <= size.height; i++) update[i] = true;
    Color fg = null, bg = null, colorSave[] = null;
    if(!colorPrinting) {
      fg = getForeground();
      bg = getBackground();
      setForeground(Color.black);
      setBackground(Color.white);
      colorSave = color;
      color = new Color[] { Color.black, 
                            Color.black, 
			    Color.black,
			    Color.black,
			    Color.black,
			    Color.black,
			    Color.black,
			    Color.white
			  };
    }

    redraw(g);

    if(!colorPrinting) {
      color = colorSave;
      setForeground(fg);
      setBackground(bg);
    }
  }

  private int checkBounds(int value, int lower, int upper) {
    if(value < lower) return lower;
    if(value > upper) return upper;
    return value;
  }

  /**
   * Convert Mouse Event coordinates into character cell coordinates
   * @param the mouse point to be converted
   * @return Character cell coordinate of passed point
   */
  public Point mouseGetPos(Point evtpt)
  {
    Point mousepos;

    mousepos = new Point(0,0);

    int xoffset = (super.getSize().width - size.width * charWidth) / 2;
    int yoffset = (super.getSize().height - size.height * charHeight) / 2;

    mousepos.x = (evtpt.x - xoffset) / charWidth;
    if(mousepos.x < 0) mousepos.x = 0;
    if(mousepos.x >= size.width) mousepos.x = size.width - 1;

    mousepos.y = (evtpt.y - yoffset) / charHeight;
    if(mousepos.y < 0) mousepos.y = 0;
    if(mousepos.y >= size.height) mousepos.y = size.height - 1;

    return mousepos;
  }

  /**
   * Set cursor FG and BG colors
   * @param fg foreground color or null
   * @param bg background color or null
   */
  public void setCursorColors(Color fg, Color bg)
  {
    if (fg == null) cursorColorFG = color[COLOR_FG_STD];
    else cursorColorFG = fg;
    if (bg == null) cursorColorBG = color[COLOR_BG_STD];
    else cursorColorBG = bg;
    repaint();
  }
          
  /**
   * Reshape character display according to resize strategy.
   * @see #setResizeStrategy
   */
  public void setBounds(int x, int y, int w, int h) {
    if(debug > 0)
      System.err.println("VDU: setBounds("+x+","+y+","+w+","+h+")");

    super.setBounds(x, y, w, h);

    int xborder = 0, yborder = 0;
    
    if(insets != null) {
      w -= (xborder = insets.left + insets.right);
      h -= (yborder = insets.top + insets.bottom);
    }

    if(debug > 0)
      System.err.println("VDU: looking for better match for "+normalFont);

    Font tmpFont = normalFont;
    String fontName = tmpFont.getName();
    int fontStyle = tmpFont.getStyle();
    fm = getFontMetrics(normalFont);
    if(fm != null) {
      charWidth = fm.charWidth('@');
      charHeight = fm.getHeight();
    }
    
    switch(resizeStrategy) {
    case RESIZE_SCREEN:
      setScreenSize(w / charWidth, size.height = h / charHeight);
      break;
    case RESIZE_FONT:
      int height = h / size.height;
      int width = w / size.width;
      
      fm = getFontMetrics(normalFont = new Font(fontName, fontStyle,
                                                charHeight));
      
      // adapt current font size (from small up to best fit)
      if(fm.getHeight() < height || fm.charWidth('@') < width)
        do {
          fm = getFontMetrics(normalFont = new Font(fontName, fontStyle,
	                                            ++charHeight));
        } while(fm.getHeight() < height || fm.charWidth('@') < width); 
      
      // now check if we got a font that is too large
      if(fm.getHeight() > height || fm.charWidth('@') > width)
        do {
          fm = getFontMetrics(normalFont = new Font(fontName, fontStyle,
	                                            --charHeight));
        } while(charHeight > 1 && 
                (fm.getHeight() > height || 
                 fm.charWidth('@') > width));
      
      if(charHeight <= 1) {
        System.err.println("VDU: error during resize, resetting");
        normalFont = tmpFont;
        System.err.println("VDU: disabling font/screen resize");
        resizeStrategy = RESIZE_NONE;
      }

      setFont(normalFont);
      fm = getFontMetrics(normalFont);
      charWidth = fm.charWidth('@');
      charHeight = fm.getHeight();
      charDescent = fm.getDescent();
      break;
    case RESIZE_NONE:
    default:
      break;
    }
    if(debug > 0) {
      System.err.println("VDU: charWidth="+charWidth+", "+
                              "charHeight="+charHeight+", "+
                              "charDescent="+charDescent);
    }

    // delete the double buffer image and mark all lines
    backingStore = null;
    markLine(0, size.height);
  }

  /**
   * Return the real size in points of the character display.
   * @return Dimension the dimension of the display
   * @see java.awt.Dimension
   */
  public Dimension getSize() {
    int xborder = 0, yborder = 0;
    if(insets != null) {
      xborder = insets.left + insets.right;
      yborder = insets.top + insets.bottom;
    }
    return new Dimension(size.width * charWidth + xborder, 
                         size.height * charHeight + yborder);
  }

  /**
   * Return the preferred Size of the character display.
   * This turns out to be the actual size.
   * @return Dimension dimension of the display
   * @see size
   */
  public Dimension getPreferredSize() {
    return getSize();
  }

  /**
   * The insets of the character display define the border.
   * @return Insets border thickness in pixels
   */
  public Insets getInsets() {
    return insets;
  }

  public void clearSelection() {
    selectBegin = new Point(0,0);
    selectEnd = new Point(0,0); 
    selection = null;
  }

  public String getSelection() {
    return selection;
  }

  private boolean buttonCheck(int modifiers, int mask) {
    return (modifiers & mask) == mask;
  }

  public void mouseMoved(MouseEvent evt) {
    /* nothing yet we do here */
  }

  public void mouseDragged(MouseEvent evt) {
    if(buttonCheck(evt.getModifiers(), MouseEvent.BUTTON1_MASK) ||
       // Windows NT/95 etc: returns 0, which is a bug
       evt.getModifiers() == 0) {
      int xoffset = (super.getSize().width - size.width * charWidth) / 2;
      int yoffset = (super.getSize().height - size.height * charHeight) / 2;
      int x = (evt.getX() - xoffset) / charWidth;
      int y = (evt.getY() - yoffset) / charHeight + windowBase;
      int oldx = selectEnd.x, oldy = selectEnd.y;

      if((x <= selectBegin.x && y <= selectBegin.y)) {
        selectBegin.x = x;
        selectBegin.y = y;
      } else {
        selectEnd.x = x;
        selectEnd.y = y;
      }
      
      if(oldx != x || oldy != y) {
	update[0] = true;
	if(debug > 0)
	  System.err.println("select(["+selectBegin.x+","+selectBegin.y+"],"+
	                            "["+selectEnd.x+","+selectEnd.y+"])");
        redraw();
      }
    }
  }

  public void mouseClicked(MouseEvent evt) {
    /* nothing yet we do here */
  }

  public void mouseEntered(MouseEvent evt) {
    /* nothing yet we do here */
  }

  public void mouseExited(MouseEvent evt) {
    /* nothing yet we do here */
  }

  /**
   * Handle mouse pressed events for copy & paste.
   * @param evt the event that occured
   * @see java.awt.event.MouseEvent
   */
  public void mousePressed(MouseEvent evt) {
    requestFocus();

    int xoffset = (super.getSize().width - size.width * charWidth) / 2;
    int yoffset = (super.getSize().height - size.height * charHeight) / 2;

    // looks like we get no modifiers here ... ... We do? -Marcus
    if(buttonCheck(evt.getModifiers(), MouseEvent.BUTTON1_MASK)) {
      selectBegin.x = (evt.getX() - xoffset) / charWidth;
      selectBegin.y = (evt.getY() - yoffset) / charHeight + windowBase;
      selectEnd.x = selectBegin.x;
      selectEnd.y = selectBegin.y;
    }
  }

  /**
   * Handle mouse released events for copy & paste.
   * @param evt the mouse event
   */
  public void mouseReleased(MouseEvent evt) {
    if(buttonCheck(evt.getModifiers(), MouseEvent.BUTTON1_MASK)) {
      int xoffset = (super.getSize().width - size.width * charWidth) / 2;
      int yoffset = (super.getSize().height - size.height * charHeight) / 2;
      mouseDragged(evt);
  
      if(selectBegin.x == selectEnd.x &&
         selectBegin.y == selectEnd.y) {
	update[0] = true;
        redraw();
        return;
      }
      selection = "";
      // fix end.x and end.y, they can get over the border
      if(selectEnd.x < 0) selectEnd.x = 0;
      if(selectEnd.y < 0) selectEnd.y = 0;
      if(selectEnd.y >= charArray.length)
        selectEnd.y = charArray.length-1;
      if(selectEnd.x > charArray[0].length)
        selectEnd.x = charArray[0].length;

      for(int l = selectBegin.y; l <= selectEnd.y; l++) {
        int start, end;
	start = (l == selectBegin.y ? start = selectBegin.x : 0);
	end = (l == selectEnd.y ? end = selectEnd.x : charArray[l].length );
	// Trim all spaces from end of line, like xterm does.
	selection += ("-" + (new String(charArray[l])).substring(start,end)).trim().substring(1);
	if(end == charArray[l].length)
	  selection += "\n";
      }
    }
  } 

  // lightweight component event handling

  private MouseListener mouseListener;

  /**
   * Add a mouse listener to the VDU. This is the implementation for
   * the lightweight event handling.
   * @param listener the new mouse listener
   */
  public void addMouseListener(MouseListener listener) {
    mouseListener = AWTEventMulticaster.add(mouseListener, listener);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
  }

  /**
   * Remove a mouse listener to the VDU. This is the implementation for
   * the lightweight event handling.
   * @param listener the mouse listener to remove
   */
  public void removeMouseListener(MouseListener listener) {
    mouseListener = AWTEventMulticaster.remove(mouseListener, listener);
  }

  private MouseMotionListener mouseMotionListener;
 
  /**
   * Add a mouse motion listener to the VDU. This is the implementation for
   * the lightweight event handling.
   * @param listener the mouse motion listener
   */
  public void addMouseMotionListener(MouseMotionListener listener) {
    mouseMotionListener = AWTEventMulticaster.add(mouseMotionListener,listener);
    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
  }

  /**
   * Remove a mouse motion listener to the VDU. This is the implementation for
   * the lightweight event handling.
   * @param listener the mouse motion listener to remove
   */
  public void removeMouseMotionListener(MouseMotionListener listener) {
    mouseMotionListener = 
      AWTEventMulticaster.remove(mouseMotionListener, listener);
  }

  /**
   * Process mouse events for this component. It will call the 
   * methods (mouseClicked() etc) in the added mouse listeners.
   * @param evt the dispatched mouse event
   */
  public void processMouseEvent(MouseEvent evt) {
    // handle simple mouse events
    if(mouseListener != null)
      switch(evt.getID()) {
        case MouseEvent.MOUSE_CLICKED:
          mouseListener.mouseClicked(evt); break;
        case MouseEvent.MOUSE_ENTERED:
          mouseListener.mouseEntered(evt); break;
        case MouseEvent.MOUSE_EXITED:
          mouseListener.mouseExited(evt); break;
        case MouseEvent.MOUSE_PRESSED:
          mouseListener.mousePressed(evt); break;
        case MouseEvent.MOUSE_RELEASED:
          mouseListener.mouseReleased(evt); break;
      }
     super.processMouseEvent(evt);
   }

  /**
   * Process mouse motion events for this component. It will call the 
   * methods (mouseDragged() etc) in the added mouse motion listeners.
   * @param evt the dispatched mouse event
   */
   public void processMouseMotionEvent(MouseEvent evt) {
    // handle mouse motion events
    if(mouseMotionListener != null)
      switch(evt.getID()) {
        case MouseEvent.MOUSE_DRAGGED:
          mouseMotionListener.mouseDragged(evt); break;
        case MouseEvent.MOUSE_MOVED:
          mouseMotionListener.mouseMoved(evt); break;
      }
    super.processMouseMotionEvent(evt);
  }
    
  private KeyListener keyListener;

  /**
   * Add a key listener to the VDU. This is necessary to be able to receive
   * keyboard input from this component. It is a prerequisite for a
   * lightweigh component.
   * @param listener the key listener
   */
  public void addKeyListener(KeyListener listener) {
    keyListener = AWTEventMulticaster.add(keyListener, listener);
    enableEvents(AWTEvent.KEY_EVENT_MASK);
  }

  /**
   * Remove key listener from the VDU. It is a prerequisite for a
   * lightweigh component.
   * @param listener the key listener to remove
   */
  public void removeKeyListener(KeyListener listener) {
    keyListener = AWTEventMulticaster.remove(keyListener, listener);
  }

  /**
   * Process key events for this component.
   * @param evt the dispatched key event
   */
  public void processKeyEvent(KeyEvent evt) {
    if(keyListener != null) 
      switch(evt.getID()) {
        case KeyEvent.KEY_PRESSED:
	  keyListener.keyPressed(evt); break;
        case KeyEvent.KEY_RELEASED:
	  keyListener.keyReleased(evt); break;
        case KeyEvent.KEY_TYPED:
	  keyListener.keyTyped(evt); break;
      }
    // consume TAB keys if they originate from our component
    if(evt.getKeyCode() == KeyEvent.VK_TAB && evt.getSource() == this)
      evt.consume();
    super.processKeyEvent(evt);
  }

  FocusListener focusListener;

  public void addFocusListener(FocusListener listener) {
    focusListener = AWTEventMulticaster.add(focusListener, listener);
  }

  public void removeFocusListener(FocusListener listener) {
    focusListener = AWTEventMulticaster.remove(focusListener, listener);
  }

  public void processFocusEvent(FocusEvent evt) {
    if(focusListener != null)
      switch(evt.getID()) {
        case FocusEvent.FOCUS_GAINED:
	  focusListener.focusGained(evt); break;
	case FocusEvent.FOCUS_LOST:
	  focusListener.focusLost(evt); break;
      }
    super.processFocusEvent(evt);
  }
}
