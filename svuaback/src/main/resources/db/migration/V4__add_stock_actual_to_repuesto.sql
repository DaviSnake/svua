ALTER TABLE repuesto
ADD COLUMN stock_actual INTEGER NOT NULL DEFAULT 0;

ALTER TABLE repuesto
ADD CONSTRAINT chk_stock_actual
CHECK (stock_actual >= 0);