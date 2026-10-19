package ru.itmo.khlopensky.config;

import jakarta.persistence.EntityManagerFactory;
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.EclipseLinkJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import javax.sql.DataSource;
import java.util.Map;

@Configuration
@EnableTransactionManagement
public class JpaConfig {
    @Bean
    @DependsOnDatabaseInitialization
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        var factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("ru.itmo.khlopensky.entity");
        factory.setJpaVendorAdapter(new EclipseLinkJpaVendorAdapter());
        factory.setJpaPropertyMap(Map.of(
                "eclipselink.weaving", "false",
                "eclipselink.target-database", "org.eclipse.persistence.platform.database.PostgreSQLPlatform",
                "eclipselink.ddl-generation", "none",
                "eclipselink.cache.shared.default", "false",
                "jakarta.persistence.validation.mode", "CALLBACK"));
        return factory;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
        return new JpaTransactionManager(factory);
    }
}
