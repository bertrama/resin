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

package com.caucho.jdbc;

import com.caucho.util.L10N;
import com.caucho.util.Log;

import javax.sql.DataSource;
import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Abstract way of grabbing data from the JDBC connection.
 */
public class JdbcMetaData {
  private static final L10N L = new L10N(JdbcMetaData.class);
  private static final Logger log = Log.open(JdbcMetaData.class);

  private DataSource _ds;

  /**
   * Create a new JDBC backing store.
   */
  protected JdbcMetaData(DataSource ds)
  {
    _ds = ds;
  }

  /**
   * Create based on the connection.
   */
  public static JdbcMetaData create(DataSource ds)
  {
    Connection conn = null;

    try {
      conn = ds.getConnection();

      DatabaseMetaData md = conn.getMetaData();

      String name = md.getDatabaseProductName();

      log.fine(L.l("Database '{0}' metadata.", name));

      if ("oracle".equalsIgnoreCase(name))
	return new OracleMetaData(ds);
      else if ("resin".equalsIgnoreCase(name))
	return new ResinMetaData(ds);
      else if ("postgres".equalsIgnoreCase(name) ||
	       "PostgreSQL".equalsIgnoreCase(name))
	return new PostgresMetaData(ds);
      else if ("mysql".equalsIgnoreCase(name))
	return new MysqlMetaData(ds);
      else if ("Microsoft SQL Server".equalsIgnoreCase(name))
	return new SqlServerMetaData(ds);
      else {
	log.fine(name + " is an unknown database type");
	return new JdbcMetaData(ds);
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
      return new JdbcMetaData(ds);
    } finally {
      try {
	if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }
  }

  /**
   * Returns the database name.
   */
  public String getDatabaseName()
  {
    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();

      return md.getDatabaseProductName();
    } catch (SQLException e) {
      log.log(Level.WARNING, e.toString(), e);
      return "unknown";
    } finally {
      try {
	if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }
  }

  /**
   * Returns the blob type.
   */
  public String getBlobType()
  {
    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();
      ResultSet rs;

      rs = md.getTypeInfo();
      try {
	while (rs.next()) {
	  if (rs.getShort("DATA_TYPE") == Types.BLOB) {
	    return rs.getString("TYPE_NAME");
	  }
	}
      } finally {
	rs.close();
      }

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          int dataType = rs.getShort("DATA_TYPE");

          if (rs.getShort("DATA_TYPE") == Types.LONGVARBINARY) {
            return rs.getString("TYPE_NAME");
          }
        }
      } finally {
        rs.close();
      }

      rs = md.getTypeInfo();
      try {
        while (rs.next()) {
          if (rs.getShort("DATA_TYPE") == Types.BINARY) {
            return rs.getString("TYPE_NAME");
          }
        }
      } finally {
        rs.close();
      }

      rs = md.getTypeInfo();
      try {
	while (rs.next()) {
	  if (rs.getShort("DATA_TYPE") == Types.VARBINARY) {
	    return rs.getString("TYPE_NAME");
	  }
	}
      } finally {
	rs.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
	if (conn != null)
	  conn.close();
      } catch (Exception e) {
      }
    }

