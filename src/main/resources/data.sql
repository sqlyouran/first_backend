-- City seed data (aligned with frontend cityGrid.data.ts)
-- Uses INSERT IGNORE to ensure idempotent startup

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a1111111-1111-1111-1111-111111111111', 'Beijing', '北京', 'beijing',
        'https://picsum.photos/800/600?random=10',
        'Ancient capital with imperial grandeur', 'Autumn',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a2222222-2222-2222-2222-222222222222', 'Shanghai', '上海', 'shanghai',
        'https://picsum.photos/800/600?random=11',
        'Modern metropolis on the Huangpu', 'Spring',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a3333333-3333-3333-3333-333333333333', 'Chengdu', '成都', 'chengdu',
        'https://picsum.photos/800/600?random=12',
        'Home of pandas and spicy cuisine', 'Spring',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a4444444-4444-4444-4444-444444444444', 'Xi''an', '西安', 'xian',
        'https://picsum.photos/800/600?random=13',
        'Terracotta warriors and Silk Road heritage', 'Autumn',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a5555555-5555-5555-5555-555555555555', 'Hangzhou', '杭州', 'hangzhou',
        'https://picsum.photos/800/600?random=14',
        'West Lake and tea plantations', 'Spring',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a6666666-6666-6666-6666-666666666666', 'Guilin', '桂林', 'guilin',
        'https://picsum.photos/800/600?random=15',
        'Karst mountains and Li River cruises', 'Summer',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a7777777-7777-7777-7777-777777777777', 'Lijiang', '丽江', 'lijiang',
        'https://picsum.photos/800/600?random=16',
        'Naxi old town and Jade Dragon Snow Mountain', 'Winter',
        NOW(), NOW(), false);

INSERT IGNORE INTO cities (id, name, name_zh, slug, cover_image, description, best_season, created_at, updated_at, deleted)
VALUES ('a8888888-8888-8888-8888-888888888888', 'Xiamen', '厦门', 'xiamen',
        'https://picsum.photos/800/600?random=17',
        'Coastal charm and Gulangyu Island', 'Autumn',
        NOW(), NOW(), false);

-- Spot seed data (15+ spots across 8 cities)
-- Uses INSERT IGNORE to ensure idempotent startup

-- Beijing spots (city_id = a1111111-1111-1111-1111-111111111111)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b1111111-1111-1111-1111-111111111111', 'Forbidden City', '故宫', 'forbidden-city',
        'The world''s largest palace complex with 600 years of imperial history',
        '世界上最大的宫殿建筑群，拥有600年的皇家历史',
        'https://picsum.photos/800/600?random=100',
        '["https://picsum.photos/800/600?random=101","https://picsum.photos/800/600?random=102"]',
        '["heritage","history","architecture"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.8, 15200, 3200,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b2222222-2222-2222-2222-222222222222', 'Great Wall at Badaling', '八达岭长城', 'great-wall-badaling',
        'The most visited section of the Great Wall of China',
        '中国长城最著名的段落',
        'https://picsum.photos/800/600?random=103',
        '["https://picsum.photos/800/600?random=104","https://picsum.photos/800/600?random=105"]',
        '["heritage","nature","hiking"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.9, 28500, 5800,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b3333333-3333-3333-3333-333333333333', 'Temple of Heaven', '天坛', 'temple-of-heaven',
        'Imperial temple where emperors prayed for good harvests',
        '皇帝祈求丰收的皇家寺庙',
        'https://picsum.photos/800/600?random=106',
        '["https://picsum.photos/800/600?random=107"]',
        '["heritage","architecture","culture"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.5, 9800, 1900,
        NOW(), NOW(), false);

