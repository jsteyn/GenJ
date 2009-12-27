/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2002 Nils Meier <nils@meiers.net>
 *
 * This piece of code is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 * $Revision: 1.37.2.3 $ $Author: nmeier $ $Date: 2009-12-27 22:45:56 $
 */
package genj.util;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JFrame;

/**
 * Registry - betterfied java.util.Properties
 */
public class Registry {
  
  private final static Logger LOG = Logger.getLogger("genj.util");
  private String prefix;
  private Properties properties;
  private static Map<String, Registry> registries = new HashMap<String, Registry>();

  /**
   * Constructor 
   */
  public Registry(Registry registry, String view) {
    this.prefix = registry.prefix + "." + view;
    this.properties = registry.properties;
  }
  
  /**
   * Constructor 
   */
  private Registry(String rootPrefix) {
    
    this.prefix = rootPrefix;
    
    // patch properties that keeps order
    properties = new Properties() {
      @Override
      public synchronized Enumeration<Object> keys() {
        Vector result = new Vector(super.keySet()); 
        Collections.sort(result);
        return result.elements();
      }
    };
    
    // read all from local registry
    File file = getFile(prefix);
    try {
      LOG.fine("Loading registry '"+prefix+".properties' from file "+file.getAbsolutePath());
      FileInputStream in = new FileInputStream(file);
      properties.load(in);
      in.close();
    } catch (Throwable t) {
      LOG.log(Level.FINE, "Failed to read registry "+prefix+" from "+file+" ("+t.getMessage()+")");
    }
    
    // remember
    registries.put(prefix,this);
  }

  /**
   * Constructor for registry loaded from InputStream
   * that can't be looked up and won't be saved
   * @param InputStream to load registry from
   */
  public Registry(InputStream in) {
    // Load settings
    prefix = "";
    try {
      properties.load(in);
    } catch (Exception ex) {
    }
  }
  
  /**
   * Accessor 
   */
  public static Registry get(Object source) {
    return get(source.getClass());
  }
  
  /**
   * Accessor 
   */
  public static Registry get(Class<?> source) {
    return get(source.getName());
  }
  
  /**
   * Accessor 
   */
  public static Registry get(String pckg) {

    String[] tokens = pckg.split("\\.");
    if (tokens.length==1)
      throw new IllegalArgumentException("default package not allowed");
    
    Registry r = registries.get(tokens[0]);
    if (r==null) 
      r = new Registry(tokens[0]);

    return tokens.length==1 ? r : new Registry(r, pckg.substring(tokens[0].length()+1));
  }

  /**
   * Remove keys
   */
  public void remove(String prefix) {
    List keys = new ArrayList(properties.keySet());
    for (int i=0,j=keys.size();i<j;i++) {
      String key = (String)keys.get(i);
      if (key.startsWith(prefix))
        properties.remove(key);
    }
  }
  
  /**
   * Returns a map of values
   */
  public Map get(String prefix, Map def) {
    Map result = new HashMap();
    // loop over keys in map
    Iterator keys = def.keySet().iterator();
    while (keys.hasNext()) {
      // grab from default
      Object key = keys.next();
      Object value = def.get(key);
      // try to get a better value
      try {
        value = getClass().getMethod("get", new Class[]{ String.class, value.getClass() })
          .invoke(this, new Object[]{ prefix+"."+key, value });
      } catch (Throwable t) {
        
      }
      // keep it
      result.put(key, value);
    }
    // done
    return result;
  }

  /**
   * Returns array of ints by key
   */
  public int[] get(String key, int[] def) {

    // Get size of array
    int size = get(key,-1);
    if (size<0)
      return def;

    // Gather array
    int result[] = new int[size];
    for (int i=0;i<size;i++) {
      result[i] = get(key+"."+(i+1),-1);
    }

    // Done
    return result;
  }

  /**
   * Returns array of Rectangles by key
   */
  public Rectangle[] get(String key, Rectangle[] def) {

    // Get size of array
    int size = get(key,-1);
    if (size==-1)
      return def;

    // Gather array
    Rectangle[] result = new Rectangle[size];
    Rectangle empty = new Rectangle(-1,-1,-1,-1);

    for (int i=0;i<size;i++) {
      result[i] = get(key+"."+(i+1),empty);
    }

    // Done
    return result;
  }

  /**
   * Returns array of strings by key
   */
  public String[] get(String key, String[] def) {

    // Get size of array
    int size = get(key,-1);
    if (size==-1)
      return def;

    // Gather array
    String result[] = new String[size];
    for (int i=0;i<size;i++) {
      result[i] = get(key+"."+(i+1),"");
    }

    // Done
    return result;
  }

  /**
   * Returns float parameter to key
   */
  public float get(String key, float def) {

    // Get property by key
    String result = get(key,(String)null);

    // .. existing ?
    if (result==null)
      return def;

    // .. number ?
    try {
      return Float.valueOf(result.trim()).floatValue();
    } catch (NumberFormatException ex) {
    }

    return def;
  }

