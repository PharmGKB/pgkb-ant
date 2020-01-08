package org.pharmgkb.ant;

import org.apache.tools.ant.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * This is a JUnit test for {@link ExpandPropertiesTask}.
 *
 * @author Mark Woon
 */
public class ExpandPropertiesTaskTest {

  @Test
  public void testTask() {

    Project project = new Project();
    project.setUserProperty("scheme", "https");
    project.setUserProperty("server.www", "www.pharmgkb.org");
    project.setUserProperty("name", "www");
    project.setUserProperty("path", "some/path");
    project.setUserProperty("test.key", "${scheme}://${server.${name}}/${path}");

    ExpandPropertiesTask task = new ExpandPropertiesTask();
    task.setProject(project);
    task.execute();
    System.out.println(project.getUserProperty("test.key"));
    System.out.println(project.getProperty("test.key"));
    assertEquals("https://www.pharmgkb.org/some/path", project.getUserProperty("test.key"));
  }
}
