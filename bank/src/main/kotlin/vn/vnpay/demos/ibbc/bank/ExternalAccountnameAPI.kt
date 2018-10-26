package vn.vnpay.demos.ibbc.bank

import com.fasterxml.jackson.databind.ObjectMapper
import com.r3.demos.ubin2a.base.AccountModel
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


object ExternalAccountnameAPI {

    @CordaService
    class Service(val services: ServiceHub) : SingletonSerializeAsToken() {

        private companion object {
            val logger = loggerFor<ExternalAccountnameAPI.Service>()
        }

        fun queryAccountName(value: AccountModel): AccountModel {
            try {
                val client = Client.create()
                val AccountNameURI = getAccountNameURI("AccountNameURI")
                logger.info("AccountName URI from properties " + AccountNameURI)
                val webResource = client.resource(AccountNameURI)
                val mapper = ObjectMapper()
                val response = webResource.accept(MediaType.APPLICATION_JSON)
                        .type(MediaType.APPLICATION_JSON_TYPE)
                        .post(ClientResponse::class.java, mapper.writeValueAsString(value))
                logger.info("Response from AccountNameURI " + response.status)
                if (response.status != HttpStatus.OK_200) {
                    throw RuntimeException("Failed : HTTP error code : "
                            + response.status)
                }
                return response.getEntity(AccountModel::class.java)
            } catch (ex: Exception) {
                logger.error(ex.message)
                return AccountModel(accountNo = "", accountName = "", bic = "", X500Name = "")
            }
        }

        // Try to read config properties to get the approve redeem URI
        fun getAccountNameURI(value: String): String {

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



