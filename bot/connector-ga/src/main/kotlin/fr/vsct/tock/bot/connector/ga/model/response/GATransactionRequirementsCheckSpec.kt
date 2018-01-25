package fr.vsct.tock.bot.connector.ga.model.response

import fr.vsct.tock.bot.connector.ga.model.GAIntent

/**
 * @see https://developers.google.com/actions/reference/rest/Shared.Types/TransactionRequirementsCheckSpec
 */
data class GATransactionRequirementsCheckSpec(
        val orderOptions: GAOrderOptions ,
        val paymentOptions: GAPaymentOptions? = null) : GAInputValueData(GAIntent.transactionRequirementsCheck.type!!)