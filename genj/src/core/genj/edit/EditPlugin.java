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
package genj.edit;

import genj.app.Workbench;
import genj.app.WorkbenchAdapter;
import genj.common.SelectEntityWidget;
import genj.crypto.Enigma;
import genj.edit.actions.AbstractChange;
import genj.edit.actions.CreateAlias;
import genj.edit.actions.CreateAssociation;
import genj.edit.actions.CreateChild;
import genj.edit.actions.CreateEntity;
import genj.edit.actions.CreateParent;
import genj.edit.actions.CreateSibling;
import genj.edit.actions.CreateSpouse;
import genj.edit.actions.CreateXReference;
import genj.edit.actions.DelEntity;
import genj.edit.actions.DelProperty;
import genj.edit.actions.OpenForEdit;
import genj.edit.actions.Redo;
import genj.edit.actions.RunExternal;
import genj.edit.actions.SetPlaceHierarchy;
import genj.edit.actions.SetSubmitter;
import genj.edit.actions.SwapSpouses;
import genj.edit.actions.TogglePrivate;
import genj.edit.actions.Undo;
import genj.edit.beans.DateBean;
import genj.edit.beans.NameBean;
import genj.edit.beans.SexBean;
import genj.gedcom.Context;
import genj.gedcom.Entity;
import genj.gedcom.Fam;
import genj.gedcom.Gedcom;
import genj.gedcom.GedcomException;
import genj.gedcom.Indi;
import genj.gedcom.MetaProperty;
import genj.gedcom.Property;
import genj.gedcom.PropertyEvent;
import genj.gedcom.PropertyFamilyChild;
import genj.gedcom.PropertyFile;
import genj.gedcom.PropertyMedia;
import genj.gedcom.PropertyNote;
import genj.gedcom.PropertyPlace;
import genj.gedcom.PropertyRepository;
import genj.gedcom.PropertySource;
import genj.gedcom.PropertySubmitter;
import genj.gedcom.Submitter;
import genj.gedcom.TagPath;
import genj.gedcom.UnitOfWork;
import genj.io.FileAssociation;
import genj.util.EnvironmentChecker;
import genj.util.Resources;
import genj.util.swing.Action2;
import genj.util.swing.NestedBlockLayout;
import genj.view.ActionProvider;
import genj.window.WindowManager;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * our editing plugin
 */
public class EditPlugin extends WorkbenchAdapter implements ActionProvider {
  
  private final static Resources RESOURCES = Resources.get(EditPlugin.class);
  
  private Workbench workbench;
  
  /**
   * Constructor
   */
  /*package*/ EditPlugin(Workbench workbench) {
    this.workbench = workbench;
    workbench.addWorkbenchListener(this);
  }
  
  public int getPriority() {
    return HIGH;
  }
  
  @Override
  public void gedcomOpened(Workbench workbench, Gedcom gedcom) {

    // ask for root of tree
    if (gedcom.getEntities(Gedcom.INDI).isEmpty()) {
      
      final NameBean name = new NameBean();
      final SexBean sex = new SexBean();
      final DateBean birth= new DateBean();
      JPanel panel = new JPanel();
      panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
      panel.add(new JLabel(RESOURCES.getString("newroot.text")));
      panel.add(name);
      panel.add(sex);
      panel.add(birth);
      
      if (0==WindowManager.getInstance().openDialog(null, RESOURCES.getString("newroot.title", gedcom.getName()), WindowManager.QUESTION_MESSAGE, panel, Action2.okCancel(), workbench)) {
        gedcom.doMuteUnitOfWork(new UnitOfWork() {
          public void perform(Gedcom gedcom) throws GedcomException {
            Indi adam = (Indi) gedcom.createEntity(Gedcom.INDI);
            name.commit(adam.addProperty("NAME", ""));
            sex.commit(adam.addProperty("SEX", ""));
            birth.commit(adam.setValue(new TagPath("INDI:BIRT:DATE"), ""));
            
            Submitter submitter = (Submitter) gedcom.createEntity(Gedcom.SUBM);
            submitter.setName(EnvironmentChecker.getProperty(this, "user.name", "?", "user name used as submitter in new gedcom"));
          }
        });
      }
      
    }
    
  }

  /** 
   * Frederic - a test action showing cross-gedcom work 
   */
  private class CopyIndividual extends AbstractChange {
    
    private Gedcom source;
    private Indi existing;
  
    public CopyIndividual(Gedcom dest, Gedcom source) {
      super(dest, Gedcom.getEntityImage(Gedcom.INDI), "Copy individual from "+source);
      this.source = source;
    }
    
