/*
 * Copyright (c) 1998-2004 Caucho Technology -- all rights reserved
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
 *   Free SoftwareFoundation, Inc.
 *   59 Temple Place, Suite 330
 *   Boston, MA 02111-1307  USA
 *
 * @author Scott Ferguson
 */

package javax.mail;

/**
 * Context of a Message.
 */
public class MessageContext {

  private Part _part;
  
  /**
   * Creates a context.
   */
  public MessageContext(Part part)
  {
    _part = part;
  }

  /**
   * Returns the message of the context.
   */
  public Message getMessage()
  {
    return unwrap(_part);
  }

  private static Message unwrap(Part p)
  {
    if (p==null || p instanceof Message)
      return (Message)p;
    return unwrap(((BodyPart)p).getParent().getParent());
  }

  /**
   * Returns the owning part of the context.
   */
  public Part getPart()
  {
    return _part;
  }

  /**
   * Returns the current session.
   */
  public Session getSession()
  {
    return getMessage().getSession();
  }
}