/*-
 * ========================LICENSE_START=================================
 * ids-connector
 * %%
 * Copyright (C) 2021 Fraunhofer AISEC
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
package de.fhg.aisec.ids

import de.fhg.aisec.ids.api.cm.ContainerManager
import de.fhg.aisec.ids.api.infomodel.InfoModel
import de.fhg.aisec.ids.api.settings.Settings
import de.fhg.aisec.ids.camel.idscp2.ListenerManager
import de.fhg.aisec.ids.camel.idscp2.Utils
import de.fhg.aisec.ids.camel.idscp2.listeners.ExchangeListener
import de.fhg.aisec.ids.camel.idscp2.listeners.TransferContractListener
import de.fhg.aisec.ids.camel.processors.UsageControlMaps
import de.fhg.aisec.ids.cmc.CmcConfig
import de.fhg.aisec.ids.cmc.prover.CmcProver
import de.fhg.aisec.ids.cmc.prover.CmcProverConfig
import de.fhg.aisec.ids.cmc.verifier.CmcVerifier
import de.fhg.aisec.ids.cmc.verifier.CmcVerifierConfig
import de.fhg.aisec.ids.idscp2.idscp_core.ra_registry.RaProverDriverRegistry
import de.fhg.aisec.ids.idscp2.idscp_core.ra_registry.RaVerifierDriverRegistry
import de.fhg.aisec.ids.rm.RouteManagerService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.net.URI
import java.util.Arrays

@Configuration
class ConnectorConfiguration {

    @Autowired(required = false) private var cml: ContainerManager? = null
    @Autowired private lateinit var settings: Settings
    @Autowired private lateinit var im: InfoModel
    @Autowired private lateinit var rm: RouteManagerService

    @Bean
    fun configureIdscp2(): CommandLineRunner {
        return CommandLineRunner {
            Utils.connectorUrlProducer = {
                settings.connectorProfile.connectorUrl
                    ?: URI.create("http://connector.ids")
            }
            Utils.maintainerUrlProducer = {
                settings.connectorProfile.maintainerUrl
                    ?: URI.create("http://connector-maintainer.ids")
            }
            Utils.dapsUrlProducer = { settings.connectorConfig.dapsUrl }
            TrustedConnector.LOG.info("Information model {} loaded", BuildConfig.INFOMODEL_VERSION)
            Utils.infomodelVersion = BuildConfig.INFOMODEL_VERSION

            ListenerManager.addExchangeListener(
                ExchangeListener {
                    connection, exchange ->
                    UsageControlMaps.setExchangeConnection(exchange, connection)
                }
            )
            ListenerManager.addTransferContractListener(
                TransferContractListener {
                    connection, transferContract ->
                    UsageControlMaps.setConnectionContract(connection, transferContract)
                }
            )

            idscp2CmcRatConfig()
        }
    }

    /**
     * Method for configuration of IDSCP2 CMC attestation driver.
     */
    fun idscp2CmcRatConfig() {
        // RAT prover configuration
        val cmcHostAndPort: Array<String> = "172.21.0.1".split(":").toTypedArray()
        var cmcPort: Int = CmcConfig.DEFAULT_CMC_PORT
        if (cmcHostAndPort.size > 1) {
            cmcPort = cmcHostAndPort[1].toInt()
        }
        val proverConfig: CmcProverConfig = CmcProverConfig.Builder()
            .setCmcHost(cmcHostAndPort[0])
            .setCmcPort(cmcPort)
            .build()
        RaProverDriverRegistry.registerDriver(
            CmcProver.ID,
            { fsmListener -> CmcProver(fsmListener) },
            proverConfig
        )

        // RAT verifier configuration
        val verifierConfig: CmcVerifierConfig = CmcVerifierConfig.Builder()
            .setCmcHost(cmcHostAndPort[0])
            .setCmcPort(cmcPort)
            .build()
        RaVerifierDriverRegistry.registerDriver(
            CmcVerifier.ID,
            { fsmListener -> CmcVerifier(fsmListener) },
            verifierConfig
        )
    }

    @Bean
    fun listBeans(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            val beans: Array<String> = ctx.beanDefinitionNames

            Arrays.sort(beans)

            for (bean in beans) {
                TrustedConnector.LOG.info("Loaded bean: {}", bean)
            }
        }
    }

    @Bean
    fun listContainers(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            val containers = cml?.list(false)

            for (container in containers ?: emptyList()) {
                TrustedConnector.LOG.debug("Container: {}", container.names)
            }
        }
    }

    @Bean
    fun showConnectorProfile(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            val connector = im.connector

            if (connector == null) {
                TrustedConnector.LOG.info("No connector profile stored yet.")
            } else {
                TrustedConnector.LOG.info("Connector profile: {}", connector)
            }
        }
    }

    @Bean
    fun showCamelInfo(ctx: ApplicationContext): CommandLineRunner {
        return CommandLineRunner {
            val routes = rm.routes

            for (route in routes) {
                TrustedConnector.LOG.debug("Route: {}", route.shortName)
            }

            val components = rm.listComponents()

            for (component in components) {
                TrustedConnector.LOG.debug("Component: {}", component.bundle)
            }
        }
    }
}
