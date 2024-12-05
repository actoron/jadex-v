#/bin/sh
./gradlew cleanBenchmark benchmark -x benchmark-thirdparty:benchmark --max-workers 1
