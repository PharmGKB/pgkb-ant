package org.pharmgkb.ant;

import java.util.Map;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;


/**
 * This task goes through existing user properties in the project and expands them.  This allows embedding
 * multiple/nested properties such as:
 * <pre>{@code
 * url = ${scheme}://${server.${name}}/${path}
 * }</pre>
 *
 * @author Mark Woon
 */
public class ExpandPropertiesTask extends Task {


  public void execute() throws BuildException {

    Project project = getProject();
    Map<String, Object> map = project.getProperties();
    for (String key : map.keySet()) {
      if (key.startsWith("env.BASH_FUNC_")) {
        continue;
      }
      String value = (String)map.get(key);
      try {
        String newValue = ExpandingPropertyTask.resolveValue(project, key, value);
        if (!value.equals(newValue)) {
          project.setUserProperty(key, newValue);
        }
      } catch (BuildException ex) {
        throw new BuildException("Error evaluating " + key + "='" + value + "' (" + ex.getMessage() + ")", ex);
      }
    }
  }
}
