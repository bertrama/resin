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
 * @author Adam Megacz
 */

package javax.xml.bind;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

class FactoryLoader {
  private static Logger log =
    Logger.getLogger("javax.xml.bind.FactoryLoader");

  private static HashMap<String,FactoryLoader> _factoryLoaders
    = new HashMap<String,FactoryLoader>();

  private static final String JAXB_CONTEXT_FACTORY
    = "javax.xml.bind.context.factory";

  private String _factoryId;

  private WeakHashMap<ClassLoader,Object[]> _providerMap
    = new WeakHashMap<ClassLoader,Object[]>();

  public static FactoryLoader getFactoryLoader(String factoryId) 
  {
    FactoryLoader ret = _factoryLoaders.get(factoryId);

    if (ret == null) {
      ret = new FactoryLoader(factoryId);
      _factoryLoaders.put(factoryId, ret);
    }

    return ret;
  }

  private FactoryLoader(String factoryId)
  {
    _factoryId = factoryId;
  }

  private String classNameFromPropertiesFile(ClassLoader classLoader, 
                                             String fileName)
  {
    InputStream is = null;
    try {
      is = classLoader.getResourceAsStream(fileName);

      if (is == null)
        return null;

      Properties props = new Properties();
      props.load(is);

      return props.getProperty(JAXB_CONTEXT_FACTORY);
    }
    catch (IOException e) {
      log.log(Level.FINER, "ignoring exception", e);

      return null;
    }
    finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          log.log(Level.FINER, "ignoring exception", e);
        }
      }
    }
  }

  public Object newInstance(ClassLoader classLoader, String contextPath)
    throws FactoryConfigurationError
  {
    String[] pkgs = contextPath.split(":");

    for (int i = 0; i < pkgs.length; i++) {
      String pkg = pkgs[i].replaceAll("\\.", File.separator);
      String fileName = /*File.separatorChar + */pkg + 
                        File.separatorChar + "jaxb.properties";

      String className = classNameFromPropertiesFile(classLoader, fileName);

      if (className != null) {
        try {
          return classLoader.loadClass(className).newInstance();
        }
        catch (Exception e) {
          throw new FactoryConfigurationError(e);
        }
      }
    }

    return newInstance(classLoader);
  }

  public Object newInstance(ClassLoader classLoader)
    throws FactoryConfigurationError
  {
    String className = null;

    className = System.getProperty(_factoryId);

    if (className == null) {
      String fileName =
        System.getProperty("java.home") +
        File.separatorChar +
        "lib" +
        File.separatorChar +
        "jaxb.properties";

      className = classNameFromPropertiesFile(classLoader, fileName);
    }

    if (className == null) {
      Object factory = createFactory("META-INF/services/"+_factoryId,
                                     classLoader);
      if (factory != null)
        return factory;
    }

    if (className != null) {
        
      try {
        return classLoader.loadClass(className).newInstance();
      }
      catch (Exception e) {
        throw new FactoryConfigurationError(e);
      }
    }

    return null;
  }

  public Object createFactory(String name, ClassLoader loader)
  {
    Object[] providers = getProviderList(name, loader);

    for (int i = 0; i < providers.length; i++) {
      Object factory;

      factory = providers[i];

      if (factory != null)
        return factory;
    }
    
    return null;
  }
  
  private Object []getProviderList(String service, ClassLoader loader)
  {

    Object []providers = _providerMap.get(loader);

    if (providers != null)
      return providers;
    
    ArrayList<Object> list = new ArrayList<Object>();

    try {
      Enumeration e = loader.getResources(service);

      while (e.hasMoreElements()) {
        URL url = (URL) e.nextElement();

        Object provider = loadProvider(url, loader);

        if (provider != null)
          list.add(provider);
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    }

    providers = new Object[list.size()];
    list.toArray(providers);

    _providerMap.put(loader, providers);
    
    return providers;
  }

  private Object loadProvider(URL url, ClassLoader loader)
  {
    InputStream is = null;
    try {
      is = url.openStream();
      int ch;

      while ((ch = is.read()) >= 0) {
        if (Character.isWhitespace((char) ch)) {
        }
        else if (ch == '#') {
          for (; ch >= 0 && ch != '\n' && ch != '\r'; ch = is.read()) {
          }
        }
        else {
          StringBuilder sb = new StringBuilder();

          for (;
               ch >= 0 && ! Character.isWhitespace((char) ch);
               ch = is.read()) {
            sb.append((char) ch);
          }

          String className = sb.toString();

          Class cl = Class.forName(className, false, loader);

          return (Object) cl.newInstance();
        }
      }
    } catch (Throwable e) {
      log.log(Level.WARNING, e.toString(), e);
    } finally {
      try {
        if (is != null)
          is.close();
      } catch (Throwable e) {
      }
    }

    return null;
  }
}


