ALTER TABLE customers ADD COLUMN first_name VARCHAR(128);
ALTER TABLE customers ADD COLUMN last_name VARCHAR(128);

-- «Старый» писатель знает только full_name, «новый» - только first_name/last_name.
-- Триггер держит обе версии согласованными до contract-шага (V4).
CREATE OR REPLACE FUNCTION customers_name_dual_write() RETURNS trigger
LANGUAGE plpgsql AS $$
DECLARE
    parts text[];
BEGIN
    IF TG_OP = 'INSERT' THEN
        IF NEW.first_name IS NULL AND NEW.last_name IS NULL THEN
            -- вставка старой версией приложения: есть только full_name
            parts := regexp_split_to_array(btrim(NEW.full_name), '\s+');
            NEW.first_name := COALESCE(parts[1], '');
            NEW.last_name := COALESCE(array_to_string(parts[2:], ' '), '');
        ELSE
            -- вставка новой версией приложения: собираем legacy-представление
            NEW.full_name := btrim(NEW.first_name || ' ' || COALESCE(NEW.last_name, ''));
        END IF;
    ELSIF NEW.first_name IS NOT DISTINCT FROM OLD.first_name
          AND NEW.last_name IS NOT DISTINCT FROM OLD.last_name THEN
        -- обновление старой версией приложения: пересчитываем новые колонки
        parts := regexp_split_to_array(btrim(NEW.full_name), '\s+');
        NEW.first_name := COALESCE(parts[1], '');
        NEW.last_name := COALESCE(array_to_string(parts[2:], ' '), '');
    ELSE
        -- обновление новой версией приложения: поддерживаем full_name
        NEW.full_name := btrim(NEW.first_name || ' ' || COALESCE(NEW.last_name, ''));
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_customers_name_dual_write
BEFORE INSERT OR UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION customers_name_dual_write();

-- backfill существующих клиентов по тем же правилам, что и в триггере.
UPDATE customers c
SET first_name = s.parts[1],
    last_name = COALESCE(array_to_string(s.parts[2:], ' '), '')
FROM (
    SELECT id, regexp_split_to_array(btrim(full_name), '\s+') AS parts
    FROM customers
) s
WHERE s.id = c.id;

ALTER TABLE customers ALTER COLUMN first_name SET NOT NULL;
ALTER TABLE customers ALTER COLUMN last_name SET NOT NULL;
