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
package genj.gedcom;

import java.util.Date;

/**
 * Class for encapsulating a person
 */
public class Indi extends PropertyIndi implements Entity {

  private String id = "";
  private Gedcom gedcom;

  /**
   * Constructor for Individual
   */
  /*package*/ Indi(Gedcom gedcom) {

    // Call super's constructor
    super();

    // Entity
    this.gedcom = gedcom;
  }

  /**
   * Adds a birth information to an individual
   */
  /*package*/ void addBirth(String value, String place) {
    // BIRTH
    Property p = new PropertyEvent("BIRT");
    addProperty(p);
    PropertyDate date = new PropertyDate();
    date.setValue(value);
    p.addProperty(date);
    if (place!=null) {
      p.addProperty(new PropertyPlace(place));
    }
  }

  /**
   * Adds a family in which the individual is a partner
   */
  /*package*/ Indi addFam(Fam fam) throws GedcomException {

    // Remember Fam where this is spouse in
    PropertyFamilySpouse pfs = new PropertyFamilySpouse("",fam.getId());
    addProperty(pfs);

    // Link !
    try {
      pfs.link();
    } catch (GedcomException ex) {
      delProperty(pfs);
    }

    return this;
  }

  /**
   * Notification to entity that it has been added to a Gedcom
   */
  public void addNotify(Gedcom gedcom) {
    this.gedcom = gedcom;
  }

  /**
   * Deletes a family in which the person was a partner
   */
  /*package*/ Indi delFam(int which ) {
    Property[] fams = getProperties(new TagPath("INDI:FAMS"),true);
    if (which > fams.length)
      throw new IllegalArgumentException("Individual isn't spouse in "+which+" families");
    delProperty(fams[which-1]);
    return this;
  }

  /**
   * Deletes the family in which the Individual was child
   */
  /*package*/ Indi delFamc() {
    Property prop = getProperty(new TagPath("INDI:FAMC"),true);
    if (prop==null) {
      return this;
    }
    delProperty(prop);
    return this;
  }

  /**
   * Notification to entity that it has been deleted from a Gedcom
   */
  public void delNotify() {

    // Notify to properties
    super.delNotify();

    // Break connection
    this.gedcom = null;
  }

  /**
   * Calculate indi's birth date
   */
  public PropertyDate getBirth() {

    // Calculate BIRT|DATE
    PropertyDate p = (PropertyDate)getProperty(new TagPath("INDI:BIRT:DATE"),true);
    if (p==null) {
      return null;
    }

    // Return string value
    return p;
  }

  /**
   * Calculate indi's birth date
   */
  public String getBirthAsString() {

    PropertyDate p = getBirth();
    if (p==null) {
      return "";
    }

    // Return string value
    return p.toString();
  }

  /**
   * Returns the selected family in which the individual is a partner
   */
  public Fam getFam(int which) {
    Property[] props = getProperties(new TagPath("INDI:FAMS"),true);
    if (which>=props.length) {
      return null;
    }
    return ((PropertyFamilySpouse)props[which]).getFamily();
  }

  /**
   * Returns the family in which the person is child
   */
  public Fam getFamc( ) {
    Property prop = getProperty(new TagPath("INDI:FAMC"),true);
    if (prop==null) {
      return null;
    }
    return ((PropertyFamilyChild)prop).getFamily();
  }

  /**
   * Returns indi's first name
   */
  public String getFirstName() {

    // Calculate NAME
    PropertyName p = (PropertyName)getProperty(new TagPath("INDI:NAME"),true);
    if (p==null) {
      return "";
    }

    // Return string value
    return p.getFirstName();
  }

  /**
   * Gedcom this entity's in
   * @return containing Gedcom
   */
  public Gedcom getGedcom() {
    return gedcom;
  }

  /**
   * This individual's id
   */
  public String getId() {
    return id;
  }

  /**
   * Calculate indi's last name
   */
  public String getLastName() {

    // Calculate NAME
    PropertyName p = (PropertyName)getProperty(new TagPath("INDI:NAME"),true);
    if (p==null) {
      return "";
    }

    // Return string value
    return p.getLastName();
  }

  /**
   * Returns indi's name
   */
  public String getName() {

    // Calculate NAME
    PropertyName p = (PropertyName)getProperty(new TagPath("INDI:NAME"),true);
    if (p==null) {
      return "";
    }

    // Return string value
    return p.getName();
  }

  /**
   * Returns the number of families in which the individual is a partner
   */
  public int getNoOfFams( ) {
    Property[] props = getProperties(new TagPath("INDI:FAMS"),true);
    return props.length;
  }

  /**
   * This individual's root property (self)
   */
  public Property getProperty() {
    return this;
  }

  /**
   * Returns indi's sex
   */
  public int getSex() {

    // Calculate SEX
    PropertySex p = (PropertySex)getProperty(new TagPath("INDI:SEX"),true);
    if (p==null) {
      return 0;
    }

    // Return value
    return p.getSex();
  }

  /**
   * Returns the type to which this entity belongs
   * INDIVIDUALS, FAMILIES, MULTIMEDIAS, NOTES, ...
   */
  public int getType() {
    return Gedcom.INDIVIDUALS;
  }

  /**
   * Checks wether this individual is descendant of individual
   */
  /*package*/ boolean isDescendantOf(Indi indi) {

    // Me ?
    if (this==indi) {
      return true;
    }

    // Childhood ?
    Fam fam = getFamc();
    if (fam==null) {
      return false;
    }

    // Recursive call
    return fam.isDescendantOf(indi);
  }

  /**
   * Sets the family in which the person is child
   */
  /*package*/ Indi setFamc(Fam fam) throws GedcomException {

    // Remove old
    Property p = getProperty(new TagPath("INDI:FAMC"),true);
    if (p!=null) {
      delProperty(p);
    }

    // Remember new Fam where this is child in
    PropertyFamilyChild pfc = new PropertyFamilyChild("",fam.getId());
    addProperty(fam);

    // Link !
    try {
      pfc.link();
    } catch (GedcomException ex) {
      delProperty(pfc);
    }

    return this;
  }

  /**
   * Set Gedcom this entity's in
   */
  public void setGedcom(Gedcom gedcom) {
    this.gedcom=gedcom;
  }

  /**
   * Sets entity's id.
   * @param id new id
   */
  public void setId(String id) {

    this.id=id;

    // Done
  }

  /**
   * Returns this entity as String description
   */
  public String toString() {

    String result = getId()+":"+getName();

    return result;
  }
}
