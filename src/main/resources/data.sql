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

-- Spot seed data (19+ spots across 8 cities, Beijing has 19 with real-world data)
-- Uses INSERT IGNORE to ensure idempotent startup

-- Beijing spots (city_id = a1111111-1111-1111-1111-111111111111)
-- 3 existing spots updated with practical info + 16 new spots from Ctrip data

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('b1111111-1111-1111-1111-111111111111', 'Forbidden City', '故宫博物院', 'forbidden-city',
        'The imperial palace of the Ming and Qing dynasties, symbolizing ancient China''s majesty and cultural heritage. Housing over 1.8 million artifacts, it represents the pinnacle of Chinese architectural and artistic achievement.',
        '故宫博物院又称紫禁城，是明、清两代的皇宫，也是古老中国的标志和象征。当你置身于气派规整的高墙深院，能真真切切地感受到它曾经的荣耀。',
        'https://picsum.photos/800/600?random=100',
        '["https://picsum.photos/800/600?random=101","https://picsum.photos/800/600?random=102"]',
        '["heritage","history","architecture"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.8, 15200, 3200,
        '旺季60元/淡季40元', '08:30-17:00（4月-10月周二-周日）/ 08:30-16:30（11月-3月周二-周日），周一闭馆', '北京市东城区景山前街4号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('b2222222-2222-2222-2222-222222222222', 'Great Wall at Badaling', '八达岭长城', 'great-wall-badaling',
        'The most visited section of the Great Wall of China, known as one of the Nine Fortified Passes, featuring watchtowers and magnificent mountain views.',
        '八达岭长城号称天下九塞之一，风光集巍峨险峻、秀丽苍翠于一体，是明长城景色中的精华。不到长城非好汉。',
        'https://picsum.photos/800/600?random=103',
        '["https://picsum.photos/800/600?random=104","https://picsum.photos/800/600?random=105"]',
        '["heritage","hiking","scenic"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 28500, 5800,
        '旺季40元/淡季35元', '06:30-17:00（春夏季）/ 07:30-16:00（秋冬季）', '北京市延庆区G6京藏高速58号出口',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('b3333333-3333-3333-3333-333333333333', 'Temple of Heaven', '天坛公园', 'temple-of-heaven',
        'A UNESCO World Heritage Site where emperors performed rituals to heaven. Its iconic Hall of Prayer for Good Harvests represents the harmony between man and heaven.',
        '天坛是明、清两代帝王祭祀皇天、祈求丰收的场所，是世界上现存最大、保存最完整的祭天建筑群。',
        'https://picsum.photos/800/600?random=106',
        '["https://picsum.photos/800/600?random=107"]',
        '["heritage","architecture","culture"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 9800, 1900,
        '旺季15元/淡季10元', '06:00-22:00（公园）/ 08:00-17:00（景点）', '北京市东城区天坛路1号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

-- New Beijing spots (16 additional, sourced from Ctrip)

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000001', 'Beijing Universal Resort', '北京环球度假区', 'beijing-universal-resort',
        'A comprehensive theme park featuring Universal Studios Beijing, CityWalk, and two resort hotels with seven themed lands, 37 attractions, and over 100 shops and restaurants.',
        '北京环球度假区是一个广受期待的主题公园旅游目的地，包括环球主题公园、北京环球城市大道以及两家度假酒店，引入中外各大IP。',
        'https://picsum.photos/800/600?random=140',
        '["https://picsum.photos/800/600?random=141"]',
        '["amusement","family","modern"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.6, 32000, 6500,
        '约548元起（根据日期浮动）', '09:00-21:00', '北京市通州区京哈高速与东六环路交汇处西北角',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000002', 'Prince Gong''s Mansion', '恭王府', 'prince-gongs-mansion',
        'The largest preserved royal mansion from the Qing Dynasty, featuring exquisite architecture, classic gardens, and the famous Fortune Garden.',
        '恭王府位于北京西城区前海西街，先后作为和珅、庆亲王永璘的宅邸，故有一座恭王府，半部清代史的说法。',
        'https://picsum.photos/800/600?random=142',
        '["https://picsum.photos/800/600?random=143"]',
        '["heritage","architecture","history"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 18000, 3600,
        '40元', '08:30-17:00（周二-周日）', '北京市西城区前海西街17号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000003', 'Beijing Wildlife Park', '北京野生动物园', 'beijing-wildlife-park',
        'A 4A-rated zoo featuring over 200 species and 5,000 animals including drive-through safari areas and interactive feeding opportunities.',
        '北京野生动物园坐落于永定河畔，在3600余亩的土地上养育着200余种、5000余头只来自世界各地的珍稀野生动物。',
        'https://picsum.photos/800/600?random=144',
        '["https://picsum.photos/800/600?random=145"]',
        '["wildlife","family","nature"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 16000, 3200,
        '成人票约145元', '08:30-17:30（春夏季）/ 09:00-17:00（秋冬季）', '北京市大兴区榆垡镇万亩森林',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000004', 'Beijing Happy Valley', '北京欢乐谷', 'beijing-happy-valley',
        'A major amusement park featuring thrilling roller coasters, family-friendly attractions, live performances, and themed entertainment zones.',
        '北京欢乐谷是北京最大的现代主题公园，占地100万平米，拥有国际一流的娱乐设施和精彩纷呈的文艺表演。',
        'https://picsum.photos/800/600?random=146',
        '["https://picsum.photos/800/600?random=147"]',
        '["amusement","family"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 9500, 2000,
        '约199元起', '10:00-22:00', '北京市朝阳区金盏乡蛎壳港路1号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000005', 'National Museum of China', '中国国家博物馆', 'national-museum-of-china',
        'The national institution housing 1.4 million pieces spanning ancient relics to modern artifacts, showcasing China''s rich history and cultural achievements.',
        '中国国家博物馆是代表国家收藏、研究、展示中华优秀传统文化代表性物证的机构，是国家历史文化艺术殿堂。',
        'https://picsum.photos/800/600?random=148',
        '["https://picsum.photos/800/600?random=149"]',
        '["culture","history","heritage"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 12000, 2500,
        '免费（需提前预约）', '09:00-17:30（6月-10月）/ 09:00-17:00（11月-5月），周一闭馆', '北京市东城区东长安街16号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000006', 'Summer Palace', '颐和园', 'summer-palace',
        'A masterpiece of Chinese garden design and one of the best-preserved imperial gardens, featuring Kunming Lake, Longevity Hill, and the famous Long Corridor.',
        '颐和园是中国古代皇家园林的典范，以昆明湖、万寿山为基础，汲取江南园林的设计手法而改建。',
        'https://picsum.photos/800/600?random=150',
        '["https://picsum.photos/800/600?random=151"]',
        '["garden","heritage","scenic"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 25000, 5000,
        '旺季30元/淡季20元', '06:30-18:00', '北京市海淀区新建宫门路19号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000007', 'Mutianyu Great Wall', '慕田峪长城', 'mutianyu-great-wall',
        'A well-preserved Ming Dynasty Great Wall section with lush forests, cable car facilities, and a scenic toboggan ride, offering fewer crowds than Badaling.',
        '慕田峪长城是明代长城的重要组成部分，登山道路险峻，景色优美，既保留了古长城的原始风貌，又具有现代化的游览设施。',
        'https://picsum.photos/800/600?random=152',
        '["https://picsum.photos/800/600?random=153"]',
        '["hiking","scenic","heritage"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.8, 20000, 4200,
        '成人票45元', '08:00-17:00', '北京市怀柔区慕田峪村',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000008', 'Shichahai Scenic Area', '什刹海风景区', 'shichahai-scenic-area',
        'A historic area with three interconnected lakes surrounded by traditional hutongs, temples, and vibrant nightlife along the waterfront.',
        '什刹海包括前海、后海和西海三个湖泊，汇集了众多名人故居和文化景观，是体验北京古城文化的好地方。',
        'https://picsum.photos/800/600?random=154',
        '["https://picsum.photos/800/600?random=155"]',
        '["nightlife","culture","scenic"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 3048, 800,
        '免费', '全天开放', '北京市西城区前海西街后海地区',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000009', 'POP MART City Paradise', '泡泡玛特城市乐园', 'pop-mart-city-paradise',
        'An immersive art toy theme park featuring collectible art toys, interactive installations, and creative experiences in a modern setting.',
        '泡泡玛特城市乐园是一个集艺术展览、互动娱乐、文创商品和餐饮于一体的创意主题乐园。',
        'https://picsum.photos/800/600?random=156',
        '["https://picsum.photos/800/600?random=157"]',
        '["modern","family"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.5, 2457, 500,
        '约88元起', '10:00-22:00', '北京市朝阳区郎家湖西路1号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000010', 'Chaoyang Park', '朝阳公园', 'chaoyang-park',
        'One of Beijing''s largest urban parks featuring scenic lakes, recreational facilities, sports venues, and cultural attractions.',
        '朝阳公园是北京市最大的城市公园之一，园内有人工湖、骑马场、高尔夫球场等休闲娱乐设施。',
        'https://picsum.photos/800/600?random=158',
        '["https://picsum.photos/800/600?random=159"]',
        '["nature","scenic"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 1159, 300,
        '免费', '06:00-21:00', '北京市朝阳区建国路1号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000011', 'Tiananmen Square', '天安门广场', 'tiananmen-square',
        'China''s largest public square and political heart, surrounded by iconic buildings including the Great Hall of the People and National Museum.',
        '天安门广场位于北京市中心，面积达44万平方米，是世界上最大的城市广场，中心矗立着人民英雄纪念碑。',
        'https://picsum.photos/800/600?random=160',
        '["https://picsum.photos/800/600?random=161"]',
        '["heritage","cityscape","history"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.6, 8000, 1500,
        '免费（需提前预约）', '全天开放', '北京市东城区长安街中段',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000012', 'Yonghe Temple', '雍和宫', 'yonghe-temple',
        'The largest Tibetan Buddhist temple outside Tibet, housing the world''s tallest wooden Buddha statue with stunning golden-roofed architecture.',
        '雍和宫是北京最大的喇嘛庙宇，拥有中国最大的木雕弥勒佛，庭院殿堂建筑宏伟壮观。',
        'https://picsum.photos/800/600?random=162',
        '["https://picsum.photos/800/600?random=163"]',
        '["spiritual","culture","heritage"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 3830, 900,
        '25元', '09:00-16:30', '北京市东城区雍和宫大街12号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000013', 'Old Summer Palace', '圆明园', 'old-summer-palace',
        'A historical site of one of the world''s most magnificent imperial gardens from the Qing Dynasty, largely destroyed in 1860, now preserved as a park and museum.',
        '圆明园是清代皇帝的夏日行宫，由圆明园、长春园和万春园三园组成，1860年被英法联军焚毁，现为遗址公园。',
        'https://picsum.photos/800/600?random=164',
        '["https://picsum.photos/800/600?random=165"]',
        '["heritage","garden","history"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.5, 15000, 3000,
        '10元', '07:00-19:00（4月-10月）/ 07:00-18:00（11月-3月）', '北京市海淀区清华西路28号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000014', 'National Natural History Museum of China', '国家自然博物馆', 'national-natural-history-museum',
        'A comprehensive museum showcasing China''s natural history with fossils, minerals, animal specimens, and interactive educational exhibits.',
        '国家自然博物馆是综合性的自然历史博物馆，馆内珍藏着大量古生物化石、矿物标本和动物标本。',
        'https://picsum.photos/800/600?random=166',
        '["https://picsum.photos/800/600?random=167"]',
        '["culture","family"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.7, 2057, 450,
        '免费（需提前预约）', '09:00-17:00（周二-周日）', '北京市东城区天桥南大街126号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000015', 'Beijing Zoo', '北京动物园', 'beijing-zoo',
        'One of China''s earliest modern zoos established in 1906, housing over 7,000 animals of 600+ species, famous for giant pandas.',
        '北京动物园是中国建立最早的城市动物园，园内饲养着来自世界各地的各类动物600多种、7000多只，大熊猫是明星动物。',
        'https://picsum.photos/800/600?random=168',
        '["https://picsum.photos/800/600?random=169"]',
        '["wildlife","family"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.5, 10000, 2200,
        '15元', '07:30-18:00（春夏季）/ 08:00-17:00（秋冬季）', '北京市西城区西直门外大街137号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

INSERT IGNORE INTO spots (id, name, name_zh, slug, description, description_zh, cover_image, gallery, tags, city_id, city_name, status, rating, view_count, bookmark_count, ticket_price, opening_hours, address, data_refreshed_at, created_at, updated_at, deleted)
VALUES ('c1000000-0000-0000-0000-000000000016', 'Jingshan Park', '景山公园', 'jingshan-park',
        'A historic hilltop park offering panoramic views of the Forbidden City and Beijing skyline, the best vantage point for photographing the palace.',
        '景山公园是北京城区的最高点，从景山顶峰可以俯瞰整个北京城，特别是可以饱览故宫全景，是摄影爱好者的天堂。',
        'https://picsum.photos/800/600?random=170',
        '["https://picsum.photos/800/600?random=171"]',
        '["scenic","heritage"]',
        'a1111111-1111-1111-1111-111111111111', 'Beijing', 'PUBLISHED', 4.6, 5157, 1100,
        '2元', '06:00-21:00', '北京市西城区景山西街44号',
        '2026-07-11 02:00:00', NOW(), NOW(), false);

-- Update existing 3 Beijing spots with enriched data (INSERT IGNORE does not update existing rows in MySQL)
UPDATE spots SET
  name_zh = '故宫博物院',
  description = 'The imperial palace of the Ming and Qing dynasties, symbolizing ancient China''s majesty and cultural heritage. Housing over 1.8 million artifacts, it represents the pinnacle of Chinese architectural and artistic achievement.',
  description_zh = '故宫博物院又称紫禁城，是明、清两代的皇宫，也是古老中国的标志和象征。当你置身于气派规整的高墙深院，能真真切切地感受到它曾经的荣耀。',
  ticket_price = '旺季60元/淡季40元',
  opening_hours = '08:30-17:00（4月-10月周二-周日）/ 08:30-16:30（11月-3月周二-周日），周一闭馆',
  address = '北京市东城区景山前街4号',
  updated_at = NOW()
WHERE id = 'b1111111-1111-1111-1111-111111111111';

UPDATE spots SET
  description = 'The most visited section of the Great Wall of China, known as one of the Nine Fortified Passes, featuring watchtowers and magnificent mountain views.',
  description_zh = '八达岭长城号称天下九塞之一，风光集巍峨险峻、秀丽苍翠于一体，是明长城景色中的精华。不到长城非好汉。',
  tags = '["heritage","hiking","scenic"]',
  ticket_price = '旺季40元/淡季35元',
  opening_hours = '06:30-17:00（春夏季）/ 07:30-16:00（秋冬季）',
  address = '北京市延庆区G6京藏高速58号出口',
  updated_at = NOW()
WHERE id = 'b2222222-2222-2222-2222-222222222222';

UPDATE spots SET
  name_zh = '天坛公园',
  description = 'A UNESCO World Heritage Site where emperors performed rituals to heaven. Its iconic Hall of Prayer for Good Harvests represents the harmony between man and heaven.',
  description_zh = '天坛是明、清两代帝王祭祀皇天、祈求丰收的场所，是世界上现存最大、保存最完整的祭天建筑群。',
  rating = 4.7,
  ticket_price = '旺季15元/淡季10元',
  opening_hours = '06:00-22:00（公园）/ 08:00-17:00（景点）',
  address = '北京市东城区天坛路1号',
  updated_at = NOW()
WHERE id = 'b3333333-3333-3333-3333-333333333333';

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

INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d1111111-1111-1111-1111-111111111111',
        'a-week-in-beijing-imperial-wonders',
        'A Week in Beijing: Imperial Wonders',
        '# Beijing Travel Guide\nSpent an unforgettable week exploring the Forbidden City, climbing the Great Wall at Badaling, and wandering through the Temple of Heaven.',
        'https://picsum.photos/800/600?random=200',
        '["beijing","heritage","travel"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d2222222-2222-2222-2222-222222222222',
        'shanghai-where-old-meets-new',
        'Shanghai: Where Old Meets New',
        '# Shanghai Highlights\nFrom the colonial Bund to the classical Yu Garden, Shanghai blends history with modernity like nowhere else.',
        'https://picsum.photos/800/600?random=201',
        '["shanghai","cityscape","culture"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d3333333-3333-3333-3333-333333333333',
        'pandas-and-spice-my-chengdu-diary',
        'Pandas and Spice: My Chengdu Diary',
        '# Chengdu Adventures\nMet the giant pandas at the research base and explored the ancient Wuhou Shrine. The spicy hotpot was the real highlight.',
        'https://picsum.photos/800/600?random=202',
        '["chengdu","pandas","food"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d4444444-4444-4444-4444-444444444444',
        'walking-the-ancient-xian-city-wall',
        'Walking the Ancient Xi''an City Wall',
        '# Xi''an Heritage\nCycling atop the ancient city wall at sunset, then visiting the Terracotta Army the next morning — pure magic.',
        'https://picsum.photos/800/600?random=203',
        '["xian","heritage","history"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d5555555-5555-5555-5555-555555555555',
        'west-lake-serenity-hangzhou-weekend',
        'West Lake Serenity: Hangzhou Weekend',
        '# Hangzhou Escape\nA peaceful weekend by West Lake, visiting Lingyin Temple and sipping Longjing tea in the surrounding plantations.',
        'https://picsum.photos/800/600?random=204',
        '["hangzhou","nature","westlake"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d6666666-6666-6666-6666-666666666666',
        'southern-coast-gems-guilin-and-xiamen',
        'Southern Coast Gems: Guilin and Xiamen',
        '# South China Tour\nCruised the Li River past karst peaks, then island-hopped to Gulangyu for colonial architecture and piano music.',
        'https://picsum.photos/800/600?random=205',
        '["guilin","xiamen","coastal"]',
        'PUBLISHED', 'e0000000-0000-0000-0000-000000000001',
        NOW(), NOW(), false);

-- Draft post (used to verify DRAFT filtering in association queries)
INSERT IGNORE INTO posts (id, slug, title, content, cover_image, tags, status, author_id, created_at, updated_at, deleted)
VALUES ('d7777777-7777-7777-7777-777777777777',
        'unfinished-lijiang-notes-draft',
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
