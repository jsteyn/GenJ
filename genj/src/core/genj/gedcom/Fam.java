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

import java.util.HashSet;
import java.util.Set;

import genj.util.WordBuffer;

/**
 * Class for encapsulating a family with parents and children
 */
public class Fam extends Entity {
  
  private final static TagPath
    PATH_FAMHUSB = new TagPath("FAM:HUSB"),
    PATH_FAMWIFE = new TagPath("FAM:WIFE"),
    PATH_FAMMARRDATE = new TagPath("FAM:MARR:DATE"),
    PATH_FAMDIVDATE = new TagPath("FAM:DIV:DATE");

  /**
   * Adds another child to the family
   */
  Fam addChild(Indi newChild) throws GedcomException {

    // Remember Indi who is child
    PropertyChild pc = new PropertyChild(newChild.getId());
    addProperty(pc);

    // Link !
    try {
      pc.link();
    } catch (GedcomException ex) {
      delProperty(pc);
      throw ex;
    }

    return this;
  }

  /**
   * Returns child #i
   */
  public Indi getChild(int which) {
    Property[] chils = getProperties("CHIL",QUERY_VALID_TRUE);
    if (which > chils.length) {
      throw new IllegalArgumentException("Family doesn't have "+which+" children");
    }
    return ((PropertyChild)chils[which]).getChild();
  }

  /**
   * Returns children
   */
  public Indi[] getChildren() {
    Property chils[] = getProperties("CHIL", QUERY_VALID_TRUE);
    Indi result[] = new Indi[chils.length];
    for (int i=0;i<result.length;i++) {
      result[i] = ((PropertyChild)chils[i]).getChild();
    }
    return result;
  }

  /**
   * Returns the husband of the family
   */
  public Indi getHusband() {
    Property husb = getProperty("HUSB", true);
    if (husb instanceof PropertyHusband)
      return ((PropertyHusband)husb).getHusband();
    return null;    
  }

  /**
   * The number of children
   */
  public int getNoOfChildren() {
    Property[] chils = getProperties("CHIL", QUERY_VALID_TRUE);
    return chils.length;
  }
  
  /**
   * The number of spouses
   */
  public int getNoOfSpouses() {
    int result = 0;
    if (getHusband()!=null) result++;
    if (getWife   ()!=null) result++;
    return result;
  } 

  /**
   * Returns the other parent to the given one
   */
  public Indi getOtherSpouse(Indi spouse) {
    Indi wife = getWife();
    if (wife==spouse) return getHusband();
    return wife;
  }

  /**
   * Returns the wife of the family
   */
  public Indi getWife() {
    
    Property wife = getProperty("WIFE", true);
    if (wife instanceof PropertyWife) 
      return ((PropertyWife)wife).getWife();
    return null;
  }

  /**
   * Calculate a set of ancestors of this family
   * (husband + wife and their ancestors) 
   */
  public Set getAncestors() {
    return getAncestors(new HashSet());
  }
  
  /*package*/ Set getAncestors(Set collect) {
    
    // Husband ?
    Indi husband = getHusband();
    if (husband!=null&&!collect.contains(husband)) {
      collect.add(husband); 
      husband.getAncestors(collect);
    }
  
    // Wife ?
    Indi wife = getWife();
    if (wife!=null&&!collect.contains(wife)) {
      collect.add(wife); 
      wife.getAncestors(collect);
    }
      
    // done
    return collect;
  }
  
  /**
   * Calculate a set of descendants of this family
   * (children and their descendants) 
   */
  public Set getDescendants() {
    return getDescendants(new HashSet());
  }
  
  /*package*/ Set getDescendants(Set collect) {
    
    // children?
    Indi[] children = getChildren();
    for (int c=0; c<children.length; c++) {
      Indi child = children[c];
      if (collect.contains(child)) continue;
      collect.add(child);
      child.getDescendants(collect);    	
    }
    
    // done
    return collect;
  }

