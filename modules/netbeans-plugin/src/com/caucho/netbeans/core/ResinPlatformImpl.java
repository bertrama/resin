/*
 * Copyright (c) 1998-2007 Caucho Technology -- all rights reserved
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
 * @author Sam
 */


package com.caucho.netbeans.core;

import com.caucho.netbeans.PluginL10N;
import com.caucho.netbeans.util.ResinProperties;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.modules.j2ee.deployment.common.api.J2eeLibraryTypeProvider;
import org.netbeans.modules.j2ee.deployment.devmodules.api.J2eeModule;
import org.netbeans.modules.j2ee.deployment.plugins.api.J2eePlatformImpl;
import org.netbeans.spi.project.libraries.LibraryImplementation;
import org.openide.util.Utilities;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


public class ResinPlatformImpl
  extends J2eePlatformImpl
{
  private static final PluginL10N L = new PluginL10N(ResinPlatformImpl.class);

  private static final Set<Object> SUPPORTED_MODULE_TYPES
    = new LinkedHashSet<Object>();

  private static final Set<String> SUPPORTED_SPEC_VERSIONS
    = new LinkedHashSet<String>();

  private static final Set<String> SUPPORTED_JAVA_PLATFORM_VERSIONS
    = new LinkedHashSet<String>();

  private final String _displayName;
  private final ResinProperties _resinProperties;

  private final List<LibraryImplementation> _libraries
    = new ArrayList<LibraryImplementation>();

  public ResinPlatformImpl(ResinProperties properties)
  {
    _resinProperties = properties;
    _displayName = properties.getDisplayName();

    J2eeLibraryTypeProvider libProvider = new J2eeLibraryTypeProvider();

    LibraryImplementation library = libProvider.createLibrary();

    library.setName(L.l("Resin J2EE Library for {0}", _displayName));

    initLibrary(library);

    _libraries.add(library);
  }

  private void initLibrary(LibraryImplementation library)
  {
    library.setContent(J2eeLibraryTypeProvider.VOLUME_TYPE_CLASSPATH,
                       _resinProperties.getClasses());

    library.setContent(J2eeLibraryTypeProvider.VOLUME_TYPE_JAVADOC,
                       _resinProperties.getJavadocs());

    library.setContent(J2eeLibraryTypeProvider.VOLUME_TYPE_SRC,
                       _resinProperties.getSources());
  }

  public void notifyLibrariesChanged()
  {
    LibraryImplementation libraryImplementation = _libraries.get(0);

    initLibrary(libraryImplementation);

    firePropertyChange(PROP_LIBRARIES, null, _libraries);
  }

  public LibraryImplementation[] getLibraries()
  {
    return _libraries.toArray(new LibraryImplementation[_libraries.size()]);
  }

  public String getDisplayName()
  {
    return _displayName;
  }

  public Image getIcon()
  {
    return Utilities.loadImage("com/caucho/netbeans/resources/resin.png"); // NOI18N
  }

  public File[] getPlatformRoots()
  {
    return new File[] { _resinProperties.getResinHome() };
  }

  public File[] getToolClasspathEntries(String toolName)
  {
    return null;
  }

  public boolean isToolSupported(String toolName)
  {
    return false;
  }

  public Set getSupportedModuleTypes()
  {
    return SUPPORTED_MODULE_TYPES;
  }

  public Set<String> getSupportedSpecVersions()
  {
    return SUPPORTED_SPEC_VERSIONS;
  }

  public Set<String> getSupportedJavaPlatformVersions()
  {
    return SUPPORTED_JAVA_PLATFORM_VERSIONS;
  }

  public JavaPlatform getJavaPlatform()
  {
    return _resinProperties.getJavaPlatform();
  }

  static {
    SUPPORTED_MODULE_TYPES.add(J2eeModule.WAR);
    SUPPORTED_MODULE_TYPES.add(J2eeModule.EAR);
    SUPPORTED_MODULE_TYPES.add(J2eeModule.EJB);

    SUPPORTED_SPEC_VERSIONS.add(J2eeModule.J2EE_13);
    SUPPORTED_SPEC_VERSIONS.add(J2eeModule.J2EE_14);
    SUPPORTED_SPEC_VERSIONS.add(J2eeModule.JAVA_EE_5);

    SUPPORTED_JAVA_PLATFORM_VERSIONS.add("1.4"); // NOI18N
    SUPPORTED_JAVA_PLATFORM_VERSIONS.add("1.5"); // NOI18N
    SUPPORTED_JAVA_PLATFORM_VERSIONS.add("1.6"); // NOI18N

  }

}