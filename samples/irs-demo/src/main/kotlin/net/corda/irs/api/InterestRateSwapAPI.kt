package net.corda.irs.api

import net.corda.core.contracts.filterStatesOfType
import net.corda.core.crypto.Party
import net.corda.core.getOrThrow
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startFlow
import net.corda.core.utilities.loggerFor
import net.corda.irs.contract.InterestRateSwap
import net.corda.irs.flows.AutoOfferFlow
import net.corda.irs.flows.UpdateBusinessDayFlow
import java.net.URI
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

/**
 * This provides a simplified API, currently for demonstration use only.
 *
 * It provides several JSON REST calls as follows:
 *
 * GET /api/irs/deals - returns an array of all deals tracked by the wallet of this node.
 * GET /api/irs/deals/{ref} - return the deal referenced by the externally provided refence that was previously uploaded.
 * POST /api/irs/deals - Payload is a JSON formatted [InterestRateSwap.State] create a new deal (includes an externally provided reference for use above).
 *
 * TODO: where we currently refer to singular external deal reference, of course this could easily be multiple identifiers e.g. CUSIP, ISIN.
 *
 * GET /api/irs/demodate - return the current date as viewed by the system in YYYY-MM-DD format.
 * PUT /api/irs/demodate - put date in format YYYY-MM-DD to advance the current date as viewed by the system and
 * simulate any associated business processing (currently fixing).
 *
 * TODO: replace simulated date advancement with business event based implementation
 */
@Path("irs")
class InterestRateSwapAPI(val rpc: CordaRPCOps) {

    private val logger = loggerFor<InterestRateSwapAPI>()

    private fun generateDealLink(deal: InterestRateSwap.State<*>) = "/api/irs/deals/" + deal.common.tradeID

    private fun getDealByRef(ref: String): InterestRateSwap.State<*>? {
        return rpc.vaultAndUpdates().use {
            val states = it.snapshot.filterStatesOfType<InterestRateSwap.State<*>>().filter { it.state.data.ref == ref }
            if (states.isEmpty()) null else {
                val deals = states.map { it.state.data }
                if (deals.isEmpty()) null else deals[0]
            }
        }
    }

    private fun getAllDeals(): Array<InterestRateSwap.State<*>> {
        return rpc.vaultAndUpdates().use {
            val states = it.snapshot.filterStatesOfType<InterestRateSwap.State<*>>()
            states.map { it.state.data }.toTypedArray()
        }
    }

    @GET
    @Path("deals")
    @Produces(MediaType.APPLICATION_JSON)
    fun fetchDeals(): Array<InterestRateSwap.State<*>> = getAllDeals()

    @POST
    @Path("deals")
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeDeal(newDeal: InterestRateSwap.State<Party>): Response {
        return try {
            rpc.startFlow(AutoOfferFlow::Requester, newDeal.toAnonymous()).returnValue.getOrThrow()
            Response.created(URI.create(generateDealLink(newDeal))).build()
        } catch (ex: Throwable) {
            logger.info("Exception when creating deal: $ex")
            Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ex.toString()).build()
        }
    }

    @GET
    @Path("deals/{ref}")
    @Produces(MediaType.APPLICATION_JSON)
    fun fetchDeal(@PathParam("ref") ref: String): Response {
        val deal = getDealByRef(ref)
        if (deal == null) {
            return Response.status(Response.Status.NOT_FOUND).build()
        } else {
            return Response.ok().entity(deal).build()
        }
    }

    @PUT
    @Path("demodate")
    @Consumes(MediaType.APPLICATION_JSON)
    fun storeDemoDate(newDemoDate: LocalDate): Response {
        val priorDemoDate = fetchDemoDate()
        // Can only move date forwards
        if (newDemoDate.isAfter(priorDemoDate)) {
            // TODO: Remove this suppress when we upgrade to kotlin 1.1 or when JetBrain fixes the bug.
            @Suppress("UNSUPPORTED_FEATURE")
            rpc.startFlow(UpdateBusinessDayFlow::Broadcast, newDemoDate).returnValue.getOrThrow()
            return Response.ok().build()
        }
        val msg = "demodate is already $priorDemoDate and can only be updated with a later date"
        logger.error("Attempt to set demodate to $newDemoDate but $msg")
        return Response.status(Response.Status.CONFLICT).entity(msg).build()
    }

    @GET
    @Path("demodate")
    @Produces(MediaType.APPLICATION_JSON)
    fun fetchDemoDate(): LocalDate {
        return LocalDateTime.ofInstant(rpc.currentNodeTime(), ZoneId.systemDefault()).toLocalDate()
    }
}
