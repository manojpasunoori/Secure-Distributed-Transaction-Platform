CREATE TABLE IF NOT EXISTS transactions (
  id UUID PRIMARY KEY,
  sender VARCHAR(128) NOT NULL,
  receiver VARCHAR(128) NOT NULL,
  amount NUMERIC(18,2) NOT NULL,
  currency VARCHAR(8) NOT NULL,
  reference VARCHAR(128) UNIQUE NOT NULL,
  status VARCHAR(32) NOT NULL,
  validator_summary JSONB NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_transactions_status ON transactions(status);
CREATE INDEX IF NOT EXISTS idx_transactions_created_at ON transactions(created_at);
