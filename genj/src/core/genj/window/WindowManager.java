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
package genj.window;

import genj.gedcom.Gedcom;
import genj.util.Registry;
import genj.util.swing.Action2;
import genj.util.swing.ImageIcon;
import genj.util.swing.TextAreaWidget;
import genj.util.swing.TextFieldWidget;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

/**
 * Abstract base type for WindowManagers
 */
public class WindowManager {
  
  /** screen we're dealing with */
  private Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
  
  /** a hidden default frame */
  private JFrame defaultFrame = new JFrame();
  
  // FIXME docket get rid of window manager
  private final static WindowManager INSTANCE = new WindowManager();

  /** message types*/
  public static final int  
    ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE,
    INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE,
    WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE,
    QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE,
    PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

  /** registry */
  protected Registry registry;

  /** a counter for temporary keys */
  private int temporaryKeyCounter = 0;  

  /** a mapping between key to top level windows */
  private Map key2window = new HashMap();
    
  /** a log */
  /*package*/ final static Logger LOG = Logger.getLogger("genj.window");
  
  /** 
   * Constructor
   */
  private WindowManager() {
    registry = new Registry("genj.window");
    defaultFrame.setIconImage(Gedcom.getImage().getImage());
  }
  
  /**
   * @see genj.window.WindowManager#closeFrame(java.lang.String)
   */
  public void close(String key) {

    Object framedlg = recall(key);
    
    if (framedlg instanceof JFrame) {
      JFrame frame = (JFrame)framedlg;
      frame.dispose(); 
      return;
    }

    if (framedlg instanceof JDialog) {
      JDialog dlg = (JDialog)framedlg;
      dlg.setVisible(false); // we're using the optionpane signal for a closing dialog: hide it
      return;
    }

    // done
  }
  
  /**
   * Return the content of a dialog/frame 
   * @param key the dialog/frame's key 
   */
  public JComponent getContent(String key) {
    
    Object framedlg = recall(key);
    
    if (framedlg instanceof JFrame)
      return (JComponent)((JFrame)framedlg).getContentPane().getComponent(0); 

    if (framedlg instanceof JDialog)
      return (JComponent)((JDialog)framedlg).getContentPane().getComponent(0);

    return null;
  }

  
  /**
   * Makes sure the dialog/frame is visible
   * @param key the dialog/frame's key 
   * @return success or no valid key supplied
   */
  public boolean show(String key) {

    Object framedlg = recall(key);
    
    if (framedlg instanceof JFrame) {
      ((JFrame)framedlg).toFront(); 
      return true;
    }

    if (framedlg instanceof JDialog) {
      ((JDialog)framedlg).toFront();
      return true;
    }

    return false;
  }
  
  
  /**
   * Returns an appropriate WindowManager instance for given component
   * @return manager or null if no appropriate manager could be found
   */
  public static WindowManager getInstance(Component component) {
    return getInstance();
  }
  public static WindowManager getInstance() {
    return INSTANCE;
  }
  
  /**
   * Setup a new independant window
   */
  public final String openWindow(String key, String title, ImageIcon image, JComponent content, JMenuBar menu, Runnable onClose) {
    // create a key?
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    boolean maximized = registry.get(key+".maximized", false);
    // deal with it in impl
    Component window = openWindowImpl(key, title, image, content, menu, bounds, maximized, onClose);
    // remember it
    key2window.put(key, window);
    // done
    return key;
  }
  
