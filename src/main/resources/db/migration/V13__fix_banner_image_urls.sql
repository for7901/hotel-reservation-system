-- 修复不可访问的 picsum 外链，改用与演示酒店封面一致的 Unsplash 图
UPDATE banner
SET image_url = CASE sort_order
    WHEN 1 THEN 'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=320&fit=crop&q=80'
    WHEN 2 THEN 'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800&h=320&fit=crop&q=80'
    ELSE 'https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?w=800&h=320&fit=crop&q=80'
END
WHERE image_url LIKE '%picsum.photos%';

INSERT INTO banner (title, image_url, link_url, sort_order, status, created_at, updated_at)
SELECT '春日出行季',
       'https://images.unsplash.com/photo-1566073771259-6a8506099945?w=800&h=320&fit=crop&q=80',
       '/',
       1,
       1,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE title = '春日出行季');

INSERT INTO banner (title, image_url, link_url, sort_order, status, created_at, updated_at)
SELECT '周末特惠酒店',
       'https://images.unsplash.com/photo-1520250497591-112f2f40a3f4?w=800&h=320&fit=crop&q=80',
       '/hotels',
       2,
       1,
       NOW(),
       NOW()
WHERE NOT EXISTS (SELECT 1 FROM banner WHERE title = '周末特惠酒店');