-- Shanghai spots (city_id = a2222222-2222-2222-2222-222222222222)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b4444444-4444-4444-4444-444444444444', 'The Bund', '外滩', 'the-bund',
        'Iconic waterfront promenade with colonial-era architecture',
        '标志性的滨江长廊，拥有殖民时期建筑',
        'https://picsum.photos/800/600?random=108',
        '["https://picsum.photos/800/600?random=109","https://picsum.photos/800/600?random=110"]',
        '["architecture","cityscape","nightlife"]',
        'a2222222-2222-2222-2222-222222222222', 'Shanghai', 'PUBLISHED', 4.7, 22300, 4100,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b5555555-5555-5555-5555-555555555555', 'Yu Garden', '豫园', 'yu-garden',
        'Classical Chinese garden from the Ming Dynasty',
        '明代古典园林',
        'https://picsum.photos/800/600?random=111',
        '["https://picsum.photos/800/600?random=112"]',
        '["garden","heritage","culture"]',
        'a2222222-2222-2222-2222-222222222222', 'Shanghai', 'PUBLISHED', 4.3, 11200, 2400,
        NOW(), NOW(), false);

-- Chengdu spots (city_id = a3333333-3333-3333-3333-333333333333)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b6666666-6666-6666-6666-666666666666', 'Chengdu Research Base of Giant Panda Breeding', '成都大熊猫繁育研究基地', 'chengdu-panda-base',
        'World-famous facility for giant panda conservation and research',
        '世界著名的大熊猫保护研究基地',
        'https://picsum.photos/800/600?random=113',
        '["https://picsum.photos/800/600?random=114","https://picsum.photos/800/600?random=115"]',
        '["wildlife","nature","family"]',
        'a3333333-3333-3333-3333-333333333333', 'Chengdu', 'PUBLISHED', 4.6, 18700, 4500,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b7777777-7777-7777-7777-777777777777', 'Wuhou Shrine', '武侯祠', 'wuhou-shrine',
        'Temple dedicated to the Three Kingdoms hero Zhuge Liang',
        '纪念三国英雄诸葛亮的祠堂',
        'https://picsum.photos/800/600?random=116',
        '["https://picsum.photos/800/600?random=117"]',
        '["history","culture","heritage"]',
        'a3333333-3333-3333-3333-333333333333', 'Chengdu', 'PUBLISHED', 4.2, 7600, 1500,
        NOW(), NOW(), false);

-- Xi'an spots (city_id = a4444444-4444-4444-4444-444444444444)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b8888888-8888-8888-8888-888888888888', 'Terracotta Army', '兵马俑', 'terracotta-army',
        'Life-sized terracotta soldiers guarding the tomb of Emperor Qin Shi Huang',
        '守护秦始皇陵的真人大小兵马俑',
        'https://picsum.photos/800/600?random=118',
        '["https://picsum.photos/800/600?random=119","https://picsum.photos/800/600?random=120"]',
        '["heritage","history","archaeology"]',
        'a4444444-4444-4444-4444-444444444444', 'Xi''an', 'PUBLISHED', 4.9, 25000, 5200,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('b9999999-9999-9999-9999-999999999999', 'Xi''an City Wall', '西安城墙', 'xian-city-wall',
        'One of the oldest and best-preserved Chinese city walls',
        '中国最古老、保存最完好的城墙之一',
        'https://picsum.photos/800/600?random=121',
        '["https://picsum.photos/800/600?random=122"]',
        '["heritage","history","cycling"]',
        'a4444444-4444-4444-4444-444444444444', 'Xi''an', 'PUBLISHED', 4.4, 8900, 1800,
        NOW(), NOW(), false);

