# Jadex V

## Description

Jadex V is a versatile framework designed with a focus on modularity and simplicity, enabling a wide range of customizations with ease. By breaking down functionalities into separate modules, Jadex V adopts a feature-oriented approach that fosters flexibility and adaptability. Its architecture is rooted in the Java service locator pattern, allowing seamless integration of various features by simply adding modules to the classpath.

## Features

### Concurrency Support

Jadex V embraces the Actor model, offering a straightforward and robust approach to concurrent programming. Each actor, or Jadex component, operates independently, providing a natural metaphor for concurrency. Leveraging Java virtual threads, Jadex ensures highly efficient execution, making it suitable for demanding applications.

Key Highlights:
- Actors as concurrency metaphor
- High efficiency in execution

### Reasoning Engine

Building upon the Actor model, Jadex introduces the concept of active components with an embedded reasoning engine. This empowers developers to define behavior dynamically, facilitating the utilization of various engines such as micro, BDI, and BPMN. Notably, Jadex eliminates the need to pre-start a platform for actor execution, streamlining the development process.

Key Highlights:
- Support for multiple engines
- Seamless interaction among component types
- On-the-fly behavior determination

### Distributed Service Infrastructure

Facilitating interaction among active components, Jadex employs service interfaces for communication. Each component can provide and invoke services, locally or remotely, with service discovery managed by an integrated registry. This ensures smooth communication and effortless scalability, bolstered by built-in communication security measures.

Key Highlights:
- Service-oriented interaction model
- Local and remote service invocation
- Automatic service discovery
- Enhanced communication security

## Installation

### Maven Repository

- Create a gradle or maven Java project
- Add the dependencies of features you plan to use
- Here is a rather minimal example gradle

```
plugins {
    id 'java-library'
}

sourceCompatibility = '17'
targetCompatibility = '17'	

repositories {
    mavenCentral() 
}

dependencies
{
	implementation 'org.activecomponents.jadex:json:5.0-alpha3'
	implementation 'org.activecomponents.jadex:binary:5.0-alpha3'
	implementation 'org.activecomponents.jadex:traverser:5.0-alpha3'
	implementation 'org.activecomponents.jadex:serialization:5.0-alpha3'
	implementation 'org.activecomponents.jadex:idgenerator:5.0-alpha3'
	implementation 'org.activecomponents.jadex:common:5.0-alpha3'
	implementation 'org.activecomponents.jadex:collection:5.0-alpha3'
	implementation 'org.activecomponents.jadex:classreader:5.0-alpha3'
	implementation 'org.activecomponents.jadex:future:5.0-alpha3'
	implementation 'org.activecomponents.jadex:core:5.0-alpha3'
	implementation 'org.activecomponents.jadex:model:5.0-alpha3'
	implementation 'org.activecomponents.jadex:execution:5.0-alpha3'
	implementation 'org.activecomponents.jadex:micro:5.0-alpha3'
	implementation 'org.activecomponents.jadex:providedservice:5.0-alpha3'
	implementation 'org.activecomponents.jadex:providedservicemicro:5.0-alpha3'
	implementation 'org.activecomponents.jadex:requiredservice:5.0-alpha3'
	implementation 'org.activecomponents.jadex:requiredservicemicro:5.0-alpha3'
	implementation 'org.activecomponents.jadex:publishservice:5.0-alpha3'
	implementation 'org.activecomponents.jadex:publishservicejetty:5.0-alpha3'
	implementation 'org.activecomponents.jadex:application-micro:5.0-alpha3'
	testImplementation 'org.junit.jupiter:junit-jupiter-api:5.9.3'
}

task runHelloWorld(type: JavaExec) {
    classpath = sourceSets.main.runtimeClasspath
    main = 'jadex.micro.helloworld.HelloWorldAgent'
}
```

You can execute an example via the gradle task from the command line  
./gradlew build  
./gradlew runHelloWorld  

### Integrated Development Environments (IDEs)

You can clone the repo/download and import Jadex as a gradle project to your IDE.
To execute an example, e.g. start HelloWorldAgent.java in application/micro/jadex/micro/helloworld
as Java application.


## Quick Start


## Support


## Authors

Kai Jander  
Alexander Pokahr  
Lars Braubach  







