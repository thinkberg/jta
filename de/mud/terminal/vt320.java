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

import java.awt.Font;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import java.util.Properties;

/**
 * Implementation of a VT terminal emulation plus ANSI compatible.
 * <P>
 * <B>Maintainer:</B> Marcus Meiﬂner
 *
 * @version $Id$
 * @author  Matthias L. Jugel, Marcus Meiﬂner
 */
public abstract class vt320 extends VDU implements KeyListener {
  /** The current version id tag.<P>
   * $Id$
   */
  public final static String ID = "$Id$";

  /** the debug level */
  private final static int debug = 0;

  /**
   * Write an answer back to the remote host. This is needed to be able to
   * send terminal answers requests like status and type information.
   * @param b the array of bytes to be sent
   */
  protected abstract void write(byte[] b);

  /**
   * Put string at current cursor position. Moves cursor
   * according to the String. Does NOT wrap.
   * @param s the string
   */
  public void putString(String s) {
    int  i,len=s.length();

    if(len > 0) {
      markLine(R,1);
      for (i=0;i<len;i++)
        putChar(s.charAt(i),false);
      setCursorPosition(C, R);
      redraw();
    }
  }

  /**
   * Create a new vt320 terminal and intialize it with useful settings.
   * @param width the width of the character screen
   * @param height the amount of rows on screen
   * @param font the font to be used for rendering characters
   */
  public vt320(int width, int height, Font font) {
    super(width, height, font);

    setVMS(false);
    setIBMCharset(false);
    setTerminalID("vt320");
    setBufferSize(100);
    setBorder(2, false);

    int nw = getColumns();
    if (nw<132) nw=132; //catch possible later 132/80 resizes
    Tabs = new byte[nw];
    for (int i=0;i<nw;i+=8) {
      Tabs[i]=1;
    }

    /* top row of numpad */
    PF1  = "\u001bOP";
    PF2  = "\u001bOQ";
    PF3  = "\u001bOR";
    PF4  = "\u001bOS";

    /* the 3x2 keyblock on PC keyboards */
    Insert = new String[4];
    Remove = new String[4];
    KeyHome = new String[4];
    KeyEnd = new String[4];
    NextScn = new String[4];
    PrevScn = new String[4];
    Escape = new String[4];
    BackSpace = new String[4];
    Insert[0]  = Insert[1]  = Insert[2]  = Insert[3]  = "\u001b[2~";
    Remove[0]  = Remove[1]  = Remove[2]  = Remove[3]  = "\u001b[3~";
    PrevScn[0] = PrevScn[1] = PrevScn[2] = PrevScn[3] = "\u001b[5~";
    NextScn[0] = NextScn[1] = NextScn[2] = NextScn[3] = "\u001b[6~";
    KeyHome[0] = KeyHome[1] = KeyHome[2] = KeyHome[3] = "\u001b[H";
    KeyEnd[0]  = KeyEnd[1]  = KeyEnd[2]  = KeyEnd[3]  = "\u001b[F";
    Escape[0]  = Escape[1]  = Escape[2]  = Escape[3]  = "\u001b";
    BackSpace[0]  = BackSpace[1]  = BackSpace[2]  = BackSpace[3]  = "\b";

    /* some more VT100 keys */
    Find   = "\u001b[1~";
    Select = "\u001b[4~";
    Help   = "\u001b[28~";
    Do     = "\u001b[29~";

    FunctionKey = new String[21];
    FunctionKey[0]= "";
    FunctionKey[1]= PF1;
    FunctionKey[2]= PF2;
    FunctionKey[3]= PF3;
    FunctionKey[4]= PF4;
    /* following are defined differently for vt220 / vt132 ... */
    FunctionKey[5]= "\u001b[15~";
    FunctionKey[6]= "\u001b[17~";
    FunctionKey[7]= "\u001b[18~";
    FunctionKey[8]= "\u001b[19~";
    FunctionKey[9]= "\u001b[20~";
    FunctionKey[10]= "\u001b[21~";
    FunctionKey[11]= "\u001b[23~";
    FunctionKey[12]= "\u001b[24~";
    FunctionKey[13]= "\u001b[25~";
    FunctionKey[14]= "\u001b[26~";
    FunctionKey[15]= Help;
    FunctionKey[16]= Do;
    FunctionKey[17]= "\u001b[31~";
    FunctionKey[18]= "\u001b[32~";
    FunctionKey[19]= "\u001b[33~";
    FunctionKey[20]= "\u001b[34~";

    FunctionKeyShift = new String[21];
    FunctionKeyAlt = new String[21];
    FunctionKeyCtrl = new String[21];

    for (int i=0;i<20;i++) {
      FunctionKeyShift[i]="";
      FunctionKeyAlt[i]="";
      FunctionKeyCtrl[i]="";
    }
    FunctionKeyShift[15] = Find;
    FunctionKeyShift[16] = Select;


    KeyTab     = "\u0009";
    KeyBacktab = "\u001bOP\u0009";
    KeyUp      = new String[4];
    KeyUp[0]   = "\u001b[A";
    KeyDown    = new String[4];
    KeyDown[0] = "\u001b[B";
    KeyRight   = new String[4];
    KeyRight[0]= "\u001b[C";
    KeyLeft    = new String[4];
    KeyLeft[0] = "\u001b[D";
    Numpad = new String[10];
    Numpad[0]  = "\u001bOp";
    Numpad[1]  = "\u001bOq";
    Numpad[2]  = "\u001bOr";
    Numpad[3]  = "\u001bOs";
    Numpad[4]  = "\u001bOt";
    Numpad[5]  = "\u001bOu";
    Numpad[6]  = "\u001bOv";
    Numpad[7]  = "\u001bOw";
    Numpad[8]  = "\u001bOx";
    Numpad[9]  = "\u001bOy";
    KPMinus  = "\u001bOm";
    KPComma  = "\u001bOl";
    KPPeriod  = "\u001bOn";
    KPEnter  = "\u001bOM";

    /* ... */

    addKeyListener(this);
    /* I don't think we want that here ... */
    addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent evt) {
        requestFocus();
      }
    });
      
  }

  /**
   * Create a new terminal emulation with specific width and height.
   * @param width the width of the character screen
   * @param height the amount of rows on screen
   */
  public vt320(int width, int height) {
    this(width, height, new Font("Monospaced", Font.PLAIN, 10));
  }

  /**
   * Create a new terminal emulation with a specific font.
   * @param font the font to be used for rendering characters
   */
  public vt320(Font font) {
    this(80, 24, font);
  }

  /**
   * Create a new terminal emulation with a specific font.
   */
  public vt320() {
    this(80, 24, new Font("Monospaced", Font.PLAIN, 10));
  }

  /** we should do localecho (passed from other modules). false is default */
  private boolean localecho = false;

  /**
   * Enable or disable the local echo property of the terminal.
   * @param echo true if the terminal should echo locally
   */
  public void setLocalEcho(boolean echo) { localecho = echo; }

  /**
   * Enable the VMS mode of the terminal to handle some things differently
   * for VMS hosts.
   * @param vms true for vms mode, false for normal mode
   */
  public void setVMS(boolean vms) {
    this.vms = vms;
  }

  /**
   * Enable the usage of the IBM character set used by some BBS's. Special
   * graphical character are available in this mode.
   * @param ibm true to use the ibm character set
   */
  public void setIBMCharset(boolean ibm) {
    useibmcharset = ibm;
  }

  /**
   * Override the standard key codes used by the terminal emulation.
   * @param codes a properties object containing key code definitions
   */
  public void setKeyCodes(Properties codes) {
    String res, prefixes[] = {"","S","C","A"};
    int i;
    
    for (i=0;i<10;i++) {
	res = codes.getProperty("NUMPAD"+i);
	if(res!=null) Numpad[i] = unEscape(res);
    }
    for (i=1;i<20;i++) {
	res = codes.getProperty("F"+i);
	if(res!=null) FunctionKey[i] = unEscape(res);
	res = codes.getProperty("SF"+i);
	if(res!=null) FunctionKeyShift[i] = unEscape(res);
	res = codes.getProperty("CF"+i);
	if(res!=null) FunctionKeyCtrl[i] = unEscape(res);
	res = codes.getProperty("AF"+i);
	if(res!=null) FunctionKeyAlt[i] = unEscape(res);
    }
    for (i=0;i<4;i++) {
      res = codes.getProperty(prefixes[i]+"PGUP");
      if (res!=null) PrevScn[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"PGDOWN");
      if (res!=null) NextScn[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"END");
      if (res!=null) KeyEnd[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"HOME");
      if (res!=null) KeyHome[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"INSERT");
      if (res!=null) Insert[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"REMOVE");
      if (res!=null) Remove[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"UP");
      if (res!=null) KeyUp[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"DOWN");
      if (res!=null) KeyDown[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"LEFT");
      if (res!=null) KeyLeft[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"RIGHT");
      if (res!=null) KeyRight[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"ESCAPE");
      if (res!=null) Escape[i]=unEscape(res);
      res = codes.getProperty(prefixes[i]+"BACKSPACE");
      if (res!=null) BackSpace[i]=unEscape(res);
    }

  }

  /**
   * Set the terminal id used to identify this terminal.
   * @param terminalID the id string
   */
  public void setTerminalID(String terminalID) { 
    this.terminalID = terminalID; 
  }

  /**
   * Get the terminal id used to identify this terminal.
   * @param terminalID the id string
   */
  public String getTerminalID() { 
    return terminalID; 
  }

  /**
   * A small conveniance method thar converts the string to a byte array
   * for sending.
   * @param s the string to be sent
   */
  private boolean write(String s,boolean doecho) {
    write(s.getBytes());
    if (doecho)
    	putString(s);
    return true;
  }

  private boolean write(String s) { return write(s,localecho); }

  // ===================================================================
  // the actual terminal emulation code comes here:
  // ===================================================================

  // due to a bug with Windows we need a keypress cache
  private int pressedKey = ' ';
  // private long pressedWhen = ' ';

  private String terminalID = "vt320";

  // X - COLUMNS, Y - ROWS
  int  R,C;
  int  Sc,Sr,Sa;
  int  attributes  = 0;
  int  insertmode  = 0;
  int  statusmode = 0;
  int  vt52mode  = 0;
  int  normalcursor  = 0;
  boolean moveoutsidemargins = true;
  boolean sendcrlf = true;
  boolean capslock = false;
  boolean numlock = false;

  private boolean  useibmcharset = false;

  private  static  int  lastwaslf = 0;
  private static  int i;
  private final static char ESC = 27;
  private final static char IND = 132;
  private final static char NEL = 133;
  private final static char RI  = 141;
  private final static char HTS = 136;
  private final static char DCS = 144;
  private final static char CSI = 155;
  private final static char OSC = 157;
  private final static int TSTATE_DATA  = 0;
  private final static int TSTATE_ESC  = 1; /* ESC */
  private final static int TSTATE_CSI  = 2; /* ESC [ */
  private final static int TSTATE_DCS  = 3; /* ESC P */
  private final static int TSTATE_DCEQ  = 4; /* ESC [? */
  private final static int TSTATE_ESCSQUARE= 5; /* ESC # */
  private final static int TSTATE_OSC= 6;       /* ESC ] */
  private final static int TSTATE_SETG0= 7;     /* ESC (? */
  private final static int TSTATE_SETG1= 8;     /* ESC )? */
  private final static int TSTATE_SETG2= 9;     /* ESC *? */
  private final static int TSTATE_SETG3= 10;    /* ESC +? */
  private final static int TSTATE_CSI_DOLLAR  = 11; /* ESC [ Pn $ */

  /* The graphics charsets
   * B - default ASCII
   * A - default UK
   * 0 - DEC SPECIAL
   * < - User defined
   * ....
   */
  private static char gx[] = {
    'B',      // g0
    '0',      // g1
    'A',      // g2
    '<',      // g3
  };
  private static char gr = 1;  // default GR to G1
  private static char gl = 0;  // default GL to G0

  // array to store DEC Special -> Unicode mapping
  //  Unicode   DEC  Unicode name    (DEC name)
  private static char DECSPECIAL[] = {
    '\u0040', //5f blank
    '\u2666', //60 black diamond
    '\u2592', //61 grey square
    '\u2409', //62 Horizontal tab  (ht) pict. for control
    '\u240c', //63 Form Feed       (ff) pict. for control
    '\u240d', //64 Carriage Return (cr) pict. for control
    '\u240a', //65 Line Feed       (lf) pict. for control
    '\u00ba', //66 Masculine ordinal indicator
    '\u00b1', //67 Plus or minus sign
    '\u2424', //68 New Line        (nl) pict. for control
    '\u240b', //69 Vertical Tab    (vt) pict. for control
    '\u2518', //6a Forms light up   and left
    '\u2510', //6b Forms light down and left
    '\u250c', //6c Forms light down and right
    '\u2514', //6d Forms light up   and right
    '\u253c', //6e Forms light vertical and horizontal
    '\u2594', //6f Upper 1/8 block                        (Scan 1)
    '\u2580', //70 Upper 1/2 block                        (Scan 3)
    '\u2500', //71 Forms light horizontal or ?em dash?    (Scan 5)
    '\u25ac', //72 \u25ac black rect. or \u2582 lower 1/4 (Scan 7)
    '\u005f', //73 \u005f underscore  or \u2581 lower 1/8 (Scan 9)
    '\u251c', //74 Forms light vertical and right
    '\u2524', //75 Forms light vertical and left
    '\u2534', //76 Forms light up   and horizontal
    '\u252c', //77 Forms light down and horizontal
    '\u2502', //78 vertical bar
    '\u2264', //79 less than or equal
    '\u2265', //7a greater than or equal
    '\u00b6', //7b paragraph
    '\u2260', //7c not equal
    '\u00a3', //7d Pound Sign (british)
    '\u00b7'  //7e Middle Dot
  };

  /** Strings to send on function key pressing */
  private String Numpad[];
  private String FunctionKey[];
  private String FunctionKeyShift[];
  private String FunctionKeyCtrl[];
  private String FunctionKeyAlt[];
  private String KeyUp[],KeyDown[],KeyLeft[],KeyRight[];
  private String KeyTab,KeyBacktab;
  private String KPMinus, KPComma, KPPeriod,KPEnter;
  private String PF1, PF2, PF3, PF4;
  private String Help, Do, Find, Select;

  private String KeyHome[], KeyEnd[], Insert[], Remove[], PrevScn[], NextScn[], Escape[], BackSpace[];

  private String osc,dcs;  /* to memorize OSC & DCS control sequence */

  /** vt320 state variable (internal) */
  private int term_state = TSTATE_DATA;
  /** in vms mode, set by Terminal.VMS property */
  private boolean vms = false;
  /** Tabulators */
  private byte[]  Tabs;
  /** The list of integers as used by CSI */
  private int[]  DCEvars = new int [10];
  private int  DCEvar;

  /**
   * Replace escape code characters (backslash + identifier) with their
   * respective codes.
   * @param tmp the string to be parsed
   * @return a unescaped string
   */
  static String unEscape(String tmp) {
    int idx = 0, oldidx = 0;
    String cmd;
    // System.err.println("unescape("+tmp+")");
    cmd = "";
    while((idx = tmp.indexOf('\\', oldidx)) >= 0 &&
	  ++idx <= tmp.length()) {
      cmd += tmp.substring(oldidx, idx-1);
      if(idx == tmp.length()) return cmd;
      switch(tmp.charAt(idx)) {
      case 'b': cmd += "\b"; break;
      case 'e': cmd += "\u001b"; break;
      case 'n': cmd += "\n"; break;
      case 'r': cmd += "\r"; break;
      case 't': cmd += "\t"; break;
      case 'v': cmd += "\u000b"; break;
      case 'a': cmd += "\u0012"; break;
      default : 
	if ( (tmp.charAt(idx)>='0') && (tmp.charAt(idx)<='9')) {
	  for (i = idx;i<tmp.length();i++)
	    if ( (tmp.charAt(i)<'0') || (tmp.charAt(i)>'9'))
	      break;
	  cmd += (char)Integer.parseInt(tmp.substring(idx, i));
	  idx = i-1;
	} else
	  cmd += tmp.substring(idx, ++idx);break;
      }
      oldidx = ++idx;
    }
    if(oldidx <= tmp.length()) cmd += tmp.substring(oldidx);
    return cmd;
  }

  /**
   * main keytyping event handler...
   */
  public void keyTyped(KeyEvent evt) {
    boolean control = evt.isControlDown();
    boolean shift = evt.isShiftDown();
    boolean alt = evt.isAltDown();

    int keyCode = evt.getKeyCode();
    char keyChar = evt.getKeyChar();

    if (keyChar == '\u001b' || keyChar == '\b') return;

    /* DISABLED: this is non-portable :(
    if (keyChar == 0x7f) {
      write(Remove[0],false);
      return;
    }
    */
    if(shift && (keyChar == '\t')) {
      write(KeyBacktab,false);
      return;
    }
    if (alt) {
      write(""+((char)(keyChar|0x80)));
      return;
    }
    if(!(keyChar == '\r' || keyChar == '\n') || control)
      write(""+keyChar);
  }

  /**
   * Not used.
   */
  public void keyReleased(KeyEvent evt) {
    // nothing to to, however maybe we should use it?
  }

  /**
   * Handle events for the terminal. Only accept events for the scroll
   * bar. Any other events have to be propagated to the parent.
   * @param evt the event
   */
  public void keyPressed(KeyEvent evt) {
    boolean control = evt.isControlDown();
    boolean shift = evt.isShiftDown();
    boolean alt = evt.isAltDown();

    int keyCode = evt.getKeyCode();
    char keyChar = evt.getKeyChar();


    /*
    if(pressedKey == KeyEvent.VK_ENTER && 
        (keyCode == KeyEvent.VK_ENTER || keyChar == 10)
        && evt.getWhen() - pressedWhen < 50) 
      return;

    pressedWhen = evt.getWhen();
    */

    if(keyCode == KeyEvent.VK_ENTER && !control) {
      write("\n",false);
      if (localecho) putString("\r\n"); // bad hack
    } 
    
    // FIXME: on german PC keyboards you have to use Alt-Ctrl-q to get an @,
    // so we can't just use it here... will probably break some other VMS
    // codes.  -Marcus
    // if(((!vms && keyChar == '2') || keyChar == '@' || keyChar == ' ') 
    //    && control)
    if (((!vms && keyChar == '2') || keyChar == ' ') && control)
      write("" + (char)0);
    
    if(vms) {
      if (keyChar == 8) {
	if(shift && !control)
	  write("" + (char)10);    //  VMS shift deletes word back
	else if(control && !shift)
	  write("" + (char)24);    //  VMS control deletes line back
	else
	  write("" + (char)127);   //  VMS other is delete
      } else if(keyChar == 127 && !control) {
	if (shift) 
	  write(Insert[0]);        //  VMS shift delete = insert
	else
	  write(Remove[0]);        //  VMS delete = remove
      } else if(control)
	switch(keyChar) {
	case '0': write(Numpad[0]); return;
	case '1': write(Numpad[1]); return;
	case '2': write(Numpad[2]); return;
	case '3': write(Numpad[3]); return;
	case '4': write(Numpad[4]); return;
	case '5': write(Numpad[5]); return;
	case '6': write(Numpad[6]); return;
	case '7': write(Numpad[7]); return;
	case '8': write(Numpad[8]); return;
	case '9': write(Numpad[9]); return;
	case '.': write(KPPeriod); return;
	case '-':
	case 31:  write(KPMinus); return;
	case '+': write(KPComma); return;
	case 10:  write(KPEnter); return;
	case '/': write(PF2); return;
	case '*': write(PF3); return;
	}
	if (shift && keyChar < 32)
	  write(PF1+(char)(keyChar + 64));
        return;
    }

    if (debug>2) System.out.println("vt320: keyPressed "+evt+"\""+keyChar+"\"");

    String fmap[];
    int    xind;
    xind = 0;
    fmap = FunctionKey;
    if(shift) { fmap = FunctionKeyShift; xind=1; }
    if(control) { fmap = FunctionKeyCtrl; xind=2; }
    if(alt) { fmap = FunctionKeyAlt; xind=3; }

    if(evt.isActionKey()) switch(keyCode) {
      case KeyEvent.VK_NUMPAD0: write(Numpad[0],false); break;
      case KeyEvent.VK_NUMPAD1: write(Numpad[1],false); break;
      case KeyEvent.VK_NUMPAD2: write(Numpad[2],false); break;
      case KeyEvent.VK_NUMPAD3: write(Numpad[3],false); break;
      case KeyEvent.VK_NUMPAD4: write(Numpad[4],false); break;
      case KeyEvent.VK_NUMPAD5: write(Numpad[5],false); break;
      case KeyEvent.VK_NUMPAD6: write(Numpad[6],false); break;
      case KeyEvent.VK_NUMPAD7: write(Numpad[7],false); break;
      case KeyEvent.VK_NUMPAD8: write(Numpad[8],false); break;
      case KeyEvent.VK_NUMPAD9: write(Numpad[9],false); break;
    }

    switch (keyCode) {
      case KeyEvent.VK_F1: write(fmap[1],false); break;
      case KeyEvent.VK_F2: write(fmap[2],false); break;
      case KeyEvent.VK_F3: write(fmap[3],false); break;
      case KeyEvent.VK_F4: write(fmap[4],false); break;
      case KeyEvent.VK_F5: write(fmap[5],false); break;
      case KeyEvent.VK_F6: write(fmap[6],false); break;
      case KeyEvent.VK_F7: write(fmap[7],false); break;
      case KeyEvent.VK_F8: write(fmap[8],false); break;
      case KeyEvent.VK_F9: write(fmap[9],false); break;
      case KeyEvent.VK_F10: write(fmap[10],false); break;
      case KeyEvent.VK_F11: write(fmap[11],false); break;
      case KeyEvent.VK_F12: write(fmap[12],false); break;
      case KeyEvent.VK_UP: write(KeyUp[xind],false); break;
      case KeyEvent.VK_DOWN: write(KeyDown[xind],false); break;
      case KeyEvent.VK_LEFT: write(KeyLeft[xind],false); break;
      case KeyEvent.VK_RIGHT: write(KeyRight[xind],false); break;
      case KeyEvent.VK_PAGE_DOWN: write(NextScn[xind],false); break;
      case KeyEvent.VK_PAGE_UP: write(PrevScn[xind],false); break;
      case KeyEvent.VK_INSERT: write(Insert[xind],false); break;
      case KeyEvent.VK_DELETE: write(Remove[xind],false); break;
      case KeyEvent.VK_ESCAPE: write(Escape[xind],false); break;
      case KeyEvent.VK_BACK_SPACE: write(BackSpace[xind],false); break;
      case KeyEvent.VK_HOME:
        if(vms) 
	  write("" + (char)8,false);
        else
	  write(KeyHome[xind],false);
	break;
      case KeyEvent.VK_END:
        if(vms)
	  write("" + (char)5,false);
        else
	  write(KeyEnd[xind],false);
	break;
      case KeyEvent.VK_NUM_LOCK:
        if(vms && control)
	  if(pressedKey != keyCode) {
	    pressedKey = keyCode;
	    write(PF1,false);
	  } else
	    //  Here, we eat a second numlock since that returns numlock state
	    pressedKey = ' ';
	if(!control)
	  numlock = !numlock;
	break;
      case KeyEvent.VK_CAPS_LOCK:
      	capslock = !capslock;
        break;
      default:
	if(debug > 2)
	  System.out.println("vt320: unknown event: "+evt);
	break;
    }
  }

  private void handle_dcs(String dcs) {
    System.out.println("DCS: "+dcs);
  }
  private void handle_osc(String osc) {
    System.out.println("OSC: "+osc);
  }

  private final static char unimap[] = {
    //#
    //#    Name:     cp437_DOSLatinUS to Unicode table
    //#    Unicode version: 1.1
    //#    Table version: 1.1
    //#    Table format:  Format A
    //#    Date:          03/31/95
    //#    Authors:       Michel Suignard <michelsu@microsoft.com>
    //#                   Lori Hoerth <lorih@microsoft.com>
    //#    General notes: none
    //#
    //#    Format: Three tab-separated columns
    //#        Column #1 is the cp1255_WinHebrew code (in hex)
    //#        Column #2 is the Unicode (in hex as 0xXXXX)
    //#        Column #3 is the Unicode name (follows a comment sign, '#')
    //#
    //#    The entries are in cp437_DOSLatinUS order
    //#

    0x0000,// #NULL
    0x0001,// #START OF HEADING
    0x0002,// #START OF TEXT
    0x0003,// #END OF TEXT
    0x0004,// #END OF TRANSMISSION
    0x0005,// #ENQUIRY
    0x0006,// #ACKNOWLEDGE
    0x0007,// #BELL
    0x0008,// #BACKSPACE
    0x0009,// #HORIZONTAL TABULATION
    0x000a,// #LINE FEED
    0x000b,// #VERTICAL TABULATION
    0x000c,// #FORM FEED
    0x000d,// #CARRIAGE RETURN
    0x000e,// #SHIFT OUT
    0x000f,// #SHIFT IN
    0x0010,// #DATA LINK ESCAPE
    0x0011,// #DEVICE CONTROL ONE
    0x0012,// #DEVICE CONTROL TWO
    0x0013,// #DEVICE CONTROL THREE
    0x0014,// #DEVICE CONTROL FOUR
    0x0015,// #NEGATIVE ACKNOWLEDGE
    0x0016,// #SYNCHRONOUS IDLE
    0x0017,// #END OF TRANSMISSION BLOCK
    0x0018,// #CANCEL
    0x0019,// #END OF MEDIUM
    0x001a,// #SUBSTITUTE
    0x001b,// #ESCAPE
    0x001c,// #FILE SEPARATOR
    0x001d,// #GROUP SEPARATOR
    0x001e,// #RECORD SEPARATOR
    0x001f,// #UNIT SEPARATOR
    0x0020,// #SPACE
    0x0021,// #EXCLAMATION MARK
    0x0022,// #QUOTATION MARK
    0x0023,// #NUMBER SIGN
    0x0024,// #DOLLAR SIGN
    0x0025,// #PERCENT SIGN
    0x0026,// #AMPERSAND
    0x0027,// #APOSTROPHE
    0x0028,// #LEFT PARENTHESIS
    0x0029,// #RIGHT PARENTHESIS
    0x002a,// #ASTERISK
    0x002b,// #PLUS SIGN
    0x002c,// #COMMA
    0x002d,// #HYPHEN-MINUS
    0x002e,// #FULL STOP
    0x002f,// #SOLIDUS
    0x0030,// #DIGIT ZERO
    0x0031,// #DIGIT ONE
    0x0032,// #DIGIT TWO
    0x0033,// #DIGIT THREE
    0x0034,// #DIGIT FOUR
    0x0035,// #DIGIT FIVE
    0x0036,// #DIGIT SIX
    0x0037,// #DIGIT SEVEN
    0x0038,// #DIGIT EIGHT
    0x0039,// #DIGIT NINE
    0x003a,// #COLON
    0x003b,// #SEMICOLON
    0x003c,// #LESS-THAN SIGN
    0x003d,// #EQUALS SIGN
    0x003e,// #GREATER-THAN SIGN
    0x003f,// #QUESTION MARK
    0x0040,// #COMMERCIAL AT
    0x0041,// #LATIN CAPITAL LETTER A
    0x0042,// #LATIN CAPITAL LETTER B
    0x0043,// #LATIN CAPITAL LETTER C
    0x0044,// #LATIN CAPITAL LETTER D
    0x0045,// #LATIN CAPITAL LETTER E
    0x0046,// #LATIN CAPITAL LETTER F
    0x0047,// #LATIN CAPITAL LETTER G
    0x0048,// #LATIN CAPITAL LETTER H
    0x0049,// #LATIN CAPITAL LETTER I
    0x004a,// #LATIN CAPITAL LETTER J
    0x004b,// #LATIN CAPITAL LETTER K
    0x004c,// #LATIN CAPITAL LETTER L
    0x004d,// #LATIN CAPITAL LETTER M
    0x004e,// #LATIN CAPITAL LETTER N
    0x004f,// #LATIN CAPITAL LETTER O
    0x0050,// #LATIN CAPITAL LETTER P
    0x0051,// #LATIN CAPITAL LETTER Q
    0x0052,// #LATIN CAPITAL LETTER R
    0x0053,// #LATIN CAPITAL LETTER S
    0x0054,// #LATIN CAPITAL LETTER T
    0x0055,// #LATIN CAPITAL LETTER U
    0x0056,// #LATIN CAPITAL LETTER V
    0x0057,// #LATIN CAPITAL LETTER W
    0x0058,// #LATIN CAPITAL LETTER X
    0x0059,// #LATIN CAPITAL LETTER Y
    0x005a,// #LATIN CAPITAL LETTER Z
    0x005b,// #LEFT SQUARE BRACKET
    0x005c,// #REVERSE SOLIDUS
    0x005d,// #RIGHT SQUARE BRACKET
    0x005e,// #CIRCUMFLEX ACCENT
    0x005f,// #LOW LINE
    0x0060,// #GRAVE ACCENT
    0x0061,// #LATIN SMALL LETTER A
    0x0062,// #LATIN SMALL LETTER B
    0x0063,// #LATIN SMALL LETTER C
    0x0064,// #LATIN SMALL LETTER D
    0x0065,// #LATIN SMALL LETTER E
    0x0066,// #LATIN SMALL LETTER F
    0x0067,// #LATIN SMALL LETTER G
    0x0068,// #LATIN SMALL LETTER H
    0x0069,// #LATIN SMALL LETTER I
    0x006a,// #LATIN SMALL LETTER J
    0x006b,// #LATIN SMALL LETTER K
    0x006c,// #LATIN SMALL LETTER L
    0x006d,// #LATIN SMALL LETTER M
    0x006e,// #LATIN SMALL LETTER N
    0x006f,// #LATIN SMALL LETTER O
    0x0070,// #LATIN SMALL LETTER P
    0x0071,// #LATIN SMALL LETTER Q
    0x0072,// #LATIN SMALL LETTER R
    0x0073,// #LATIN SMALL LETTER S
    0x0074,// #LATIN SMALL LETTER T
    0x0075,// #LATIN SMALL LETTER U
    0x0076,// #LATIN SMALL LETTER V
    0x0077,// #LATIN SMALL LETTER W
    0x0078,// #LATIN SMALL LETTER X
    0x0079,// #LATIN SMALL LETTER Y
    0x007a,// #LATIN SMALL LETTER Z
    0x007b,// #LEFT CURLY BRACKET
    0x007c,// #VERTICAL LINE
    0x007d,// #RIGHT CURLY BRACKET
    0x007e,// #TILDE
    0x007f,// #DELETE
    0x00c7,// #LATIN CAPITAL LETTER C WITH CEDILLA
    0x00fc,// #LATIN SMALL LETTER U WITH DIAERESIS
    0x00e9,// #LATIN SMALL LETTER E WITH ACUTE
    0x00e2,// #LATIN SMALL LETTER A WITH CIRCUMFLEX
    0x00e4,// #LATIN SMALL LETTER A WITH DIAERESIS
    0x00e0,// #LATIN SMALL LETTER A WITH GRAVE
    0x00e5,// #LATIN SMALL LETTER A WITH RING ABOVE
    0x00e7,// #LATIN SMALL LETTER C WITH CEDILLA
    0x00ea,// #LATIN SMALL LETTER E WITH CIRCUMFLEX
    0x00eb,// #LATIN SMALL LETTER E WITH DIAERESIS
    0x00e8,// #LATIN SMALL LETTER E WITH GRAVE
    0x00ef,// #LATIN SMALL LETTER I WITH DIAERESIS
    0x00ee,// #LATIN SMALL LETTER I WITH CIRCUMFLEX
    0x00ec,// #LATIN SMALL LETTER I WITH GRAVE
    0x00c4,// #LATIN CAPITAL LETTER A WITH DIAERESIS
    0x00c5,// #LATIN CAPITAL LETTER A WITH RING ABOVE
    0x00c9,// #LATIN CAPITAL LETTER E WITH ACUTE
    0x00e6,// #LATIN SMALL LIGATURE AE
    0x00c6,// #LATIN CAPITAL LIGATURE AE
    0x00f4,// #LATIN SMALL LETTER O WITH CIRCUMFLEX
    0x00f6,// #LATIN SMALL LETTER O WITH DIAERESIS
    0x00f2,// #LATIN SMALL LETTER O WITH GRAVE
    0x00fb,// #LATIN SMALL LETTER U WITH CIRCUMFLEX
    0x00f9,// #LATIN SMALL LETTER U WITH GRAVE
    0x00ff,// #LATIN SMALL LETTER Y WITH DIAERESIS
    0x00d6,// #LATIN CAPITAL LETTER O WITH DIAERESIS
    0x00dc,// #LATIN CAPITAL LETTER U WITH DIAERESIS
    0x00a2,// #CENT SIGN
    0x00a3,// #POUND SIGN
    0x00a5,// #YEN SIGN
    0x20a7,// #PESETA SIGN
    0x0192,// #LATIN SMALL LETTER F WITH HOOK
    0x00e1,// #LATIN SMALL LETTER A WITH ACUTE
    0x00ed,// #LATIN SMALL LETTER I WITH ACUTE
    0x00f3,// #LATIN SMALL LETTER O WITH ACUTE
    0x00fa,// #LATIN SMALL LETTER U WITH ACUTE
    0x00f1,// #LATIN SMALL LETTER N WITH TILDE
    0x00d1,// #LATIN CAPITAL LETTER N WITH TILDE
    0x00aa,// #FEMININE ORDINAL INDICATOR
    0x00ba,// #MASCULINE ORDINAL INDICATOR
    0x00bf,// #INVERTED QUESTION MARK
    0x2310,// #REVERSED NOT SIGN
    0x00ac,// #NOT SIGN
    0x00bd,// #VULGAR FRACTION ONE HALF
    0x00bc,// #VULGAR FRACTION ONE QUARTER
    0x00a1,// #INVERTED EXCLAMATION MARK
    0x00ab,// #LEFT-POINTING DOUBLE ANGLE QUOTATION MARK
    0x00bb,// #RIGHT-POINTING DOUBLE ANGLE QUOTATION MARK
    0x2591,// #LIGHT SHADE
    0x2592,// #MEDIUM SHADE
    0x2593,// #DARK SHADE
    0x2502,// #BOX DRAWINGS LIGHT VERTICAL
    0x2524,// #BOX DRAWINGS LIGHT VERTICAL AND LEFT
    0x2561,// #BOX DRAWINGS VERTICAL SINGLE AND LEFT DOUBLE
    0x2562,// #BOX DRAWINGS VERTICAL DOUBLE AND LEFT SINGLE
    0x2556,// #BOX DRAWINGS DOWN DOUBLE AND LEFT SINGLE
    0x2555,// #BOX DRAWINGS DOWN SINGLE AND LEFT DOUBLE
    0x2563,// #BOX DRAWINGS DOUBLE VERTICAL AND LEFT
    0x2551,// #BOX DRAWINGS DOUBLE VERTICAL
    0x2557,// #BOX DRAWINGS DOUBLE DOWN AND LEFT
    0x255d,// #BOX DRAWINGS DOUBLE UP AND LEFT
    0x255c,// #BOX DRAWINGS UP DOUBLE AND LEFT SINGLE
    0x255b,// #BOX DRAWINGS UP SINGLE AND LEFT DOUBLE
    0x2510,// #BOX DRAWINGS LIGHT DOWN AND LEFT
    0x2514,// #BOX DRAWINGS LIGHT UP AND RIGHT
    0x2534,// #BOX DRAWINGS LIGHT UP AND HORIZONTAL
    0x252c,// #BOX DRAWINGS LIGHT DOWN AND HORIZONTAL
    0x251c,// #BOX DRAWINGS LIGHT VERTICAL AND RIGHT
    0x2500,// #BOX DRAWINGS LIGHT HORIZONTAL
    0x253c,// #BOX DRAWINGS LIGHT VERTICAL AND HORIZONTAL
    0x255e,// #BOX DRAWINGS VERTICAL SINGLE AND RIGHT DOUBLE
    0x255f,// #BOX DRAWINGS VERTICAL DOUBLE AND RIGHT SINGLE
    0x255a,// #BOX DRAWINGS DOUBLE UP AND RIGHT
    0x2554,// #BOX DRAWINGS DOUBLE DOWN AND RIGHT
    0x2569,// #BOX DRAWINGS DOUBLE UP AND HORIZONTAL
    0x2566,// #BOX DRAWINGS DOUBLE DOWN AND HORIZONTAL
    0x2560,// #BOX DRAWINGS DOUBLE VERTICAL AND RIGHT
    0x2550,// #BOX DRAWINGS DOUBLE HORIZONTAL
    0x256c,// #BOX DRAWINGS DOUBLE VERTICAL AND HORIZONTAL
    0x2567,// #BOX DRAWINGS UP SINGLE AND HORIZONTAL DOUBLE
    0x2568,// #BOX DRAWINGS UP DOUBLE AND HORIZONTAL SINGLE
    0x2564,// #BOX DRAWINGS DOWN SINGLE AND HORIZONTAL DOUBLE
    0x2565,// #BOX DRAWINGS DOWN DOUBLE AND HORIZONTAL SINGLE
    0x2559,// #BOX DRAWINGS UP DOUBLE AND RIGHT SINGLE
    0x2558,// #BOX DRAWINGS UP SINGLE AND RIGHT DOUBLE
    0x2552,// #BOX DRAWINGS DOWN SINGLE AND RIGHT DOUBLE
    0x2553,// #BOX DRAWINGS DOWN DOUBLE AND RIGHT SINGLE
    0x256b,// #BOX DRAWINGS VERTICAL DOUBLE AND HORIZONTAL SINGLE
    0x256a,// #BOX DRAWINGS VERTICAL SINGLE AND HORIZONTAL DOUBLE
    0x2518,// #BOX DRAWINGS LIGHT UP AND LEFT
    0x250c,// #BOX DRAWINGS LIGHT DOWN AND RIGHT
    0x2588,// #FULL BLOCK
    0x2584,// #LOWER HALF BLOCK
    0x258c,// #LEFT HALF BLOCK
    0x2590,// #RIGHT HALF BLOCK
    0x2580,// #UPPER HALF BLOCK
    0x03b1,// #GREEK SMALL LETTER ALPHA
    0x00df,// #LATIN SMALL LETTER SHARP S
    0x0393,// #GREEK CAPITAL LETTER GAMMA
    0x03c0,// #GREEK SMALL LETTER PI
    0x03a3,// #GREEK CAPITAL LETTER SIGMA
    0x03c3,// #GREEK SMALL LETTER SIGMA
    0x00b5,// #MICRO SIGN
    0x03c4,// #GREEK SMALL LETTER TAU
    0x03a6,// #GREEK CAPITAL LETTER PHI
    0x0398,// #GREEK CAPITAL LETTER THETA
    0x03a9,// #GREEK CAPITAL LETTER OMEGA
    0x03b4,// #GREEK SMALL LETTER DELTA
    0x221e,// #INFINITY
    0x03c6,// #GREEK SMALL LETTER PHI
    0x03b5,// #GREEK SMALL LETTER EPSILON
    0x2229,// #INTERSECTION
    0x2261,// #IDENTICAL TO
    0x00b1,// #PLUS-MINUS SIGN
    0x2265,// #GREATER-THAN OR EQUAL TO
    0x2264,// #LESS-THAN OR EQUAL TO
    0x2320,// #TOP HALF INTEGRAL
    0x2321,// #BOTTOM HALF INTEGRAL
    0x00f7,// #DIVISION SIGN
    0x2248,// #ALMOST EQUAL TO
    0x00b0,// #DEGREE SIGN
    0x2219,// #BULLET OPERATOR
    0x00b7,// #MIDDLE DOT
    0x221a,// #SQUARE ROOT
    0x207f,// #SUPERSCRIPT LATIN SMALL LETTER N
    0x00b2,// #SUPERSCRIPT TWO
    0x25a0,// #BLACK SQUARE
    0x00a0,// #NO-BREAK SPACE
  };

  public char map_cp850_unicode(char x) {
    if (x>=0x100)
      return x;
    return unimap[x];
  }

  private void _SetCursor(int row,int col) {
    int maxr = getRows();
    int tm = getTopMargin();

    R = (row<0)?0:row;
    C = (col<0)?0:col;

    if (!moveoutsidemargins) {
      R	+= getTopMargin();
      maxr	 = getBottomMargin();
    }
    if (R>maxr) R = maxr;
  }

  private void putChar(char c, boolean doshowcursor) {
    Dimension size;
    int  rows = getRows(); //statusline
    int  columns = getColumns();
    int  tm = getTopMargin();
    int  bm = getBottomMargin();
    byte  msg[];

    if (debug>4) System.out.println("putChar("+c+" ["+((int)c)+"]) at R="+R+" , C="+C+", columns="+columns+", rows="+rows);
    markLine(R,1);
    if (c>255) {
      if (debug>0)
        System.out.println("char > 255:"+((int)c));
      return;
    }
    switch (term_state) {
    case TSTATE_DATA:
      /* FIXME: we shouldn't use chars with bit 8 set if ibmcharset.
       * probably... but some BBS do anyway...
       */
      if (!useibmcharset) {
        boolean doneflag = true;
        switch (c) {
        case OSC:
          osc="";
          term_state = TSTATE_OSC;
          break;
        case RI:
          if (R>tm)
            R--;
          else
            insertLine(R,1,SCROLL_DOWN);
          if (debug>1)
            System.out.println("RI");
          break;
        case IND:
          if (R == tm - 1 || R == bm || R == rows - 1) //  Ray: not bottom margin - 1
            insertLine(R,1,SCROLL_UP);
          else
            R++;
          if (debug>1)
            System.out.println("IND (at "+R+" )");
          break;
        case NEL:
          if (R == tm - 1 || R == bm || R == rows - 1) //  Ray: not bottom margin - 1
            insertLine(R,1,SCROLL_UP);
          else
            R++;
          C=0;
          if (debug>1)
            System.out.println("NEL (at "+R+" )");
          break;
        case HTS:
          Tabs[C] = 1;
          if (debug>1)
            System.out.println("HTS");
          break;
        case DCS:
          dcs="";
          term_state = TSTATE_DCS;
          break;
        default:
          doneflag = false;
          break;
        }
        if (doneflag) break;
      }
      switch (c) {
      case CSI: // should be in the 8bit section, but some BBS use this
        term_state = TSTATE_DCEQ;
        break;
      case ESC:
        term_state = TSTATE_ESC;
        lastwaslf=0;
        break;
      case '\b':
        C--;
        if (C<0)
          C=0;
        lastwaslf = 0;
        break;
      case '\t':
        if (insertmode == 1) {
          int  nr,newc;

          newc = C;
          do {
            insertChar(C,R,' ',attributes);
            newc++;
          } while (newc<columns && Tabs[newc]==0);
        } else {
          do {
            putChar(C++,R,' ',attributes);
          } while (C<columns && (Tabs[C]==0));
        }
        lastwaslf = 0;
        break;
      case '\r':
        C=0;
        break;
      case '\n':
	if (debug>3)
	  System.out.println("R= "+R+", bm "+bm+", tm="+tm+", rows="+rows);
        if (!vms)
	  {
            if (lastwaslf!=0 && lastwaslf!=c)   //  Ray: I do not understand this logic.
              break;
            lastwaslf=c;
            /*C = 0;*/
	  }
	// note: we do not scroll at the topmargin! only at the bottommargin
	// of the scrollregion and at the bottom.
	if ( R == bm || R >= rows - 1)
	  insertLine(R,1);
	else
	  R++;
        break;
      case '\016':
        /* ^N, Shift out - Put G1 into GL */
        gl = 1;
        break;
      case '\017':
        /* ^O, Shift in - Put G0 into GL */
        gl = 0;
        break;
      default:
        lastwaslf=0;
        if (c<32) {
          if (c!=0)
            if (debug>0)
              System.out.println("TSTATE_DATA char: "+((int)c));
          break;
        }
        if(C >= columns) {
          if(R < rows - 1)
            R++;
          else
            insertLine(R,SCROLL_UP);
          C = 0;
        }

        // Mapping if DEC Special is chosen charset
        if ( gx[gl] == '0' ) {
          if ( c >= '\u005f' && c <= '\u007e' ) {
	    if (debug>3)
	      System.out.print("Mapping "+c+" (index "+((short)c-0x5f)+" to ");
	    c = DECSPECIAL[(short)c - 0x5f];
	    if (debug>3)
	      System.out.println(c+" ("+(int)c+")");
          }
        }
	/*
	  if ( gx[gr] == '0' ) {
          if ( c >= '\u00bf' && c <= '\u00fe' ) {
          if (debug>2)
	  System.out.print("Mapping "+c);
	  c = DECSPECIAL[(short)c - 0xbf];
	  if (debug>2)
	  System.out.println("to "+c);
          }
	  }
	*/
        if (useibmcharset)
          c = map_cp850_unicode(c);

	/*if(true || (statusmode == 0)) { */
	if (debug>4) System.out.println("output "+c+" at "+C+","+R);
	if (insertmode==1) {
	  insertChar(C, R, c, attributes);
	} else {
	  putChar(C, R, c, attributes);
	}
	/*
	  } else {
	  if (insertmode==1) {
	  insertChar(C, rows, c, attributes);
	  } else {
	  putChar(C, rows, c, attributes);
	  }
	  }
	*/
        C++;
        break;
      } /* switch(c) */
      break;
    case TSTATE_OSC:
      if ((c<0x20) && (c!=ESC)) {// NP - No printing character
        handle_osc(osc);
        term_state = TSTATE_DATA;
        break;
      }
      //but check for vt102 ESC \
      if (c=='\\' && osc.charAt(osc.length()-1)==ESC) {
	handle_osc(osc);
	term_state = TSTATE_DATA;
	break;
      }
      osc = osc + c;
      break;
    case TSTATE_ESC:
      term_state = TSTATE_DATA;
      switch (c) {
      case '#':
        term_state = TSTATE_ESCSQUARE;
        break;
      case 'c':
        /* Hard terminal reset */
	/* reset character sets */
	gx[0] = 'B';
	gx[1] = '0';
	gx[2] = 'A';
	gx[3] = '<';
	gl = 0;  // default GL to G0
	gr = 1;  // default GR to G1
	/* reset tabs */
	int nw = getColumns();
	if (nw<132) nw=132;
	Tabs = new byte[nw];
	for (int i=0;i<nw;i+=8) {
	  Tabs[i]=1;
	}
        /*FIXME:*/
        break;
      case '[':
        DCEvar    = 0;
        DCEvars[0]  = 0;
        DCEvars[1]  = 0;
        DCEvars[2]  = 0;
        DCEvars[3]  = 0;
        term_state = TSTATE_CSI;
        break;
      case ']':
        osc="";
        term_state = TSTATE_OSC;
        break;
      case 'P':
        dcs="";
        term_state = TSTATE_DCS;
        break;
      case 'E':
        if (R == tm - 1 || R == bm || R == rows - 1) //  Ray: not bottom margin - 1
          insertLine(R,1,SCROLL_UP);
        else
          R++;
        C=0;
        if (debug>1)
          System.out.println("ESC E (at "+R+")");
        break;
      case 'D':
        if (R == tm - 1 || R == bm || R == rows - 1)
          insertLine(R,1,SCROLL_UP);
        else
          R++;
        if (debug>1)
          System.out.println("ESC D (at "+R+" )");
        break;
      case 'M': // IL
        if ((R>=tm) && (R<=bm)) // in scrolregion
          insertLine(R,1,SCROLL_DOWN);
	/* else do nothing ; */
        if (debug>1)
          System.out.println("ESC M ");
        break;
      case 'H':
        if (debug>1)
          System.out.println("ESC H at "+C);
        /* right border probably ...*/
        if (C>=columns)
          C=columns-1;
        Tabs[C] = 1;
        break;
      case '=':
        /*application keypad*/
        if (debug>0)
          System.out.println("ESC =");
        break;
      case '>': /*normal keypad*/
        if (debug>0)
          System.out.println("ESC >");
        break;
      case '7': /*save cursor */
        Sc = C;
        Sr = R;
        Sa = attributes;
        if (debug>1)
          System.out.println("ESC 7");
        break;
      case '8': /*restore cursor */
        C = Sc;
        R = Sr;
        attributes = Sa;
        if (debug>1)
          System.out.println("ESC 7");
        break;
      case '(': /* Designate G0 Character set (ISO 2022) */
        term_state = TSTATE_SETG0;
        break;
      case ')': /* Designate G0 character set (ISO 2022) */
        term_state = TSTATE_SETG1;
        break;
      case '*': /* Designate G1 Character set (ISO 2022) */
        term_state = TSTATE_SETG2;
        break;
      case '+': /* Designate G1 Character set (ISO 2022) */
        term_state = TSTATE_SETG3;
        break;
      case '~': /* Locking Shift 1, right */
        gr = 1;
        break;
      case 'n': /* Locking Shift 2 */
        gl = 2;
        break;
      case '}': /* Locking Shift 2, right */
        gr = 2;
        break;
      case 'o': /* Locking Shift 3 */
        gl = 3;
        break;
      case '|': /* Locking Shift 3, right */
        gr = 3;
        break;
      default:
        System.out.println("ESC unknown letter: ("+((int)c)+")");
        break;
      }
      break;
    case TSTATE_SETG0:
      if(c!='0' && c!='A' && c!='B')
        System.out.println("ESC ( : G0 char set?  ("+((int)c)+")");
      else {
        if (debug>2) System.out.println("ESC ( : G0 char set  ("+c+" "+((int)c)+")");
        gx[0] = c;
      }
      term_state = TSTATE_DATA;
      break;
    case TSTATE_SETG1:
      if(c!='0' && c!='A' && c!='B')
        System.out.println("ESC ) :G1 char set?  ("+((int)c)+")");
      else {
        if (debug>2) System.out.println("ESC ) :G1 char set  ("+c+" "+((int)c)+")");
        gx[1] = c;
      }
      term_state = TSTATE_DATA;
      break;
    case TSTATE_SETG2:
      if(c!='0' && c!='A' && c!='B')
        System.out.println("ESC*:G2 char set?  ("+((int)c)+")");
      else {
        if (debug>2) System.out.println("ESC*:G2 char set  ("+c+" "+((int)c)+")");
	gx[2] = c;
      }
      term_state = TSTATE_DATA;
      break;
    case TSTATE_SETG3:
      if(c!='0' && c!='A' && c!='B')
        System.out.println("ESC+:G3 char set?  ("+((int)c)+")");
      else {
        if (debug>2) System.out.println("ESC+:G3 char set  ("+c+" "+((int)c)+")");
        gx[3] = c;
      }
      term_state = TSTATE_DATA;
      break;
    case TSTATE_ESCSQUARE:
      switch (c) {
      case '8':
        for (int i=0;i<columns;i++)
          for (int j=0;j<rows;j++)
            putChar(i,j,'E',0);
        break;
      default:
        System.out.println("ESC # "+c+" not supported.");
        break;
      }
      term_state = TSTATE_DATA;
      break;
    case TSTATE_DCS:
      if (c=='\\' && dcs.charAt(dcs.length()-1)==ESC) {
        handle_dcs(dcs);
        term_state = TSTATE_DATA;
        break;
      }
      dcs = dcs + c;
      break;

    case TSTATE_DCEQ:
      term_state = TSTATE_DATA;
      switch (c) {
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        DCEvars[DCEvar]=DCEvars[DCEvar]*10+((int)c)-48;
        term_state = TSTATE_DCEQ;
        break;
      case ';':
        DCEvar++;
        DCEvars[DCEvar] = 0;
        term_state = TSTATE_DCEQ;
        break;
      case 'r': // XTERM_RESTORE
        if (true || debug>1)
          System.out.println("ESC [ ? "+DCEvars[0]+" r");
        /* DEC Mode reset */
        switch (DCEvars[0]){
        case 3: /* 80 columns*/
          size = getSize();
          setScreenSize(80,rows);
          break;
        case 4: /* scrolling mode, smooth */
          break;
        case 5: /* light background */
          break;
        case 6: /* move inside margins ? */
          moveoutsidemargins = true;
          break;
        case 12:/* local echo off */
          break;
        }
        break;
      case 'h': // DECSET
        if (debug>0)
          System.out.println("ESC [ ? "+DCEvars[0]+" h");
        /* DEC Mode set */
        switch (DCEvars[0]){
        case 1:  /* Application cursor keys */
          KeyUp[0]  = "\u001bOA";
          KeyDown[0]  = "\u001bOB";
          KeyRight[0]= "\u001bOC";
          KeyLeft[0]  = "\u001bOD";
          break;
        case 3: /* 132 columns*/
          size = getSize();
          setScreenSize(132,rows);
          break;
        case 4: /* scrolling mode, smooth */
          break;
        case 5: /* light background */
          break;
        case 6: /* move inside margins ? */
          moveoutsidemargins = false;
          break;
        case 12:/* local echo off */
          break;
        case 18:/* DECPFF - Printer Form Feed Mode -> On */
          break;
        case 19:/* DECPEX - Printer Extent Mode -> Screen */
          break;
        }
        break;
      case 'i': // DEC Printer Control, autoprint, echo screenchars to printer
		// This is different to CSI i!
		// Also: "Autoprint prints a final display line only when the
		// cursor is moved off the line by an autowrap or LF, FF, or
		// VT (otherwise do not print the line)."
        switch (DCEvars[0]) {
        case 1:
	  if (debug>1)
            System.out.println("CSI ? 1 i : Print line containing cursor");
          break;
        case 4:
	  if (debug>1)
            System.out.println("CSI ? 4 i : Start passthrough printing");
          break;
        case 5:
	  if (debug>1)
            System.out.println("CSI ? 4 i : Stop passthrough printing");
          break;
        }
        break;
      case 'l':	//DECRST
        /* DEC Mode reset */
        if (debug>0)
          System.out.println("ESC [ ? "+DCEvars[0]+" l");
        switch (DCEvars[0]){
        case 1:  /* Application cursor keys */
          KeyUp[0]  = "\u001b[A";
          KeyDown[0]  = "\u001b[B";
          KeyRight[0]= "\u001b[C";
          KeyLeft[0]  = "\u001b[D";
          break;
        case 3: /* 80 columns*/
          size = getSize();
          setScreenSize(80,rows);
          break;
        case 4: /* scrolling mode, jump */
          break;
        case 5: /* dark background */
          break;
        case 6: /* move outside margins ? */
          moveoutsidemargins = true;
          break;
        case 12:/* local echo on */
          break;
        case 18:/* DECPFF - Printer Form Feed Mode -> Off*/
          break;
        case 19:/* DECPEX - Printer Extent Mode -> Scrolling Region */
          break;
        }
        break;
      case 'n':
        if (debug>0)
          System.out.println("ESC [ ? "+DCEvars[0]+" n");
        switch (DCEvars[0]) {
        case 15:
          /* printer? no printer. */
          write(((char)ESC)+"[?13n",false);
          System.out.println("ESC[5n");
          break;
        default:
          break;
        }
        break;
      default:
        if (debug>0)
          System.out.println("ESC [ ? "+DCEvars[0]+" "+c);
        break;
      }
      break;
    case TSTATE_CSI_DOLLAR:
      switch (c) {
      case '}':
	System.out.println("Active Status Display now "+DCEvars[0]);
	statusmode = DCEvars[0];
	break;
	/* bad documentation?
	   case '-':
	   System.out.println("Set Status Display now "+DCEvars[0]);
	   break;
	*/
      case '~':
	System.out.println("Status Line mode now "+DCEvars[0]);
	break;
      default:
	System.out.println("UNKNOWN Status Display code "+c+", with Pn="+DCEvars[0]);
	break;
      }
      term_state = TSTATE_DATA;
      break;
    case TSTATE_CSI:
      term_state = TSTATE_DATA;
      switch (c) {
      case '$':
	term_state=TSTATE_CSI_DOLLAR;
	break;
      case '?':
        DCEvar=0;
        DCEvars[0]=0;
        term_state=TSTATE_DCEQ;
        break;
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        DCEvars[DCEvar]=DCEvars[DCEvar]*10+((int)c)-48;
        term_state = TSTATE_CSI;
        break;
      case ';':
        DCEvar++;
        DCEvars[DCEvar] = 0;
        term_state = TSTATE_CSI;
        break;
      case 'c':/* send primary device attributes */
        /* send (ESC[?61c) */
        write(((char)ESC)+"[?1;2c",false);
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" c");
        break;
      case 'q':
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" q");
        break;
      case 'g':
        /* used for tabsets */
        switch (DCEvars[0]){
        case 3:/* clear them */
          int nw = getColumns();
          Tabs = new byte[nw];
          break;
        case 0:
          Tabs[C] = 0;
          break;
        }
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" g");
        break;
      case 'h':
        switch (DCEvars[0]) {
        case 4:
          insertmode = 1;
          break;
        case 20:
          System.out.println("Setting CRLF to TRUE");
          sendcrlf = true;
          break;
        default:
          System.out.println("unsupported: ESC [ "+DCEvars[0]+" h");
          break;
        }
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" h");
        break;
      case 'i': // Printer Controller mode.
		// "Transparent printing sends all output, except the CSI 4 i
		//  termination string, to the printer and not the screen,
		//  uses an 8-bit channel if no parity so NUL and DEL will be
		//  seen by the printer and by the termination recognizer code,
		//  and all translation and character set selections are
		//  bypassed."
        switch (DCEvars[0]) {
	case 0:
	  if (debug>1)
            System.out.println("CSI 0 i:  Print Screen, not implemented.");
	  break;
        case 4:
	  if (debug>1)
	    System.out.println("CSI 4 i:  Enable Transparent Printing, not implemented.");
          break;
        case 5:
	  if (debug>1)
	    System.out.println("CSI 4/5 i:  Disable Transparent Printing, not implemented.");
          break;
	}
        break;
      case 'l':
        switch (DCEvars[0]) {
        case 4:
          insertmode = 0;
          break;
        case 20:
          System.out.println("Setting CRLF to FALSE");
          sendcrlf = false;
          break;
        }
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" l");
        break;
      case 'A': // CUU
	{
	  int limit;
	  /* FIXME: xterm only cares about 0 and topmargin */
	  if (R > bm)
            limit = bm+1;
	  else if (R >= tm) {
            limit = tm;
	  } else
            limit = 0;
	  if (DCEvars[0]==0)
	    R--;
	  else
	    R-=DCEvars[0];
	  if (R < limit)
            R = limit;
	  if (debug>1)
	    System.out.println("ESC [ "+DCEvars[0]+" A");
	  break;
	}
      case 'B':	// CUD
        /* cursor down n (1) times */
	{
	  int limit;
	  if (R < tm)
            limit = tm-1;
	  else if (R <= bm) {
            limit = bm;
	  } else
            limit = rows - 1;
	  if (DCEvars[0]==0)
	    R++;
	  else
	    R+=DCEvars[0];
	  if (R > limit)
            R = limit;
	  else {
            if (debug>2) System.out.println("Not limited.");
	  }
	  if (debug>2) System.out.println("to: " + R);
	  if (debug>1)
	    System.out.println("ESC [ "+DCEvars[0]+" B (at C="+C+")");
	  break;
	}
      case 'C':
        if (DCEvars[0]==0)
          C++;
        else
          C+=DCEvars[0];
        if (C>columns-1)
          C=columns-1;
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" C");
        break;
      case 'd': // CVA
	R = DCEvars[0];
        System.out.println("ESC [ "+DCEvars[0]+" d");
	break;
      case 'D':
        if (DCEvars[0]==0)
          C--;
        else
          C-=DCEvars[0];
        if (C<0) C=0;
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" D");
        break;
      case 'r': // DECSTBM
        if (DCEvar>0)   //  Ray:  Any argument is optional
	  {
	    R = DCEvars[1]-1;
	    if (R < 0)
	      R = rows-1;
	    else if (R >= rows) {
	      R = rows - 1;
	    }
	  } else
	    R = rows - 1;
        setBottomMargin(DCEvars[1]-1);
        if (R >= DCEvars[0])
	  {
	    R = DCEvars[0]-1;
	    if (R < 0)
	      R = 0;
	  }
        setTopMargin(DCEvars[0]-1);
	_SetCursor(0,0);
        if (debug>1)
          System.out.println("ESC ["+DCEvars[0]+" ; "+DCEvars[1]+" r");
        break;
      case 'G':  /* CUP  / cursor absolute column */
	C = DCEvars[0];
	if (debug>1) System.out.println("ESC [ "+DCEvars[0]+" G");
	break;
      case 'H':  /* CUP  / cursor position */
        /* gets 2 arguments */
	_SetCursor(DCEvars[0]-1,DCEvars[1]-1);
        if (debug>2) {
          System.out.println("ESC [ "+DCEvars[0]+";"+DCEvars[1]+" H, moveoutsidemargins "+moveoutsidemargins);
          System.out.println("	-> R now "+R+", C now "+C);
	}
        break;
      case 'f':  /* move cursor 2 */
        /* gets 2 arguments */
        R = DCEvars[0]-1;
        C = DCEvars[1]-1;
        if (C<0) C=0;
        if (R<0) R=0;
        if (debug>2)
          System.out.println("ESC [ "+DCEvars[0]+";"+DCEvars[1]+" f");
        break;
      case 'L':
        /* insert n lines */
        if (DCEvars[0]==0)
          insertLine(R,SCROLL_DOWN);
        else
          insertLine(R,DCEvars[0],SCROLL_DOWN);
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" L (at R "+R+")");
        break;
      case 'M':
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+"M at R="+R);
        if (DCEvars[0]==0)
          deleteLine(R);
        else
          for (int i=0;i<DCEvars[0];i++)
            deleteLine(R);
        break;
      case 'K':
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" K");
        /* clear in line */
        switch (DCEvars[0]) {
        case 0:/*clear to right*/
          if (C<columns-1)
            deleteArea(C,R,columns-C,1);
          break;
        case 1:/*clear to the left*/
          if (C>0)
            deleteArea(0,R,C,1);    // Ray: Should at least include character before this one, not C-1
          break;
        case 2:/*clear whole line */
          deleteArea(0,R,columns,1);
          break;
        }
        break;
      case 'J':
        /* clear below current line */
        switch (DCEvars[0]) {
        case 0:
          if (R<rows-1)
            deleteArea(0,R + 1,columns,rows-R-1);
          if (C<columns-1)
            deleteArea(C,R,columns-C,1);
          break;
        case 1:
          if (R>0)
            deleteArea(0,0,columns,R-1);
          if (C>0)
            deleteArea(0,R,C,1);    // Ray: Should at least include character before this one, not C-1
          break;
        case 2:
          deleteArea(0,0,columns,rows);
          break;
        }
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" J");
        break;
      case '@':
	if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" @");
	for (int i=0;i<DCEvars[0];i++)
	  insertChar(C,R,' ',attributes);
	break;
      case 'P':
        if (debug>1)
          System.out.println("ESC [ "+DCEvars[0]+" P, C="+C+",R="+R);
	if (DCEvars[0]==0) DCEvars[0]=1;
	for (int i=0;i<DCEvars[0];i++)
          deleteChar(C,R);
        break;
      case 'n':
        switch (DCEvars[0]){
        case 5: /* malfunction? No malfunction. */
          write(((char)ESC)+"[0n",false);
          if(debug > 1)
            System.out.println("ESC[5n");
          break;
        case 6:
          write(((char)ESC)+"["+R+";"+C+"R",false);
          if(debug > 1)
            System.out.println("ESC[6n");
          break;
        default:
          if (debug>0)
            System.out.println("ESC [ "+DCEvars[0]+" n??");
          break;
        }
        break;
      case 'm':  /* attributes as color, bold , blink,*/
        if (debug>3)
          System.out.print("ESC [ ");
        if (DCEvar == 0 && DCEvars[0] == 0)
          attributes = 0;
        for (i=0;i<=DCEvar;i++) {
          switch (DCEvars[i]) {
          case 0:
            if (DCEvar>0)
              attributes =0;
            break;
          case 4:
            attributes |= UNDERLINE;
            break;
          case 1:
            attributes |= BOLD;
            break;
          case 7:
            attributes |= INVERT;
            break;
          case 5: /* blink on */
            break;
          case 25: /* blinking off */
            break;
          case 27:
            attributes &= ~INVERT;
            break;
          case 24:
            attributes &= ~UNDERLINE;
            break;
          case 22:
            attributes &= ~BOLD;
            break;
          case 30:
          case 31:
          case 32:
          case 33:
          case 34:
          case 35:
          case 36:
          case 37:
            attributes &= ~(0xf<<3);
            attributes |= ((DCEvars[i]-30)+1)<<3;
            break;
          case 39:
            attributes &= ~(0xf<<3);
            break;
          case 40:
          case 41:
          case 42:
          case 43:
          case 44:
          case 45:
          case 46:
          case 47:
            attributes &= ~(0xf<<7);
            attributes |= ((DCEvars[i]-40)+1)<<7;
            break;
          case 49:
            attributes &= ~(0xf<<7);
            break;

          default:
            System.out.println("ESC [ "+DCEvars[i]+" m unknown...");
            break;
          }
          if (debug>3)
            System.out.print(""+DCEvars[i]+";");
        }
        if (debug>3)
          System.out.print(" (attributes = "+attributes+")m \n");
        break;
      default:
        System.out.println("ESC [ unknown letter:"+c+" ("+((int)c)+")");
        break;
      }
      break;
    default:
      term_state = TSTATE_DATA;
      break;
    }
  if (C > columns) C = columns;
  if (R > rows)  R = rows;
  if (C < 0)  C = 0;
  if (R < 0)  R = 0;
  if (doshowcursor)
    setCursorPosition(C, R);
  markLine(R,1);
  }

  /* hard reset the terminal */
  public void reset() {
    gx[0] = 'B';
    gx[1] = '0';
    gx[2] = 'A';
    gx[3] = '<';
    gl = 0;  // default GL to G0
    gr = 1;  // default GR to G1
    /* reset tabs */
    int nw = getColumns();
    if (nw<132) nw=132;
    Tabs = new byte[nw];
    for (int i=0;i<nw;i+=8) {
      Tabs[i]=1;
    }
    /*FIXME:*/
    term_state = TSTATE_DATA;
  }
}
