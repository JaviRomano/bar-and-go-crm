-- Lo que necesita el descuento de stock por venta.

-- Momento en que un pedido se entregó (aunque después se cancele): un pedido entregado ya no se borra
ALTER TABLE orders ADD COLUMN delivered_at TIMESTAMPTZ;
UPDATE orders SET delivered_at = date_order WHERE status = 'DELIVERED';

-- Almacén del que salen las ventas: como mucho uno por local
ALTER TABLE warehouse ADD COLUMN default_for_sales BOOLEAN NOT NULL DEFAULT FALSE;
CREATE UNIQUE INDEX uq_warehouse_sales_per_site ON warehouse (site_id) WHERE default_for_sales;

-- Segunda red de idempotencia (D0 3.2): un pedido no descuenta dos veces el mismo artículo. Las
-- compensaciones llevan reverses_movement_id y quedan fuera del índice
CREATE UNIQUE INDEX uq_stock_movement_order_sale ON stock_movement (source_id, stock_item_id)
    WHERE source_type = 'ORDER' AND type = 'SALE' AND reverses_movement_id IS NULL;
