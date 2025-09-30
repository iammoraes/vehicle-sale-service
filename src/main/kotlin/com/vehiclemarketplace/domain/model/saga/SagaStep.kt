package com.vehiclemarketplace.domain.model.saga

enum class SagaStep(
    val description: String,
    val isCompensatable: Boolean = true
) {
    VALIDATE_BUYER("Validate buyer information"),
    VALIDATE_VEHICLE("Validate vehicle availability"),
    RESERVE_VEHICLE("Reserve vehicle for sale", true),
    CREATE_PAYMENT("Create payment", true),
    UPDATE_INVENTORY("Update inventory", true),
    GENERATE_DOCUMENTS("Generate sale documents", false),
    CONFIRM_DELIVERY("Confirm vehicle delivery", true),
    NOTIFY_PARTIES("Notify all parties", false);

    companion object {
        fun from(step: Int): SagaStep? {
            return SagaStep.entries.getOrNull(step)
        }
    }
}