    return null;
  }

  /**
   * Returns the long type.
   */
  public String getLongType()
  {
    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();
      ResultSet rs;

      rs = md.getTypeInfo();
      try {
	while (rs.next()) {
	  if (rs.getShort("DATA_TYPE") == Types.BIGINT) {
	    return rs.getString("TYPE_NAME");
	  }
	}
      } finally {
	rs.close();
      }
    } catch (SQLException e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
	if (conn != null) conn.close();
      } catch (SQLException e) {
      }
    }

    return null;
  }

  /**
   * Returns true if identity is supported.
   */
  public boolean supportsIdentity()
  {
    return false;
  }

  /**
   * Returns the identity property
   */
  public String createIdentitySQL(String sqlType)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns true if sequences are supported.
   */
  public boolean supportsSequences()
  {
    return false;
  }

  /**
   * Returns a sequence select expression.
   */
  public String createSequenceSQL(String name, int size)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a sequence select expression.
   */
  public String selectSequenceSQL(String name)
  {
    throw new UnsupportedOperationException(getClass().getName());
  }

  /**
   * Returns a sequence select expression.
   */
  public String testSequenceSQL(String name)
  {
    return selectSequenceSQL(name) + " WHERE 1=0";
  }

  /**
   * Returns the code to test for a boolean value for a term.
   */
  public String generateBoolean(String term)
  {
    return term;
  }

  /**
   * Returns a limit.
   */
  public String limit(String sql, int max)
  {
    return sql;
  }
  /**
   * New version to Return SQL for the table with the given
   * SQL type.  Takes, length, precision and scale.
   */
  public String getCreateColumnSQL(int sqlType, int length, int precision, int scale)
  {
    String type = null;

    switch (sqlType) {
    case Types.BOOLEAN:
      type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
      if (type == null)
        type = getCreateColumnSQLImpl(Types.BIT, length, precision, scale);
      break;

    case Types.DATE:
      type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
      if (type == null)
	type = getCreateColumnSQLImpl(Types.TIMESTAMP, length, precision, scale);
      break;

    case Types.TIME:
      type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
      if (type == null)
	type = getCreateColumnSQLImpl(Types.TIMESTAMP, length, precision, scale);
      break;

    case Types.DOUBLE:
      type = getCreateColumnSQLImpl(Types.DOUBLE, length, precision, scale);
      break;

    case Types.NUMERIC:
        type = getCreateColumnSQLImpl(Types.NUMERIC, length, precision, scale);
        break;

    default:
      type = getCreateColumnSQLImpl(sqlType, length, precision, scale);
      break;
    }

    if (type == null)
      type = getDefaultCreateTableSQL(sqlType, length, precision, scale);

    return type;
  }

  /**
   * Returns the SQL for the table with the given SQL type.
   */
  protected String getCreateColumnSQLImpl(int sqlType, int length, int precision, int scale)
  {
    Connection conn = null;

    try {
      conn = getConnection();

      DatabaseMetaData md = conn.getMetaData();
      ResultSet rs;

      rs = md.getTypeInfo();

      try {
	while (rs.next()) {
	  if (rs.getShort("DATA_TYPE") == sqlType) {
	    String typeName = rs.getString("TYPE_NAME");
	    String params = rs.getString("CREATE_PARAMS");

	    if (params == null || params.equals(""))
	      return typeName;
	    else if (params.startsWith("(M)")) {
	      if (length > 0)
		return typeName + "(" + length + ")";
	      else
		return typeName;
	    }
	    else if (params.startsWith("(M,D)") || params.equals("precision,scale")) {
              if (precision > 0) {
                typeName += "(" + precision;
                if (scale > 0) {
                  typeName += "," + scale;
                }
                typeName += ")";
              }
	      return typeName;
	    }
	    else if (params.startsWith("(")) {
	      int tail = params.indexOf(')');

	      if (tail > 0) {
		String value = params.substring(1, tail);
		boolean isConstant = true;

		for (int i = 0; i < value.length(); i++) {
		  if (value.charAt(i) >= 'a' && value.charAt(i) <= 'z')
		    isConstant = false;
		  else if (value.charAt(i) >= 'A' && value.charAt(i) <= 'Z')
		    isConstant = false;
		}

		if (isConstant)
		  return typeName + "(" + value + ")";
	      }

	      return typeName;
	    }
	    else {
	      return typeName;
	    }
	  }
	}
      } finally {
	rs.close();
      }
    } catch (Throwable e) {
      log.log(Level.FINE, e.toString(), e);
    } finally {
      try {
	if (conn != null)
	  conn.close();
      } catch (Exception e) {
      }
    }

    return null;
  }

  protected String getDefaultCreateTableSQL(int sqlType, int length, int precision, int scale)
  {
    switch (sqlType) {
    case java.sql.Types.BOOLEAN:
      return "CHAR";
    case java.sql.Types.BIT:
    case java.sql.Types.TINYINT:
    case java.sql.Types.SMALLINT:
    case java.sql.Types.INTEGER:
    case java.sql.Types.BIGINT:
      return "INTEGER";
    case java.sql.Types.NUMERIC:
    case java.sql.Types.DECIMAL:
      String typeString = "NUMERIC";
      if (precision > 0) {
        typeString += "(" + precision;
        if (scale > 0) {
          typeString += "," + scale;
        }
        typeString += ")";
      }
      return typeString;
    case java.sql.Types.DOUBLE:
    case java.sql.Types.FLOAT:
      return "DOUBLE";
    case java.sql.Types.CHAR:
      return "CHAR";
    case java.sql.Types.DATE:
    case java.sql.Types.TIME:
    case java.sql.Types.TIMESTAMP:
        return "TIMESTAMP";
    default:
      return "VARCHAR(" + length + ")";
    }
  }

  /**
   * Returns a connection.
   */
  protected Connection getConnection()
    throws SQLException
  {
    return _ds.getConnection();
  }
}
