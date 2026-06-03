# Jadex V
---

![GitHub top language](https://img.shields.io/github/languages/top/actoron/jadex-v)
[![GitHub License](https://img.shields.io/github/license/actoron/jadex-v)](https://github.com/actoron/jadex-v?tab=GPL-3.0-1-ov-file)
[![Maven Central Version](https://img.shields.io/maven-central/v/org.activecomponents.jadex/core?label=current%20version)](https://mvnrepository.com/artifact/org.activecomponents.jadex/core)


![Maven Central Last Update](https://img.shields.io/maven-central/last-update/org.activecomponents.jadex/core?label=last%20release)
![GitHub last commit](https://img.shields.io/github/last-commit/actoron/jadex-v)
[![Discord](https://img.shields.io/discord/1511822854017253579)](https://discord.gg/5vt97NcCNq)



## Introduction

Jadex V is a versatile framework for actor and distributed service programming. It has been designed with a focus on modularity and simplicity, enabling a wide range of customizations with ease. By breaking down functionalities into separate modules, Jadex V adopts a feature-oriented approach that fosters flexibility and adaptability. Its architecture is rooted in the Java service locator pattern, allowing seamless integration of various features by simply adding modules to the classpath.


## Features

### Concurrency Support

Jadex V embraces the Actor model, offering a straightforward and robust approach to concurrent programming. Each actor, or Jadex component, operates independently, providing a natural metaphor for concurrency. Leveraging Java virtual threads, Jadex ensures highly efficient execution, making it suitable for demanding applications.

Key points:
- Actors as concurrency metaphor
- High efficiency in execution

### Reasoning Engine

Building upon the Actor model, Jadex introduces the concept of active components with an embedded reasoning engine. This empowers developers to define behavior dynamically, facilitating the utilization of various engines such as micro, BDI, and BPMN. Notably, Jadex eliminates the need to pre-start a platform for actor execution, streamlining the development process.

Key points:
- Support for multiple engines
- Seamless interaction among component types
- Directly start actors (without upfront platform/infrastructure setup and startup)

### Distributed Service Infrastructure

Facilitating interaction among active components, Jadex employs service interfaces for communication. Each component can provide and invoke services, locally or remotely, with service discovery managed by an integrated registry. This ensures smooth communication and effortless scalability, bolstered by built-in communication security measures.

Key points:
- Service-oriented interaction model
- Local and remote service invocation
- Automatic service discovery
- Enhanced communication security

### Environment Support

For many applications, having a virtual environment where active components can operate is beneficial. Jadex provides a lightweight, ready-to-use solution for environment construction. The environment manages all objects, including component representatives (typically called avatars).
Domain-specific environment actions can be defined, and components are notified about objects within their field of vision. Additionally, components can observe specific environment objects and receive updates when changes occur.
For more details on environment setup and programming, see [Details about environment setup and programming](environment.md).

## Installation

### Maven Repository

- Create a gradle or maven Java project
- Add the dependencies of features you plan to use
- Here is a rather minimal example gradle
- change `JADEX_VERSION` setting to desired version, e.g., latest version from maven central:

[![Maven Central Version](https://img.shields.io/maven-central/v/org.activecomponents.jadex/core)](https://mvnrepository.com/artifact/org.activecomponents.jadex/core)



```gradle
plugins {
    id 'java-library'
}

java {
	sourceCompatibility = '21'
	targetCompatibility = '21'	
}

repositories {
    mavenCentral() 
}

def JADEX_VERSION = '5.0-beta1'
dependencies
{
	implementation 'org.activecomponents.jadex:core:'+JADEX_VERSION
	implementation 'org.activecomponents.jadex:execution:'+JADEX_VERSION
	implementation 'org.activecomponents.jadex:injection:'+JADEX_VERSION
}

task runHelloWorld(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'helloworld.HelloAgent'
}
```

You can execute an example via the gradle task from the command line  
`./gradlew build`  
`./gradlew runHelloWorld`  

In order to set up active components with services we need to include the provided and
required service features. So we additionally include the following:

```gradle
    implementation 'org.activecomponents.jadex:providedservice:'+JADEX_VERSION
    implementation 'org.activecomponents.jadex:requiredservice:'+JADEX_VERSION
```

If we additionally want to publish services of active components as REST web services we need also the 
publication feature and a suitable web server implementation such as Jetty:

```gradle
    implementation 'org.activecomponents.jadex:publishservice:'+JADEX_VERSION
    implementation 'org.activecomponents.jadex:publishservicejetty:'+JADEX_VERSION
```

### Integrated Development Environments (IDEs)

You can clone the repo/download and import Jadex as a gradle project to your IDE.
To execute an example, e.g. start HelloWorldAgent.java in application/micro/jadex/micro/helloworld
as Java application.


## Quick Start

After a successful installation, one can start programming with actors.
Here is a small example. Add it to a `src/main/java/helloworld` folder
in your project directory:

```java
package helloworld;

import jadex.core.IComponent;
import jadex.core.IComponentManager;
import jadex.execution.IExecutionFeature;
import jadex.injection.annotation.OnStart;

public class HelloAgent 
{
	@OnStart
	protected void start(IComponent agent)
	{
		System.out.println("agent started: "+agent.getId());
		
		for(int i=3; i>0; i--)
		{
			System.out.println("seconds to terminate: "+i);
			agent.getFeature(IExecutionFeature.class).waitForDelay(1000).get();
		}
		
		agent.terminate();
	}
	
	public static void main(String[] args) 
	{
		IComponentManager.get().create(new HelloAgent());
		
		IComponentManager.get().waitForLastComponentTerminated();
	}
}
```

- `@OnStart` is called once the actor is created and starts execution
- API access is possible via the IComponent interface. It can be injected 
into methods calls or it can be declared as agent variable `@Inject private IComponent agent;`  
- Components can be created via IComponentManager interface. `IComponentManager.get().create(new HelloAgent());`
creates a new active component from a HelloWorld POJO object. The component immediately
starts execution.
- Active components are started as daemons so that the main thread has to be blocked to
prohibit program termination. This is done via `IComponentManager.get().create.waitForLastComponentTerminated();`

You can find a lot of different examples in the application packages:

- application/micro
- application/bdi and bdi-service
- application/bpmn
- application/bt


## Authors

Kai Jander  
Alexander Pokahr  
Lars Braubach  

This project is supported by [www.actoron.com](http://www.actoron.com)





