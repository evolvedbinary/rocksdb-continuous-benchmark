# RocksDB - Continuous Benchmarks

[![Java 11](https://img.shields.io/badge/java-11-blue.svg)](https://adoptopenjdk.net/)

This is a tool for providing Continuous Benchmarks for RocksDB.

The process is trigger by a WebHook listener which receives commit events from a GitHub repository.
Results are published as a line-chart to GitHub Pages - https://adamretter.github.io/rocksdb-continuous-benchmark/

## System Architecture
Micro Services using Message Parsing to persist state and enable multiple benchmark *runner* servers.

<img src="https://raw.githubusercontent.com/adamretter/rocksdb-continuous-benchmark/main/architecture.svg"/>