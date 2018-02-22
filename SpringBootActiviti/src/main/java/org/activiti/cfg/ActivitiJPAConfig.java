package org.activiti.cfg;

import java.util.Map;

import javax.annotation.Resource;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.alibaba.druid.pool.DruidDataSource;

@Configuration
@EnableTransactionManagement
// 开启事物管理
@EnableJpaRepositories(// 自定义数据管理的配置
// 指定EntityManager的创建工厂Bean
entityManagerFactoryRef = "entityManagerFactory",
// 指定事物管理的Bean
transactionManagerRef = "transactionManager",
// 指定管理的实体位置
basePackages = { "com.gm.activiti.domain" })
public class ActivitiJPAConfig {

	// 注入数据源配置信息
	@Autowired
	DataSourceProperties config;

	@Resource
	DataSource dataSource;// 注入配置好的数据源
	
	/*
	 * 配置实体管理工厂Bean
	 */
	@Bean
	public LocalContainerEntityManagerFactoryBean entityManagerFactory(
			EntityManagerFactoryBuilder builder) {
		return builder.dataSource(dataSource)
				.packages("com.gm.activiti.domain")
				// 设置实体类所在位置
				.persistenceUnit("activiti")
				.properties(getProperties(dataSource))// 设置hibernate通用配置
				.build();
	}

	// 注入spring自带的jpa属性类
	@Autowired
	private JpaProperties jpaProperties;

	/*
	 * 拿到hibernate的通用配置
	 */
	private Map<String, String> getProperties(DataSource dataSource) {
		return jpaProperties.getHibernateProperties(dataSource);
	}

	/*
	 * 配置事物管理的Bean
	 */
	@Bean
	public PlatformTransactionManager transactionManager(
			EntityManagerFactoryBuilder builder) {
		return new JpaTransactionManager(entityManagerFactory(builder)
				.getObject());
	}

}