package com.vehiclemarketplace.infrastructure.logging

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.cloudwatchlogs.CloudWatchLogsClient
import software.amazon.awssdk.services.cloudwatchlogs.model.*
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Serviço para enviar logs da aplicação para o AWS CloudWatch
 * Implementa log centralizado para melhor monitoramento e análise de segurança
 */
@Service
class AwsCloudWatchLogger(
    private val logsClient: CloudWatchLogsClient,
    @Value("\${aws.cloudwatch.log-group}") private val logGroup: String,
    @Value("\${spring.application.name:vehicle-marketplace}") private val applicationName: String
) {
    private val logger = LoggerFactory.getLogger(javaClass)
    private val logStream = "$applicationName-${UUID.randomUUID()}"
    private val logQueue = Collections.synchronizedList(mutableListOf<InputLogEvent>())
    private var sequenceToken: String? = null
    
    init {
        createLogGroupIfNotExists()
        createLogStream()
        
        // Iniciar thread para envio periódico de logs
        val executor = Executors.newSingleThreadScheduledExecutor()
        executor.scheduleAtFixedRate(this::flushLogs, 10, 10, TimeUnit.SECONDS)
    }
    
    /**
     * Envia um log para o CloudWatch
     */
    fun log(message: String, logLevel: String = "INFO", metadata: Map<String, String> = emptyMap()) {
        try {
            val timestamp = System.currentTimeMillis()
            
            val metadataString = if (metadata.isNotEmpty()) {
                metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }
            } else ""
            
            val fullMessage = if (metadataString.isNotEmpty()) {
                "$logLevel - $message - [$metadataString]"
            } else {
                "$logLevel - $message"
            }
            
            val event = InputLogEvent.builder()
                .message(fullMessage)
                .timestamp(timestamp)
                .build()
                
            logQueue.add(event)
            
            // Flush imediatamente se for um erro ou se a fila estiver grande
            if (logLevel == "ERROR" || logQueue.size >= 50) {
                flushLogs()
            }
        } catch (e: Exception) {
            logger.error("Falha ao enviar log para CloudWatch: ${e.message}")
        }
    }
    
    /**
     * Envio de logs em lote para o CloudWatch
     */
    private fun flushLogs() {
        if (logQueue.isEmpty()) return
        
        try {
            val events = synchronized(logQueue) {
                val currentEvents = logQueue.toList()
                logQueue.clear()
                currentEvents
            }
            
            if (events.isEmpty()) return
            
            // Ordenar eventos por timestamp
            val sortedEvents = events.sortedBy { it.timestamp() }
            
            val putRequest = if (sequenceToken != null) {
                PutLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .logEvents(sortedEvents)
                    .sequenceToken(sequenceToken)
                    .build()
            } else {
                PutLogEventsRequest.builder()
                    .logGroupName(logGroup)
                    .logStreamName(logStream)
                    .logEvents(sortedEvents)
                    .build()
            }
            
            val response = logsClient.putLogEvents(putRequest)
            sequenceToken = response.nextSequenceToken()
        } catch (e: InvalidSequenceTokenException) {
            // Obter o token correto e tentar novamente
            sequenceToken = e.expectedSequenceToken()
            flushLogs()
        } catch (e: Exception) {
            logger.error("Falha ao enviar logs em lote para CloudWatch: ${e.message}")
        }
    }
    
    /**
     * Cria o grupo de logs se não existir
     */
    private fun createLogGroupIfNotExists() {
        try {
            val request = DescribeLogGroupsRequest.builder()
                .logGroupNamePrefix(logGroup)
                .limit(1)
                .build()
                
            val response = logsClient.describeLogGroups(request)
            
            if (response.logGroups().isEmpty() || response.logGroups().none { it.logGroupName() == logGroup }) {
                val createRequest = CreateLogGroupRequest.builder()
                    .logGroupName(logGroup)
                    .build()
                    
                logsClient.createLogGroup(createRequest)
                logger.info("Grupo de logs criado: $logGroup")
            }
        } catch (e: Exception) {
            logger.error("Falha ao verificar/criar grupo de logs: ${e.message}")
        }
    }
    
    /**
     * Cria stream de logs para a aplicação
     */
    private fun createLogStream() {
        try {
            val request = CreateLogStreamRequest.builder()
                .logGroupName(logGroup)
                .logStreamName(logStream)
                .build()
                
            logsClient.createLogStream(request)
            logger.info("Stream de logs criado: $logStream")
        } catch (e: ResourceAlreadyExistsException) {
            // Stream já existe, ignorar
        } catch (e: Exception) {
            logger.error("Falha ao criar stream de logs: ${e.message}")
        }
    }
    
    /**
     * Registra atividade de segurança (para auditoria)
     */
    fun logSecurityEvent(
        action: String,
        userId: String,
        outcome: String,
        details: String? = null,
        metadata: Map<String, String> = emptyMap()
    ) {
        val secMetadata = metadata.toMutableMap()
        secMetadata["userId"] = userId
        secMetadata["action"] = action
        secMetadata["outcome"] = outcome
        
        if (details != null) {
            secMetadata["details"] = details
        }
        
        log("Security event: $action by user $userId - $outcome", "SECURITY", secMetadata)
    }
    
    /**
     * Registra atividade de dados sensíveis (LGPD compliance)
     */
    fun logDataAccessEvent(
        entityType: String,
        entityId: String,
        action: String,
        userId: String,
        fields: List<String>? = null
    ) {
        val metadata = mutableMapOf(
            "entityType" to entityType,
            "entityId" to entityId,
            "userId" to userId
        )
        
        if (fields != null) {
            metadata["fields"] = fields.joinToString(",")
        }
        
        log("Data access: $action $entityType:$entityId by user $userId", "DATA_ACCESS", metadata)
    }
}
