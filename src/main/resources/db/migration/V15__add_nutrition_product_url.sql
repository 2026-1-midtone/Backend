ALTER TABLE nutrition_products
    ADD COLUMN product_url VARCHAR(2048) NULL AFTER image_url;

UPDATE nutrition_products SET product_url = 'https://www.vivegen.co.kr/shop_view?idx=321'
    WHERE code = 'DEEP_SLEEP_VISION';
UPDATE nutrition_products SET product_url = 'https://www.vivegen.co.kr/skin/?idx=153'
    WHERE code = 'VITAL_SKIN_SHOT';
UPDATE nutrition_products SET product_url = 'https://www.vivegen.co.kr/energy/?idx=152'
    WHERE code = 'REVIVE_ENERGY_SHOT';
