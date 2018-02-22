package org.activiti.cfg;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

import com.alibaba.druid.pool.DruidDataSource;

@ComponentScan
@Configuration
@ConfigurationProperties(prefix = "spring.datasource.activiti")
@PropertySource("classpath:/applicaion.properties")
public class DataSourceProperties {

	private String url;

	private String username;

	private String password;

	private String driverClassName;

	private Integer maxActive;

	private Integer initialSize;

	private Integer minIdle;

	private Integer maxWait;

	private Integer maxPoolPreparedStatementPerConnectionSize;

	private Integer timeBetweenEvictionRunsMillis;

	private Integer minEvictableIdleTimeMillis;

	private Boolean poolPreparedStatements;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDriverClassName() {
		return driverClassName;
	}

	public void setDriverClassName(String driverClassName) {
		this.driverClassName = driverClassName;
	}

	public Integer getMaxActive() {
		return maxActive;
	}

	public void setMaxActive(Integer maxActive) {
		this.maxActive = maxActive;
	}

	public Integer getInitialSize() {
		return initialSize;
	}

	public void setInitialSize(Integer initialSize) {
		this.initialSize = initialSize;
	}

	public Integer getMinIdle() {
		return minIdle;
	}

	public void setMinIdle(Integer minIdle) {
		this.minIdle = minIdle;
	}

	public Integer getMaxWait() {
		return maxWait;
	}

	public void setMaxWait(Integer maxWait) {
		this.maxWait = maxWait;
	}

	public Integer getMaxPoolPreparedStatementPerConnectionSize() {
		return maxPoolPreparedStatementPerConnectionSize;
	}

	public void setMaxPoolPreparedStatementPerConnectionSize(
			Integer maxPoolPreparedStatementPerConnectionSize) {
		this.maxPoolPreparedStatementPerConnectionSize = maxPoolPreparedStatementPerConnectionSize;
	}

	public Integer getTimeBetweenEvictionRunsMillis() {
		return timeBetweenEvictionRunsMillis;
	}

	public void setTimeBetweenEvictionRunsMillis(
			Integer timeBetweenEvictionRunsMillis) {
		this.timeBetweenEvictionRunsMillis = timeBetweenEvictionRunsMillis;
	}

	public Integer getMinEvictableIdleTimeMillis() {
		return minEvictableIdleTimeMillis;
	}

	public void setMinEvictableIdleTimeMillis(Integer minEvictableIdleTimeMillis) {
		this.minEvictableIdleTimeMillis = minEvictableIdleTimeMillis;
	}

	public Boolean getPoolPreparedStatements() {
		return poolPreparedStatements;
	}

	public void setPoolPreparedStatements(Boolean poolPreparedStatements) {
		this.poolPreparedStatements = poolPreparedStatements;
	}

	
	/*
	 * 配置数据源
	 */
	@Bean(name = "dataSource")
	public DataSource dataSource() {
		DruidDataSource dataSource = new DruidDataSource();
		// 设置数据源的属性
		setDruidProperties(dataSource);
		return dataSource;
	}
	
	// 设置数据源的属性的方法
	private void setDruidProperties(DruidDataSource dataSource) {
		dataSource.setUrl(getUrl());
		dataSource.setUsername(getUsername());
		dataSource.setPassword(getPassword());
		dataSource.setDriverClassName(getDriverClassName());
		dataSource.setMaxActive(getMaxActive());
		dataSource.setInitialSize(getInitialSize());
		dataSource.setMinIdle(getMinIdle());
		dataSource.setMaxWait(getMaxWait());
		dataSource.setMaxPoolPreparedStatementPerConnectionSize(getMaxPoolPreparedStatementPerConnectionSize());
		dataSource.setTimeBetweenEvictionRunsMillis(getTimeBetweenEvictionRunsMillis());
		dataSource.setMinEvictableIdleTimeMillis(getMinEvictableIdleTimeMillis());
		dataSource.setPoolPreparedStatements(getPoolPreparedStatements());
	}
}
