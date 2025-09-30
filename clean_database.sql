
-- Desabilitar verificação de chaves estrangeiras
SET session_replication_role = 'replica';

-- Limpar todas as tabelas principais do sistema
TRUNCATE TABLE vehicles CASCADE;
TRUNCATE TABLE buyers CASCADE;
TRUNCATE TABLE sales CASCADE;
TRUNCATE TABLE payments CASCADE;
TRUNCATE TABLE sale_events CASCADE;
TRUNCATE TABLE sale_sagas CASCADE;
TRUNCATE TABLE documents CASCADE;
TRUNCATE TABLE addresses CASCADE;

-- Reabilitar verificação de chaves estrangeiras
SET session_replication_role = 'origin';

-- Confirmar limpeza
SELECT 'Base de dados limpa com sucesso!' AS mensagem;
