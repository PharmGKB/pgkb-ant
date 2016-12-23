package org.pharmgkb.ant;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Property;


/**
 * This task expands the value based on existing user properties.  This allows embedding multiple/nested properties such
 * as: {@code value="${scheme}://${server.${name}}/${path}"}.
 *
 * @author Mark Woon
 */
public class ExpandingPropertyTask extends Property {


  @Override
  public void execute() throws BuildException {
    if (getValue() != null) {
      setValue(resolveValue(getProject(), getName(), getValue()));
    }
    super.execute();
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
