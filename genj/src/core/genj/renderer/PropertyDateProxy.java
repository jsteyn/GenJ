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
 */
package genj.renderer;

import genj.gedcom.Property;
import genj.gedcom.PropertyDate;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

/**
 * 
 */
public class PropertyDateProxy extends PropertyProxy {

  /**
   * @see genj.renderer.PropertyProxy#getSize(FontMetrics, Property, boolean, boolean)
   */
  public Dimension getSize(FontMetrics metrics, Property prop, boolean isText, boolean isImage) {
    return super.getSize(metrics, prop.getImage(false), getDate(prop), isText, isImage);
  }

  /**
   * @see genj.renderer.PropertyProxy#render(Graphics, FontMetrics, Rectangle, Property, boolean, boolean)
   */
  public void render( Graphics g, FontMetrics metrics, Rectangle bounds, Property prop, boolean isText, boolean isImage) {
    super.render(g, metrics, bounds, prop.getImage(false), getDate(prop), isText, isImage);
  }

  /**
   * Helper to get the date of PropertyDate
   */
  private String getDate(Property prop) {
    if (!(prop instanceof PropertyDate)||!prop.isValid()) 
      return prop.getValue();
    PropertyDate date = (PropertyDate)prop;
    return date.toString(false, true);
  }  

} //PropertyDateProxy
