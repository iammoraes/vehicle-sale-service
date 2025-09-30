package com.vehiclemarketplace.infrastructure.security

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cloudtrail.CloudTrailClient
import software.amazon.awssdk.services.cloudtrail.model.*
import software.amazon.awssdk.services.cloudtrail.paginators.LookupEventsIterable
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AwsCloudTrailService(
    private val cloudTrailClient: CloudTrailClient,
    private val trailName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun lookupEvents(
        startTime: Instant = Instant.now().minus(7, ChronoUnit.DAYS),
        endTime: Instant = Instant.now(),
        username: String? = null,
        eventName: String? = null,
        resourceType: String? = null,
        resourceName: String? = null,
        maxResults: Int = 50
    ): List<Event> {
        try {
            val lookupBuilder = LookupEventsRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .maxResults(maxResults)

            val attributes = mutableListOf<LookupAttribute>()
            
            username?.let {
                attributes.add(
                    LookupAttribute.builder()
                        .attributeKey(LookupAttributeKey.USERNAME)
                        .attributeValue(it)
                        .build()
                )
            }
            
            eventName?.let {
                attributes.add(
                    LookupAttribute.builder()
                        .attributeKey(LookupAttributeKey.EVENT_NAME)
                        .attributeValue(it)
                        .build()
                )
            }
            
            resourceType?.let {
                attributes.add(
                    LookupAttribute.builder()
                        .attributeKey(LookupAttributeKey.RESOURCE_TYPE)
                        .attributeValue(it)
                        .build()
                )
            }
            
            resourceName?.let {
                attributes.add(
                    LookupAttribute.builder()
                        .attributeKey(LookupAttributeKey.RESOURCE_NAME)
                        .attributeValue(it)
                        .build()
                )
            }
            
            if (attributes.isNotEmpty()) {
                lookupBuilder.lookupAttributes(attributes)
            }
            
            val events = mutableListOf<Event>()
            val lookupEventsIterable: LookupEventsIterable = cloudTrailClient.lookupEventsPaginator(lookupBuilder.build())
            
            for (response in lookupEventsIterable) {
                events.addAll(response.events())
            }
            
            return events
        } catch (e: Exception) {
            logger.error("Get events in CloudTrail: ${e.message}")
            return emptyList()
        }
    }

    fun getTrailStatus(): GetTrailStatusResponse? {
        try {
            val request = GetTrailStatusRequest.builder()
                .name(trailName)
                .build()
                
            return cloudTrailClient.getTrailStatus(request)
        } catch (e: Exception) {
            throw RuntimeException("Get trail status in CloudTrail: ${e.message}", e)
        }
    }

    fun startLogging() {
        try {
            val status = getTrailStatus()
            
            if (status?.isLogging == false) {
                val request = StartLoggingRequest.builder()
                    .name(trailName)
                    .build()
                    
                cloudTrailClient.startLogging(request)
            }
        } catch (e: Exception) {
            throw RuntimeException("Start logging in CloudTrail: ${e.message}", e)
        }
    }

    fun lookupSecurityEvents(
        startTime: Instant = Instant.now().minus(7, ChronoUnit.DAYS),
        endTime: Instant = Instant.now()
    ): List<Event> {
        val securityEventNames = listOf(
            "ConsoleLogin",
            "AssumeRole",
            "CreateAccessKey",
            "DeleteAccessKey",
            "UpdateAccessKey",
            "CreateUser",
            "DeleteUser",
            "UpdateUser",
            "PutUserPolicy",
            "DeleteUserPolicy",
            "AttachUserPolicy",
            "DetachUserPolicy",
            "CreateRole",
            "DeleteRole",
            "UpdateAssumeRolePolicy",
            "PutRolePolicy",
            "DeleteRolePolicy",
            "AttachRolePolicy",
            "DetachRolePolicy"
        )
        
        val allEvents = mutableListOf<Event>()
        
        securityEventNames.forEach { eventName ->
            try {
                val events = lookupEvents(
                    startTime = startTime,
                    endTime = endTime,
                    eventName = eventName,
                    maxResults = 10
                )
                
                allEvents.addAll(events)
            } catch (e: Exception) {
                logger.warn("Lookup security events in CloudTrail: ${e.message}", e)
            }
        }
        
        return allEvents.sortedByDescending { it.eventTime() }
    }
}
