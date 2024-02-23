#/bin/sh
./gradlew :benchmark-core:cleanBenchmark :benchmark-core:benchmark
./gradlew :benchmark-execution:cleanBenchmark :benchmark-execution:benchmark
./gradlew :benchmark-micro:cleanBenchmark :benchmark-micro:benchmark
./gradlew :benchmark-bdi:cleanBenchmark :benchmark-bdi:benchmark
./gradlew :benchmark-bpmn:cleanBenchmark :benchmark-bpmn:benchmark
