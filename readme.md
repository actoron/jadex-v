# Jadex V

## Use Cases

Jadex V has been developed to be a feature-oriented framework, that allows  
for a high degree of customization in a very simple way. Each feature is 
realized as separate module, that can be added to the classpath to be activated.
Feature activation is based on the Java service locator pattern.
Main reason for the feature orientation is that in this way the software can
be used for very different use cases

** Concurrency Support **

Jadex follows the Actor model and thus allows for programming concurrency 
in a simple error resistant way. Each actor aka Jadex component is executed
independently from other actors making them a natural concurrency metaphor.
Execution of Jadex actors is highly efficient based on Java virtual threads.

Key points:
- Actors as concurrency metaphor
- Highly efficient execution

** Reasoning Engine **

Jadex extends the Actor model towards the notion of *active components*. An active
component supports an internal reasoning engine, which determines its behavior
description. This allows for employing different engines. No platform needs
to be started upfront in order to execute an actor.

Key points:
- Different engines like micro, BDI, BPMN
- Seamless interaction of different component types
- Reasoning engine without platform

** Distributed Service Infrastructure **

Interaction of active components is based on service interfaces. Each active component
can provide services and invoke services. Service invocation works locally as well as 
remotely. Service discovery is automatically managed by a built-in service registry.

Key points: 
- Service based interaction
- Active components have provided and required services
- Service invocation is possible locally and remotely
- Service discovery is managed by the infrastructure
- Build-in communication security


## Usage

### Maven Repo

### IDE

## Quick Start

