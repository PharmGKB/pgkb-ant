package org.pharmgkb.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.Property;


/**
 * This task checks that the Java version used to build the project meets a specified minimum.
 *
 * @author Mark Woon
 */
public class MinimumJavaVersionTask extends Property {
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
    if (convertVersionString(curVersionString) < convertVersionString(m_version)) {
      throw new BuildException("Build requires minimum Java version of " + m_version + " but running on " +
          curVersionString);
    }
  }


  public static int convertVersionString(String version) {
    int idx = version.indexOf(".");
    if (idx != -1) {
      idx = version.indexOf(".", idx + 1);
      if (idx != -1) {
        version = version.substring(0, idx);
      }
      return (int)(Float.parseFloat(version) * 10);
    } else {
      return (int)(Float.parseFloat(version) * 10);
    }
  }
}
