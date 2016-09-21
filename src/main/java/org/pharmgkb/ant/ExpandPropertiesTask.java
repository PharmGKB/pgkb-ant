package org.pharmgkb.ant;

import java.util.Map;
import org.apache.tools.ant.BuildException;
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

    Map<String, Object> map = getProject().getProperties();
    for (String key : map.keySet()) {
      String value = (String)map.get(key);

      int endIdx = value.indexOf("}");
      while (endIdx != -1) {
        int startIdx = value.lastIndexOf("${", endIdx);
        if (startIdx == -1) {
          throw new BuildException("Found '}' but cannot find matching '${' while trying to expand '" + key + "': '" + value + "'");
        }
        String prop = value.substring(startIdx, endIdx);
        String subKey = prop.substring(2);
        String subValue = (String)getProject().getProperties().get(subKey);
        if (subValue == null) {
          throw new BuildException("Cannot find value for '" + subKey + "' in key '" + key + "'");
        } else if (subValue.contains("${" + subKey + "}")) {
          throw new BuildException("Recursive keys: [" + key + "] --> [" + subKey + "] --> [" + subValue + "]");
        }
        value = value.substring(0, startIdx) + subValue + value.substring(endIdx + 1);
        getProject().setUserProperty(key, value);
        endIdx = value.indexOf("}");
      }
    }
  }
}
