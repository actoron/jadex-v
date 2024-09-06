# Benchmarking in Jadex

This document describes how to run benchmarks and analyze the output.
It also covers how to write new benchmarks and how the benchmarking works.

## Types of Benchmarks

Currently benchmarking **execution time** and benchmarking **memory footprint** is supported.

## Running Benchmarks

There a two scripts in the jadex-v main directory to run all benchmarks: `benchmark.bat/.sh` and `benchmark_thirdparty.bat/.sh`.
The first one will run all Jadex benchmarks. The second one runs similar benchmarks on a number of other frameworks (Akka, JADE, ...).

The running time of the scripts is quite long (currently around 15-20 minutes) as for benchmarking execution time a ten seconds cooldown time is used before each run and each benchmark is run ten times leading to 100 seconds of waiting time per benchmark.

Benchmarks should not be run in parallel as that would invalidate execution time measurements. Also no other programs should execute during benchmark execution.

Benchmarks are implemented as JUnit tests so you can also run benchmarks individually from your IDE. 

## Benchmark Results

Results are printed during execution. Also you can generate plots of the results.

### Printed Output

 For time benchmarks:
```
    took: 1882846
    runs: 5
    Change(%): 37.43531872930181
    Used memory: 6158472
    Mem change(%): 0.0613515273540724
```
where *took* is the average execution time in nanoseconds, *runs* states how often the code snippet was run and *change* compares the current result to the previous best value. For reference purposes, also memory consumption is printed during time benchmarks allowing to detect possible memory leaks.

For memory benchmarks:
```
    Per component: 5545
    runs: 17785
    Change(%): 1.9301470588235294
```
where *per component* is the memory consumption of a single component in bytes. *runs* and *change* are the same as above.

### Saved Data

Benchmark results are saved in *.benchmark[_gradle]* folders, e.g., *jadex-v/benchmarks/execution/.benchmark_gradle/LambdaAgentBenchmark.benchmarkTime.json*. For some strange reason, benchmark results differ when run from gradle or from IDE. Thus for better comparison, two different folders are used.

The file names correspond to the class and method name of the benchmark implementation. They contain the all-time best value as well as the last value in JSON format, e.g.:`{"best":33705,"last":35372}`.

### Plot Results

There is a Python script for plotting the results. It can be executed from gradle with `./gradlew benchmark:plot`. It will show the memory and time plots and save images to the *jadex-v/benchmark* directory.

## Benchmark Implementation

Benchmarks are implemented using the class `jadex.benchmark.BenchmarkHelper` from the module *benchmark-core* located in *jadex-v/benchmark/core*. Two methods are provided: `benchmarkTime(Runnable)` and`benchmarkMemory(Callable<Runnable>)`.

Both methods return a double values that indicates the percentual change with respect to the best value. In the Jadex benchmarks, this value is used to fail a benchmark when the result degrades by more than 20%.

### Time Benchmarks

The implementation of time benchmarks is easy as you only have to provide a `java.lang.Runnable` for the code to be benchmarked. E.g.:
```
@Test
void benchmarkTime()
{
    double pct = BenchmarkHelper.benchmarkTime(() ->
    {
        Component.createComponent(Component.class, () -> new Component()).terminate().get();
    });
    assertTrue(pct<20);	// Fail when more than 20% worse
}
```

### Memory Benchmarks

For memory benchmarks you have to provide startup and shutdown code. The benchmark will execute the startup code several times and then compare the used memory to the value before. After the measurement, the shutdown code is executed.

The is done by using a `java.util.concurrent.Callable` that executes the startup code and returns a `java.lang.Runnable` for the corresponding shutdown code. E.g.:
```
@Test
void benchmarkMemory()
{
    double pct = BenchmarkHelper.benchmarkMemory(() ->
    {
        Component comp = Component.createComponent(Component.class, () -> new Component());
        return () -> comp.terminate().get();
    });
    assertTrue(pct<20);	// Fail when more than 20% worse
}
```