/*
 * Copyright (c) 1998-2006 Caucho Technology -- all rights reserved
 *
 * This file is part of Resin(R) Open Source
 *
 * Each copy or derived work must preserve the copyright notice and this
 * notice unmodified.
 *
 * Resin Open Source is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * Resin Open Source is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, or any warranty
 * of NON-INFRINGEMENT.  See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Resin Open Source; if not, write to the
 *
 *   Free Software Foundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package com.caucho.loader.enhancer;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.caucho.bytecode.JAnnotation;
import com.caucho.bytecode.JMethod;

import com.caucho.config.ConfigException;

import com.caucho.java.gen.GenClass;

import com.caucho.loader.enhancer.EnhancerManager;

/**
 * Configuration for a method-enhancer builder.
 */
public interface MethodEnhancer {
  /**
   * Sets the annotation.
   */
  public void setAnnotation(Class ann)
    throws ConfigException;
  
  /**
   * Enhances the method.
   *
   * @param genClass the generated class
   * @param jMethod the method to be enhanced
   * @param jAnn the annotation to be enhanced
   */
  public void enhance(GenClass genClass,
		      JMethod jMethod,
		      JAnnotation jAnn);
}