/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2010 Nils Meier <nils@meiers.net>
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
package export;

import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.Property;
import genj.gedcom.time.PointInTime;
import genj.io.Filter;
import genj.util.Registry;
import genj.util.Resources;
import genj.util.swing.DateWidget;
import genj.util.swing.NestedBlockLayout;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * A data-based filter
 */
/*package*/ class DataFilter extends JPanel implements Filter {
  
  private final static Registry REGISTRY = Registry.get(DataFilter.class);
  private final static Resources RESOURCES = Resources.get(DataFilter.class);
  private final static String LAYOUT = 
    "<table pad=\"8\">"+
      "<row><even/><event wx=\"1\"/></row>"+
      "<row><born/><born wx=\"1\"/></row>"+
      "<row><living cols=\"2\"/></row>"+
      "<row><notes/></row>"+
      "<row><medias/></row>"+
      "<row><sources/></row>"+
    "</table>";

  private DateWidget eventsBefore, bornBefore;
  private JCheckBox living;
  private JCheckBox notes = new JCheckBox(Gedcom.getName("NOTE", true), true);
  private JCheckBox medias = new JCheckBox(Gedcom.getName("OBJE", true), true);
  private JCheckBox sources = new JCheckBox(Gedcom.getName("SOUR", true), true);
  
  DataFilter(Gedcom gedcom) {
    
    super(new NestedBlockLayout(LAYOUT));
    
    PointInTime tomorrow = PointInTime.getNow().add(1,0,0);
    eventsBefore = new DateWidget(tomorrow);
    bornBefore = new DateWidget(tomorrow);
    living = new JCheckBox(RESOURCES.getString("data.living"), true);

    add(new JLabel(RESOURCES.getString("data.even")));
    add(eventsBefore);
    add(new JLabel(RESOURCES.getString("data.born")));
    add(bornBefore);
    add(living);

    add(notes);
    add(medias);
    add(sources);
  }
  
  public String getName() {
    return RESOURCES.getString("data");
  }
  
  @Override
  public boolean veto(Property property) {
    return false;
  }

  @Override
  public boolean veto(Entity entity) {
    return false;
  }

}
