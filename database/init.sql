-- Database initialization script for REST Performance Benchmark
-- PostgreSQL 14+

-- Drop tables if they exist
DROP TABLE IF EXISTS item CASCADE;
DROP TABLE IF EXISTS category CASCADE;

-- Create Category table
CREATE TABLE category (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32) UNIQUE NOT NULL,
    name        VARCHAR(128) NOT NULL,
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Create Item table
CREATE TABLE item (
    id          BIGSERIAL PRIMARY KEY,
    sku         VARCHAR(64) UNIQUE NOT NULL,
    name        VARCHAR(128) NOT NULL,
    price       NUMERIC(10,2) NOT NULL,
    stock       INT NOT NULL,
    category_id BIGINT NOT NULL REFERENCES category(id),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    description TEXT
);

-- Create indexes
CREATE INDEX idx_item_category ON item(category_id);
CREATE INDEX idx_item_updated_at ON item(updated_at);
CREATE INDEX idx_category_code ON category(code);

-- Function to generate test data
-- This will be called by the application or can be executed manually

-- Generate 2000 categories
DO $$
BEGIN
    FOR i IN 1..2000 LOOP
        INSERT INTO category (code, name, updated_at)
        VALUES (
            'CAT' || LPAD(i::TEXT, 4, '0'),
            'Category ' || i,
            NOW()
        );
    END LOOP;
END $$;

-- Generate 100,000 items (~50 items per category)
DO $$
DECLARE
    category_count INT := 2000;
    items_per_category INT := 50;
    total_items INT := 100000;
    current_category_id BIGINT;
BEGIN
    FOR i IN 1..total_items LOOP
        -- Distribute items across categories
        SELECT id INTO current_category_id 
        FROM category 
        ORDER BY id 
        LIMIT 1 
        OFFSET ((i - 1) % category_count);
        
        INSERT INTO item (sku, name, price, stock, category_id, updated_at)
        VALUES (
            'SKU' || LPAD(i::TEXT, 6, '0'),
            'Item ' || i,
            ROUND((RANDOM() * 1000 + 10)::NUMERIC, 2),
            FLOOR(RANDOM() * 1000)::INT,
            current_category_id,
            NOW()
        );
    END LOOP;
END $$;

-- Display statistics
SELECT 
    'Categories' as table_name, 
    COUNT(*) as row_count 
FROM category
UNION ALL
SELECT 
    'Items' as table_name, 
    COUNT(*) as row_count 
FROM item;

-- Verify distribution
SELECT 
    c.code,
    c.name,
    COUNT(i.id) as item_count
FROM category c
LEFT JOIN item i ON c.id = i.category_id
GROUP BY c.id, c.code, c.name
ORDER BY c.id
LIMIT 10;