  /**
   * Returns integer parameter to key
   */
  public int get(String key, int def) {

    // Get property by key
    String result = get(key,(String)null);

    // .. existing ?
    if (result==null)
      return def;

    // .. number ?
    try {
      return Integer.parseInt(result.trim());
    } catch (NumberFormatException ex) {
    }

    return def;
  }

  /**
   * Returns dimension parameter by key
   */
  public Dimension get(String key, Dimension def) {

    // Get box dimension
    int w = get(key+".w", -1);
    int h = get(key+".h", -1);

    // Missing ?
    if ( (w==-1) || (h==-1) )
      return def;

    // Done
    return new Dimension(w,h);
  }

  /**
   * Returns font parameter by key
   */
  public Font get(String key, Font def) {

    String face = get(key+".name" ,(String)null);
    int style   = get(key+".style",-1);
    int size    = get(key+".size" ,-1);

    if ( (face==null)||(style==-1)||(size==-1) )
      return def;

    return new Font(face,style,size);
  }

  /**
   * Returns point parameter by key
   */
  public Point get(String key, Point def) {

    // Get box dimension
    int x = get(key+".x", Integer.MAX_VALUE);
    int y = get(key+".y", Integer.MAX_VALUE);

    // Missing ?
    if ( x==Integer.MAX_VALUE || y==Integer.MAX_VALUE )
      return def;

    // Done
    return new Point(x,y);
  }

  /**
   * Returns point parameter by key
   */
  public Point2D get(String key, Point2D def) {

    // Get box dimension
    float x = get(key+".x", Float.NaN);
    float y = get(key+".y", Float.NaN);

    // Missing ?
    if ( Float.isNaN(x) || Float.isNaN(y) )
      return def;

    // Done
    return new Point2D.Double(x,y);
  }

  /**
   * Returns rectangle parameter by key
   */
  public Rectangle get(String key, Rectangle def) {

    // Get box dimension
    int x = get(key+".x", Integer.MAX_VALUE);
    int y = get(key+".y", Integer.MAX_VALUE);
    int w = get(key+".w", Integer.MAX_VALUE);
    int h = get(key+".h", Integer.MAX_VALUE);

    // Missing ?
    if ( (x==Integer.MAX_VALUE) || (y==Integer.MAX_VALUE) || (w==Integer.MAX_VALUE) || (h==Integer.MAX_VALUE) )
      return def;

    // Done
    return new Rectangle(x,y,w,h);
  }

  /**
   * Returns vector of strings by key
   */
  /*
  public Vector get(String key, Vector def) {

    // Get size of array
    int size = get(key,-1);
    if (size==-1)
      return def;

    // Gather array
    Vector result = new Vector(size);
    for (int i=0;i<size;i++) {
      result.addElement(get(key+"."+(i+1),""));
    }

    // Done
    return result;
  }
  */

  /**
   * Returns a collection of strings by key
   */
  public Collection get(String key, Collection def) {

    // Get size of array
    int size = get(key,-1);
    if (size==-1)
      return def;

    // Create result
    Collection result;
    try {
      result = (Collection)def.getClass().newInstance();
    } catch (Throwable t) {
      return def;
    }
    
    // Collection content
    for (int i=0;i<size;i++) {
      result.add(get(key+"."+(i+1),""));
    }

    // Done
    return result;
  }

  /**
   * Returns boolean parameter by key
   */
  public boolean get(String key, boolean def) {

    // Get property by key
    String result = get(key,(String)null);

    // .. existing ?
    if (result==null)
      return def;

    // boolean value
    if (result.equals("1"))
      return true;
    if (result.equals("0"))
      return false;

    // Done
    return def;
  }

  /**
   * Returns color parameter by key
   */
  public Color get(String key, Color def) {

    // Get property by key
    int result = get(key,Integer.MAX_VALUE);

    // .. existing ?
    if (result==Integer.MAX_VALUE)
      return def;

    // done
    return new Color(result);
  }

  /**
   * Returns String parameter to key
   */
  public String get(String key, String def) {

    // Get property by key
    String result = (String)properties.get(prefix+"."+key);

    // verify it exists
    // 20060222 NM can't assume length()==0 means default should apply - it could indeed mean an empty value!
    // 20040523 NM removed trim() to allow for leading/trailing space values
    if (result==null)
      return def;
      
    // Done
    return result;
  }

  /**
   * Remembers a String value
   */
  public void put(String key, String value) {

    if (value==null)
      properties.remove(prefix+"."+key);
    else
      properties.put(prefix+"."+key,value);
  }

