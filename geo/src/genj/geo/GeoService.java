/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2005 Nils Meier <nils@meiers.net>
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
package genj.geo;

import genj.util.EnvironmentChecker;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A service for geographical computations / data services. It keeps well known
 * locations in files in ./geo with format
 * <pre>
 *   PLACE \t STATE \t COUNTRY \t LAT \t LON
 * </pre>
 */
public class GeoService {
  
  private final static Logger LOG = Logger.getLogger("genj.geo");
  
  /** our work directory */
  private static final String GEO_DIR = "./geo";

  /** our sqls */
  /*package*/ static final String 
    CREATE_TABLES =
      "CREATE CACHED TABLE countries (country CHAR(2) PRIMARY KEY); " +
      "CREATE CACHED TABLE locations (city VARCHAR(32), state CHAR(2), country CHAR(2) NOT NULL, lat FLOAT, lon FLOAT); "+
      "CREATE INDEX cities ON locations (city)",
    DELETE_LOCATIONS = "DELETE FROM locations WHERE country = ?",
    DELETE_LOCATIONS2 = "DELETE FROM locations WHERE country = ? AND state = ?",
    INSERT_COUNTRY = "INSERT INTO countries (country) VALUES (?)",
    DELETE_COUNTRY = "DELETE FROM countries WHERE country = ?",
    INSERT_LOCATION = "INSERT INTO locations (city, state, country, lat, lon) VALUES (?, ?, ?, ?, ?)",
    SELECT_COUNTRIES = "SELECT country FROM countries",
    SELECT_LOCATIONS = "SELECT city, state, country, lat, lon FROM locations WHERE city = ?",
    QUERY_LOCATIONS = "SELECT city, state, country, lat, lon FROM locations WHERE city LIKE ?";

  /*package*/ static final int
    DELETE_LOCATIONS_COUNTRY = 1,
    DELETE_LOCATIONS_STATE = 2,
    
    INSERT_COUNTRY_COUNTRY = 1,
    DELETE_COUNTRY_COUNTRY = 1,
    
    INSERT_LOCATION_CITY = 1,
    INSERT_LOCATION_STATE = 2,
    INSERT_LOCATION_COUNTRY = 3,
    INSERT_LOCATION_LAT = 4,
    INSERT_LOCATION_LON = 5,
    
    SELECT_COUNTRIES_OUT_COUNTRY = 1,
    
    SELECT_LOCATIONS_IN_CITY = 1,
    SELECT_LOCATIONS_OUT_CITY = 1,
    SELECT_LOCATIONS_OUT_STATE = 2,
    SELECT_LOCATIONS_OUT_COUNTRY = 3,
    SELECT_LOCATIONS_OUT_LAT = 4,
    SELECT_LOCATIONS_OUT_LON = 5;

  /** directories */
  private File localDir, globalDir;

  /** singleton */
  private static GeoService instance;
  
  /** maps */
  private List maps;
  
  /** database ready */
  private Connection connection;
  
  /** listeners */
  private List listeners = new ArrayList();
  
