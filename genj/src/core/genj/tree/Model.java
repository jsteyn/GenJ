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
package genj.tree;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.Indi;
import gj.awt.geom.Path;
import gj.layout.Layout;
import gj.layout.LayoutException;
import gj.layout.tree.NodeOptions;
import gj.layout.tree.TreeLayout;
import gj.model.Arc;
import gj.model.Graph;
import gj.model.Node;
import gj.util.ModelHelper;

/**
 * Model of our tree
 */
public class Model implements Graph {
  
  /** shape of marriage rings */
  private final static Shape 
    SHAPE_MARRIAGE_RINGS = calcMarriageRings();

  /** listeners */
  private List listeners = new ArrayList(3);

  /** arcs */
  private Collection arcs = new ArrayList(100);

  /** nodes */
  private Collection nodes = new ArrayList(100);

  /** bounds */
  private Rectangle2D bounds = new Rectangle2D.Double();
  
  /** the layout we use */
  private TreeLayout layout = new TreeLayout();
  
  /** parameters */
  private double 
    padIndis = 1.0D;

  /** shapes */
  private Rectangle2D.Double 
    shapeIndi = new Rectangle2D.Double(-1.5D,-1.0D,3.0D,2.0D),
    shapeFam  = new Rectangle2D.Double(-1.5D,-0.5D,3.0D,1.0D),
    shapeFoo  = new Rectangle2D.Double(-0.5D,-1.0D,1.0D,2.0D);

  /**
   * Constructor
   */
  public Model() {
  }
  
  /**
   * Sets the root
   */
  public void setRoot(Entity root) {
    // clear old
    arcs.clear();
    nodes.clear();
    bounds.setFrame(0,0,0,0);
    // parse the tree
    Node node = parse(root);
    // layout
    try {
      layout.setDebug(true);
      layout.setRoot(node);
      layout.setNodeOptions(new MyOptions());
      layout.setBendArcs(true);
      layout.applyTo(this);
    } catch (LayoutException e) {
      e.printStackTrace();
    }
    // notify
    fireStructureChanged();
    // done
  }
  
  /**
   * Gets the layout used
   */
  /*package*/ Layout getLayout() {
    return layout;
  }
  
  /**
   * Add listener
   */
  public void addListener(ModelListener l) {
    listeners.add(l);
  }
  
  /**
   * Remove listener
   */
  public void removeListener(ModelListener l) {
    listeners.remove(l);
  }
  
  /**
   * Fire even
   */
  private void fireStructureChanged() {
    for (int l=listeners.size()-1; l>=0; l--) {
      ((ModelListener)listeners.get(l)).structureChanged(this);
    }
  }
  
  /**
   * builds the tree
   */
  private Node parse(Entity entity) {
    if (entity instanceof Indi) return parse((Indi)entity);
    if (entity instanceof Fam ) return parse((Fam )entity);
    throw new IllegalArgumentException("Indi and Fam only");
  }
  
  /**
   * builds the tree for an individual
   */
  private Node parse(Indi indi) {
    DummyNode dn = new DummyNode();
    IndiNode in = new IndiNode(indi);
    new MyArc(dn, in, false);
    in.addDependants(dn);
    return dn;
  }
  
  /**
   * builds the tree for a family
   */
  private Node parse(Fam fam) {
    return new FamNode(fam);
  }
  
  /**
   * An entity by position
   */
  public Entity getEntityAt(double x, double y) {
    // loop nodes
    Iterator it = nodes.iterator();
    while (it.hasNext()) {
      MyNode node = (MyNode)it.next();
      Point2D pos = node.getPosition();
      Shape shape = node.getShape();
      if (shape!=null&&shape.getBounds().contains(x-pos.getX(),y-pos.getY())) 
        return node.entity;
    }
    // nothing found
    return null;
  }
  
  /**
   * @see gj.model.Graph#getArcs()
   */
  public Collection getArcs() {
    return arcs;
  }

  /**
   * @see gj.model.Graph#getBounds()
   */
  public Rectangle2D getBounds() {
    return bounds;
  }

  /**
   * @see gj.model.Graph#getNodes()
   */
  public Collection getNodes() {
    return nodes;
  }

  /**
   * A node for an entity
   */
  private abstract class MyNode implements Node {
    
    /** the entity */
    protected Entity entity;
    
    /** arcs of this entity */
    private List arcs = new ArrayList(5);
    
    /** position of this entity */
    private Point2D pos = new Point2D.Double();
    
    /**
     * Constructor
     */
    private MyNode(Entity etity) {
      // remember
      entity = etity;
      // publish
      nodes.add(this);
      // done
    }
    
    /**
     * @see gj.model.Node#getArcs()
     */
    public List getArcs() {
      return arcs;
    }

    /**
     * @see gj.model.Node#getContent()
     */
    public Object getContent() {
      return entity;
    }

    /**
     * @see gj.model.Node#getPosition()
     */
    public Point2D getPosition() {
      return pos;
    }

    /**
     * @see gj.model.Node#getShape()
     */
    public abstract Shape getShape();
    
    /**
     * Resolve Padding
     */
    protected abstract double getPadding(int side);
    
  } //MyNode
  
