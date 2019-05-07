package org.pharmgkb.ant;

import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import junit.framework.AssertionFailedError;
import junit.framework.Test;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitResultFormatter;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTest;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitVersionHelper;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.StringUtils;


/**
 * This is a JUnit result formatter that is designed to print test results on the command-line as cleanly as possible.
 * <p>
 * Based on http://shaman-sir.wikidot.com/one-liner-output-formatter.
 *
 * @author Mark Woon
 */
public class ResultFormatter implements JUnitResultFormatter {
  private final String sf_tabString = "    ";
  // (\w+\.)+(\w+)\((\w+).(?:\w+):(\d+)\)
  private final Pattern sf_traceLinePattern = Pattern.compile("(\\w+\\.)+(\\w+)\\((\\w+).(?:\\w+):(\\d+)\\)");

  /** Where to write the log to. */
  private OutputStream m_outputStream;
  /** Used for writing the results. */
  private PrintWriter m_outputWriter;

  /** Holds the formatted results. */
  private StringWriter m_errors = new StringWriter();
  /** Used for writing formatted results. */
  private PrintWriter m_errorWriter = new PrintWriter(m_errors);

  /** Holds the output a test suite has written to {@link System#out}. */
  private String m_systemOutput = null;
  /** Holds the output a test suite has written to {@link System#err}. */
  private String m_systemError = null;

  /** Tests that failed. */
  private Set<Test> m_failedTests = new HashSet<>();

  /** Maps tests to start times. */
  private Map<Test, Long> m_testStarts = new HashMap<>();
  /** Formatter for timings. */
  private NumberFormat m_numberFormat = NumberFormat.getInstance();

  private boolean m_showSystemOut = false;
  private boolean m_showSystemErr = true;
  private boolean m_showCausesLines = true;



  private void resetErrorWriter() {
    m_errors = new StringWriter();
    m_errorWriter = new PrintWriter(m_errors);
  }


  /**
   * Sets the stream the formatter is supposed to write its results to.
   *
   * @param out the output stream to write to
   */
  @Override
  public void setOutput(OutputStream out) {
    m_outputStream = out;
    m_outputWriter = new PrintWriter(out);
  }


  /**
   * @see JUnitResultFormatter#setSystemOutput(String)
   */
  @Override
  public void setSystemOutput(String out) {
    m_systemOutput = out;
  }

  /**
   * @see JUnitResultFormatter#setSystemError(String)
   */
  @Override
  public void setSystemError(String err) {
    m_systemError = err;
  }


  /**
   * The whole test suite started.
   *
   * @param suite the test suite
   */
  @Override
  public void startTestSuite(JUnitTest suite) {

    if (m_outputWriter == null) {
      return;
    }
    m_outputWriter.println();
    m_outputWriter.println("----------------------------------------------------------");
    m_outputWriter.println("Test suite: " + suite.getName());
    m_outputWriter.flush();
  }

  /**
   * The whole test suite ended.
   *
   * @param suite the test suite
   */
  @Override
  public void endTestSuite(JUnitTest suite) {
    StringBuilder sb = new StringBuilder("Tests run: ");
    sb.append(suite.runCount());
    sb.append(", Failures: ");
    sb.append(suite.failureCount());
    sb.append(", Errors: ");
    sb.append(suite.errorCount());
    sb.append(", Time elapsed: ");
    sb.append(m_numberFormat.format(suite.getRunTime() / 1000.0));
    sb.append(" sec");
    sb.append(System.lineSeparator());

    // append the err and output streams to the log
    if (m_showSystemOut) {
      if (m_systemOutput != null && m_systemOutput.length() > 0) {
        sb.append("------------- Standard Output ---------------")
            .append(System.lineSeparator())
            .append(m_systemOutput)
            .append("------------- ---------------- ---------------")
            .append(System.lineSeparator());
      }
    }
    if (m_showSystemErr) {
      if (m_systemError != null && m_systemError.length() > 0) {
        sb.append("------------- Standard Error -----------------")
            .append(System.lineSeparator())
            .append(m_systemError)
            .append("------------- ---------------- ---------------")
            .append(System.lineSeparator());
      }
    }

    if (m_outputWriter != null) {
      try {
        m_outputWriter.write(sb.toString());
        // print errors, if any
        m_errorWriter.flush();
        String errors = m_errors.toString();
        if (errors.length() > 0) {
          m_outputWriter.println();
          m_outputWriter.println(errors);
        }
        m_outputWriter.flush();

        resetErrorWriter();

      } finally {
        if (m_outputStream != System.out && m_outputStream != System.err) {
          FileUtils.close(m_outputStream);
        }
      }
    }
  }

