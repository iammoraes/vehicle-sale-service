package com.vehiclemarketplace.infrastructure.aws

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.guardduty.GuardDutyClient
import software.amazon.awssdk.services.guardduty.model.*
import software.amazon.awssdk.services.guardduty.paginators.ListFindingsIterable
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class AwsGuardDutyService(
    private val guardDutyClient: GuardDutyClient,
    private val detectorId: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getFindings(
        severityThreshold: Long = 4,
        maxResults: Int = 50,
        filterByDays: Int = 30
    ): List<Finding> {
        try {
            val startTime: Instant? = Instant.now().minus(filterByDays.toLong(), ChronoUnit.DAYS)
            
            val findingCriteria = FindingCriteria.builder()
                .criterion(
                    mapOf(
                        "severity" to Condition.builder()
                            .greaterThanOrEqual(severityThreshold)
                            .build(),
                        "updatedAt" to Condition.builder()
                            .greaterThanOrEqual(startTime?.toEpochMilli())
                            .build()
                    )
                )
                .build()

            val request = ListFindingsRequest.builder()
                .detectorId(detectorId)
                .findingCriteria(findingCriteria)
                .maxResults(maxResults)
                .sortCriteria(
                    SortCriteria.builder()
                        .attributeName("severity")
                        .orderBy(OrderBy.DESC)
                        .build()
                )
                .build()

            val findings = mutableListOf<Finding>()
            val listFindingsIterable: ListFindingsIterable = guardDutyClient.listFindingsPaginator(request)
            
            for (response in listFindingsIterable) {
                val findingIds = response.findingIds()
                if (findingIds.isNotEmpty()) {
                    findings.addAll(getDetailedFindings(findingIds))
                }
            }
            
            return findings
        } catch (e: Exception) {
            logger.error(e.message)
            return emptyList()
        }
    }

    private fun getDetailedFindings(findingIds: List<String>): List<Finding> {
        try {
            val request = GetFindingsRequest.builder()
                .detectorId(detectorId)
                .findingIds(findingIds)
                .build()

            val response = guardDutyClient.getFindings(request)
            return response.findings()
        } catch (e: Exception) {
            logger.error(e.message)
            return emptyList()
        }
    }

    fun getHighSeverityFindings(): List<Finding> {
        return getFindings(severityThreshold = 7L)
    }

}