-- Hangzhou spots (city_id = a5555555-5555-5555-5555-555555555555)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('c0000000-0000-0000-0000-000000000001', 'West Lake', '西湖', 'west-lake',
        'UNESCO World Heritage lake renowned for its beauty and pagodas',
        '联合国教科文组织世界遗产湖泊',
        'https://picsum.photos/800/600?random=123',
        '["https://picsum.photos/800/600?random=124","https://picsum.photos/800/600?random=125"]',
        '["nature","heritage","scenic"]',
        'a5555555-5555-5555-5555-555555555555', 'Hangzhou', 'PUBLISHED', 4.7, 20100, 3900,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('c0000000-0000-0000-0000-000000000002', 'Lingyin Temple', '灵隐寺', 'lingyin-temple',
        'One of the largest and wealthiest Buddhist temples in China',
        '中国最大最富有的佛教寺庙之一',
        'https://picsum.photos/800/600?random=126',
        '["https://picsum.photos/800/600?random=127"]',
        '["culture","heritage","spiritual"]',
        'a5555555-5555-5555-5555-555555555555', 'Hangzhou', 'PUBLISHED', 4.5, 10500, 2100,
        NOW(), NOW(), false);

-- Guilin spots (city_id = a6666666-6666-6666-6666-666666666666)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('c0000000-0000-0000-0000-000000000003', 'Li River Cruise', '漓江游船', 'li-river-cruise',
        'Scenic cruise through karst mountain landscapes',
        '穿越喀斯特山脉的观光游船',
        'https://picsum.photos/800/600?random=128',
        '["https://picsum.photos/800/600?random=129","https://picsum.photos/800/600?random=130"]',
        '["nature","scenic","cruise"]',
        'a6666666-6666-6666-6666-666666666666', 'Guilin', 'PUBLISHED', 4.8, 16800, 3500,
        NOW(), NOW(), false);

-- Lijiang spots (city_id = a7777777-7777-7777-7777-777777777777)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('c0000000-0000-0000-0000-000000000004', 'Lijiang Old Town', '丽江古城', 'lijiang-old-town',
        'UNESCO World Heritage Naxi old town with cobblestone streets',
        '联合国教科文组织世界遗产的纳西古城',
        'https://picsum.photos/800/600?random=131',
        '["https://picsum.photos/800/600?random=132","https://picsum.photos/800/600?random=133"]',
        '["heritage","culture","nightlife"]',
        'a7777777-7777-7777-7777-777777777777', 'Lijiang', 'PUBLISHED', 4.6, 19500, 4200,
        NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('c0000000-0000-0000-0000-000000000005', 'Jade Dragon Snow Mountain', '玉龙雪山', 'jade-dragon-snow-mountain',
        'Sacred mountain with glaciers and alpine meadows',
        '拥有冰川和高山草甸的神圣雪山',
        'https://picsum.photos/800/600?random=134',
        '["https://picsum.photos/800/600?random=135"]',
        '["nature","hiking","scenic"]',
        'a7777777-7777-7777-7777-777777777777', 'Lijiang', 'PUBLISHED', 4.7, 14300, 3100,
        NOW(), NOW(), false);

-- Xiamen spots (city_id = a8888888-8888-8888-8888-888888888888)
INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, created_at, updated_at, deleted)
VALUES ('c0000000-0000-0000-0000-000000000006', 'Gulangyu Island', '鼓浪屿', 'gulangyu-island',
        'Car-free island with colonial architecture and piano museum',
        '无车岛屿，拥有殖民建筑和钢琴博物馆',
        'https://picsum.photos/800/600?random=136',
        '["https://picsum.photos/800/600?random=137","https://picsum.photos/800/600?random=138"]',
        '["heritage","architecture","island"]',
        'a8888888-8888-8888-8888-888888888888', 'Xiamen', 'PUBLISHED', 4.4, 12600, 2800,
        NOW(), NOW(), false);

-- Post seed data (6 PUBLISHED posts + 1 DRAFT for filtering tests)
-- author_id uses a fixed seed UUID for deterministic data

INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d1111111-1111-1111-1111-111111111111',
        'A Week in Beijing: Imperial Wonders',
        '# Beijing Travel Guide\nSpent an unforgettable week exploring the Forbidden City, climbing the Great Wall at Badaling, and wandering through the Temple of Heaven.',
        'https://picsum.photos/800/600?random=200',
        '["beijing","heritage","travel"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d2222222-2222-2222-2222-222222222222',
        'Shanghai: Where Old Meets New',
        '# Shanghai Highlights\nFrom the colonial Bund to the classical Yu Garden, Shanghai blends history with modernity like nowhere else.',
        'https://picsum.photos/800/600?random=201',
        '["shanghai","cityscape","culture"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d3333333-3333-3333-3333-333333333333',
        'Pandas and Spice: My Chengdu Diary',
        '# Chengdu Adventures\nMet the giant pandas at the research base and explored the ancient Wuhou Shrine. The spicy hotpot was the real highlight.',
        'https://picsum.photos/800/600?random=202',
        '["chengdu","pandas","food"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d4444444-4444-4444-4444-444444444444',
        'Walking the Ancient Xi''an City Wall',
        '# Xi''an Heritage\nCycling atop the ancient city wall at sunset, then visiting the Terracotta Army the next morning — pure magic.',
        'https://picsum.photos/800/600?random=203',
        '["xian","heritage","history"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d5555555-5555-5555-5555-555555555555',
        'West Lake Serenity: Hangzhou Weekend',
        '# Hangzhou Escape\nA peaceful weekend by West Lake, visiting Lingyin Temple and sipping Longjing tea in the surrounding plantations.',
        'https://picsum.photos/800/600?random=204',
        '["hangzhou","nature","westlake"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d6666666-6666-6666-6666-666666666666',
        'Southern Coast Gems: Guilin and Xiamen',
        '# South China Tour\nCruised the Li River past karst peaks, then island-hopped to Gulangyu for colonial architecture and piano music.',
        'https://picsum.photos/800/600?random=205',
        '["guilin","xiamen","coastal"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

-- Draft post (used to verify DRAFT filtering in association queries)
INSERT IGNORE INTO posts (id, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d7777777-7777-7777-7777-777777777777',
        'Unfinished Lijiang Notes (Draft)',
        '# Draft\nWork in progress notes on Lijiang Old Town and Jade Dragon Snow Mountain.',
        'https://picsum.photos/800/600?random=206',
        '["lijiang","draft"]',
        'DRAFT', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

-- Spots-Posts association seed data
-- Covers multiple spots and posts with cross-linking for thorough testing

-- Beijing: Forbidden City linked to Beijing post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f1111111-1111-1111-1111-111111111111',
        'b1111111-1111-1111-1111-111111111111', 'd1111111-1111-1111-1111-111111111111',
        NOW(), NOW(), false);

-- Beijing: Great Wall linked to Beijing post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f2222222-2222-2222-2222-222222222222',
        'b2222222-2222-2222-2222-222222222222', 'd1111111-1111-1111-1111-111111111111',
        NOW(), NOW(), false);

