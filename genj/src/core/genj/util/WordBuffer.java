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
package genj.util;

/**
 * A buffer that accepts words - meaning spaces will be automatially
 * inserted if appropriate
 */
public class WordBuffer {
  
  /** a buffer we collect words in */
  private StringBuffer buffer;
  
  /** the filler between words */
  private String filler = " ";
  
  /** 
   * Constructor
   */
  public WordBuffer(String content) {
    buffer = new StringBuffer(content.length()*2);
    buffer.append(content);
  }
  
  /** 
   * Constructor
   */
  public WordBuffer() {
    buffer = new StringBuffer(80);
  }
  
  /** 
   * Constructor
   */
  public WordBuffer(char fill) {
    buffer = new StringBuffer(80);
    filler = ""+fill;
  }
  
  /**
   * Set the filler to use between words   */
  public WordBuffer setFiller(String set) {
    filler = set;
    return this;
  }
  
  /** 
   * String representation of the content
   */
  public String toString() {
    return buffer.toString();
  }

  /**
   * Append a generic object (null->"")
   */
  public WordBuffer append(Object object) {
    if (object!=null) append(object.toString());
    return this;
  }

  /**
   * Append a generic object (null->"")
   */
  public WordBuffer append(Object object, String nullSubst) {
    if (object==null) return append(nullSubst);
    return append(object.toString(), nullSubst);
  }

  /**
   * Append a word
   */  
  public WordBuffer append(String word) {
    return (word==null) ? this :append(word, null);
  }
  
  /**
   * Append a word
   */  
  public WordBuffer append(String word, String nullSubst) {
    // nothing to do?
    if ((word==null)||(word.length()==0)) return append(nullSubst);
    // need a word-filler?
    if ((buffer.length()>0)&&(!isStartingWithPunctuation(word))) buffer.append(filler);
    // get the word
    buffer.append(word.trim());
    // done
    return this;
  }
  
  /**
   * Checks whether a word starts with a punctuation
   */
  private final boolean isStartingWithPunctuation(String word) {
    switch (word.charAt(0)) {
      default: return false;
      case '.': return true;
      case ',': return true;
      case ':': return true;
    }
  }

}
