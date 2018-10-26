package com.r3.demos.ubin2a.api.controller

import com.r3.demos.ubin2a.base.ExceptionModel
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.utilities.getOrThrow
import net.corda.core.utilities.loggerFor
import org.slf4j.Logger
import vn.vnpay.demos.ibbc.bank.QueryAccountNameFlow
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response


@Path("bank")
class BankApi(val services: CordaRPCOps) {
    companion object {
        private val logger: Logger = loggerFor<BankApi>()
    }

    /** Returns the Account Name in Bank. */
    @GET
    @Path("queryAccountName")
    @Produces(MediaType.APPLICATION_JSON)
    fun queryAccountName(): Response {
        logger.info("Running BankApi.queryAccountName")
        val (status, message) = try {
            val flowHandle = services.startTrackedFlowDynamic(QueryAccountNameFlow::class.java)
            val result = flowHandle.returnValue.getOrThrow()
            flowHandle.progress.subscribe { logger.info("BankApi.queryAccountName: $it") }
            Response.Status.OK to result
        } catch (ex: Exception) {
            logger.error("BankApi.queryAccountName: $ex")
            Response.Status.INTERNAL_SERVER_ERROR to ExceptionModel(statusCode = Response.Status.INTERNAL_SERVER_ERROR.statusCode, msg = ex.message.toString())
        }
        return Response.status(status).entity(message).build()
    }

}