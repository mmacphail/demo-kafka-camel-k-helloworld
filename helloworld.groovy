// camel-k: dependency=camel-kafka
// camel-k: dependency=camel-jdbc
// camel-k: dependency=mvn:org.codehaus.groovy:groovy-json:3.0.8
// camel-k: dependency=mvn:io.confluent:kafka-avro-serializer:6.0.0
// camel-k: dependency=mvn:com.github.mmacphail:temp-schemas:1.2
// camel-k: dependency=mvn:org.postgresql:postgresql:42.2.23

import groovy.json.JsonSlurper
import groovy.json.JsonOutput

def vaultUri = 'http://vault:8200'
def role = 'camel'
def jwtToken = new File('/var/run/secrets/kubernetes.io/serviceaccount/token').text
def loginPath = '/v1/auth/kubernetes/login'
def secretPath = '/v1/secret/data/pg/config'

class PostgresSecret {
  String username
  String password
}

def jsonSlurper = new JsonSlurper()

def getClientToken = {
  def url = new URL(vaultUri + loginPath)
  def http = url.openConnection()
  http.setDoOutput(true)
  http.setRequestMethod('PUT')
  http.setRequestProperty('Content-Type', 'application/json')

  def out = new OutputStreamWriter(http.outputStream)
  def payload = JsonOutput.toJson( [role: role, jwt: jwtToken] ) 
  out.write(payload)
  out.close()

  def response = http.inputStream.text
  def authResult = jsonSlurper.parseText(response)
  return authResult.auth.client_token
}

def getPostgresSecret = { clientToken ->
  def url = new URL(vaultUri + secretPath)
  def http = url.openConnection()
  http.setDoOutput(false)
  http.setRequestMethod('GET')
  http.setRequestProperty('Accept', 'application/json')
  http.setRequestProperty('X-Vault-Token', clientToken)

  def response = http.inputStream.text
  def result = jsonSlurper.parseText(response)
  return new PostgresSecret(username: result.data.data.username, password: result.data.data.password)
}

def clientToken = getClientToken()
def postgresSecret = getPostgresSecret(clientToken)

def kafka = "kafka:{{kafka.topic}}?brokers={{kafka.bootstrap}}&groupId={{kafka.groupId}}&autoOffsetReset={{kafka.autoOffsetReset}}&keyDeserializer={{kafka.keyDeserializer}}&valueDeserializer={{kafka.valueDeserializer}}&schemaRegistryURL={{kafka.schemaRegistryURL}}&specificAvroReader=true"

def ds = new org.postgresql.ds.PGSimpleDataSource()
ds.url = context.resolvePropertyPlaceholders('{{postgres.url}}')
ds.user = postgresSecret.username
ds.password = postgresSecret.password

context.getRegistry().bind("ds", ds)

from(kafka)
  .process{ exchange ->
    def sensor = exchange.getIn().getBody(eu.mmacphail.data.Sensor.class);
    def sql = "INSERT INTO sensor (sensor_id, name, temperature, x, y) VALUES ('${sensor.id}', '${sensor.name}', ${sensor.temperature}, ${sensor.x}, ${sensor.y})";
    exchange.getIn().setBody(sql);
  }
  .to('jdbc:ds')