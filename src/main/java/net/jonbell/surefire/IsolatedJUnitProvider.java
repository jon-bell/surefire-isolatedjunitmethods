package net.jonbell.surefire;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.apache.maven.shared.utils.io.SelectorUtils;
import org.apache.maven.surefire.common.junit3.JUnit3TestChecker;
import org.apache.maven.surefire.common.junit4.JUnit4RunListener;
import org.apache.maven.surefire.common.junit4.JUnit4RunListenerFactory;
import org.apache.maven.surefire.common.junit4.JUnit4TestChecker;
import org.apache.maven.surefire.providerapi.AbstractProvider;
import org.apache.maven.surefire.providerapi.ProviderParameters;
import org.apache.maven.surefire.report.ConsoleOutputCapture;
import org.apache.maven.surefire.report.ConsoleOutputReceiver;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterFactory;
import org.apache.maven.surefire.report.RunListener;
import org.apache.maven.surefire.report.SimpleReportEntry;
import org.apache.maven.surefire.suite.RunResult;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.apache.maven.surefire.util.RunOrderCalculator;
import org.apache.maven.surefire.util.ScanResult;
import org.apache.maven.surefire.util.TestsToRun;
import org.junit.runner.Description;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.notification.RunNotifier;

public class IsolatedJUnitProvider extends AbstractProvider {
	private final ClassLoader testClassLoader;

	private final ProviderParameters providerParameters;

	private final RunOrderCalculator runOrderCalculator;

	private final ScanResult scanResult;
	private JUnit3TestChecker jUnit3TestChecker;
	private JUnit4TestChecker junit4TestChecker;

	private PojoAndJUnit3Checker v3testChecker;

	private List<org.junit.runner.notification.RunListener> customRunListeners;

	boolean isJunit4;

	PrintWriter filePrinter;

	public IsolatedJUnitProvider(ProviderParameters booterParameters) {
		this.providerParameters = booterParameters;
		this.testClassLoader = booterParameters.getTestClassLoader();
		this.scanResult = booterParameters.getScanResult();
		this.runOrderCalculator = booterParameters.getRunOrderCalculator();
		String mode = providerParameters.getProviderProperties().getProperty("compatMode");
		if (!"junit4".equals(mode) && !"junit3".equals(mode))
			mode = "junit4";
		this.isJunit4 = "junit4".equals(mode);
		if (isJunit4) {
			junit4TestChecker = new JUnit4TestChecker(testClassLoader);
		} else {
			jUnit3TestChecker = new JUnit3TestChecker(testClassLoader);
			this.v3testChecker = new PojoAndJUnit3Checker(jUnit3TestChecker);
		}
		customRunListeners = JUnit4RunListenerFactory.createCustomListeners(providerParameters.getProviderProperties().getProperty("listener"));
	}

	private TestsToRun scanClassPath() {
		if (isJunit4) {
			final TestsToRun testsToRun = scanResult.applyFilter(junit4TestChecker, testClassLoader);
			return runOrderCalculator.orderTestClasses(testsToRun);
		} else {
			final TestsToRun testsToRun = scanResult.applyFilter(v3testChecker, testClassLoader);
			return runOrderCalculator.orderTestClasses(testsToRun);
		}
	}

	public Iterator<?> getSuites() {
		TestMethodsToRun toRun = new TestMethodsToRun(scanClassPath(), isJunit4);
		return toRun.iterator();
	}

	public RunResult invoke(Object forkTestSet) throws TestSetFailedException, ReporterException, InvocationTargetException {
		if (forkTestSet == null || !(forkTestSet instanceof String))
			throw new TestSetFailedException("IsolatedJUnitProvider requires reuseForks=false");
		String toRun = (String) forkTestSet;
		try {
			String[] d = toRun.split("#");
			Class<?> tc = testClassLoader.loadClass(d[0]);
			String method = d[1];
			return invokeJunit4(tc, method);
		} catch (ClassNotFoundException e) {
			throw new TestSetFailedException(e);
		}
	}

	private RunNotifier getRunNotifier(org.junit.runner.notification.RunListener main, Result result, List<org.junit.runner.notification.RunListener> others) {
		RunNotifier fNotifier = new RunNotifier();
		fNotifier.addListener(main);
		fNotifier.addListener(result.createListener());
		for (org.junit.runner.notification.RunListener listener : others) {
			fNotifier.addListener(listener);
		}
		return fNotifier;
	}

	//Mostly taken from Junit4Provider.java
	public RunResult invokeJunit4(Class<?> clazz, String requestedTestMethod) throws TestSetFailedException {

		final ReporterFactory reporterFactory = providerParameters.getReporterFactory();

		RunListener reporter = reporterFactory.createReporter();

		ConsoleOutputCapture.startCapture((ConsoleOutputReceiver) reporter);

		JUnit4RunListener jUnit4TestSetReporter = new JUnit4RunListener(reporter);

		Result result = new Result();
		RunNotifier listeners = getRunNotifier(jUnit4TestSetReporter, result, customRunListeners);

		listeners.fireTestRunStarted(Description.createTestDescription(clazz, requestedTestMethod));

		final ReportEntry report = new SimpleReportEntry(getClass().getName() + "." + requestedTestMethod, clazz.getName() + "." + requestedTestMethod);
		reporter.testSetStarting(report);
		try {
			for (final Method method : clazz.getMethods()) {
				if (method.getParameterTypes().length == 0 && requestedTestMethod.equals(method.getName())) {
					Request.method(clazz, method.getName()).getRunner().run(listeners);
				}
			}
		} catch (Throwable e) {
			reporter.testError(SimpleReportEntry.withException(report.getSourceName(), report.getName(), new PojoStackTraceWriter(report.getSourceName(), report.getName(), e)));
		} finally {
			reporter.testSetCompleted(report);
		}
		listeners.fireTestRunFinished(result);
		JUnit4RunListener.rethrowAnyTestMechanismFailures(result);

		closeRunNotifier(jUnit4TestSetReporter, customRunListeners);
		return reporterFactory.close();
	}

	private void closeRunNotifier(org.junit.runner.notification.RunListener main, List<org.junit.runner.notification.RunListener> others) {
		RunNotifier fNotifier = new RunNotifier();
		fNotifier.removeListener(main);
		for (org.junit.runner.notification.RunListener listener : others) {
			fNotifier.removeListener(listener);
		}
	}

}
