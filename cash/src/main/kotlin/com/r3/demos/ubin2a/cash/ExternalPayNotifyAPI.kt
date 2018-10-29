package com.r3.demos.ubin2a.cash

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.demos.ubin2a.base.TransactionModel
import com.sun.jersey.api.client.Client
import com.sun.jersey.api.client.ClientResponse
import net.corda.core.node.ServiceHub
import net.corda.core.node.services.CordaService
import net.corda.core.serialization.SingletonSerializeAsToken
import net.corda.core.utilities.loggerFor
import org.eclipse.jetty.http.HttpStatus
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.util.*
import javax.ws.rs.core.MediaType


object ExternalPayNotifyAPI {

    @CordaService
    class Service(val services: ServiceHub) : SingletonSerializeAsToken() {

        private companion object {
            val logger = loggerFor<ExternalPayNotifyAPI.Service>()
        }

        fun PayNotify(value: TransactionModel) {
            try {
                val client = Client.create()
                val mapper = ObjectMapper()
                val msg = "{\"chat_id\":\"-1001281556940\",\"text\":\"" + mapper.writeValueAsString(value) + "\"}"
                val PayNotifyURI = getPayNotifyURI("PayNotifyURI")
                logger.info("PayNotify URI from properties " + PayNotifyURI)
                val webResource = client.resource(PayNotifyURI)
                val response = webResource.accept(MediaType.APPLICATION_JSON)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse::class.java, msg)
                logger.info("Response from PayNotifyURI " + response.status)
                if (response.status != HttpStatus.OK_200) {
                    throw RuntimeException("Failed : HTTP error code : "
                            + response.status)
                } else {
                    logger.info("PayNotify successed to " + value.receiver)
                }
            } catch (ex: Exception) {
                logger.error(ex.message)
            }
        }

        // Try to read config properties to get the approve redeem URI
        fun getPayNotifyURI(value: String): String {

            val prop = Properties()
            var input: InputStream? = null

            try {
                input = FileInputStream("./config.properties")

                // load a properties file
                prop.load(input)
                val result = prop.getProperty(value)
                logger.info("prop loaded " + result)
                return result
            } catch (ex: IOException) {
                throw ex
            } finally {
                if (input != null) {
                    try {
                        input.close()
                    } catch (ex: IOException) {
                        ex.printStackTrace()
                    }
                } else {
                    logger.info("Input from FileInputStream " + input.toString())
                    throw IllegalArgumentException("config.properties not found or is null")
                }
            }
        }

    }
}



