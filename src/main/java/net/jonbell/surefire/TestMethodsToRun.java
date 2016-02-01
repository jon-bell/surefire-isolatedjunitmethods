package net.jonbell.surefire;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.maven.surefire.util.TestsToRun;

public class TestMethodsToRun implements Iterable<String> {
	private final List<String> tests;

	public TestMethodsToRun(TestsToRun tcs, boolean isJUnit4) {
		tests = new LinkedList<String>();
		for (Class<?> c : tcs) {
			ArrayList<String> methods = new ArrayList<String>();
			for (Method m : c.getDeclaredMethods()) {
				boolean found = false;
				if (isJUnit4 && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0)
					for (Annotation a : m.getAnnotations()) {
						if (a.annotationType().getCanonicalName().equals("org.junit.Test"))
							found = true;
					}
				if (!isJUnit4 && (m.getName().startsWith("test")) && m.getReturnType() == Void.TYPE && m.getParameterTypes().length == 0) {
					found = true;
				}
				if (found)
					methods.add(m.getName());
			}
			Collections.sort(methods);
			for (String s : methods) {
				tests.add(c.getName() + "#" + s);
			}
		}
	}

	@Override
	public String toString() {
		return "TestMethodsToRun [tests=" + tests + "]";
	}

	public Iterator<String> iterator() {
		return tests.iterator();
	}
}
