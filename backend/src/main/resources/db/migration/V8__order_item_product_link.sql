-- Enlace de cada línea de pedido con el producto de la carta. Se mantiene la copia de
-- nombre y precio (product_name, unit_price): es lo que se cobró y no debe cambiar.

ALTER TABLE order_items ADD COLUMN product_id BIGINT;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products (id);
CREATE INDEX idx_order_items_product ON order_items (product_id);

-- Líneas antiguas que no se pudieron enlazar, para revisión manual
CREATE TABLE order_item_link_issue (
    order_item_id BIGINT       PRIMARY KEY REFERENCES order_items (id) ON DELETE CASCADE,
    product_name  VARCHAR(200) NOT NULL,
    reason        VARCHAR(20)  NOT NULL CHECK (reason IN ('NOT_FOUND', 'AMBIGUOUS')),
    detected_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Solo coincidencia exacta de nombre con un único producto: products.name no es único
UPDATE order_items oi
SET product_id = p.id
FROM products p
WHERE p.name = oi.product_name
  AND (SELECT COUNT(*) FROM products p2 WHERE p2.name = oi.product_name) = 1;

INSERT INTO order_item_link_issue (order_item_id, product_name, reason)
SELECT oi.id,
       oi.product_name,
       CASE WHEN EXISTS (SELECT 1 FROM products p WHERE p.name = oi.product_name)
            THEN 'AMBIGUOUS' ELSE 'NOT_FOUND' END
FROM order_items oi
WHERE oi.product_id IS NULL;
