SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for tb_blog
-- ----------------------------
DROP TABLE IF EXISTS `tb_blog`;
CREATE TABLE `tb_blog`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `images` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` varchar(2048) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `liked` int(8) UNSIGNED NULL DEFAULT 0,
  `comments` int(8) UNSIGNED NULL DEFAULT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 23 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_blog
-- ----------------------------

-- ----------------------------
-- Table structure for tb_follow
-- ----------------------------
DROP TABLE IF EXISTS `tb_follow`;
CREATE TABLE `tb_follow`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `follow_user_id` bigint(20) UNSIGNED NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_follow
-- ----------------------------

-- ----------------------------
-- Table structure for tb_seckill_voucher
-- ----------------------------
DROP TABLE IF EXISTS `tb_seckill_voucher`;
CREATE TABLE `tb_seckill_voucher`  (
  `voucher_id` bigint(20) UNSIGNED NOT NULL,
  `stock` int(8) NOT NULL,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `begin_time` timestamp NOT NULL,
  `end_time` timestamp NOT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`voucher_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_seckill_voucher
-- ----------------------------

-- ----------------------------
-- Table structure for tb_shop
-- ----------------------------
DROP TABLE IF EXISTS `tb_shop`;
CREATE TABLE `tb_shop`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type_id` bigint(20) UNSIGNED NOT NULL,
  `images` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `area` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `x` double UNSIGNED NOT NULL,
  `y` double UNSIGNED NOT NULL,
  `avg_price` bigint(10) UNSIGNED NULL DEFAULT NULL,
  `sold` int(10) UNSIGNED ZEROFILL NOT NULL,
  `comments` int(10) UNSIGNED ZEROFILL NOT NULL,
  `score` int(2) UNSIGNED ZEROFILL NOT NULL,
  `open_hours` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `foreign_key_type`(`type_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 15 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_shop
-- ----------------------------

-- ----------------------------
-- Table structure for tb_shop_type
-- ----------------------------
DROP TABLE IF EXISTS `tb_shop_type`;
CREATE TABLE `tb_shop_type`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sort` int(3) UNSIGNED NULL DEFAULT NULL,
  `create_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_shop_type
-- ----------------------------

-- ----------------------------
-- Table structure for tb_user
-- ----------------------------
DROP TABLE IF EXISTS `tb_user`;
CREATE TABLE `tb_user`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `nick_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `icon` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uniqe_key_phone`(`phone`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1010 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_user
-- ----------------------------

-- ----------------------------
-- Table structure for tb_user_info
-- ----------------------------
DROP TABLE IF EXISTS `tb_user_info`;
CREATE TABLE `tb_user_info`  (
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `city` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT '',
  `introduce` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `fans` int(8) UNSIGNED NULL DEFAULT 0,
  `followee` int(8) UNSIGNED NULL DEFAULT 0,
  `gender` tinyint(1) UNSIGNED NULL DEFAULT 0,
  `birthday` date NULL DEFAULT NULL,
  `credits` int(8) UNSIGNED NULL DEFAULT 0,
  `level` tinyint(1) UNSIGNED NULL DEFAULT 0,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_user_info
-- ----------------------------

-- ----------------------------
-- Table structure for tb_voucher
-- ----------------------------
DROP TABLE IF EXISTS `tb_voucher`;
CREATE TABLE `tb_voucher`  (
  `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT,
  `shop_id` bigint(20) UNSIGNED NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `sub_title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `rules` varchar(1024) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `pay_value` bigint(10) UNSIGNED NOT NULL,
  `actual_value` bigint(10) NOT NULL,
  `type` tinyint(1) UNSIGNED NOT NULL DEFAULT 0,
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_voucher
-- ----------------------------

-- ----------------------------
-- Table structure for tb_voucher_order
-- ----------------------------
DROP TABLE IF EXISTS `tb_voucher_order`;
CREATE TABLE `tb_voucher_order`  (
  `id` bigint(20) NOT NULL,
  `user_id` bigint(20) UNSIGNED NOT NULL,
  `voucher_id` bigint(20) UNSIGNED NOT NULL,
  `pay_type` tinyint(1) UNSIGNED NOT NULL DEFAULT 1,
  `status` tinyint(1) UNSIGNED NOT NULL DEFAULT 1,
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `pay_time` timestamp NULL DEFAULT NULL,
  `use_time` timestamp NULL DEFAULT NULL,
  `refund_time` timestamp NULL DEFAULT NULL,
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Compact;

-- ----------------------------
-- Records of tb_voucher_order
-- ----------------------------

-- Minimal English demo data for local development.
INSERT INTO `tb_shop_type` VALUES (1, 'Dining', '/types/dining.png', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `tb_user` VALUES (1, '13900000001', '', 'demo_user', '', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `tb_shop` VALUES (1, 'Demo Cafe', 1, '', 'Downtown', '100 Market Street', 120.150000, 30.320000, 50, 0, 0, 45, '09:00-22:00', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `tb_blog` VALUES (1, 1, 1, 'Demo post', '', 'A sample post for local development.', 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `tb_voucher` VALUES (1, 1, 'Demo voucher', 'Local development only', 'One voucher per user', 4750, 5000, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
INSERT INTO `tb_seckill_voucher` VALUES (1, 100, CURRENT_TIMESTAMP, '2020-01-01 00:00:00', '2099-12-31 23:59:59', CURRENT_TIMESTAMP);

SET FOREIGN_KEY_CHECKS = 1;
