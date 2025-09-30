-- Script para extrair o esquema do banco de dados PostgreSQL do Vehicle Sale Service
-- Este script gera informações detalhadas sobre tabelas, colunas e relações
-- para ajudar na criação de um diagrama de modelagem de dados

-- Tabelas e suas colunas com tipos de dados, restrições e chaves primárias
SELECT 
    t.table_name AS "Tabela",
    c.column_name AS "Coluna",
    c.data_type AS "Tipo de Dados",
    c.character_maximum_length AS "Tamanho Máximo",
    c.is_nullable AS "Aceita Nulo",
    CASE 
        WHEN pk.column_name IS NOT NULL THEN 'SIM'
        ELSE 'NÃO'
    END AS "Chave Primária"
FROM 
    information_schema.tables t
JOIN 
    information_schema.columns c ON t.table_name = c.table_name
LEFT JOIN (
    SELECT 
        tc.table_name, kcu.column_name
    FROM 
        information_schema.table_constraints tc
    JOIN 
        information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
    WHERE 
        tc.constraint_type = 'PRIMARY KEY'
) pk ON t.table_name = pk.table_name AND c.column_name = pk.column_name
WHERE 
    t.table_schema = 'public'
    AND t.table_type = 'BASE TABLE'
ORDER BY 
    t.table_name,
    c.ordinal_position;

-- Relacionamentos (chaves estrangeiras) entre as tabelas
SELECT
    tc.table_name AS "Tabela Origem",
    kcu.column_name AS "Coluna Origem",
    ccu.table_name AS "Tabela Destino",
    ccu.column_name AS "Coluna Destino",
    tc.constraint_name AS "Nome da Restrição"
FROM 
    information_schema.table_constraints tc
JOIN 
    information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name
JOIN 
    information_schema.constraint_column_usage ccu ON tc.constraint_name = ccu.constraint_name
WHERE 
    tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = 'public'
ORDER BY 
    tc.table_name,
    kcu.column_name;

-- Índices nas tabelas
SELECT
    tablename AS "Tabela",
    indexname AS "Nome do Índice",
    indexdef AS "Definição do Índice"
FROM
    pg_indexes
WHERE
    schemaname = 'public'
ORDER BY
    tablename,
    indexname;

-- Contagem de registros por tabela
SELECT
    schemaname AS "Schema",
    relname AS "Tabela",
    n_live_tup AS "Número de Registros"
FROM
    pg_stat_user_tables
WHERE
    schemaname = 'public'
ORDER BY
    n_live_tup DESC;
