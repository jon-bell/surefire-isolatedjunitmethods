package net.jonbell.surefire;

import org.apache.maven.surefire.NonAbstractClassFilter;
import org.apache.maven.surefire.common.junit3.JUnit3TestChecker;
import org.apache.maven.surefire.util.ScannerFilter;

public class PojoAndJUnit3Checker implements ScannerFilter {
	private final JUnit3TestChecker jUnit3TestChecker;

	private final NonAbstractClassFilter nonAbstractClassFilter = new NonAbstractClassFilter();

	public PojoAndJUnit3Checker(JUnit3TestChecker jUnit3TestChecker) {
		this.jUnit3TestChecker = jUnit3TestChecker;
	}

	public boolean accept(@SuppressWarnings("rawtypes") Class testClass) {
		return jUnit3TestChecker.accept(testClass) || nonAbstractClassFilter.accept(testClass) && isPojoTest(testClass);
	}

	@SuppressWarnings("unchecked")
	private boolean isPojoTest(@SuppressWarnings("rawtypes") Class testClass) {
		try {
			testClass.getConstructor(new Class[0]);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

}
