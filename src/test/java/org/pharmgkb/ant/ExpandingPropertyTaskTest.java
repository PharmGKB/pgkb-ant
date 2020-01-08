package org.pharmgkb.ant;

import org.apache.tools.ant.Project;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This is a JUnit test for {@link ExpandingPropertyTask}.
 *
 * @author Mark Woon
 */
public class ExpandingPropertyTaskTest {
  private Project m_project;

  @BeforeEach
  public void setup() {
    m_project = new Project();
    m_project.setUserProperty("scheme", "https");
    m_project.setUserProperty("server.www", "www.pharmgkb.org");
    m_project.setUserProperty("name", "www");
    m_project.setUserProperty("path", "some/path");
  }


  @Test
  public void testResolveValue() {

    String value = ExpandingPropertyTask.resolveValue(m_project, "key", "${scheme}://${server.${name}}/${path}");
    assertEquals("https://www.pharmgkb.org/some/path", value);
  }


  @Test
  public void testTask() {

    ExpandingPropertyTask task = new ExpandingPropertyTask();
    task.setProject(m_project);
    task.setName("key");
    task.setValue("${scheme}://${server.${name}}/${path}");
    task.execute();
    assertEquals("https://www.pharmgkb.org/some/path", m_project.getProperty("key"));
  }
}
