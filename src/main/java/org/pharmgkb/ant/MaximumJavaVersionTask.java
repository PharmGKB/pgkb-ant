package org.pharmgkb.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Property;

import static org.pharmgkb.ant.MinimumJavaVersionTask.convertVersionString;


/**
 * This task checks that the Java version used to build the project meets a specified minimum.
 *
 * @author Mark Woon
 */
public class MaximumJavaVersionTask extends Property {
  private String m_version;

  public void setVersion(String version) {
    m_version = version;
  }

  @Override
  public void execute() throws BuildException {

    if (m_version == null) {
      throw new BuildException("Missing version parameter");
    }

    String curVersionString = System.getProperty("java.version");
    if (convertVersionString(curVersionString) > convertVersionString(m_version)) {
      throw new BuildException("Build requires maximum Java version of " + m_version + " but running on " +
          curVersionString);
    }
  }
}
