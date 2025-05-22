#/bin/sh
./gradlew cleanBenchmark benchmark -x benchmark-thirdparty:benchmark --max-workers 1
#./gradlew cleanBenchmark benchmark-bt:benchmark --max-workers 1