-- Shanghai: The Bund linked to Shanghai post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f3333333-3333-3333-3333-333333333333',
        'b4444444-4444-4444-4444-444444444444', 'd2222222-2222-2222-2222-222222222222',
        NOW(), NOW(), false);

-- Chengdu: Panda Base linked to Chengdu post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f4444444-4444-4444-4444-444444444444',
        'b6666666-6666-6666-6666-666666666666', 'd3333333-3333-3333-3333-333333333333',
        NOW(), NOW(), false);

-- Xi'an: Terracotta Army linked to Xi'an post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f5555555-5555-5555-5555-555555555555',
        'b8888888-8888-8888-8888-888888888888', 'd4444444-4444-4444-4444-444444444444',
        NOW(), NOW(), false);

-- Hangzhou: West Lake linked to Hangzhou post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f6666666-6666-6666-6666-666666666666',
        'c0000000-0000-0000-0000-000000000001', 'd5555555-5555-5555-5555-555555555555',
        NOW(), NOW(), false);

-- Guilin: Li River linked to South Coast post
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f7777777-7777-7777-7777-777777777777',
        'c0000000-0000-0000-0000-000000000003', 'd6666666-6666-6666-6666-666666666666',
        NOW(), NOW(), false);

-- Xiamen: Gulangyu linked to South Coast post (same post, multiple spots)
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f8888888-8888-8888-8888-888888888888',
        'c0000000-0000-0000-0000-000000000006', 'd6666666-6666-6666-6666-666666666666',
        NOW(), NOW(), false);

-- Cross-link: Forbidden City also linked to South Coast post (verifies multi-association)
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('f9999999-9999-9999-9999-999999999999',
        'b1111111-1111-1111-1111-111111111111', 'd6666666-6666-6666-6666-666666666666',
        NOW(), NOW(), false);

-- Draft-only association: Temple of Heaven linked to DRAFT post (should be filtered out)
INSERT IGNORE INTO spots_posts (id, spot_id, post_id, created_at, updated_at, deleted)
VALUES ('faaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
        'b3333333-3333-3333-3333-333333333333', 'd7777777-7777-7777-7777-777777777777',
        NOW(), NOW(), false);
