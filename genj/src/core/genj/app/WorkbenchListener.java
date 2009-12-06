/**
 * GenJ - GenealogyJ
 *
 * Copyright (C) 1997 - 2009 Nils Meier <nils@meiers.net>
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
package genj.app;

import genj.gedcom.Context;
import genj.gedcom.Gedcom;
import genj.view.View;

/**
 * Workbench callbacks
 */
public interface WorkbenchListener {

  /**
   * notification that selection has changed
   * @param context the new selection
   * @param isActionPerformed whether to perform action (normally double-click)
   */
  public void selectionChanged(Context context, boolean isActionPerformed);

  /**
   * notification that commit of edits is requested
   */
  public void commitRequested();
  
  /** 
   * notification that workbench is closing
   * @return whether to continue with close operation or not
   */
  public boolean workbenchClosing();
  
  /** 
   * notification that gedcom was closed
   * @return whether to continue with close operation or not
   */
  public void gedcomClosed(Gedcom gedcom);
  
  /** 
   * notification that gedcom was opened
   * @return whether to continue with close operation or not
   */
  public void gedcomOpened(Gedcom gedcom);
  
  /**
   * notification that a view has been opened
   */
  public void viewOpened(View view);

  /**
   * notification that a view has been opened
   */
  public void viewClosed(View view);

}