  /**
   * A test started.
   */
  @Override
  public void startTest(Test test) {
    m_testStarts.put(test, System.currentTimeMillis());
  }

  /**
   * A test ended.
   */
  @Override
  public void endTest(Test test) {
    // Fix for bug #5637 - if a junit.extensions.TestSetup is used and throws an exception during setUp then startTest
    // would never have been called
    if (!m_testStarts.containsKey(test)) {
      startTest(test);
    }

    boolean failed = m_failedTests.contains(test);
    Long startTestTimestamp = m_testStarts.get(test);

    m_outputWriter.print("Ran [");
    m_outputWriter.print(((System.currentTimeMillis() - startTestTimestamp) / 1000.0) + "] ");
    m_outputWriter.print(getTestName(test) + " ... " + (failed ? "FAILED" : "OK"));
    m_outputWriter.println();
    m_outputWriter.flush();
  }


  /**
   * A test failed.
   */
  @Override
  public void addFailure(Test test, AssertionFailedError t) {
    formatError("\tFAILED", test, t);
  }

  /**
   * A test caused an error.
   */
  @Override
  public void addError(Test test, Throwable error) {
    formatError("\tCaused an ERROR", test, error);
  }

  /**
   * Gets test name.
   */
  private String getTestName(Test test) {
    if (test == null) {
      return "null";
    } else {
      return JUnitVersionHelper.getTestCaseName(test);
    }
  }

  /**
   * Get test case full class name
   */
  private String getTestCaseClassName(Test test) {
    if (test == null) {
      return "null";
    } else {
      return JUnitVersionHelper.getTestCaseClassName(test);
    }
  }

  /**
   * Format the test for printing.
   */
  private String formatTest(Test test) {
    if (test == null) {
      return "Null Test: ";
    } else {
      String classname = getTestCaseClassName(test);
      int idx = classname.lastIndexOf(".");
      if (idx > -1) {
        classname = classname.substring(idx + 1);
      }
      return getTestName(test) + " in " + classname + ":";
    }
  }

  /**
   * Formats an error.
   */
  private synchronized void formatError(String type, Test test, Throwable error) {

    if (test != null) {
      m_failedTests.add(test);
    }

    m_errorWriter.println(formatTest(test) + type);
    m_errorWriter.println(sf_tabString + "(" + error.getClass().getSimpleName() + "): " +
        ((error.getMessage() != null) ? error.getMessage() : error));

    if (m_showCausesLines) {
      // resultWriter.append(System.lineSeparator());
      m_errorWriter.println(filterErrorTrace(test, error));
    }

    m_errorWriter.println();

    /* String strace = JUnitTestRunner.getFilteredTrace(error);
        resultWriter.println(strace);
        resultWriter.println(); */
  }

  private String filterErrorTrace(Test test, Throwable error) {
    String trace = StringUtils.getStackTrace(error);
    StringWriter sw = new StringWriter();
    PrintWriter pw = new PrintWriter(sw);
    StringReader sr = new StringReader(trace);
    BufferedReader br = new BufferedReader(sr);

    String line;
    try {
      while ((line = br.readLine()) != null) {
        if (line.contains(getTestCaseClassName(test))) {
          Matcher matcher = sf_traceLinePattern.matcher(line);
          // pw.println(matcher + ": " + matcher.find());
          if (matcher.find()) {
            pw.print(sf_tabString);
            pw.print("(" + matcher.group(3) + ") ");
            pw.print(matcher.group(2) + ": ");
            pw.println(matcher.group(4));
          } else {
            pw.println(line);
          }

        }
      }
    } catch (Exception e) {
      // return the trace unfiltered
      return trace;
    }

    return sw.toString();
  }
}
