/**
 * This file is part of GraphJ
 * 
 * Copyright (C) 2002-2004 Nils Meier
 * 
 * GraphJ is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GraphJ is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GraphJ; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package gj.layout.hierarchical;

import gj.model.Edge;
import gj.model.Vertex;

import java.awt.Point;
import java.util.Collection;

/**
 * Layer provided by LayerAssignment
 */
public interface Layer {

  public int size();
  
  public Vertex getVertex(int u);
  
  public void swap(int u, int v);
  
  public Point[] getRouting(int u, Edge edge);
  
  public int[] getIncoming(int u);
  
  public int[] getOutgoing(int u);
  
  public static Vertex DUMMY = new Vertex() {
    @Override
    public String toString() {
      return "Dummy";
    }
    public Collection<? extends Edge> getEdges() {
      throw new IllegalArgumentException("n/a");
    }
  };
  
}