  /**
   * Implementation for handling an independant window
   */
  private Component openWindowImpl(final String key, String title, ImageIcon image, JComponent content, JMenuBar menu, Rectangle bounds, boolean maximized, final Runnable onClosing) {
    
    // Create a frame
    final JFrame frame = new JFrame() {
      /**
       * dispose is our onClose hook because
       * WindowListener.windowClosed is too 
       * late (one frame) after dispose()
       */
      public void dispose() {
        // forget about key but keep bounds
        closeNotify(key, getBounds(), getExtendedState()==MAXIMIZED_BOTH);
        // continue
        super.dispose();
      }
    };

    // setup looks
    if (title!=null) frame.setTitle(title);
    if (image!=null) frame.setIconImage(image.getImage());
    if (menu !=null) frame.setJMenuBar(menu);

    // add content
    frame.getContentPane().add(content);

    // DISPOSE_ON_CLOSE?
    if (onClosing==null) {
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    } else {
      // responsibility to dispose passed to onClosing?
      frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
      frame.addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          onClosing.run();
        }
      });
    }

    // place
    if (bounds==null) {
      frame.pack();
      Dimension dim = frame.getSize();
      bounds = new Rectangle(screen.width/2-dim.width/2, screen.height/2-dim.height/2,dim.width,dim.height);
      LOG.log(Level.FINE, "Sizing window "+key+" to "+bounds+" after pack()");
    }
    frame.setBounds(bounds.intersection(screen));
    
    if (maximized)
      frame.setExtendedState(Frame.MAXIMIZED_BOTH);

    // show
    frame.setVisible(true);
    
    // done
    return frame;
  }
  
  
  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.lang.String, String[], javax.swing.JComponent)
   */
  public final int openDialog(String key, String title,  int messageType, String txt, Action[] actions, Component owner) {
    
    // analyze the text
    int maxLine = 40;
    int cols = 40, rows = 1;
    StringTokenizer lines = new StringTokenizer(txt, "\n\r");
    while (lines.hasMoreTokens()) {
      String line = lines.nextToken();
      if (line.length()>maxLine) {
        cols = maxLine;
        rows += line.length()/maxLine;
      } else {
        cols = Math.max(cols, line.length());
        rows++;
      }
    }
    rows = Math.min(10, rows);
    
    // create a textpane for the txt
    TextAreaWidget text = new TextAreaWidget("", rows, cols);
    text.setLineWrap(true);
    text.setWrapStyleWord(true);
    text.setText(txt);
    text.setEditable(false);    
    text.setCaretPosition(0);
    text.setRequestFocusEnabled(false);

    // wrap in reasonable sized scroll
    JScrollPane content = new JScrollPane(text);
      
    // delegate
    return openDialog(key, title, messageType, content, actions, owner);
  }
  
  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.awt.Dimension, javax.swing.JComponent[], java.lang.String[], javax.swing.JComponent)
   */
  public final int openDialog(String key, String title,  int messageType, JComponent[] content, Action[] actions, Component owner) {
    // assemble content into Box (don't use Box here because
    // Box extends Container in pre JDK 1.4)
    JPanel box = new JPanel();
    box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
    for (int i = 0; i < content.length; i++) {
      if (content[i]==null) continue;
      box.add(content[i]);
      content[i].setAlignmentX(0F);
    }
    // delegate
    return openDialog(key, title, messageType, box, actions, owner);
  }

  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, java.lang.String, java.lang.String, javax.swing.JComponent)
   */
  public final String openDialog(String key, String title,  int messageType, String txt, String value, Component owner) {

    // prepare text field and label
    TextFieldWidget tf = new TextFieldWidget(value, 24);
    JLabel lb = new JLabel(txt);
    
    // delegate
    int rc = openDialog(key, title, messageType, new JComponent[]{ lb, tf}, Action2.okCancel(), owner);
    
    // analyze
    return rc==0?tf.getText().trim():null;
  }

  /**
   * dialog core routine
   */
  public final int openDialog(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner) {
    // check options - default to OK
    if (actions==null) 
      actions = Action2.okOnly();
    // key is necessary
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    // do it
    Object rc = openDialogImpl(key, title, messageType, content, actions, owner, bounds);
    // analyze - check which action was responsible for close
    for (int a=0; a<actions.length; a++) 
      if (rc==actions[a]) return a;
    return -1;
  }
  
  /**
   * Dialog implementation
   */
  private Object openDialogImpl(final String key, String title,  int messageType, JComponent content, Action[] actions, Component owner, Rectangle bounds) {

    // create an option pane
    JOptionPane optionPane = new Content(messageType, content, actions);
    
    // let it create the dialog
    final JDialog dlg = optionPane.createDialog(owner != null ? owner : defaultFrame, title);
    dlg.setResizable(true);
    dlg.setModal(true);
    if (bounds==null) {
      dlg.pack();
      if (owner!=null)
        dlg.setLocationRelativeTo(owner.getParent());
    } else {
      if (owner==null) {
        dlg.setBounds(bounds.intersection(screen));
      } else {
        dlg.setBounds(new Rectangle(bounds.getSize()).intersection(screen));
        dlg.setLocationRelativeTo(owner.getParent());
      }
    }

    // hook up to the dialog being hidden by the optionpane - that's what is being called after the user selected a button (setValue())
    dlg.addComponentListener(new ComponentAdapter() {
      public void componentHidden(ComponentEvent e) {
        closeNotify(key, dlg.getBounds(), false);
        dlg.dispose();
      }
    });
    
    // show it
    dlg.setVisible(true);
    
    // return result
    return optionPane.getValue();
  }

  /**
   * @see genj.window.WindowManager#openDialog(java.lang.String, java.lang.String, javax.swing.Icon, javax.swing.JComponent, javax.swing.JComponent)
   */
  public final String openNonModalDialog(String key, String title,  int messageType, JComponent content, Action[] actions, Component owner) {
    // check options - none ok
    if (actions==null) actions = new Action[0];
    // key is necessary
    if (key==null) 
      key = getTemporaryKey();
    // close if already open
    close(key);
    // grab parameters
    Rectangle bounds = registry.get(key, (Rectangle)null);
    // do it
    Component window = openNonModalDialogImpl(key, title, messageType, content, actions, owner, bounds);
    // remember it
    key2window.put(key, window);
    // done
    return key;
  }

  /**
   * Dialog implementation
   */
  private Component openNonModalDialogImpl(final String key, String title,  int messageType, JComponent content, Action[] actions, Component owner, Rectangle bounds) {

    // create an option pane
    JOptionPane optionPane = new Content(messageType, content, actions);
    
    // let it create the dialog
    final JDialog dlg = optionPane.createDialog(owner != null ? owner : defaultFrame, title);
    dlg.setResizable(true);
    dlg.setModal(false);
    if (bounds==null) {
      dlg.pack();
      if (owner!=null)
        dlg.setLocationRelativeTo(owner.getParent());
    } else {
      if (owner==null) {
        dlg.setBounds(bounds.intersection(screen));
      } else {
        dlg.setBounds(new Rectangle(bounds.getSize()).intersection(screen));
        dlg.setLocationRelativeTo(owner.getParent());
      }
    }

    // hook up to the dialog being hidden by the optionpane - that's what is being called after the user selected a button (setValue())
    dlg.addComponentListener(new ComponentAdapter() {
      public void componentHidden(ComponentEvent e) {
        closeNotify(key, dlg.getBounds(), false);
        dlg.dispose();
      }
    });
    
    // show it
    dlg.setVisible(true);
    
    // return result
    return dlg;
  }
  

  /**
   * Create a temporary key
   */
  protected String getTemporaryKey() {
    return "_"+temporaryKeyCounter++;
  }

  /**
   * Recall Keys
   */
  protected String[] recallKeys() {
    return (String[])key2window.keySet().toArray(new String[0]);
  }

  /**
   * Recall frame/dialog
   */
  protected Component recall(String key) {
    // no key - no result
    if (key==null) 
      return null;
    // look it up
    return (Component)key2window.get(key);
  }

  /**
   * Forget about frame/dialog, stash away bounds
   */
  protected void closeNotify(String key, Rectangle bounds, boolean maximized) {
    // no key - no action
    if (key==null) 
      return;
    // forget frame/dialog
    key2window.remove(key);
    // temporary key? nothing to stash away
    if (key.startsWith("_")) 
      return;
    // keep bounds
    if (bounds!=null&&!maximized)
      registry.put(key, bounds);
    registry.put(key+".maximized", maximized);
    // done
  }
  
  /**
   * @see genj.window.WindowManager#closeAll()
   */
  public void closeAll() {

    // loop through keys    
    String[] keys = recallKeys();
    for (int k=0; k<keys.length; k++) {
      close(keys[k]);
    }
    
    // done
  }
  
  /**
   * Get the window for given owner component
   */  
  private Window getWindowForComponent(Component c) {
    if (c instanceof Frame || c instanceof Dialog || c==null)
      return (Window)c;
    return getWindowForComponent(c.getParent());
  }
  
  /**
   * A patched up JOptionPane
   */
  protected class Content extends JOptionPane {
    
    /** constructor */
    protected Content(int messageType, JComponent content, Action[] actions) {
      super(new JLabel(),messageType, JOptionPane.DEFAULT_OPTION, null, new String[0] );
      
      // wrap content in a JPanel - the OptionPaneUI has some code that
      // depends on this to stretch it :(
      JPanel wrapper = new JPanel(new BorderLayout());
      wrapper.add(BorderLayout.CENTER, content);
      setMessage(wrapper);

      // create our action buttons
      Option[] options = new Option[actions.length];
      for (int i=0;i<actions.length;i++)
        options[i] = new Option(actions[i]);
      setOptions(options);
      
      // set defalut?
      if (options.length>0) 
        setInitialValue(options[0]);
      
      // done
    }

    /** patch up layout - don't allow too small */
    public void doLayout() {
      // let super do its thing
      super.doLayout();
      // check minimum size
      Container container = getTopLevelAncestor();
      Dimension minimumSize = container.getMinimumSize();
      Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
      minimumSize.width = Math.min(screen.width/2, minimumSize.width);
      minimumSize.height = Math.min(screen.height/2, minimumSize.height);
      Dimension size        = container.getSize();
      if (size.width < minimumSize.width || size.height < minimumSize.height) {
        Dimension newSize = new Dimension(Math.max(minimumSize.width,  size.width),
                                          Math.max(minimumSize.height, size.height));
        container.setSize(newSize);
      }
      // checked
    }
    
    /** an option in our option-pane */
    private class Option extends JButton implements ActionListener {
      
      /** constructor */
      private Option(Action action) {
        super(action);
        addActionListener(this);
      }
      
      /** trigger */
      public void actionPerformed(ActionEvent e) {
        // this will actually force the dialog to hide - JOptionPane listens to property changes
        setValue(getAction());
      }
      
    } //Action2Button
    
  } // Content 
  
} //AbstractWindowManager
