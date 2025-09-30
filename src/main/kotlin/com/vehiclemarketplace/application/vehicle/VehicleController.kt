package com.vehiclemarketplace.application.vehicle

import com.vehiclemarketplace.application.vehicle.dto.UpdateVehicleRequest
import com.vehiclemarketplace.application.vehicle.dto.VehicleDto
import com.vehiclemarketplace.domain.model.vehicle.VehicleStatus
import com.vehiclemarketplace.domain.service.vehicle.VehicleService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springdoc.core.annotations.ParameterObject
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("/v1/vehicles")
@Tag(name = "Vehicles", description = "Vehicle management APIs")
class VehicleController(
    private val vehicleService: VehicleService
) {

    @PostMapping(
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Create a new vehicle")
    @ResponseStatus(HttpStatus.CREATED)
    suspend fun createVehicle(@RequestBody request: VehicleDto) =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(vehicleService.createVehicle(request))

    @GetMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
    @Operation(summary = "List all vehicles with pagination and filtering")
    suspend fun listVehicles(
        @RequestParam(required = false) status: VehicleStatus?,
        @ParameterObject pageable: Pageable
    ) = ResponseEntity.ok(
        vehicleService.listVehicles(
            status = status,
            pageable = pageable
        )
    )

    @PutMapping(
        "/{id}",
        consumes = [MediaType.APPLICATION_JSON_VALUE],
        produces = [MediaType.APPLICATION_JSON_VALUE]
    )
    @Operation(summary = "Update a vehicle")
    suspend fun updateVehicle(
        @PathVariable id: UUID,
        @RequestBody request: UpdateVehicleRequest
    ) = vehicleService.updateVehicle(id, request)
        ?.let { ResponseEntity.ok(it) }
        ?: ResponseEntity.notFound().build()
}
