package org.pharmgkb.ant;

import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestExecutionContext;
import org.apache.tools.ant.taskdefs.optional.junitlauncher.TestResultFormatter;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.TestExecutionResult.Status;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.reporting.ReportEntry;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.junit.platform.launcher.TestPlan;


/**
 * This is a JUnit 5 result formatter that is designed to print test results on the command-line as cleanly as possible.
 * <p>
 * This expects the property {@code testReportDir} to be set, and will generate a summary report named {@code index.txt}
 * in that directory.
 * <p>
 * If the property {@code testReportProblemsOnly} is set to true, only skipped/failed/aborted tests will be listed
 * (no stats).
 * <p>
 * Based on https://www.selikoff.net/2018/07/28/ant-and-junit-5-outputting-test-duration-and-failure-to-the-log/
 *
 * @author Mark Woon
 */
public class JupiterResultFormatter implements TestResultFormatter {
  /** Theoretically used for writing the results.  Doesn't seem to actually do anything. */
  private PrintWriter m_outputWriter;
  private String m_filename;
  private boolean m_problemsOnly;

  private TestPlan m_currentTestPlan;
  private StringWriter m_testResults;
  private PrintWriter m_testPrinter;
  private Map<String, TestClassStats> m_testedClasses = new HashMap<>();



  //-- BEGIN TestResultFormatter methods --//

  @Override
  public void setDestination(OutputStream os) {
    m_outputWriter = new PrintWriter(os);
  }

  @Override
  public void setContext(TestExecutionContext context) {
    String dir = context.getProperties().getProperty("testReportDir", "build");
    if (dir.endsWith("/")) {
      dir = dir.substring(0, dir.length() - 1);
    }
    m_filename = dir + "/index.txt";

    String problemsOnly = context.getProperties().getProperty("testReportProblemsOnly", "true");
    m_problemsOnly = Boolean.parseBoolean(problemsOnly);
  }


  @Override
  public void sysOutAvailable(byte[] data) {
    // ignored
  }

  @Override
  public void sysErrAvailable(byte[] data) {
    // ignored
  }

  //-- END TestResultFormatter methods --//


  @Override
  public void close() {
  }

  private boolean isEngineContainer(TestIdentifier testIdentifier) {
    return "[engine:junit-jupiter]".equals(testIdentifier.getUniqueId());
  }

  private String getParentClassName(TestIdentifier testIdentifier) {
    if (testIdentifier.getSource().isPresent()) {
      TestSource source = testIdentifier.getSource().get();
      if (source instanceof MethodSource) {
        return ((MethodSource)source).getClassName();
      }
      if (source instanceof ClassSource) {
        return ((ClassSource)source).getClassName();
      }
      throw new UnsupportedOperationException("Unexpected source class: " + source.getClass());
    }
    throw new IllegalStateException("Cannot get parent class name");
  }

  private String getFullTestName(TestIdentifier testIdentifier) {
    if (testIdentifier.getType() == TestDescriptor.Type.TEST) {
      return getParentClassName(testIdentifier) + "." + testIdentifier.getDisplayName();
    }
    return getParentClassName(testIdentifier);
  }


  private TestClassStats getStats(TestIdentifier testIdentifier) {
    return m_testedClasses.computeIfAbsent(getParentClassName(testIdentifier), TestClassStats::new);
  }

  private void println(String txt) {
    System.out.println(txt);
    m_testPrinter.println(txt);
    m_outputWriter.println(txt);
  }



  //-- BEGIN TestExecutionListener methods --//

  @Override
  public void testPlanExecutionStarted(TestPlan testPlan) {
    m_currentTestPlan = testPlan;
    m_testResults = new StringWriter();
    m_testPrinter = new PrintWriter(m_testResults);
    m_testedClasses.clear();
  }

  @Override
  public void testPlanExecutionFinished(TestPlan testPlan) {
    if (m_problemsOnly) {
      m_testedClasses.values().stream()
          .filter(TestClassStats::hasProblem)
          .forEach(stats -> m_testPrinter.println(stats));
    } else {
      m_testedClasses.values().forEach(stats -> {
        stats.writeStats();
        println(stats.toString());
      });
    }
    m_testPrinter.flush();

    try (FileWriter writer = new FileWriter(m_filename, true)) {
      writer.write(m_testResults.toString());
    } catch (IOException ex) {
      throw new UncheckedIOException("Error writing test results", ex);
    }
  }


