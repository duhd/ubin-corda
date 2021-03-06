package vn.vnpay.demos.ibbc.bank

import co.paralleluniverse.fibers.Suspendable
import com.r3.demos.ubin2a.base.AccountModel
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap


/**
 * Initiating flow to query an account name.
 * Useful to avoid exposing Bank's logic to clients.
 *
 * @param account the source [String] for the account number.
 * @param bank the rate provider [Party].
 *
 * @return a [String] account name, or `null` if account is unknown.
 */
@StartableByRPC
@InitiatingFlow
class QueryAccountNameFlow(private val account: AccountModel) : FlowLogic<AccountModel?>() {

    private companion object {

        val ASKING_RATE_TO_SERVICE = object : ProgressTracker.Step("Asking query account name to Corda service") {}
        val RETURNING_RATE = object : ProgressTracker.Step("Returning account name") {}
    }

    override val progressTracker = ProgressTracker(ASKING_RATE_TO_SERVICE, RETURNING_RATE)

    @Suspendable
    override fun call(): AccountModel {

        progressTracker.currentStep = ASKING_RATE_TO_SERVICE
        val bic = account.bic ?: ""
        val maybeOtherParty = serviceHub.identityService.partiesFromName(bic, exactMatch = true)
        if (maybeOtherParty.size != 1) {
            return AccountModel(accountNo = "", accountName = "Bank Not Found", bic = "", X500Name = "")
        } else {
            if (maybeOtherParty.first() == ourIdentity) throw IllegalArgumentException("Failed requirement: The payer and payee cannot be the same identity")
            val otherParty = maybeOtherParty.single()
            val session = initiateFlow(otherParty)
            val resp = session.sendAndReceive<QueryAccountNameResponse>(QueryAccountNameRequest(account))

            progressTracker.currentStep = RETURNING_RATE
            return resp.unwrap { it.name }
        }
    }
}

/**
 * Handler flow for [QueryAccountNameFlow].
 */
@InitiatedBy(QueryAccountNameFlow::class)
class QueryAccountNameFlowHandler(private val session: FlowSession) : FlowLogic<Unit>() {

    private companion object {

        val RECEIVING_REQUEST = object : ProgressTracker.Step("Receiving query account name request") {}
        val INVOKING_CORDA_SERVICE = object : ProgressTracker.Step("Invoking corda service for the account name") {}
        val RETURNING_RATE = object : ProgressTracker.Step("Generating spend to fulfil name of account") {}
    }

    override val progressTracker = ProgressTracker(RECEIVING_REQUEST, INVOKING_CORDA_SERVICE, RETURNING_RATE)

    @Suspendable
    override fun call() {

        progressTracker.currentStep = RECEIVING_REQUEST
        val (account) = session.receive<QueryAccountNameRequest>().unwrap { it }

        progressTracker.currentStep = INVOKING_CORDA_SERVICE
        val AccountNameAPI = serviceHub.cordaService(ExternalAccountnameAPI.Service::class.java)
        val name = AccountNameAPI.queryAccountName(account)

        progressTracker.currentStep = RETURNING_RATE

        session.send(QueryAccountNameResponse(name))
    }
}

/**
 * Request object for [QueryAccountNameFlow].
 */
@CordaSerializable
data class QueryAccountNameRequest(val account: AccountModel)

/**
 * Response object for [QueryAccountNameFlow].
 */
@CordaSerializable
data class QueryAccountNameResponse(val name: AccountModel)