    /**
     * Override content components to show to user 
     */
    @Override
    protected JPanel getDialogContent() {
      
      JPanel result = new JPanel(new NestedBlockLayout("<col><row><select wx=\"1\"/></row><row><text wx=\"1\" wy=\"1\"/></row><row><check/><text/></row></col>"));
  
      // create selector
      final SelectEntityWidget select = new SelectEntityWidget(source, Gedcom.INDI, null);
  
      // wrap it up
      result.add(select);
      result.add(getConfirmComponent());
  
      // add listening
      select.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          // grab current selection (might be null)
          existing = (Indi)select.getSelection();
          refresh();
        }
      });
      
      existing = (Indi)select.getSelection();
      refresh();
      
      // done
      return result;
    }
    
    @Override
    protected void refresh() {
      // TODO Auto-generated method stub
      super.refresh();
    }
    
    private boolean dupe() {
      return gedcom.getEntity(existing.getId())!=null;
    }
    
    @Override
    protected String getConfirmMessage() {
      if (existing==null)
        return "Please select an individual";
      String result = "Copying individual "+existing+" from "+source.getName()+" to "+gedcom.getName();
      if (dupe())
        result += "\n\nNote: Duplicate ID - a new ID will be assigned";
      return result;
    }
    
    @Override
    protected Context execute(Gedcom gedcom, ActionEvent event) throws GedcomException {
      Entity e = gedcom.createEntity(Gedcom.INDI, dupe() ? null : existing.getId());
      e.copyProperties(existing.getProperties(), true);
      return new Context(e);
    }
  
  }

  /**
   * @see genj.view.ActionProvider#createActions(Entity[], ViewManager)
   */
  private void createActions(List<? extends Property> properties, Action2.Group group) {
    
    // Toggle "Private"
    if (Enigma.isAvailable())
      group.add(new TogglePrivate(properties.get(0).getGedcom(), properties));
    
    // Delete
    group.add(new DelProperty(properties));
    
    // done
  }

  /**
   * @see genj.view.ContextSupport#createActions(Property)
   */
  private void createActions(Property property, Action2.Group group) {
    
    // FileAssociationActions for PropertyFile
    if (property instanceof PropertyFile)  
      createActions(group, (PropertyFile)property); 
      
    // Place format for PropertyFile
    if (property instanceof PropertyPlace)  
      group.add(new SetPlaceHierarchy((PropertyPlace)property)); 
      
    // Check what xrefs can be added
    MetaProperty[] subs = property.getNestedMetaProperties(0);
    for (int s=0;s<subs.length;s++) {
      // NOTE REPO SOUR SUBM (BIRT|ADOP)FAMC
      Class<? extends Property> type = subs[s].getType();
      if (type==PropertyNote.class||
          type==PropertyRepository.class||
          type==PropertySource.class||
          type==PropertySubmitter.class||
          type==PropertyFamilyChild.class||
          type==PropertyMedia.class 
        ) {
        // .. make sure @@ forces a non-substitute!
        group.add(new CreateXReference(property,subs[s].getTag()));
        // continue
        continue;
      }
    }
    
    // Add Association to events if property is contained in individual
    // or ASSO allows types
    if ( property instanceof PropertyEvent
        && ( (property.getEntity() instanceof Indi)
            || property.getGedcom().getGrammar().getMeta(new TagPath("INDI:ASSO")).allows("TYPE"))  )
      group.add(new CreateAssociation(property));
    
    // Toggle "Private"
    if (Enigma.isAvailable())
      group.add(new TogglePrivate(property.getGedcom(), Collections.singletonList(property)));
    
    // Delete
    if (!property.isTransient()) 
      group.add(new DelProperty(property));
  
    // done
  }
  
  /**
   * Actions for context
   */
  public List<Action2> createActions(Context context, Purpose purpose) {
    
    List<Action2> result = new ArrayList<Action2>();

    switch (purpose) {
      case MENU:
    	  
        // create entities
        Action2.Group edit = new EditActionGroup();
        if (context.getEntity()==null)
          createActions(context.getGedcom(), edit);
        else if (context.getEntities().size()==1 && context.getEntity() instanceof Indi)
          createActions((Indi)context.getEntity(), edit);
        result.add(edit);
          
        edit.add(new ActionProvider.SeparatorAction());
        edit.add(new Undo(context.getGedcom()));
        edit.add(new Redo(context.getGedcom()));
        
        break;
        
      case CONTEXT:
        
        // sub-menu for properties
        if (context.getProperties().size()>1) {
          Action2.Group group = new ActionProvider.PropertiesActionGroup(context.getProperties());
          createActions(context.getProperties(), group);
          if (group.size()>0)
            result.add(group);
        } else if (context.getProperties().size()==1) {
          Action2.Group group = new ActionProvider.PropertyActionGroup(context.getProperty());
          createActions(context.getProperty(), group);
          if (group.size()>0)
            result.add(group);
        }
     
        // sub-menu for entity
        if (context.getEntities().size()==1) {
          Action2.Group group = new ActionProvider.EntityActionGroup(context.getEntity());
          createActions(context.getEntity(), group);
          if (group.size()>0)
            result.add(group);
          
          // add an "edit in EditView"
          if (null==workbench.getView(EditViewFactory.class))
            result.add(new OpenForEdit(workbench, context));


        }
        
        // sub-menu for gedcom
        Action2.Group group = new ActionProvider.GedcomActionGroup(context.getGedcom());
        createActions(context.getGedcom(), group);
        if (group.size()>0)
          result.add(group);
        
        result.add(new ActionProvider.SeparatorAction());
        result.add(new Undo(context.getGedcom()));
        result.add(new Redo(context.getGedcom()));
        
        break;
        
      case TOOLBAR:
        result.add(new Undo(context.getGedcom()));
        result.add(new Redo(context.getGedcom()));
        break;
    }

    
    // done
    return result;
  }

  /**
   * @see genj.view.ViewFactory#createActions(Entity)
   */
  private void createActions(Entity entity, Action2.Group group) {
    
    // indi?
    if (entity instanceof Indi) 
      createActions((Indi)entity, group);
      
    // fam?
    if (entity instanceof Fam) createActions(group, (Fam)entity);
    // submitter?
    if (entity instanceof Submitter) createActions(group, (Submitter)entity);
    
    // separator
    group.add(new ActionProvider.SeparatorAction());

    // Check what xrefs can be added
    MetaProperty[] subs = entity.getNestedMetaProperties(0);
    for (int s=0;s<subs.length;s++) {
      // NOTE||REPO||SOUR||SUBM
      Class<? extends Property> type = subs[s].getType();
      if (type==PropertyNote.class||
          type==PropertyRepository.class||
          type==PropertySource.class||
          type==PropertySubmitter.class||
          type==PropertyMedia.class
          ) {
        group.add(new CreateXReference(entity,subs[s].getTag()));
      }
    }

    // add delete
    group.add(new ActionProvider.SeparatorAction());
    group.add(new DelEntity(entity));
    
    // done
  }

  /**
   * @see genj.view.ContextMenuSupport#createActions(Gedcom)
   */
  private void createActions(Gedcom gedcom, Action2.Group group) {
    
    // create the actions
    group.add(new CreateEntity(gedcom, Gedcom.INDI));
    group.add(new CreateEntity(gedcom, Gedcom.FAM));
    group.add(new CreateEntity(gedcom, Gedcom.NOTE));
    group.add(new CreateEntity(gedcom, Gedcom.OBJE));
    group.add(new CreateEntity(gedcom, Gedcom.REPO));
    group.add(new CreateEntity(gedcom, Gedcom.SOUR));
    group.add(new CreateEntity(gedcom, Gedcom.SUBM));
  
    // done
  }

  /**
   * Create actions for Individual
   */
  private void createActions(Indi indi, Action2.Group group) {
    
    Action2.Group more = new Action2.Group(Resources.get(this).getString("add.more"));
    
    if (indi.getParents().size()<2)
      group.add(new CreateParent(indi));
    else
      more.add(new CreateParent(indi));

    if (indi.getPartners().length==0)
      group.add(new CreateSpouse(indi));
    else
      more.add(new CreateSpouse(indi));

    group.add(new CreateChild(indi, true));
    group.add(new CreateChild(indi, false));
    group.add(new CreateSibling(indi, true));
    group.add(new CreateSibling(indi, false));
    
    more.add(new CreateAlias(indi));
    
    group.add(more);
  }

  /**
   * Create actions for Families
   */
  private void createActions(Action2.Group group, Fam fam) {
    group.add(new CreateChild(fam, true));
    group.add(new CreateChild(fam, false));
    if (fam.getNoOfSpouses()<2)
      group.add(new CreateParent(fam));
    if (fam.getNoOfSpouses()!=0)
      group.add(new SwapSpouses(fam));
  }

  /**
   * Create actions for Submitters
   */
  private void createActions(Action2.Group group, Submitter submitter) {
    group.add(new SetSubmitter(submitter));
  }

  /**  
   * Create actions for PropertyFile
   */
  private void createActions(Action2.Group group, PropertyFile file) {
  
    // find suffix
    String suffix = file.getSuffix();
      
    // lookup associations
    List<FileAssociation> assocs = FileAssociation.getAll(suffix);
    if (assocs.isEmpty()) {
      group.add(new RunExternal(file));
    } else {
      for (FileAssociation fa : assocs) {
        group.add(new RunExternal(file,fa));
      }
    }
    // done
  }

}

