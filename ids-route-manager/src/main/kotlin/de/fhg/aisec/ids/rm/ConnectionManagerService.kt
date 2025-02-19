/*-
 * ========================LICENSE_START=================================
 * ids-route-manager
 * %%
 * Copyright (C) 2022 Fraunhofer AISEC
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * =========================LICENSE_END==================================
 */
@file:Suppress("DEPRECATION")

package de.fhg.aisec.ids.rm

import de.fhg.aisec.ids.api.conm.ConnectionManager
import de.fhg.aisec.ids.api.conm.IDSCPIncomingConnection
import de.fhg.aisec.ids.api.conm.IDSCPOutgoingConnection
import de.fhg.aisec.ids.api.conm.RatResult
import de.fhg.aisec.ids.api.conm.ServerEndpoint
import de.fhg.aisec.ids.camel.idscp2.ListenerManager
import de.fhg.aisec.ids.camel.idscp2.client.Idscp2ClientEndpoint
import de.fhg.aisec.ids.camel.idscp2.listeners.ConnectionListener
import de.fhg.aisec.ids.camel.idscp2.server.Idscp2ServerEndpoint
import de.fhg.aisec.ids.idscp2.api.connection.Idscp2ConnectionListener
import de.fhg.aisec.ids.idscp2.applayer.AppLayerConnection
import de.fhg.aisec.ids.idscp2.defaultdrivers.remoteattestation.demo.DemoRaVerifier
import de.fhg.aisec.ids.idscp2.defaultdrivers.remoteattestation.dummy.RaVerifierDummy
import de.fhg.aisec.ids.idscp2.defaultdrivers.remoteattestation.dummy.RaVerifierDummy2
import org.apache.camel.CamelContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component
import javax.annotation.PostConstruct
import javax.annotation.PreDestroy

@Component
class ConnectionManagerService : ConnectionManager {
    @Autowired private lateinit var ctx: ApplicationContext

    private val camelContexts: List<CamelContext>
        get() {
            return try {
                val appContexts = ctx.getBeansOfType(CamelContext::class.java).values.toList()
                val watcherContexts = XmlDeployWatcher.getBeansOfType(CamelContext::class.java)
                return listOf(appContexts, watcherContexts).flatten().sortedBy { it.name }
            } catch (e: Exception) {
                RouteManagerService.LOG.warn("Cannot retrieve the list of Camel contexts.", e)
                emptyList()
            }
        }
    override fun listAvailableEndpoints(): List<ServerEndpoint> {
        return camelContexts.flatMapTo(mutableSetOf()) { cCtx ->
            cCtx.endpointRegistry.values.mapNotNull { ep ->
                if (ep is Idscp2ServerEndpoint) {
                    val baseUri = ep.endpointBaseUri
                    val matchGroups = listOf(Regex("(.*?)://(.*?):([0-9]+).*"), Regex("(.*?)://(.*?).*"))
                        .asSequence().mapNotNull { it.matchEntire(baseUri)?.groupValues }.firstOrNull()
                    ServerEndpoint(
                        baseUri,
                        matchGroups?.get(1) ?: "?",
                        matchGroups?.get(2) ?: "?",
                        matchGroups?.get(3) ?: "?"
                    )
                } else {
                    null
                }
            }
        }.toList()
    }

    // TODO: Register Listener, get connection information and return results in listOutgoing/IncomingConnections()

    // Register a connection listener with idscp2-camel.
    // The connection listener is notified each time a new connection is created.
    // We use this in order to make a list of connections available to the web console

    // Return the attestation status of an endpoint based on it's supported and expected RA suites.
    // An attestation is considered successfull if it does not accpet any known insecure driver.
    private fun getAttestationStatus(supportedRaSuites: String, expectedRaSuites: String): RatResult {
        // This array contains all insecure default verifiers.
        // If one of these is detected, the attestation will be considered insecure.
        val insecureVerifier = setOf(
            RaVerifierDummy2.RA_VERIFIER_DUMMY2_ID,
            RaVerifierDummy.RA_VERIFIER_DUMMY_ID,
            DemoRaVerifier.DEMO_RA_VERIFIER_ID
        )

        val supportedRaSuitesList = supportedRaSuites.split('|')
        val expectedRaSuitesList = expectedRaSuites.split('|')
        return if (expectedRaSuitesList.any(insecureVerifier::contains)) {
            RatResult(RatResult.Status.FAILED, "Endpoint accepts dummy attestation")
        } else {
            RatResult(
                RatResult.Status.SUCCESS,
                "Supported RA Suites: ${supportedRaSuitesList.joinToString()}, " +
                    "Expected RA Suites: ${expectedRaSuitesList.joinToString()}"
            )
        }
    }

    private val outgoingConnections: MutableList<IDSCPOutgoingConnection> = mutableListOf()
    private val incomingConnections: MutableList<IDSCPIncomingConnection> = mutableListOf()

    private val connectionListener = object : ConnectionListener {
        fun <C> handleConnection(
            connection: C,
            appLayerConnection: AppLayerConnection,
            connectionsList: MutableList<C>
        ) {
            // first register a idscp2connectionListener to keep track of connection cleanup
            appLayerConnection.addConnectionListener(object : Idscp2ConnectionListener {
                private fun removeConnection() {
                    appLayerConnection.removeConnectionListener(this)
                    connectionsList -= connection
                }

                override fun onError(t: Throwable) {}

                override fun onClose() {
                    removeConnection()
                }
            })

            connectionsList += connection
        }

        override fun onClientConnection(connection: AppLayerConnection, endpoint: Idscp2ClientEndpoint) {
            // When we are a client endpoint, we create an outgoing connection
            val outgoing = IDSCPOutgoingConnection()

            handleConnection(outgoing, connection, outgoingConnections)

            // TODO handle information from connection and endpoint

            outgoing.apply {
                endpointIdentifier = endpoint.endpointBaseUri
                attestationResult = getAttestationStatus(endpoint.supportedRaSuites, endpoint.expectedRaSuites)
                remoteIdentity = connection.remotePeer()
            }
        }

        override fun onServerConnection(connection: AppLayerConnection, endpoint: Idscp2ServerEndpoint) {
            // Since we are a server and therefore listening, all connections should be incomming
            val incoming = IDSCPIncomingConnection()

            handleConnection(incoming, connection, incomingConnections)

            // TODO handle information from connection and endpoint

            incoming.apply {
                endpointIdentifier = endpoint.endpointBaseUri
                attestationResult = getAttestationStatus(endpoint.supportedRaSuites, endpoint.expectedRaSuites)
                remoteHostName = connection.remotePeer()
            }
        }
    }

    @PostConstruct
    private fun registerConnectionListener() {
        ListenerManager.addConnectionListener(connectionListener)
    }

    @PreDestroy
    private fun deregisterConnectionListener() {
        // TODO: Also remove all idscp2 connection listeners
        ListenerManager.removeConnectionListener(connectionListener)
    }

    override fun listIncomingConnections(): List<IDSCPIncomingConnection> {
        return incomingConnections
    }

    override fun listOutgoingConnections(): List<IDSCPOutgoingConnection> {
        return outgoingConnections
    }
}
