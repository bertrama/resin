package example;

import java.io.PrintWriter;
import java.io.IOException;

import java.util.List;
import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import javax.servlet.ServletException;

import javax.ejb.Query;

import com.caucho.ejb.entity2.EntityManagerProxy;

/**
 * A client to illustrate the many-to-many relation.
 */
public class ManyToManyServlet extends HttpServlet {

  private EntityManagerProxy _entityManager;

  /**
   * Sets the entity manager.
   */
  public void setEntityManager(EntityManagerProxy entityManager)
  {
    _entityManager = entityManager;
  }

  public void init()
  {
    Student student = null;
      
    try {
      student = (Student) _entityManager.find("Student", new Long(1));
    } catch (Throwable e) {
    }

    if (student == null) {
      Student harry = new Student("Harry Potter");
      _entityManager.create(harry);
	
      Student ron = new Student("Ron Weasley");
      _entityManager.create(ron);
	
      Student hermione = new Student("Hermione Granger");
      _entityManager.create(hermione);
	
      Course darkArts = new Course("Defense Against the Dark Arts");
      _entityManager.create(darkArts);
	
      Course potions = new Course("Potions");
      _entityManager.create(potions);
	
      Course divination = new Course("Divination");
      _entityManager.create(divination);
	
      Course arithmancy = new Course("Arithmancy");
      _entityManager.create(arithmancy);
	
      Course transfiguration = new Course("Transfiguration");
      _entityManager.create(transfiguration);
	
      Grade grade;

      _entityManager.create(new Grade(harry, darkArts, "A"));
      _entityManager.create(new Grade(harry, potions, "C-"));
      _entityManager.create(new Grade(harry, transfiguration, "B+"));
      _entityManager.create(new Grade(harry, divination, "B"));

      _entityManager.create(new Grade(ron, darkArts, "A-"));
      _entityManager.create(new Grade(ron, potions, "C+"));
      _entityManager.create(new Grade(ron, transfiguration, "B"));
      _entityManager.create(new Grade(ron, divination, "B+"));

      _entityManager.create(new Grade(hermione, darkArts, "A+"));
      _entityManager.create(new Grade(hermione, potions, "A-"));
      _entityManager.create(new Grade(hermione, transfiguration, "A+"));
      _entityManager.create(new Grade(hermione, arithmancy, "A+"));
    }
  }

  public void service(HttpServletRequest req, HttpServletResponse res)
    throws java.io.IOException, ServletException
  {
    PrintWriter out = res.getWriter();

    res.setContentType("text/html");

    Query allStudent = _entityManager.createQuery("SELECT o FROM Student o");
    
    List students = allStudent.listResults();

    for (int i = 0; i < students.size(); i++) {
      Student student = (Student) students.get(i);

      out.println("<h3>" + student.getName() + "</h3>");

      Collection courses = student.getCourses();

      out.println("<ul>");
      Iterator iter = courses.iterator();
      while (iter.hasNext()) {
	Course course = (Course) iter.next();

	out.println("<li>" + course.getName());
      }
      out.println("</ul>");
    }
  }
}
