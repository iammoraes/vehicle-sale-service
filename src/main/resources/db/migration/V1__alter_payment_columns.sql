ALTER TABLE payments
    ALTER COLUMN payment_details TYPE jsonb USING payment_details::jsonb;

ALTER TABLE payments
    ALTER COLUMN gateway_response TYPE TEXT;
    
ALTER TABLE payments
    ALTER COLUMN transaction_id TYPE varchar(500);
    
