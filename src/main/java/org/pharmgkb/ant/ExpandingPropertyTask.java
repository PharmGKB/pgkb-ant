package org.pharmgkb.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;


/**
 * This task expands the value based on existing user properties.  This allows embedding multiple/nested properties such
 * as: {@code value="${scheme}://${server.${name}}/${path}"}.
 * <p>
 * This behavior is only applied to properties set via the {@code value} attribute.
 * <p>
 * This also allows properties to be overriden via the {@code override} attribute.  This assumes that the message is a
 * user property set.
 *
 * @author Mark Woon
 */
public class ExpandingPropertyTask extends Property {
  private boolean m_override;


  public void setOverride(boolean override) {
    m_override = override;
  }


  @Override
  public void execute() throws BuildException {
    if (getValue() != null) {
      setValue(resolveValue(getProject(), getName(), getValue()));
    }
    if (m_override) {
      getProject().setUserProperty(getName(), getValue());
    } else {
      super.execute();
    }
  }


  static String resolveValue(Project project, String key, String value) {

    int endIdx = value.indexOf("}");
    while (endIdx != -1) {
      int startIdx = value.lastIndexOf("${", endIdx);
      if (startIdx == -1) {
        throw new BuildException("Found '}' but cannot find matching '${' while trying to expand '" + key + "': '" +
            value + "'");
      }
      String prop = value.substring(startIdx, endIdx);
      String subKey = prop.substring(2);
      String subValue = (String)project.getProperties().get(subKey);
      if (subValue == null) {
        throw new BuildException("Cannot find value for '" + subKey + "' in key '" + key + "'");
      } else if (subValue.contains("${" + subKey + "}")) {
        throw new BuildException("Recursive keys: [" + key + "] --> [" + subKey + "] --> [" + subValue + "]");
      }
      value = value.substring(0, startIdx) + subValue + value.substring(endIdx + 1);
      endIdx = value.indexOf("}");
    }
    return value;
  }
}
