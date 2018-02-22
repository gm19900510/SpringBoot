package org.activiti.cfg;

import java.util.Map;

import org.activiti.engine.*;
import org.activiti.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.activiti.spring.ProcessEngineFactoryBean;
import org.activiti.spring.SpringProcessEngineConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;
import javax.annotation.Resource;
import javax.sql.DataSource;

/**
 * activiti工作流配置
 */
@Configuration
public class ActivitiConfig {

	@Resource
	DataSource dataSource;// 注入配置好的数据源

	@Resource
	PlatformTransactionManager transactionManager;// 注入配置好的事物管理器

	// 流程配置，与spring整合采用SpringProcessEngineConfiguration这个实现
	@Bean
	public ProcessEngineConfiguration processEngineConfiguration() {
		SpringProcessEngineConfiguration processEngineConfiguration = new SpringProcessEngineConfiguration();
		processEngineConfiguration.setDataSource(dataSource);
		processEngineConfiguration
				.setTransactionManager(transactionManager);

		// 流程图字体
		processEngineConfiguration.setActivityFontName("宋体");
		processEngineConfiguration.setAnnotationFontName("宋体");
		processEngineConfiguration.setLabelFontName("宋体");

		return processEngineConfiguration;
	}

	// 流程引擎，与spring整合使用factoryBean
	@Bean
	public ProcessEngineFactoryBean processEngine(
			ProcessEngineConfiguration processEngineConfiguration) {
		ProcessEngineFactoryBean processEngineFactoryBean = new ProcessEngineFactoryBean();
		processEngineFactoryBean
				.setProcessEngineConfiguration((ProcessEngineConfigurationImpl) processEngineConfiguration);
		return processEngineFactoryBean;
	}

	// 八大接口
	@Bean
	public RepositoryService repositoryService(ProcessEngine processEngine) {
		return processEngine.getRepositoryService();
	}

	@Bean
	public RuntimeService runtimeService(ProcessEngine processEngine) {
		return processEngine.getRuntimeService();
	}

	@Bean
	public TaskService taskService(ProcessEngine processEngine) {
		return processEngine.getTaskService();
	}

	@Bean
	public HistoryService historyService(ProcessEngine processEngine) {
		return processEngine.getHistoryService();
	}

	@Bean
	public FormService formService(ProcessEngine processEngine) {
		return processEngine.getFormService();
	}

	@Bean
	public IdentityService identityService(ProcessEngine processEngine) {
		return processEngine.getIdentityService();
	}

	@Bean
	public ManagementService managementService(ProcessEngine processEngine) {
		return processEngine.getManagementService();
	}

	@Bean
	public DynamicBpmnService dynamicBpmnService(ProcessEngine processEngine) {
		return processEngine.getDynamicBpmnService();
	}

	// 八大接口 end
}
