CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50)
);

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone FROM orders;

ALTER TABLE orders ADD COLUMN customer_id BIGINT;

UPDATE orders o
SET customer_id = c.id
FROM customers c
WHERE c.full_name = o.customer_full_name
  AND COALESCE(c.address, '') = COALESCE(o.customer_address, '')
  AND COALESCE(c.phone, '') = COALESCE(o.customer_phone, '');

ALTER TABLE orders ADD CONSTRAINT fk_orders_customer
    FOREIGN KEY (customer_id) REFERENCES customers(id) NOT VALID;
ALTER TABLE orders VALIDATE CONSTRAINT fk_orders_customer;

ALTER TABLE order_items ADD COLUMN product_id BIGINT;

UPDATE order_items oi
SET product_id = p.id
FROM products p
WHERE p.name = oi.product_name;

ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id) NOT VALID;
ALTER TABLE order_items VALIDATE CONSTRAINT fk_order_items_product;