DROP TRIGGER trg_customers_name_dual_write ON customers;
DROP FUNCTION customers_name_dual_write();

ALTER TABLE customers DROP COLUMN full_name;

ALTER TABLE orders
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone,
    ALTER COLUMN customer_id SET NOT NULL;

ALTER TABLE order_items
    DROP COLUMN product_name,
    DROP COLUMN product_price,
    ALTER COLUMN product_id SET NOT NULL;
