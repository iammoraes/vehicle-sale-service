package com.vehiclemarketplace.infrastructure.payment.datasource

import com.mercadopago.MercadoPagoConfig
import com.mercadopago.client.common.IdentificationRequest
import com.mercadopago.client.payment.PaymentClient
import com.mercadopago.client.payment.PaymentCreateRequest
import com.mercadopago.client.payment.PaymentPayerRequest
import com.mercadopago.exceptions.MPApiException
import com.mercadopago.exceptions.MPException
import com.vehiclemarketplace.domain.model.buyer.Buyer
import com.vehiclemarketplace.domain.model.buyer.DocumentType
import com.vehiclemarketplace.infrastructure.aws.AwsSecretsManagerService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.OffsetDateTime
import javax.annotation.PostConstruct

@Component
class MercadoPagoDataSource(
    @Value("\${mercadopago.secret-name:payment-gateway-config}") private val secretName: String,
    @Value("\${app.notification.url}") private val notificationUrl: String,
    private val secretsManagerService: AwsSecretsManagerService
) : PaymentGatewayDataSource {
    private val paymentClient: PaymentClient = PaymentClient()


    @PostConstruct
    fun init() {
        val accessToken = secretsManagerService.getSecretJsonValue(secretName, "access_token")
            ?: throw IllegalStateException("Failed to load Mercado Pago access token from Secrets Manager")
            
        MercadoPagoConfig.setAccessToken(accessToken)
    }

    override fun createPayment(
        amount: BigDecimal,
        method: String,
        buyer: Buyer,
        callbackUrl: String?
    ): Map<String, Any> {
        try {
            validatePaymentRequest(amount, method, buyer)

            val cpfDocument = buyer.documents.find { it.type == DocumentType.CPF }
            val cpfNumber = cpfDocument?.number?.replace("[^0-9]".toRegex(), "")
                ?: throw IllegalArgumentException("Buyer CPF document is required for Mercado Pago payments")


            val nameParts = buyer.name.split(" ")
            val firstName = nameParts.firstOrNull() ?: ""
            val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

            val paymentCreateRequest = PaymentCreateRequest.builder()
                .transactionAmount(amount)
                .paymentMethodId(method)
                .dateOfExpiration(OffsetDateTime.now().plusDays(3))
                .payer(
                    PaymentPayerRequest.builder()
                        .email(buyer.email)
                        .firstName(firstName)
                        .lastName(lastName)
                        .identification(
                            IdentificationRequest.builder()
                                .type("CPF")
                                .number(cpfNumber)
                                .build()
                        )
                        .build()
                )
                .callbackUrl(callbackUrl ?: notificationUrl)
                .build()

            val payment = paymentClient.create(paymentCreateRequest)

            return mapOf(
                "id" to payment.id,
                "status" to payment.status,
                "external_reference" to (payment.externalReference ?: ""),
                "payment_method_id" to payment.paymentMethodId,
                "transaction_amount" to payment.transactionAmount,
                "date_created" to payment.dateCreated.toString(),
                "date_approved" to (payment.dateApproved?.toString() ?: ""),
                "date_of_expiration" to (payment.dateOfExpiration?.toString() ?: ""),
                "transaction_id" to payment.id.toString()
            )
        } catch (e: MPException) {
            throw RuntimeException("Error creating Mercado Pago payment: ${e.message}")
        } catch (e: MPApiException) {
            throw RuntimeException("Mercado Pago API error: ${e.message}")
        }
    }

    private fun validatePaymentRequest(amount: BigDecimal, method: String, buyer: Buyer) {
        if (amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("Payment amount must be greater than zero")
        }

        if (method.isBlank()) {
            throw IllegalArgumentException("Payment method is required")
        }

        if (buyer.email.isBlank()) {
            throw IllegalArgumentException("Buyer email is required")
        }

        if (buyer.name.isBlank()) {
            throw IllegalArgumentException("Buyer name is required")
        }
    }

    override fun getPayment(paymentId: String): Map<String, Any> {
        try {
            val payment = paymentClient.get(paymentId.toLong())
            return mapMercadoPagoResponse(payment)
        } catch (ex: MPException) {
            throw ex
        } catch (ex: MPApiException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
    }

    override fun cancelPayment(paymentId: String): Map<String, Any> {
        try {
            val result = paymentClient.cancel(paymentId.toLong())
            return mapMercadoPagoResponse(result)
        } catch (ex: MPException) {
            throw ex
        } catch (ex: MPApiException) {
            throw ex
        } catch (ex: Exception) {
            throw ex
        }
    }

    private fun mapMercadoPagoResponse(response: Any): Map<String, Any> {
        val result = mutableMapOf<String, Any>()

        if (response is com.mercadopago.resources.payment.Payment) {
            result["id"] = response.id.toString()
            result["status"] = response.status ?: "unknown"
            result["statusDetail"] = response.statusDetail ?: ""
            result["transactionAmount"] = response.transactionAmount ?: BigDecimal.ZERO
            result["dateCreated"] = response.dateCreated ?: OffsetDateTime.now()
            result["dateApproved"] = response.dateApproved ?: ""
            result["paymentMethodId"] = response.paymentMethodId ?: ""
            result["paymentTypeId"] = response.paymentTypeId ?: ""

            when (response.paymentMethodId) {
                "pix" -> {
                    val pixInfo = response.pointOfInteraction?.transactionData?.qrCodeBase64
                    val qrCode = response.pointOfInteraction?.transactionData?.qrCode
                    if (pixInfo != null) result["qrCodeBase64"] = pixInfo
                    if (qrCode != null) result["qrCode"] = qrCode
                }

                "boleto" -> {
                    val barcode = response.transactionDetails?.barcode?.content
                    val externalResource = response.transactionDetails?.externalResourceUrl
                    val dueDate = response.dateOfExpiration
                    if (barcode != null) result["barcode"] = barcode
                    if (externalResource != null) result["pdfUrl"] = externalResource
                    if (dueDate != null) result["dueDate"] = dueDate
                }
            }

            result["rawResponse"] = response
        }

        return result
    }
}
