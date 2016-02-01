# surefire-isolatedjunitmethods

This is a plugin for Maven Surefire that will cause every single (JUnit) test method to be executed in its own JVM. Out of the box, Surefire supports [running each test class in its own JVM for isolation](http://maven.apache.org/surefire/maven-surefire-plugin/examples/fork-options-and-parallel-execution.html). With this provider, each test method runs in its own JVM to get the same sort of isolation, but between individual test methods. This is interesting to those who are interested in studying the dependencies between tests; the performance overhead of running each test in its own process is likely prohibitive (although another tool of mine, [VMVM](https://github.com/Programming-Systems-Lab/vmvm) can help reduce this overhead).

To use it, first download and `mvn install` this project. Then, in the project that you're interested in, modify your surefire configuration to specify a dependency on this isolated runner, and ensure that `reuseForks` is set to false. Setting `reuseForks` to false will cause Surefire to use a fresh JVM for each test, and adding the dependency on the `isolatedRunner` will force Surefire to do that for each test method, rather than just per test class. Configuration snippet:

```xml
<project>
	...
	<build>
	...
		<plugins>
		...
			<plugin>
				<artifactId>maven-surefire-plugin</artifactId>
				<version>2.18</version>
				<configuration>
					<reuseForks>false</reuseForks>
				</configuration>
				<dependencies>
					<dependency>
						<groupId>net.jonbell.surefire</groupId>
						<artifactId>isolatedrunner</artifactId>
						<version>2.18</version>
					</dependency>
				</dependencies>
			</plugin>
		...
		</plugins>
	...
	</build>
	...
</project>
```
