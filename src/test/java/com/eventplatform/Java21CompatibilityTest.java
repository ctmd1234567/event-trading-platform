package com.eventplatform;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.parser.JsqlParserGlobal;
import com.eventplatform.config.MybatisConfig;
import com.eventplatform.entity.Shop;
import com.eventplatform.utils.RedisData;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Dependency migration checks that do not start the application or contact external services. */
class Java21CompatibilityTest {
    @Test
    void readsExistingLogicalExpiryCacheAndRoundTripsJavaTime() {
        String existing = "{\"expireTime\":\"2026-09-03T12:30:00\","
                + "\"data\":{\"id\":1,\"name\":\"shop\"}}";
        RedisData cached = JSONUtil.toBean(existing, RedisData.class);
        Shop shop = JSONUtil.toBean((JSONObject) cached.getData(), Shop.class);
        assertThat(shop.getId()).isEqualTo(1L);
        assertThat(shop.getName()).isEqualTo("shop");
        assertThat(cached.getExpireTime()).isEqualTo(LocalDateTime.of(2026, 9, 3, 12, 30));

        cached.setData(shop);
        RedisData restored = JSONUtil.toBean(JSONUtil.toJsonStr(cached), RedisData.class);
        assertThat(restored.getExpireTime()).isEqualTo(cached.getExpireTime());
        assertThat(JSONUtil.toBean((JSONObject) restored.getData(), Shop.class).getId())
                .isEqualTo(shop.getId());
    }

    @Test
    void paginationPluginAndSqlParserLoadTogether() throws Exception {
        assertThat(new MybatisConfig().mybatisPlusInterceptor().getInterceptors()).hasSize(1);
        assertThat(JsqlParserGlobal.parse("SELECT id FROM tb_shop ORDER BY id LIMIT 5").toString())
                .contains("tb_shop", "LIMIT 5");
    }

    @Test
    void redisAndJdbcConfigurationBindWithBoot3() throws Exception {
        StandardEnvironment environment = new StandardEnvironment();
        new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yaml"))
                .forEach(source -> environment.getPropertySources().addFirst(source));
        RedisProperties redis = Binder.get(environment)
                .bind("spring.data.redis", RedisProperties.class).get();
        assertThat(redis.getLettuce().getPool().getMaxActive()).isEqualTo(30);
        assertThat(environment.getProperty("spring.redis.host")).isNull();
        DataSourceProperties datasource = Binder.get(environment)
                .bind("spring.datasource", DataSourceProperties.class).get();
        assertThat(datasource.getDriverClassName()).isEqualTo("com.mysql.cj.jdbc.Driver");
        assertThat(Class.forName(datasource.getDriverClassName())).isNotNull();
    }
}