  /**
   * Remember an array of values
   */
  public void put(String prefix, Map values) {
    
    // loop over keys in map
    Iterator keys = values.keySet().iterator();
    while (keys.hasNext()) {
      // grab value
      Object key = keys.next();
      Object value = values.get(key);
      // try to store
      try {
        value = getClass().getMethod("put", new Class[]{ String.class, value.getClass() })
          .invoke(this, new Object[]{ prefix+"."+key, value });
      } catch (Throwable t) {        
      }
    }
    // done
  }
  
  /**
   * Remembers an array of ints
   */
  public void put(String key, int[] value) {

    // Remember
    int l = value.length;
    put(key,l);

    for (int i=0;i<l;i++)
      put(key+"."+(i+1),""+value[i]);

    // Done
  }

  /**
   * Remembers an array of Rectangles
   */
  public void put(String key, Rectangle[] value) {

    // Remember
    int l = value.length;

    put(key,""+l);

    for (int i=0;i<l;i++)
      put(key+"."+(i+1),value[i]);

    // Done
  }

  /**
   * Remembers an array of Strings(Objects)
   */
  public void put(String key, Object value[]) {
    put(key,value,value.length);
  }

  /**
   * Remembers an array of Strings
   */
  public void put(String key, Object value[], int length) {

    // Remember
    int l = Math.min(value.length,length);

    put(key,""+l);

    for (int i=0;i<l;i++) {
      put(key+"."+(i+1),value[i].toString());
    }

    // Done
  }

  /**
   * Remembers an float value
   */
  public void put(String key, float value) {
    put(key,""+value);
  }

  /**
   * Remembers an int value
   */
  public void put(String key, int value) {
    put(key,""+value);
  }

  /**
   * Remembers a point value
   */
  public void put(String key, Dimension value) {

    // Remember box dimension
    put(key+".w",value.width);
    put(key+".h",value.height);

    // Done
  }

  /**
   * Remembers a font value
   */
  public void put(String key, Font value) {

    // Remember box dimension
    put(key+".name" ,value.getName() );
    put(key+".style",value.getStyle());
    put(key+".size" ,value.getSize() );

    // Done
  }

  /**
   * Remembers a point value
   */
  public void put(String key, Point value) {

    // Remember box dimension
    put(key+".x",value.x);
    put(key+".y",value.y);

    // Done
  }


  /**
   * Remembers a point value
   */
  public void put(String key, Point2D value) {

    // Remember box dimension
    put(key+".x",(float)value.getX());
    put(key+".y",(float)value.getY());

    // Done
  }

  /**
   * Remembers a rectangle value
   */
  public void put(String key, Rectangle value) {

    // Remember box dimension
    put(key+".x",value.x);
    put(key+".y",value.y);
    put(key+".w",value.width);
    put(key+".h",value.height);

    // Done
  }

  /**
   * Remembers a collection of Strings
   */
  public void put(String key, Collection values) {

    // Remember
    int l = values.size();
    put(key,l);
    
    Iterator elements = values.iterator();
    for (int i=0;elements.hasNext();i++) {
      put(key+"."+(i+1),elements.next().toString());
    }

    // Done
  }

  /**
   * Remembers a boolean value
   */
  public void put(String key, boolean value) {

    // Remember
    put(key,(value?"1":"0"));

    // Done
  }
  
  /**
   * Remembers a boolean value
   */
  public void put(String key, Color value) {

    // Remember
    put(key,value.getRGB());

    // Done
  }
  
  /**
   * Calculates a filename for given registry name
   */
  private static File getFile(String name) {
    
    name = name+".properties";
    
    String dir = EnvironmentChecker.getProperty(
      Registry.class,
      new String[]{ "user.home.genj" },
      ".",
      "calculate dir for registry file "+name
    );
    
    return new File(dir,name);
  }

  /**
   * Save registries
   */
  public static void persist() {
    
    // Go through registries
    for (String pkg : registries.keySet()) {
      // Open known file
      try {
        Registry registry = registries.get(pkg);
        File file = getFile(pkg);
        LOG.fine("Storing registry in file "+file.getAbsolutePath());
        file.getParentFile().mkdirs();
        FileOutputStream out = new FileOutputStream(file);
        registry.properties.store(out,pkg);
        out.flush();
        out.close();
      } catch (IOException ex) {
      }

    }

    // Done
  }

  /**
   * store JFrame characteristics
   */
  public void put(String key, JFrame frame) {
    Rectangle bounds = frame.getBounds();
    boolean maximized = frame.getExtendedState()==JFrame.MAXIMIZED_BOTH;
    if (bounds!=null&&!maximized)
      put(key, bounds);
    put(key+".maximized", maximized);
  }
  
  public JFrame get(String key, JFrame frame) {
    
    frame.setBounds(get(key, new Rectangle(0,0,640,480)));
    if (get(key+".maximized", true))
      frame.setExtendedState(Frame.MAXIMIZED_BOTH);
    
    return frame;
  }

} //Registry