  /**
   * A node for an individual
   */
  private class IndiNode extends MyNode {
    /**
     * Constructor
     */
    private IndiNode(Indi indi) {
      super(indi);
    }
    /**
     * Add dependants - spouses and marriages
     */
    private void addDependants(MyNode node) {
      // we wrap an indi
      Indi indi = (Indi)entity;
      // loop through our fams
      Fam[] fams = indi.getFamilies();
      for (int f=0; f<fams.length; f++) {
        // the family
        Fam fam = fams[f];
        // an arc to marr
        new MyArc(node, new MarrNode(fam), false);
        if (fam.getNoOfSpouses()>1) 
          new MyArc(node, new IndiNode(fam.getOtherSpouse(indi)), false);
        // next family
      }
      // done
    }
    /**
     * @see gj.model.Node#getShape()
     */
    public Shape getShape() {
      return shapeIndi;
    }
    /**
     * @see genj.tree.Model.MyNode#getPadding(int)
     */
    protected double getPadding(int side) {
      return padIndis/2;
    }
  } //MyINode
  
  /**
   * A node for a family
   */
  private class FamNode extends MyNode {
    /**
     * Constructor
     */
    private FamNode(Fam fam) {
      // delegate
      super(fam);
      // grab the children
      Indi[] children = fam.getChildren();
      for (int c=0; c<children.length; c++) {
        // here's the child
        Indi child = children[c];
        // create a node&arc for it
        IndiNode node = new IndiNode(child);
        new MyArc(this, node, true);       
        // and add dependants
        node.addDependants(this);
        // next child
      }
      // done
    }
    /**
     * @see genj.tree.Model.MyNode#getShape()
     */
    public Shape getShape() {
      return shapeFam;
    }
    /**
     * @see genj.tree.Model.MyNode#getPadding(int)
     */
    protected double getPadding(int side) {
      if (side==MyOptions.NORTH) return -padIndis/2 + SHAPE_MARRIAGE_RINGS.getBounds2D().getWidth();
      return padIndis/2;
    }
  } //MyFNode

  /**
   * A node standing between two partners
   */
  private class MarrNode extends MyNode {
    /**
     * Constructor
     */
    private MarrNode(Fam fam) {
      // delegate
      super(null);
      // add node for fam below
      new MyArc(this, new FamNode(fam), false);
    }
    /**
     * @see genj.tree.Model.MyNode#getShape()
     */
    public Shape getShape() {
      return SHAPE_MARRIAGE_RINGS;
    }
    /**
     * @see genj.tree.Model.MyNode#getPadding(int)
     */
    protected double getPadding(int side) {
      if (side==MyOptions.NORTH||side==MyOptions.SOUTH) {
        return (shapeIndi.getHeight()+padIndis)/2 - SHAPE_MARRIAGE_RINGS.getBounds2D().getHeight()/2;
      }
      return -padIndis/2;
    }

  } //MarrNode
  
  /**
   * A dummy node 
   */
  private class DummyNode extends MyNode {
    /**
     * Constructor
     */
    private DummyNode() {
      super(null);
    }    
    /**
     * @see genj.tree.Model.MyNode#getShape()
     */
    public Shape getShape() {
      return null;
    }
    /**
     * @see genj.tree.Model.MyNode#getPadding(int)
     */
    protected double getPadding(int side) {
      return 0;
    }
  } //DummyNode
  
  /**
   * An arc between two individuals
   */
  private class MyArc implements Arc {
    /** start */
    private MyNode start; 
    /** end */
    private MyNode end; 
    /** path */
    private Path path;
    /**
     * Constructor
     */
    private MyArc(MyNode n1, MyNode n2, boolean p) {
      // remember
      start = n1;
      end   = n2;
      if (p) path = new Path();
      // register
      n1.arcs.add(this);
      n2.arcs.add(this);
      arcs.add(this);
      // done  
    }
    /**
     * @see gj.model.Arc#getEnd()
     */
    public Node getEnd() {
      return end;
    }
    /**
     * @see gj.model.Arc#getStart()
     */
    public Node getStart() {
      return start;
    }
    /**
     * @see gj.model.Arc#getPath()
     */
    public Path getPath() {
      return path;
    }
  } //Indi2Indi

  /**
   * Customs NodeOptions
   */
  private class MyOptions implements NodeOptions {
    /** our node */
    private MyNode mynode;
    /**
     * @see gj.layout.tree.NodeOptions#set(Node)
     */
    public void set(Node node) {
      mynode = (MyNode)node;
    }
    /**
     * @see gj.layout.tree.NodeOptions#getAlignment(int)
     */
    public double getAlignment(int dir) {
      return 0.5;
    }
    /**
     * @see gj.layout.tree.NodeOptions#getPadding(int)
     */
    public double getPadding(int dir) {
      return mynode.getPadding(dir);
    }
  } //MyNodeOptions

  /**
   * Calculates marriage rings
   */
  private static Shape calcMarriageRings() {
    Ellipse2D
      a = new Ellipse2D.Double(-0.15,-0.1,0.2,0.2),
      b = new Ellipse2D.Double(-0.05,-0.1,0.2,0.2);
    GeneralPath result = new GeneralPath(a);      
    result.append(b,false);
    return result;
  }
  
} //Model
