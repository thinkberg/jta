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

import java.awt.Graphics;
import java.awt.Canvas;
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

import java.awt.Graphics;
import java.awt.print.Printable;
import java.awt.print.PrinterJob;
import java.awt.print.PrinterException;
import java.awt.print.PageFormat;

import java.awt.event.AdjustmentListener;
import java.awt.event.AdjustmentEvent;

/**
 * Video Display Unit emulation. This class implements all necessary
 * features of a character display unit, but not the actual terminal emulation.
 * It can be used as the base for terminal emulations of any kind.
 * <P>
 * <B>Maintainer:</B> Matthias L. Jugel
 *
 * @version $Id$
 * @author  Matthias L. Jugel, Marcus Meiﬂner
 */
public class VDU extends Canvas implements Printable {
  /** The current version id tag */
  public final static String ID = "$Id$";

  /** Enable debug messages. */
  public final static int debug = 0;
  
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
  private Point selectBegin, selectEnd;          /* selection coordinates */

  private Scrollbar scrollBar;
  private SoftFont  sf = new SoftFont();

  private boolean screenLocked = false;      /* screen needs to be locked */
                                             /* because of paint requests */
                                             /*   during other operations */
  private boolean update[];        /* contains the lines that need update */
  
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
  private static int COLOR_FG_BOLD = 3;
  private static int COLOR_BG_STD  = 0;
  private final static int COLOR         = 0x7f8;
  private final static int COLOR_FG      = 0x78;
  private final static int COLOR_BG      = 0x780;

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
    // set the normal font to use
    setFont(font);
    // set the standard resize strategy
    setResizeStrategy(RESIZE_FONT);
    // set the display screen size
    setScreenSize(width, height);

    setForeground(Color.white);
    setBackground(Color.black);

    selectBegin = new Point(0,0);
    selectEnd = new Point(0,0); 


    addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        System.err.println(evt);
	markLine(0, 24);
        if(evt.isControlDown()) {
          PrinterJob printJob = PrinterJob.getPrinterJob();
          printJob.setPrintable(VDU.this);
          if(printJob.printDialog()) {
            try {
              printJob.print();
            } catch (Exception ex) {
              ex.printStackTrace();
            }
          }
        }
      }
    });
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
    return charArray[l][c];
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
    return charAttributes[l][c];
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
    screenLocked = true;

    l = checkBounds(l, 0, size.height - 1);

    char cbuf[][] = null;
    int abuf[][] = null;
    int offset = 0;
    int oldBase = screenBase;
    int top = (l < topMargin ? 
               0 : (l > bottomMargin ?
                    (bottomMargin + 1 < size.height ?
                     bottomMargin + 1 : size.height - 1) : topMargin));
    int bottom = (l > bottomMargin ?
                  size.height - 1 : (l < topMargin ? 
                                     (topMargin > 0 ?
                                      topMargin - 1 : 0) : bottomMargin));
    
    
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


    screenLocked = false;
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
    cursorX = (c < size.width ? c : size.width);
    cursorY = (l < size.height ? l : size.height);
    markLine(l, 1);
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
    screenLocked = true;

    if(amount < size.height) amount = size.height;
    if(amount < maxBufSize) {
      char cbuf[][] = new char[amount][size.width];
      int abuf[][] = new int[amount][size.width];
      if(charArray != null)
        System.arraycopy(charArray, bufSize - amount, cbuf, 0, amount);
      if(charAttributes != null)
        System.arraycopy(charAttributes, bufSize - amount, abuf, 0, amount);
      charArray = cbuf;
      charAttributes = abuf;
    }
    maxBufSize = amount;
 
    screenLocked = false;

    repaint();
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
    repaint();
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

    screenLocked = true;
    
    // super.update(getGraphics());
    
    if(height > maxBufSize) 
      maxBufSize = height;

    if(height > bufSize) {
      bufSize = height;
      screenBase = 0;
      windowBase = 0;
    }

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
    for(int i = 0; i <= height; i++) update[i] = true;
    screenLocked = false;
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
    for(int i = 0; i < n && l + i < size.height; i++) 
      update[l + i + 1] = true;
  }
  
  /**
   * Redraw marked lines.
   * @see #markLine
   */
  public void redraw() {
    update[0] = true;
    repaint();
  }

  /**
   * Update the display. to reduce flashing we have overridden this method.
   */
  public void update(Graphics g) {
    paint(g);
  }
  
  /**
   * Paint the current screen. All painting is done here. Only lines that have
   * changed will be redrawn!
   */
  public synchronized void paint(Graphics g) {
    if(screenLocked) return;
    int xoffset = (super.getSize().width - size.width * charWidth) / 2;
    int yoffset = (super.getSize().height - size.height * charHeight) / 2;

    Color fg = color[COLOR_FG_STD];
    Color bg = color[COLOR_BG_STD];

    g.setFont(normalFont);

    for(int l = 0; l < size.height; l++) {
      if(update[0] && !update[l + 1]) continue;
      update[l + 1] = false;
      for(int c = 0; c < size.width; c++) {
        int addr = 0;
        int currAttr = charAttributes[windowBase + l][c];

        fg = getForeground();
        bg = getBackground();
        // fg = color[COLOR_FG_STD];
    	// bg = color[COLOR_BG_STD];

	// Special handling of BOLD for terminals used on 5ESS 
        if(((currAttr & BOLD) != 0)   &&
	   ((currAttr & COLOR_FG) == 0) &&
	   ((currAttr & COLOR_BG) == 0))
	  fg = color[COLOR_FG_BOLD];

        if ((currAttr & COLOR_FG) != 0)
          fg = color[((currAttr & COLOR_FG) >> 3)-1];
        if ((currAttr & COLOR_BG) != 0)
          bg = color[((currAttr & COLOR_BG) >> 7)-1];

        if((currAttr & BOLD) != 0)
          if(fg.equals(Color.black))
            fg = Color.gray;
          else {
	    fg = fg.brighter();
	    bg = bg.brighter();
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
              !sf.inSoftFont(charArray[windowBase + l ][c+addr])
        ) {
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
    }

    if(screenBase + cursorY >= windowBase && 
       screenBase + cursorY < windowBase + size.height) {
      g.setColor(color[COLOR_FG_STD]);
      g.setXORMode(color[COLOR_BG_STD]);
      g.fillRect( cursorX * charWidth + xoffset, 
                 (cursorY + screenBase - windowBase) * charHeight + yoffset,
                 charWidth, charHeight);
      g.setPaintMode();
    }

    if(windowBase <= selectBegin.y || windowBase <= selectEnd.y) {
      int beginLine = selectBegin.y - windowBase;
      int endLine = selectEnd.y - selectBegin.y;
      if(beginLine < 0) {
        endLine += beginLine;
        beginLine = 0;
      }
      if(endLine > size.height) endLine = size.height - beginLine;
       
      g.setXORMode(color[COLOR_BG_STD]);
      g.fillRect(selectBegin.x * charWidth + xoffset,
                 beginLine * charHeight + yoffset,
                 (endLine == 0 ? (selectEnd.x - selectBegin.x) : 
                  (size.width - selectBegin.x)) 
                 * charWidth,
                 charHeight);
      if(endLine > 1)
        g.fillRect(0 + xoffset, 
                   (beginLine + 1) * charHeight + yoffset, 
                   size.width * charWidth, 
                   (endLine - 1) * charHeight);
      if(endLine > 0)
        g.fillRect(0 + xoffset, 
                   (beginLine + endLine) * charHeight + yoffset,
                   selectEnd.x * charWidth, 
                   charHeight);
      g.setPaintMode();
    }

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

  public int print(Graphics g, PageFormat pf, int pi) throws PrinterException {
    if(pi >= 1) {
      return Printable.NO_SUCH_PAGE;
    }
    paint(g);
    return Printable.PAGE_EXISTS;
  }

    // draw cursor

  private int checkBounds(int value, int lower, int upper) {
    if(value < lower) return lower;
    if(value > upper) return upper;
    return value;
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

    // now set the bounds for the whole component accordingly
    // super.setBounds(x, y, w + xborder, h + yborder);
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

  /**
   * Handle mouse events for copy & paste
   * @param evt the event that occured
   * @return boolean true if action was taken
   * @see java.awt.Event
   */
 /*
  public void mouseDown(MouseEvent evt) {
    // handle scrollbar events
    if(evt != null && evt.arg != null) {
      int val = ((Integer)evt.arg).intValue();
      setWindowBase(val);
      return true;
    }

    if(evt.id == Event.MOUSE_DOWN || evt.id == Event.MOUSE_UP ||
       evt.id == Event.MOUSE_DRAG) {
      int xoffset = (super.size().width - size.width * charWidth) / 2;
      int yoffset = (super.size().height - size.height * charHeight) / 2;
      switch(evt.id) {
      case Event.MOUSE_DOWN:
        selectBegin.x = (evt.x - xoffset) / charWidth;
        selectBegin.y = (evt.y - yoffset) / charHeight + windowBase;
        selectEnd.x = selectBegin.x;
        selectEnd.y = selectBegin.y;
        break;
      case Event.MOUSE_UP:
      case Event.MOUSE_DRAG:
        int x = (evt.x - xoffset) / charWidth;
        int y = (evt.y - yoffset) / charHeight + windowBase;
        int oldx = selectEnd.x, oldy = selectEnd.y;

        if((x < selectBegin.x && y < selectBegin.y) &&
           (x < selectEnd.x && y < selectEnd.y)) {
          selectBegin.x = x;
          selectBegin.y = y;
        } else {
          selectEnd.x = x;
          selectEnd.y = y;
        }

        if(evt.id == Event.MOUSE_UP) {
          if(selectBegin.x == selectEnd.x &&
             selectBegin.y == selectEnd.y) {
            repaint();
            return true;
          }
          String tmp = "";
	  // fix end.x and end.y, they can get over the border
	  if (selectEnd.x < 0) selectEnd.x = 0;
	  if (selectEnd.y < 0) selectEnd.y = 0;
	  if (selectEnd.y >= charArray.length) {
		selectEnd.y = charArray.length-1;
	  }
	  if (selectEnd.x >= charArray[0].length) {
		selectEnd.x = charArray[0].length-1;
	  }
          for(int l = selectBegin.y; l <= selectEnd.y; l++)
            if(l == selectBegin.y) 
              tmp = (new String(charArray[l])).substring(selectBegin.x) + "\n";
            else if(l == selectEnd.y) 
              tmp += (new String(charArray[l])).substring(0, selectEnd.x);
            else tmp += new String(charArray[l]) + "\n";

	    // for jdk-1.1
	  //   String s=(String) ((StringSelection)this.getToolkit().
	// 		       getSystemClipboard().
	// 		       getContents(this)).
	    //   getTransferData(DataFlavor.stringFlavor);
	    // System.out.println(s);
	    //
          repaint();
        } else
          if(oldx != x || oldy != y) repaint();
        break;
      }
      return true;
    }
    return false;
  }
  */
}
