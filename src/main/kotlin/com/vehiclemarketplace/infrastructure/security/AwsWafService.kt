package com.vehiclemarketplace.infrastructure.security

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.wafv2.Wafv2Client
import software.amazon.awssdk.services.wafv2.model.*
import java.util.*

@Service
class AwsWafService(
    private val wafClient: Wafv2Client,
    private val webAclId: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val scope = "REGIONAL"

    fun getWebAclRules(): List<Rule> {
        try {
            val requestBuilder = GetWebAclRequest.builder()
                .id(webAclId)
                .name(getWebAclName())
                .scope(Scope.fromValue(scope))
                .build()

            val response = wafClient.getWebACL(requestBuilder)
            return response.webACL().rules()
        } catch (e: Exception) {
            logger.error(e.message)
            return emptyList()
        }
    }

    fun createIpBlockRule(name: String, ips: List<String>): String {
        try {
            val ipSetId = createIpSet(name, ips)
            
            val rule = Rule.builder()
                .name(name)
                .priority(1)
                .action(RuleAction.builder().block(BlockAction.builder().build()).build())
                .statement(
                    Statement.builder()
                        .ipSetReferenceStatement(
                            IPSetReferenceStatement.builder()
                                .arn(ipSetId)
                                .build()
                        )
                        .build()
                )
                .visibilityConfig(
                    VisibilityConfig.builder()
                        .sampledRequestsEnabled(true)
                        .cloudWatchMetricsEnabled(true)
                        .metricName("${name}Metric")
                        .build()
                )
                .build()
                
            val updateRequestBuilder = UpdateWebAclRequest.builder()
                .id(webAclId)
                .name(getWebAclName())
                .scope(Scope.fromValue(scope))
                .defaultAction(DefaultAction.builder().allow(AllowAction.builder().build()).build())
                .rules(listOf(rule) + getWebAclRules())
                .visibilityConfig(
                    VisibilityConfig.builder()
                        .sampledRequestsEnabled(true)
                        .cloudWatchMetricsEnabled(true)
                        .metricName("${getWebAclName()}Metric")
                        .build()
                )
                .lockToken(getLockToken())
                .build()
                
            wafClient.updateWebACL(updateRequestBuilder)
            return name
        } catch (e: Exception) {
            logger.error(e.message)
            throw RuntimeException(e.message, e)
        }
    }

    private fun createIpSet(name: String, ips: List<String>): String {
        val ipSetRequestBuilder = CreateIpSetRequest.builder()
            .name("${name}IpSet")
            .scope(Scope.fromValue(scope))
            .description("IP set for blocking malicious IPs")
            .addresses(ips)
            .ipAddressVersion(IPAddressVersion.IPV4)
            .build()
            
        val response = wafClient.createIPSet(ipSetRequestBuilder)
        return response.summary().arn()
    }

    private fun getLockToken(): String {
        val requestBuilder = GetWebAclRequest.builder()
            .id(webAclId)
            .name(getWebAclName())
            .scope(Scope.fromValue(scope))
            .build()
            
        val response = wafClient.getWebACL(requestBuilder)
        return response.lockToken()
    }

    private fun getWebAclName(): String {
        return try {

            val request = ListWebAcLsRequest.builder()
                .scope(Scope.fromValue(scope))
                .build()
            val response = wafClient.listWebACLs(request)
            val webAcls = response.webACLs()
            webAcls.find { acl -> acl.id() == webAclId }?.name() ?: "DefaultWebACL"
        } catch (e: Exception) {
            logger.error(e.message)
            "DefaultWebACL"
        }
    }

    fun getBlockedRequests(startTime: Date, endTime: Date): List<Map<String, Any>> {
        return emptyList()
    }
}
