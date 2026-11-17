ALTER TABLE refresh_tokens
    MODIFY token_hash VARCHAR(64) NOT NULL;

ALTER TABLE customer_profiles
    MODIFY kyc_document_hash VARCHAR(64) NULL;
