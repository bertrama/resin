/**
 * object interface generated by Resin-EE doclet.
 * on Thu, 27 Mar 2003 19:58:15 -0800 (PST)
 */

package example;

public interface Course
  extends javax.ejb.EJBLocalObject {

  public java.lang.String getName();

  public example.Teacher getTeacher();
}