  @Override
  public void dynamicTestRegistered(TestIdentifier testIdentifier) {
    // TODO(markwoon): not sure how to use this yet
    println("dynamicTestRegistered: " + getFullTestName(testIdentifier));
  }


  @Override
  public void executionSkipped(TestIdentifier testIdentifier, String reason) {
    TestClassStats stats = getStats(testIdentifier);
    stats.skipped(testIdentifier, reason);
  }

  @Override
  public void executionStarted(TestIdentifier testIdentifier) {
    if (isEngineContainer(testIdentifier)) {
      return;
    }
    if (testIdentifier.isContainer()) {
      // initialize stats
      getStats(testIdentifier);
    }
  }

  @Override
  public void executionFinished(TestIdentifier testIdentifier, TestExecutionResult testExecutionResult) {
    if (isEngineContainer(testIdentifier)) {
      return;
    }
    if (testIdentifier.isTest()) {
      TestClassStats stats = getStats(testIdentifier);
      if (testExecutionResult.getStatus() == Status.SUCCESSFUL) {
        stats.succeeded();
      } else if (testExecutionResult.getStatus() == Status.ABORTED) {
        stats.aborted(testIdentifier);
      } else if (testExecutionResult.getStatus() == Status.FAILED) {
        stats.failed(testIdentifier);
      }
    }
  }


  @Override
  public void reportingEntryPublished(TestIdentifier testIdentifier, ReportEntry entry) {
  }
  //-- END TestExecutionListener methods --//



  private final class TestClassStats {
    String className;
    private int m_skipped;
    private int m_succeeded;
    private int m_aborted;
    private int m_failed;
    private Instant m_started = Instant.now();
    private StringWriter m_buffer = new StringWriter();

    TestClassStats(String className) {
      this.className = className;
      println(this.className);
    }

    void println(String text) {
      m_buffer.append(text)
          .append("\n");
    }

    void skipped(TestIdentifier testIdentifier, String reason) {
      if (testIdentifier.isContainer()) {
        for (TestIdentifier tid : m_currentTestPlan.getChildren(testIdentifier)) {
          if (tid.isTest()) {
            m_skipped += 1;
            println("  SKIPPED: " + tid.getDisplayName() + " (" + reason + ")");
          }
        }
      } else {
        m_skipped += 1;
        String prefix = "void " + getParentClassName(testIdentifier) + ".";
        if (reason.startsWith(prefix)) {
          String method = reason.substring(prefix.length());
          int idx = method.indexOf(" is disabled ");
          if (idx > 0) {
            reason = " (" + method.substring(idx + 4) + ")";
            method = testIdentifier.getDisplayName();
          } else if (method.endsWith("is @Disabled")) {
            method = testIdentifier.getDisplayName();
            reason = " (is @Disabled)";
          } else {
            reason = "";
          }
          println("  SKIPPED: " + method + reason);
        } else {
          println("  SKIPPED: " + testIdentifier.getDisplayName() + " (" + reason + ")");
        }
      }
    }

    void aborted(TestIdentifier testIdentifier) {
      m_aborted += 1;
      println("  ABORTED: " + testIdentifier.getDisplayName());
    }

    void failed(TestIdentifier testIdentifier) {
      println("  FAILED: " + testIdentifier.getDisplayName());
      m_failed += 1;
    }

    void succeeded() {
      m_succeeded += 1;
    }

    boolean hasProblem() {
      return m_skipped > 0 || m_failed > 0 || m_aborted > 0;
    }

    void writeStats() {
      int totalTestsInClass = m_succeeded + m_aborted + m_failed + m_skipped;
      Duration duration = Duration.between(m_started, Instant.now());
      String prettyDuration = duration.toString()
          .substring(2)
          .replaceAll("(\\d[HMS])(?!$)", "$1 ")
          .toLowerCase();
      String output = String.format("  Tests run: %d, Failures: %d, Aborted: %d, Skipped: %d, Time elapsed: %s",
          totalTestsInClass, m_failed, m_aborted, m_skipped, prettyDuration);
      println(output);
    }

    @Override
    public String toString() {
      return m_buffer.toString();
    }
  }
}
