<h1 align="center">Welcome to demo-kafka-camel-k-helloworld 👋</h1>
<p>
  <a href="#" target="_blank">
    <img alt="License: MIT" src="https://img.shields.io/badge/License-MIT-yellow.svg" />
  </a>
  <a href="https://twitter.com/MalcMacphail" target="_blank">
    <img alt="Twitter: MalcMacphail" src="https://img.shields.io/twitter/follow/MalcMacphail.svg?style=social" />
  </a>
</p>

> Demo project that combines Kafka, Camel K, and Postgresql in a helloworld usecase

## Overview

This demo project consists of a simple Camel k route that consumes sensor data from a Kafka topic and inserts them in a Postgresql database. The goal of the project is to test how to integrate all these technlogies together.

## Dependencies

This demo has multiple dependencies:
- Kubernetes
- Apache Camel and Camel K
- Kafka, Schema Regitry and Avro
- Postgresql
- Vault
- Any private container registry

## Install

### Environment
- Kubernetes must be installed.
- Kafka and Schema Registry must be installed.
- The Kafka topic sensor must exist, and contain data using the [sensor](https://github.com/mmacphail/temp-schemas/blob/master/src/main/avro/sensor.avsc) avro schema.
- A postgresql database must be installed, initialized with the [sensor.sql][./sensor.sql] script.
- Vault must be installed and initialized with a secret named `secret/pg/config` containing the postgres `username` and `password`.
- Kafka, the postgresql database and Vault must be accessible from Kubernetes.
- A container registry must be accessible from kubernetes. Export the following environment variables: `REGISTRY` (URL of the container registry), `ORGANIZATION`, `REGISTRY_SECRET` (see [Camel K documentation](https://camel.apache.org/camel-k/latest/installation/registry/registry.html)). The registry secret can be created using this command:
```
kubectl create secret docker-registry <registry_secret_name> --docker-server=<container_url> --docker-username=<docker_username> --docker-password=<docker_password> --docker-email=<docker_email>
```
- Kubernetes must be authorized to access Vault ([initialized with this doc](https://learn.hashicorp.com/tutorials/vault/kubernetes-minikube?in=vault/kubernetes)).

### Install Camel K
```sh
kamel install --registry $REGISTRY --organization $ORGANIZATION --registry-secret $REGISTRY_SECRET --maven-repository https://packages.confluent.io/maven/@id=confluent --maven-repository https://jitpack.io/@id=jitpack
```
Confluent repository is needed for `kafka-avro-serializer` dependency.
Jitpack repository is needed for `temp-schemas` dependency.

## Usage

```sh
kamel run --property file:hello.properties helloworld.groovy
```
The `helloworld.groovy` automatically fetches the postgresql secrets from Vault. For this reason, the helloworld deployment created by the `kamel run` command must be set with the `vault` Kubernetes serviceAccount.

It's impossible (for now) to set the serviceAccount in the `kamel run` command. For now, to run this example, set the `serviceAccountName: vault` by editing the `helloworld` deployment:

```
kubectl edit deployment helloworld
```

```
...
spec:
  progressDeadlineSeconds: 600
  replicas: 1
  revisionHistoryLimit: 10
  selector:
    matchLabels:
      camel.apache.org/integration: helloworld
  strategy:
    rollingUpdate:
      maxSurge: 25%
      maxUnavailable: 25%
    type: RollingUpdate
  template:
    metadata:
      creationTimestamp: null
      labels:
        camel.apache.org/integration: helloworld
    spec:
      # Add this line
      serviceAccountName: vault
      containers:
      - args:
...
```

## Notes

The following elements must be improved:
- Add the `serviceAccountName` to the `kamel run` command when this feature will be out (using the `pod` trait ?), see [this issue](https://github.com/apache/camel-k/issues/2606).
- Change the Kafka consumer groupId according to what's user defined (for now a bug forces the groupId to be `camel-k-integration`, see [this issue](https://github.com/apache/camel-k/issues/2605)).
- Document the vault secret creation / automate it. See if we can set it outside of the `helloworld.groovy` file, as an environment variable ?
- Add http error handling when getting the secret from Vault in `helloworld.groovy`.

## Author

👤 **Alexandre Fruchaud**

* Twitter: [@MalcMacphail](https://twitter.com/MalcMacphail)
* Github: [@mmacphail](https://github.com/mmacphail)

## Show your support

Give a ⭐️ if this project helped you!

***
_This README was generated with ❤️ by [readme-md-generator](https://github.com/kefranabg/readme-md-generator)_