  /**
   * Sets the husband of this family
   */
  private Indi setHusband(Indi husband) throws GedcomException {
    
    // Remove old husband
    PropertyHusband ph = (PropertyHusband)getProperty(PATH_FAMHUSB,QUERY_VALID_TRUE);
    if (ph!=null) 
      delProperty(ph);
      
    // done?
    if (husband==null)
      return null;
    
    // Add new husband
    ph = new PropertyHusband(husband.getId());
    addProperty(ph);

    // Link !
    try {
      ph.link();
    } catch (GedcomException ex) {
      delProperty(ph);
      throw ex;
    }
    
    // check sex of husband
    if (husband.getSex()!=PropertySex.MALE)
      husband.setSex(PropertySex.MALE);

    // done    
    return husband;
  }

  /**
   * Sets the wife of the family
   */
  private Indi setWife(Indi wife) throws GedcomException {

    // Remove old wife
    PropertyWife pw = (PropertyWife)getProperty(PATH_FAMWIFE,QUERY_VALID_TRUE);
    if (pw!=null) 
      delProperty(pw);

    // done?
    if (wife==null)
      return null;
    
    // Add new wife
    pw = new PropertyWife(wife.getId());
    addProperty(pw);

    // Link !
    try {
      pw.link();
    } catch (GedcomException ex) {
      delProperty(pw);
      throw ex;
    }

    // check sex of wife
    if (wife.getSex()!=PropertySex.FEMALE)
      wife.setSex(PropertySex.FEMALE);

    // Done
    return wife;
  }

  /**
   * Sets one of the spouses
   */
  /*package*/ void setSpouse(Indi spouse) throws GedcomException {  
    
    Indi husband = getHusband();
    Indi wife = getWife();
    
    // won't do if husband and wife already known
    if (husband!=null&&wife!=null)
      throw new GedcomException("Family already has two spouses");

    // check gender of spouse 
    switch (spouse.getSex()) {
      default:
      case PropertySex.UNKNOWN:
        // remember new spouse
        if (husband!=null) setWife(spouse);
        else setHusband(spouse);
        // done
        break;
      case PropertySex.MALE:
        // remember new husband
        setHusband(spouse);
        // keep old husband as wife if necessary
        if (husband!=null)
          wife = setWife(husband);
        // done
        break;
      case PropertySex.FEMALE:
        // remember new wife
        setWife(spouse);
        // keep old wife as husband if necessary
        if (wife!=null)
          husband = setHusband(wife);
        // done
        break;
    }
    
    // done
  }
  
  /**
   * Returns this entity as String description
   */
  public String toString() {
    
    WordBuffer wb = new WordBuffer();
    wb.setFiller(Options.getInstance().getTxtMarriageSymbol());

    Indi husband = getHusband();
    if (husband!=null) wb.append(husband.toString());
    
    Indi wife = getWife();
    if (wife!=null) wb.append(wife.toString());

    // Done
    return wb.length()>0 ? wb.toString() : super.toString();
  }
  
  /**
   * Calculate fam's Marriage date
   * @return date or null
   */
  public PropertyDate getMarriageDate() {
    // Calculate MARR|DATE
    return (PropertyDate)getProperty(PATH_FAMMARRDATE,QUERY_VALID_TRUE);
  }

  /**
   * Calculate fam's divorce date
   * @return date or null
   */
  public PropertyDate getDivorceDate() {
    // Calculate DIV|DATE
    return (PropertyDate)getProperty(PATH_FAMDIVDATE,QUERY_VALID_TRUE);
  }

  /**
   * Swap spouses
   */
  public void swapSpouses() throws GedcomException {
    
    Indi 
      husband = getHusband(),
      wife = getWife();

    setWife(null);
    setHusband(null);
      
    if (wife!=null)
      setHusband(wife);
    if (husband!=null)
      setWife(husband);
      
  }
  
} //Fam
