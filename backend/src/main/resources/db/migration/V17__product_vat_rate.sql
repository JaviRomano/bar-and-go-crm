-- Tipo de IVA de venta de cada producto (D14). El PVP de la carta incluye IVA; el food cost se calcula
-- sobre el precio sin IVA. Los productos existentes pasan al tipo del 10 %, el habitual en hostelería.

ALTER TABLE products ADD COLUMN vat_rate_id BIGINT REFERENCES vat_rate (id);

UPDATE products
SET vat_rate_id = (SELECT id FROM vat_rate WHERE rate = 10.00 ORDER BY id LIMIT 1);

ALTER TABLE products ALTER COLUMN vat_rate_id SET NOT NULL;
