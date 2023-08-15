# Core Module Providing the Minimal Base Component

The minimal base component provides access to its features.

## How it works

Features are loaded using `java.util.ServiceLoader`
looking for implementations of `jadex.mj.core.impl.MjFeatureProvider`.

## How to use it

You can implement your own features or even a new component type.

### Implementing a new Component Type

To implement a specific component type like, e.g., BDI (a.k.a. *kernel*)
you can subclass `jadex.mj.core.MjComponent` and provide methods
to create instances of your components. Normally a new component type
will also have new features, which can be implemented as described next.

### Implementing a new Feature

You should read about using the `java.util.ServiceLoader` for details on this mechanism,
e.g., in the [Java SE API](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/util/ServiceLoader.html).
In short you implement your feature however you like (e.g. as a class or interface) and write
a (usually separate) class that extends `jadex.mj.core.impl.MjFeatureProvider`.

In this class you need to implement at least two methods. The method `getFeatureType` should return the
(base) type of your feature, that is, how your feature is accessed using `MjComponent.getFeature(my_type)`.
This can be an interface or the implementation class of your feature, depending on your needs.

The `createFeatureInstance(MjComponent)` then provides a (usually new) instance of your feature.

### Dealing with Conflicting Implementations

Sometimes you may want to provide a new implementation for an existing feature type.
You can do so by implementing the `replacesFeatureProvider()` method,
for making sure that your implementation is chosen instead of the old one.