  /**
   * Constructor
   */
  private GeoService() {
    
    // startup now
    startup();
    
    // prepare database shutdown
    Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));

    // done
  }
  
  /**
   * our startup routine
   */
  private synchronized void startup() { 
    
    // startup database - try to place into all.home.genj if possible, user.home.genj otherwise
    File geo =  new File(EnvironmentChecker.getProperty(this, new String[]{ "all.home.genj/geo", "user.home.genj/geo"} , "", "looking for user's geo directory"));
    geo.mkdir();
  
    try {
      
      LOG.info("GeoService Startup");
      
      // initialize database
      Class.forName("org.hsqldb.jdbcDriver");
  
      // connect to the database.   
      Properties props = new Properties();
      props.setProperty("user", "sa");
      props.setProperty("password", "");
      props.setProperty("hsqldb.cache_scale", "8");  // less rows 3*2^x
      props.setProperty("hsqldb.cache_size_scale", "7"); // less size per row 2^x
      props.setProperty("sql.compare_in_locale", "0"); // Collator strength PRIMARY
      
      connection = DriverManager.getConnection("jdbc:hsqldb:file:"+geo.getAbsolutePath() + "/database" , props); 
      connection.setAutoCommit(true);
  
      // create tables
      try {
        connection.createStatement().executeUpdate(CREATE_TABLES);
      } catch (SQLException e) {
        // ignored
      }
      
    } catch (Throwable t) {
      LOG.log(Level.SEVERE, "Couldn't initialize database", t);
    }
  } 
  
  /**
   * our shutdown
   */
  private class Shutdown implements Runnable {
      public void run() { synchronized(GeoService.this) {
          LOG.info("GeoService Shutdown");
          try {
            connection.createStatement().execute("SHUTDOWN");
          } catch (SQLException e) {
            // ignored
          }
      } }
  } //Shutdown
  
  /**
   * Test
   */
  public static void main(String[] args) {
    
//    Country[] countries = getInstance().getCountries();
//    for (int i=0;i<countries.length;i++)
//      System.out.println(countries[i]);
    
//    Indi indi = new Indi();
//    Property birt = indi.addProperty("BIRT", "");
//    birt.addProperty("PLAC", "Nantes");
//    
//    GeoLocation loc  = new GeoLocation(birt);
//    getInstance().match(loc);
//    System.out.println(loc);
    
    String city = "%eloi";
    GeoService gs = getInstance();
    try {
      
      PreparedStatement ps = gs.connection.prepareStatement("SELECT city, state, country, lat, lon FROM locations WHERE city LIKE ?");
      ps.setString(1, city);
      ResultSet result = ps.executeQuery();
      while (result.next()) {
        System.out.println( result.getString(1)  +","+ result.getString(2)+","+ result.getString(3)+","+ result.getString(4)+","+ result.getString(5));
      }
      
    } catch (Throwable t) {
      t.printStackTrace();
    }
    
  }
  
  /**
   * Singleton acces
   */
  public static GeoService getInstance() {
    if (instance==null) {
      synchronized (GeoService.class) {
        if (instance==null) 
          instance = new GeoService();
      }
    }
    return instance;
  }
  
  /**
   * Return the database connection
   */
  /*package*/ Connection getConnection() {
    return connection;
  }
  
  /**
   * Lookup Files in Geo directories
   */
  /*package*/ File[] getGeoFiles() {
    
    List result = new ArrayList();
    
    String[] dirs  = {
        EnvironmentChecker.getProperty(this, "user.home.genj/geo", "", "looking for user's geo files"),
        EnvironmentChecker.getProperty(this, "all.home.genj/geo", "", "looking for shared geo files"),
        EnvironmentChecker.getProperty(this, "genj.geo.dir", GEO_DIR, "looking for installed geo files")
    };
    
    // loop directories
    for (int i=0;i<dirs.length;i++) {
      File dir = new File(dirs[i]);
      if (dir.isDirectory()) {
        result.addAll(Arrays.asList(dir.listFiles()));
      }
    }

    // done
    return (File[])result.toArray(new File[result.size()]);
  }

  /**
   * Let listeners know about change in data
   */
  /*package*/ synchronized void fireGeoDataChanged() {
    Listener[] ls = (Listener[])listeners.toArray(new Listener[listeners.size()]);
    for (int i = 0; i < ls.length; i++) {
      ls[i].handleGeoDataChange();
    }
  }
  
  /**
   * Listener registration
   */
  public void addListener(Listener l) {
    listeners.add(l);
  }
  
  public void removeListener(Listener l) {
    listeners.remove(l);
  }
  
  /**
   * Drop information for given country 
   */
  public synchronized void drop(Country country) throws IOException {
    
    try {
      PreparedStatement delete = connection.prepareStatement(DELETE_LOCATIONS);
      delete.setString(DELETE_LOCATIONS_COUNTRY, country.getCode());
      delete.executeUpdate();
      
      delete = connection.prepareStatement(DELETE_COUNTRY);
      delete.setString(DELETE_COUNTRY_COUNTRY, country.getCode());
      delete.executeUpdate();
      
    } catch (SQLException e) {
      throw new IOException(e.getMessage());
    }
    
    // done
  }
  
  /**
   * Prepare an import 
   */
  public Import getImport(Country country, String state) throws IOException {
    // look it up
    try {
      return Import.get(country, state);
    } catch (SQLException e) {
      throw new IOException("error preparing import ["+e.getMessage()+"]");
    }
  }

  /**
   * Return all available countries
   */
  public synchronized Country[] getCountries() {

    List countries = new ArrayList();
    
    try {
      ResultSet rows = connection.prepareStatement(SELECT_COUNTRIES).executeQuery();
      while (rows.next()) 
        countries.add(Country.get(rows.getString(SELECT_COUNTRIES_OUT_COUNTRY)));
      
    } catch (Throwable t) {
      LOG.log(Level.SEVERE, "unexpected throwable", t);
    }
    
    // done
    return (Country[])countries.toArray(new Country[countries.size()]);
  }
  
  /**
   * Find all matching locations for given location
   */
  public GeoLocation[] query(GeoLocation location) {
    
    String city = location.getCity() + "%";
    Jurisdiction jurisdiction = location.getJurisdiction();
    Country country = location.getCountry();
   
    // try to find 
    List result = new ArrayList();
    synchronized (this) {
      
      try {
        
        // prepare select
        PreparedStatement select = connection.prepareStatement(QUERY_LOCATIONS);
        select.setString(SELECT_LOCATIONS_IN_CITY, city);

        // loop over rows
        ResultSet rows = select.executeQuery();
        while (rows.next()) {
          // grab city as in database
          String foundCity = rows.getString(SELECT_LOCATIONS_OUT_CITY);
          // .. country known and no match -> don't consider
          Country foundCountry = Country.get(rows.getString(SELECT_LOCATIONS_OUT_COUNTRY));
          if (country!=null&&!country.equals(foundCountry))
            continue;
          // .. jurisdiction known and no match -> don't consider
          Jurisdiction foundJurisdiction = Jurisdiction.get(foundCountry, rows.getString(SELECT_LOCATIONS_OUT_STATE));
          if (jurisdiction!=null&&!jurisdiction.equals(foundJurisdiction)) 
            continue;
          // grab it
          GeoLocation loc = new GeoLocation(foundCity, foundJurisdiction, foundCountry);
          loc.set(rows.getDouble(SELECT_LOCATIONS_OUT_LAT), rows.getDouble(SELECT_LOCATIONS_OUT_LON), 1);
          result.add(loc);
          // next 
        }
      } catch (Throwable t) {
        LOG.log(Level.SEVERE, "throwable while trying to match "+location, t);
      }
    }
    
    // done
    return (GeoLocation[])result.toArray(new GeoLocation[result.size()]);
    
  }
  
  /**
   * Match given location
   */
  public Match match(GeoLocation location) {

    String city = location.getCity();
    Jurisdiction jurisdiction = location.getJurisdiction();
    Country country = location.getCountry();
   
    // try to find 
    double lat = Double.NaN, lon = Double.NaN;
    int matches = 0;
    synchronized (this) {
      try {
        
        // prepare select
        PreparedStatement select = connection.prepareStatement(SELECT_LOCATIONS);
        select.setString(SELECT_LOCATIONS_IN_CITY, city);

        // loop over rows
        int highscore = 0;
        ResultSet rows = select.executeQuery();
        while (rows.next()) {
          // compute a score
          int score = 1;
          // .. country known and no match -> don't consider
          if (country!=null) {
            if (!country.getCode().equalsIgnoreCase(rows.getString(SELECT_LOCATIONS_OUT_COUNTRY)))
              continue;
            score += 1;
          }
          // .. jurisdiction known and no match -> don't consider
          if (jurisdiction!=null) {
            if (!jurisdiction.getCode().equalsIgnoreCase(rows.getString(SELECT_LOCATIONS_OUT_STATE)))
              continue;
            score += 1;
          }
          // grab lat/lon
          if (score==highscore) matches ++;
          else if (score>highscore) {
            matches = 1;
            highscore = score;
            lat = rows.getDouble(SELECT_LOCATIONS_OUT_LAT);
            lon = rows.getDouble(SELECT_LOCATIONS_OUT_LON);
          }
          // next match
        }
      } catch (Throwable t) {
        LOG.log(Level.SEVERE, "throwable while trying to match "+location, t);
      }
    }
    
    // done
    return new Match(location, lat, lon, matches);
    
  }
  
  /**
   * Available Maps
   */
  public synchronized GeoMap[] getMaps() {
    
    // know all maps already?
    if (maps==null) {
      
      maps = new ArrayList();
      
      // loop over files 
      File[] files = getGeoFiles();
      for (int i=0;i<files.length;i++) {
        // only directories - later zip files as well
        if (!files[i].isDirectory())
          continue;
        // 20050504 don't consider directory 'CVS'
        if (files[i].getName().equals("CVS"))
          continue;
        // add it to available maps
        try {
          maps.add(new GeoMap(files[i]));
        } catch (Throwable t) {
          LOG.log(Level.SEVERE, "problem reading map from "+files[i], t);
        }
        // next
      }
      
      // finished looking for maps
    }
    
    // done
    return (GeoMap[])maps.toArray(new GeoMap[maps.size()]);
  }
  
  /**
   * A match
   */
  public class Match implements Runnable {
    private GeoLocation location;
    private double lat,lon;
    private int matches;
    private Match(GeoLocation location, double lat, double lon, int matches) { 
      this.location = location; this.lat=lat; this.lon=lon; this.matches=matches; 
    }
    public double getLatitude() {
      return lat;
    }
    public double getLongitude() {
      return lon;
    }
    public void run() {
      location.set(lat, lon, matches);
    }
  }
  
  /**
   * A (crude) Listener
   */
  public interface Listener {
    public void handleGeoDataChange();
  }
    
} //GeoService
