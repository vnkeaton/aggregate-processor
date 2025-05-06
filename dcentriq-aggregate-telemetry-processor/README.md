<p align="center">
  <a href="" rel="noopener">
 <img width=200px height=200px src="https://i.imgur.com/6wj0hh6.jpg" alt="Project logo"></a>
</p>

<h3 align="center">Aggregate Telemetry Processor</h3>

<div align="center">

[![Status](https://img.shields.io/badge/status-active-success.svg)]()
[![GitHub Issues](https://img.shields.io/github/issues/kylelobo/The-Documentation-Compendium.svg)](https://github.com/kylelobo/The-Documentation-Compendium/issues)
[![GitHub Pull Requests](https://img.shields.io/github/issues-pr/kylelobo/The-Documentation-Compendium.svg)](https://github.com/kylelobo/The-Documentation-Compendium/pulls)
[![License](https://img.shields.io/badge/license-MIT-blue.svg)](/LICENSE)

</div>

---

<p align="center"> Aggregate Telemetry Processor accepts the output active telemetry topic from the Heartbeat Telemetry Processor as input: dcentriq.aggregation.active_telem.json
    <br> 
</p>

## 📝 Table of Contents

- [About](#about)
- [Getting Started](#getting_started)
- [Deployment](#deployment)
- [Usage](#usage)
- [TODOs](#todos)

## 🧐 About <a name = "about"></a>

Aggregate Telemetry Processor is a Kafka Processor API that aggregates telemetry data to one minute, fifteen minutes, and one hour.

- The Heartbeat Telemetry Processor sends an output topic, dcentriq.aggregation.active_telem.json.
- The Aggregate Telemetry Processor will aggregate the data from the dcentriq.aggregation.active_telem.json into:
- one minute aggregation: dcentriq.aggregation.one_minute_telem.json
- fifteen minute aggregation: dcentriq.aggregation.fifteen_minute_telem.json
- one hour aggregation: dcentriq.aggregation.one_hour_telem.json

## 🏁 Getting Started <a name = "getting_started"></a>

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See [deployment](#deployment) for notes on how to deploy the project on a live system.

- Clone the dcentriq-process-common project.
- Clone the dcentriq-aggregate-telemetry-processor project.
- mvn clean install

### Prerequisites

What things you need to install the software and how to install them.

```
Java 17
Maven
IDE
```

### Installing

A step by step series of examples that tell you how to get a development env running.

Say what the step will be

```
TBD
```

To determine the output of this processor, check for the output topics dcentriq.aggregation.one_minute_telem.json, dcentriq.aggregation.fifteen_minute_telem.json, and dcentriq.aggregation.one_hour_telem.json.

## 🔧 Running the tests <a name = "tests"></a>

TBD


## 🎈 Usage <a name="usage"></a>

Add notes about how to use the system.

## 🚀 Deployment <a name = "deployment"></a>

Add additional notes about how to deploy this on a live system.

## ⛏️ ToDos <a name = "todos"></a>

- Additional tests.

