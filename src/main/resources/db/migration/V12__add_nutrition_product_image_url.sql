ALTER TABLE nutrition_products
    ADD COLUMN image_url VARCHAR(2048) NULL AFTER english_name;

UPDATE nutrition_products SET image_url = '/images/products/deep_sleep_vision.png'
    WHERE code = 'DEEP_SLEEP_VISION';
UPDATE nutrition_products SET image_url = '/images/products/vital_skin_shot.png'
    WHERE code = 'VITAL_SKIN_SHOT';
UPDATE nutrition_products SET image_url = '/images/products/revive_energy_shot.png'
    WHERE code = 'REVIVE_ENERGY_SHOT';
