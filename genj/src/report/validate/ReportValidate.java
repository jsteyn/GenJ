/**
 * Reports are Freeware Code Snippets
 *
 * This report is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */
package validate;

import genj.gedcom.Entity;
import genj.gedcom.Gedcom;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.TagPath;
import genj.report.Report;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A report that validates a Gedcom file and displays
 * anomalies and 'standard' compliancy issues
 */
public class ReportValidate extends Report {
  
  /** our options */
  public boolean isEmptyValueValid = true;

  /** options of reports are picked up via field-introspection */
  public int
    maxLife    = 90,
    minAgeMARR = 15,
    maxAgeBAPM =  6,
    minAgeRETI = 45;
    
/** Jerome's check still to be migrated:

 x  1) individuals whose death occurs before their birth
 x  2) individuals who are older then MAX_LIVING_AGE years old when they died
    3) individuals who would be older then MAX_LIVING_AGE years old today
 x  4) individuals who are Christened after the age of MAX_CHRISTENING
 x  5) individuals who are Christened after their death
 x  6) individuals who are Christened before they are born
 x  7) individuals who are burried before they die
    8) individuals who are burried more than MAX_BURRYING_OR_CREM years 
    after they die
 x  9) individuals who are cremated before they die
    10) individuals who are creamted more than MAX_BURRYING_OR_CREM years 
    after they die
 x  11) families containing an individual who marries before before the age 
    of MIN_MARRIAGE_AGE
 x  12) families containing an individual who marries after their death
 o  13) families containing a woman who has given birth before the age of 
    MIN_CHILD_BIRTH
 O  14) families containing a woman who has given birth after the age of 
    MAX_CHILD_BIRTH
 x  15) families containing a woman who has given birth after they have died
 o  16) families containing a man who has fathered a child before the age of 
    MIN_FATHER_AGE
 o  17) families containing a man who has fathered a child (more than 9 months) after they have died
 o  18) check the age difference between husband and wife is not greater than SOME_VALUE.
 o  18) find women who have given birth more than once within 9 months (discounting twins)
 o  19) children that are born before MARR or after DIV/wife has died 
 
    // check all files exist and can be opened
 x  new CheckFiles(gedcom));
    
    //family checks
 o  new FamilyDateChecker(null, marrTag, husbTag, deathTag, MAX_LIVING_AGE, NONE, DateChecker.AFTER));
 o  new FamilyDateChecker(null, marrTag, wifeTag, deathTag, MAX_LIVING_AGE, NONE, DateChecker.AFTER));
 x  new FamilyDateChecker(null, marrTag, husbTag, birthTag, MIN_MARRIAGE_AGE, MAX_LIVING_AGE, DateChecker.BEFORE));
 x  new FamilyDateChecker(null, marrTag, wifeTag, birthTag, MIN_MARRIAGE_AGE, MAX_LIVING_AGE, DateChecker.BEFORE));
  
**/  

  private final static String[] LIFETIME_DATES = {
    "INDI:ADOP:DATE",
    "INDI:ADOP:DATE",
    "INDI:BAPM:DATE",
    "INDI:BAPL:DATE",
    "INDI:BARM:DATE",
    "INDI:BASM:DATE",
    "INDI:BLES:DATE",
    "INDI:CHRA:DATE",
    "INDI:CONF:DATE",
    "INDI:ORDN:DATE",
    "INDI:NATU:DATE",
    "INDI:EMIG:DATE",
    "INDI:IMMI:DATE",
    "INDI:CENS:DATE",
    "INDI:RETI:DATE"
  };

  /**
   * @see genj.report.Report#getAuthor()
   */
  public String getAuthor() {
    return "Nils Meier";
  }
  
  /**
   * @see genj.report.Report#getVersion()
   */
  public String getVersion() {
    return "0.1";
  }

  /**
   * @see genj.report.Report#getName()
   */
  public String getName() {
    return "Validate Gedcom";
  }
  
  /**
   * @see genj.report.Report#getInfo()
   */
  public String getInfo() {
    return "Validates Gedcom file or entity for Gedcom compliancy and anomalies";
  }

