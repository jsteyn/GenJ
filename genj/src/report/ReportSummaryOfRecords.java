/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
import genj.fo.Document;
import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import genj.gedcom.MultiLineProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyFile;
import genj.gedcom.PropertyName;
import genj.gedcom.PropertyXRef;
import genj.gedcom.TagPath;
import genj.report.Report;

import javax.swing.ImageIcon;

/**
 * GenJ - Report
 * @author Nils Meier <nils@meiers.net>
 * @version 1.0
 */
public class ReportSummaryOfRecords extends Report {
  
  /**
   * We accept a gedcom file as argument 
   */
  public String accepts(Object context) {
    if (!(context instanceof Gedcom))
      return null;
    Gedcom ged = (Gedcom)context;
    return i18n("title", ged.getName());
  }
  
  /**
   * Overriden image - we're using the provided FO image 
   */
  protected ImageIcon getImage() {
    return Report.IMG_FO;
  }

  /**
   * While we generate information on stdout it's not really
   * necessary because we're returning a Document
   * that is handled by the UI anyways
   */
  public boolean usesStandardOut() {
    return false;
  }

  /**
   * The report's entry point
   */
  public void start(Object context) {
    
    // assuming Gedcom
    Gedcom gedcom = (Gedcom)context;
    
    // create a document
    Document doc = new Document(i18n("title", gedcom.getName()));
    
    doc.addText("This report shows information about all records in the Gedcom file "+gedcom.getName());

    // Loop through individuals & families
    exportEntities(gedcom.getEntities(Gedcom.INDI, "INDI:NAME"), doc);
    exportEntities(gedcom.getEntities(Gedcom.FAM, "FAM"),doc);
    
    // add index
    doc.addIndex("names", "Name Index");
    
    // Done
    showDocumentToUser(doc);
    
  }

  /**
   * Exports the given entities 
   */
  private void exportEntities(Entity[] ents, Document doc)  {
    for (int e = 0; e < ents.length; e++) {
      exportEntity(ents[e], doc);
    }
  }
  
  /**
   * Exports the given entity 
   */
  private void exportEntity(Entity ent, Document doc) {

    println(i18n("exporting", ent.toString() ));
      
    // start a new section
    doc.addSection( ent.toString() );
    
    // mark it
    doc.addAnchor(ent);
    if (ent instanceof Indi) {
      Indi indi = (Indi)ent;
      doc.addIndexTerm("names", indi.getLastName(), indi.getFirstName());
    }
    
    // start a paragraph
    doc.addParagraph();

    // add image
    PropertyFile file = (PropertyFile)ent.getProperty(new TagPath("INDI:OBJE:FILE"));
    if (file!=null)
      doc.addImage(file.getFile(), Document.HALIGN_RIGHT);
    
    // export its properties
    exportProperties(ent, doc);
    
    // end section
    doc.endSection();

    // Done
  }    
  
  /**
   * Exports the given property's properties
   */
  private void exportProperties(Property of, Document doc) {

    // anything to do?
    if (of.getNoOfProperties()==0)
      return;
    
    // create a list
    doc.addList();

    // an item per property
    for (int i=0;i<of.getNoOfProperties();i++) {
      
      Property prop = of.getProperty(i);

      // we don't do anything for xrefs to non-indi/fam
      if (prop instanceof PropertyXRef) {
        PropertyXRef xref = (PropertyXRef)prop;
        if (!(xref.getTargetEntity() instanceof Indi||xref.getTargetEntity() instanceof Fam))
          continue;
      }

      // here comes the item
      doc.addListItem();
      doc.addText(Gedcom.getName(prop.getTag()), Document.TEXT_EMPHASIZED);
      doc.addText(" ");
      
      // with its value
      exportPropertyValue(prop, doc);

      // recurse into it
      exportProperties(prop, doc);
    }
    doc.endList();
  }

  /**
   * Exports the given property's value
   */
  private void exportPropertyValue(Property prop, Document doc) {

    // check for links to other indi/fams
    if (prop instanceof PropertyXRef) {
      
      PropertyXRef xref = (PropertyXRef)prop;
      doc.addLink(xref.getTargetEntity());
      
      // done
      return;
    } 

    // multiline needs loop
    if (prop instanceof MultiLineProperty) {
      MultiLineProperty.Iterator lines = ((MultiLineProperty)prop).getLineIterator();
      do {
        doc.addText(lines.getValue());
      } while (lines.next());
      // done
      return;
    }

    // patch for NAME
    String value;    
    if (prop instanceof PropertyName)
      value = ((PropertyName)prop).getName();
    else
      value = prop.getDisplayValue();

    doc.addText(value);
      
    // done
  }

//  /**
//   * Exports index.html row
//   */
//  private void exportIndexRow(PrintWriter out, Indi indi) throws IOException {
//
//    // Standard
//    printCell(out,wrapID(indi));
//
//    printCell(out,indi.getLastName() );
//    printCell(out,indi.getFirstName());
//
//    printCell(out,indi.getProperty("SEX",true));
//    printCell(out,indi.getProperty(new TagPath("INDI:BIRT:DATE")));
//    printCell(out,indi.getProperty(new TagPath("INDI:BIRT:PLAC")));
//    printCell(out,indi.getProperty(new TagPath("INDI:DEAT:DATE")));
//    printCell(out,indi.getProperty(new TagPath("INDI:DEAT:PLAC")));
//
//    // done
//  }
//  
//  /** 
//   * Exports index.html
//   */
//  private void exportIndex(Gedcom gedcom, File dir) throws IOException {
//
//    File file = getFileForIndex(dir);
//    println(i18n("exporting", new String[]{ file.getName(), dir.toString() }));
//    PrintWriter out = getWriter(new FileOutputStream(file));
//
//    // HEAD
//    printOpenHTML(out, gedcom.getName());
//
//    // TABLE
//    out.println("<table border=1 cellspacing=1>");
//
//    // TABLE HEADER
//    out.println("<tr class=header>"); 
//    printCell(out, "ID");
//    printCell(out, PropertyName.getLabelForLastName());
//    printCell(out, PropertyName.getLabelForFirstName());
//    printCell(out, PropertySex.TXT_SEX);
//    printCell(out, Gedcom.getName("BIRT"));
//    printCell(out, Gedcom.getName("PLAC"));
//    printCell(out, Gedcom.getName("DEAT"));
//    printCell(out, Gedcom.getName("PLAC"));
//    out.println("</tr>");  //F. Massonneau 03/04/2002
//
//    // Go through individuals
//    Entity[] indis = gedcom.getEntities(Gedcom.INDI, "INDI:NAME");
//    for (int i=0;i<indis.length;i++) {
//      out.println("<tr>");
//      exportIndexRow(out, (Indi)indis[i]);
//      out.println("</tr>");
//      // .. next individual
//    }
//
//    // END TABLE
//    out.println("</table>");
//
//    // TAIL
//    printCloseHTML(out);
//
//    // done
//    out.close();
//    
//  }

} //ReportHTMLSheets