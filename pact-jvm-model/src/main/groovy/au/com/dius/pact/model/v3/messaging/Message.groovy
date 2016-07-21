package au.com.dius.pact.model.v3.messaging

import au.com.dius.pact.model.HttpPart
import au.com.dius.pact.model.Interaction
import au.com.dius.pact.model.OptionalBody
import au.com.dius.pact.model.PactSpecVersion
import au.com.dius.pact.model.ProviderState
import au.com.dius.pact.model.Response
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.Canonical

/**
 * Message in a Message Pact
 */
@Canonical
class Message implements Interaction {
  private static final String JSON = 'application/json'

  String description
  List<ProviderState> providerStates = []
  OptionalBody contents = OptionalBody.missing()
  Map<String, Map<String, Object>> matchingRules = [:]
  Map<String, String> metaData = [:]

  byte[] contentsAsBytes() {
    if (contents.present) {
      contents.value.toString().bytes
    } else {
      []
    }
  }

  String getContentType() {
    metaData.contentType ?: JSON
  }

  @SuppressWarnings('UnusedMethodParameter')
  Map toMap(PactSpecVersion pactSpecVersion = PactSpecVersion.V3) {
    def map = [
      description: description
    ]
    if (!contents.missing) {
      if (metaData.contentType == JSON) {
        map.contents = new JsonSlurper().parseText(contents.value.toString())
      } else {
        map.contents = contentsAsBytes().encodeBase64().toString()
      }
    }
    if (providerState) {
      map.providerState = providerState
    }
    if (matchingRules) {
      map.matchingRules = matchingRules
    }
    map
  }

  Message fromMap(Map map) {
    description = map.description ?: ''
    if (map.providerStates) {
      providerStates = map.providerStates.collect { ProviderState.fromMap(it) }
    } else {
      providerStates = map.providerState ? [ new ProviderState(map.providerState.toString()) ] : []
    }
    if (map.containsKey('contents')) {
      if (map.contents == null) {
        contents = OptionalBody.nullBody()
      } else if (map.contents instanceof String && map.contents.empty) {
        contents = OptionalBody.empty()
      } else {
        contents = OptionalBody.body(JsonOutput.toJson(map.contents))
      }
    }
    matchingRules = map.matchingRules ?: [:]
    metaData = map.metaData ?: [:]
    this
  }

  HttpPart asPactRequest() {
    new Response(200, ['Content-Type': contentType], contents, matchingRules)
  }

  @Override
  @Deprecated
  String getProviderState() {
    providerStates.isEmpty() ? null : providerStates.first().name
  }

  @Override
  boolean conflictsWith(Interaction other) {
    false
  }
}