  /**
   * @see genj.report.Report#start(java.lang.Object)
   */
  public void start(Object context) {
    
    println("***This Report is not finished yet - work in progress***");
    println(" Still to do:");
    println(" + migrate all of Jerome's anomaly checks");
    println(" + discuss best way to word results");
    flush();
    
    // assuming Gedcom
    Gedcom gedcom = (Gedcom)context;

    // prepare tests
    List tests = createTests();
    
    // intit list of issues
    List issues = new ArrayList();

    // Loop through entities and test 'em
    for (int t=0;t<Gedcom.ENTITIES.length;t++) {
      for (Iterator es=gedcom.getEntities(Gedcom.ENTITIES[t]).iterator();es.hasNext();) {
        Entity e = (Entity)es.next();
        TagPath path = new TagPath(e.getTag());
        test(e, path, MetaProperty.get(path), tests, issues);
      }
    }
    
    // any fixes proposed at all?
    if (issues.isEmpty()) {
      getOptionFromUser("No issues found!", new String[]{"Great"});
      return;
    }
    
    // show fixes
    showItemsToUser("Issues", gedcom, (Issue[])issues.toArray(new Issue[issues.size()]));
    
    // done
  }
  
  /**
   * Test a property (recursively)
   */
  private void test(Property prop, TagPath path, MetaProperty meta, List tests, List issues) {
    // test tests
    for (int i=0, j=tests.size(); i<j; i++) {
      Test tst = (Test)tests.get(i);
      // applicable?
      if (!tst.applies(prop, path))
        continue;
      // test it
      tst.test(prop, path, issues);
      // next
    }
    // recurse into all its properties
    for (int i=0,j=prop.getNoOfProperties();i<j;i++) {
      // for non-system, non-transient children
      Property child = prop.getProperty(i);
      if (child.isTransient()||child.isSystem()) continue;
      // get child tag
      String ctag = child.getTag();
      // check if Gedcom grammar allows it
      if (!meta.allows(ctag)) {
        issues.add(new Issue(path+":"+ctag+" is not Gedcom compliant", MetaProperty.IMG_ERROR, child));
        continue;
      }
      // dive into
      path.add(ctag);
      test(child, path, meta.get(child.getTag(), false), tests, issues);
      path.pop();
      // next child
    }
    // done
  }
  
  /**
   * Create the tests we're using
   */
  private List createTests() {
    
    List result = new ArrayList();

    // ******************** SPECIALIZED TESTS *******************************

    // non-valid properties
    result.add(new TestValid(isEmptyValueValid));
    
    // spouses with wrong gender
    result.add(new TestSpouseGender());

    // non existing files
    result.add(new TestFile());

    // ****************** DATE COMPARISON TESTS *****************************

    // birth after death
    result.add(new TestDate("INDI:BIRT:DATE",TestDate.AFTER  ,"INDI:DEAT:DATE"));
    
    // burial before death
    result.add(new TestDate("INDI:BURI:DATE",TestDate.BEFORE ,"INDI:DEAT:DATE"));
    
    // events before birth
    result.add(new TestDate(LIFETIME_DATES  ,TestDate.BEFORE ,"INDI:BIRT:DATE"));
    
    // events after death
    result.add(new TestDate(LIFETIME_DATES  ,TestDate.AFTER  ,"INDI:DEAT:DATE"));

    // marriage after divorce 
    result.add(new TestDate("FAM:MARR:DATE" ,TestDate.AFTER  ,"FAM:DIV:DATE"));
    
    // marriage after death of husband/wife
    result.add(new TestDate("FAM:MARR:DATE" ,TestDate.AFTER  ,"FAM:HUSB:INDI:BIRT:DATE"));
    result.add(new TestDate("FAM:MARR:DATE" ,TestDate.AFTER  ,"FAM:WIFE:INDI:BIRT:DATE"));

    // childbirth after death of mother
    result.add(new TestDate("FAM:CHIL"      ,"FAM:CHIL:INDI:BIRT:DATE", TestDate.AFTER  ,"FAM:WIFE:INDI:DEAT:DATE"));

    // ************************* AGE TESTS **********************************
    
    // max lifespane
    result.add(new TestAge ("INDI:DEAT:DATE","INDI", TestAge.OVER, maxLife));
    
    // max BAPM age 
    result.add(new TestAge ("INDI:BAPM:DATE","INDI", TestAge.OVER   ,maxAgeBAPM));
    
    // max CHRI age 
    result.add(new TestAge ("INDI:CHRI:DATE","INDI", TestAge.OVER   ,maxAgeBAPM));
    
    // min RETI age
    result.add(new TestAge ("INDI:RETI:DATE","INDI", TestAge.UNDER  ,minAgeRETI));

    // min MARR age of husband, wife
    result.add(new TestAge ("FAM:MARR:DATE" ,"FAM:HUSB:INDI", TestAge.UNDER  ,minAgeMARR));
    result.add(new TestAge ("FAM:MARR:DATE" ,"FAM:WIFE:INDI", TestAge.UNDER  ,minAgeMARR));

    // **********************************************************************
    return result;    
  }

} //ReportValidate