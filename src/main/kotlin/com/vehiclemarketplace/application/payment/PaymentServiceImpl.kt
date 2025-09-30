package com.vehiclemarketplace.application.payment

import com.vehiclemarketplace.application.payment.dto.NotificationRequest
import com.vehiclemarketplace.domain.exception.PaymentCreationException
import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayMethod
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayRequest
import com.vehiclemarketplace.domain.model.payment.PaymentGatewayResponse
import com.vehiclemarketplace.domain.model.sale.Payment
import com.vehiclemarketplace.domain.model.sale.PaymentMethod
import com.vehiclemarketplace.domain.model.sale.PaymentStatus
import com.vehiclemarketplace.domain.repositories.payment.PaymentGatewayRepository
import com.vehiclemarketplace.domain.service.payment.PaymentService
import com.vehiclemarketplace.domain.service.sale.SaleService
import com.vehiclemarketplace.domain.util.PaymentUtil.mapGatewayStatusToPaymentStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Service
import java.util.*

@Service
class PaymentServiceImpl(
    private val paymentGatewayRepository: PaymentGatewayRepository,
    private val saleService: SaleService
) : PaymentService {

    override fun createPayment(payment: Payment, buyer: Buyer): PaymentGatewayResponse {
        try {
            val gatewayMethod = mapPaymentMethodToGateway(payment.method)

            val paymentRequest = PaymentGatewayRequest(
                amount = payment.amount,
                paymentMethod = gatewayMethod,
                buyer = buyer,
                callbackUrl = "/api/v1/payments/webhook"
            )

            return paymentGatewayRepository.createPayment(paymentRequest)
        } catch (ex: Exception) {
            throw PaymentCreationException("Error creating payment", ex)
        }
    }

    override fun processPayment(payment: Payment): Payment {
        val updatedStatus = when {
            payment.transactionId != null -> {
                val gatewayResponse = checkPaymentStatus(payment.transactionId)
                mapGatewayStatusToPaymentStatus(gatewayResponse.status)
            }

            else -> PaymentStatus.PROCESSING
        }

        return when (updatedStatus) {
            PaymentStatus.APPROVED -> payment.approve(
                payment.transactionId ?: UUID.randomUUID().toString()
            )

            PaymentStatus.DECLINED -> payment.decline("Payment declined by gateway")
            PaymentStatus.EXPIRED -> payment.expire()
            PaymentStatus.CANCELLED -> payment.cancel()
            else -> payment.process()
        }
    }

    private fun checkPaymentStatus(paymentId: String): PaymentGatewayResponse {
        return paymentGatewayRepository.getPayment(paymentId)
    }

    override fun cancelPayment(paymentId: String): PaymentGatewayResponse {
        return paymentGatewayRepository.cancelPayment(paymentId)
    }

    override suspend fun processPaymentNotification(notification: NotificationRequest): Map<String, String> =
        withContext(Dispatchers.IO) {
            try {
                if (notification.type == "payment") {
                    val paymentResponse = checkPaymentStatus(notification.data.id)
                    return@withContext saleService.updateSale(notification.data.id, paymentResponse)
                } else {
                    return@withContext mapOf("status" to "ignored")
                }
            } catch (e: Exception) {
                return@withContext mapOf(
                    "status" to "error",
                    "message" to (e.message ?: "Unknown error")
                )
            }
        }

    private fun mapPaymentMethodToGateway(method: PaymentMethod): PaymentGatewayMethod {
        return when (method) {
            PaymentMethod.PIX -> PaymentGatewayMethod.PIX
            PaymentMethod.BOLETO -> PaymentGatewayMethod.BOLETO
        }
    }
}
