package example;

import java.util.Collection;

import javax.ejb.Entity;
import javax.ejb.Table;
import javax.ejb.Id;
import javax.ejb.Basic;
import javax.ejb.Column;
import javax.ejb.DiscriminatorColumn;
import javax.ejb.Inheritance;
import static javax.ejb.AccessType.*;
import static javax.ejb.GeneratorType.*;

/**
 * Implementation class for the Student bean.
 *
 * <code><pre>
 * CREATE TABLE ejb3_inherit_student (
 *   id INTEGER PRIMARY KEY auto_increment,
 *   type VARCHAR(10),
 *   name VARCHAR(250),
 * );
 * </pre></code>
 */
@Entity(access=FIELD)
@Inheritance(discriminatorValue="prefect")
public class Prefect extends Student {
  public Prefect()
  {
  }

  public Prefect(String name)
  {
    super(name);
  }

  public String toString()
  {
    return "Prefect[" + getName() + "]";
  }
}
