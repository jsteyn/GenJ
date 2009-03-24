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
package gj.layout;


import gj.geom.Path;
import gj.model.Edge;
import gj.model.Graph;
import gj.model.Vertex;

import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

/**
 * A graph with 2 dimensional layout information
 */
public interface Graph2D extends Graph {

  /**
   * Edge's shape
   */
  public Path getPathOfEdge(Edge edge);

  /**
   * Edge's shape
   */
  public void setPathOfEdge(Edge edge, Path shape);

  /**
   * Vertex's shape
   */
  public Shape getShapeOfVertex(Vertex vertex);

  /** 
   * Vertex transformation (idempotent)
   */
  public void setTransformOfVertex(Vertex vertex, AffineTransform transform);

  /** 
   * Vertex transformation (idempotent)
   */
  public AffineTransform getTransformOfVertex(Vertex vertex);

  /**
   * Vertex's position
   */
  public Point2D getPositionOfVertex(Vertex vertex);

  /**
   * Vertex's position
   */
  public void setPositionOfVertex(Vertex vertex, Point2D pos);
  
  /**
   * Edge's port control
   */
  public Port getPortOfEdge(Edge edge, Vertex at);
  
} //Layout2D
