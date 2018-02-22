package com.gm.activiti.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.activiti.bpmn.converter.BpmnXMLConverter;
import org.activiti.bpmn.model.BpmnModel;
import org.activiti.editor.language.json.converter.BpmnJsonConverter;
import org.activiti.engine.HistoryService;
import org.activiti.engine.ManagementService;
import org.activiti.engine.ProcessEngine;
import org.activiti.engine.ProcessEngineConfiguration;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.delegate.Expression;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.impl.RepositoryServiceImpl;
import org.activiti.engine.impl.bpmn.behavior.UserTaskActivityBehavior;
import org.activiti.engine.impl.cmd.GetDeploymentProcessDiagramCmd;
import org.activiti.engine.impl.interceptor.Command;
import org.activiti.engine.impl.javax.el.ExpressionFactory;
import org.activiti.engine.impl.javax.el.ValueExpression;
import org.activiti.engine.impl.juel.ExpressionFactoryImpl;
import org.activiti.engine.impl.juel.SimpleContext;
import org.activiti.engine.impl.persistence.entity.ExecutionEntity;
import org.activiti.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.activiti.engine.impl.pvm.PvmActivity;
import org.activiti.engine.impl.pvm.PvmTransition;
import org.activiti.engine.impl.pvm.process.ActivityImpl;
import org.activiti.engine.impl.task.TaskDefinition;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.Model;
import org.activiti.engine.repository.ProcessDefinition;
import org.activiti.engine.runtime.Execution;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.activiti.editor.constants.ModelDataJsonConstants;
import org.activiti.image.impl.DefaultProcessDiagramGenerator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gm.activiti.service.ProcessCustomService;

@Controller
@RequestMapping("/model")
public class ModuleController {

	private Logger logger = LoggerFactory.getLogger(ModuleController.class);

	@Autowired
	private RepositoryService repositoryService;
	@Autowired
	private RuntimeService runtimeService;
	@Autowired
	private TaskService taskService;
	@Autowired
	private ManagementService managementService;
	@Resource
	ProcessEngine engine;
	@Autowired
	private HistoryService historyService;
	@Autowired
	private ProcessCustomService processCustomService;

	/**
	 * 创建流程模板
	 * 
	 * @param name
	 * @param key
	 * @param description
	 * @param request
	 * @param response
	 */
	@RequestMapping(value = "create")
	public void create(@RequestParam("name") String name,
			@RequestParam("key") String key,
			@RequestParam("description") String description,
			HttpServletRequest request, HttpServletResponse response) {
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			ObjectNode editorNode = objectMapper.createObjectNode();
			editorNode.put("id", "canvas");
			editorNode.put("resourceId", "canvas");
			ObjectNode stencilSetNode = objectMapper.createObjectNode();
			stencilSetNode.put("namespace",
					"http://b3mn.org/stencilset/bpmn2.0#");
			editorNode.put("stencilset", stencilSetNode);
			Model modelData = repositoryService.newModel();

			ObjectNode modelObjectNode = objectMapper.createObjectNode();
			modelObjectNode.put(ModelDataJsonConstants.MODEL_NAME, name);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_REVISION, 1);
			description = StringUtils.defaultString(description);
			modelObjectNode.put(ModelDataJsonConstants.MODEL_DESCRIPTION,
					description);
			modelData.setMetaInfo(modelObjectNode.toString());
			modelData.setName(name);
			modelData.setKey(StringUtils.defaultString(key));

			repositoryService.saveModel(modelData);
			repositoryService.addModelEditorSource(modelData.getId(),
					editorNode.toString().getBytes("utf-8"));

			response.sendRedirect(request.getContextPath()
					+ "/modeler.html?modelId=" + modelData.getId());
		} catch (Exception e) {
			logger.error("创建模板失败：", e);
		}
	}

	/**
	 * 流程模板列表
	 * 
	 * @return
	 */
	@RequestMapping(value = "list")
	public ModelAndView modelList() {
		ModelAndView mav = new ModelAndView();
		List<Model> list = repositoryService.createModelQuery().list();
		mav.setViewName("process/model_list");
		mav.addObject("list", list);
		return mav;
	}

	/**
	 * 根据流程模板ID进行流程发布
	 * 
	 * @param modelId
	 * @param redirectAttributes
	 * @return
	 */
	@RequestMapping(value = "deploy/{modelId}")
	public String deploy(@PathVariable("modelId") String modelId,
			RedirectAttributes redirectAttributes) {
		try {
			Model modelData = repositoryService.getModel(modelId);
			ObjectNode modelNode = (ObjectNode) new ObjectMapper()
					.readTree(repositoryService.getModelEditorSource(modelData
							.getId()));
			byte[] bpmnBytes = null;

			BpmnModel model = new BpmnJsonConverter()
					.convertToBpmnModel(modelNode);
			bpmnBytes = new BpmnXMLConverter().convertToXML(model);

			String processName = modelData.getName() + ".bpmn20.xml";
			Deployment deployment = repositoryService.createDeployment()
					.name(modelData.getName())
					.addString(processName, new String(bpmnBytes, "utf-8"))
					.deploy();
			redirectAttributes.addFlashAttribute("message", "部署成功，部署ID="
					+ deployment.getId());
		} catch (Exception e) {
			logger.error("根据模板部署流程失败：modelId={}", modelId, e);
		}
		return "redirect:/model/deployed";
	}

	/**
	 * 删除流程部署
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "delete/deploy/{id}")
	public String deployDelete(@PathVariable("id") String id) {
		repositoryService.deleteDeployment(id, true);
		return "redirect:/model/deployed";
	}

	/**
	 * 已部署流程列表
	 * 
	 * @param mav
	 * @return
	 */
	@RequestMapping("deployed")
	public ModelAndView deployed(ModelAndView mav) {
		RepositoryService service = engine.getRepositoryService();
		List<ProcessDefinition> list = service.createProcessDefinitionQuery()
				.list();
		mav.addObject("list", list);
		mav.setViewName("process/deployed_list");

		return mav;
	}

	/**
	 * 导出流程模板的xml文件
	 * 
	 * @param modelId
	 * @param response
	 */
	@RequestMapping(value = "export/{modelId}")
	public void export(@PathVariable("modelId") String modelId,
			HttpServletResponse response) {
		try {
			Model modelData = repositoryService.getModel(modelId);
			BpmnJsonConverter jsonConverter = new BpmnJsonConverter();
			JsonNode editorNode = new ObjectMapper().readTree(repositoryService
					.getModelEditorSource(modelData.getId()));
			BpmnModel bpmnModel = jsonConverter.convertToBpmnModel(editorNode);
			BpmnXMLConverter xmlConverter = new BpmnXMLConverter();
			byte[] bpmnBytes = xmlConverter.convertToXML(bpmnModel);

			ByteArrayInputStream in = new ByteArrayInputStream(bpmnBytes);
			IOUtils.copy(in, response.getOutputStream());
			String filename = bpmnModel.getMainProcess().getId()
					+ ".bpmn20.xml";
			response.setHeader("Content-Disposition", "attachment; filename="
					+ filename);
			response.flushBuffer();
		} catch (Exception e) {
			logger.error("导出model的xml文件失败：modelId={}", modelId, e);
		}
	}

	/**
	 * 删除已有流程模板
	 * 
	 * @param modelId
	 * @return
	 */
	@RequestMapping(value = "delete/{modelId}")
	public String delete(@PathVariable("modelId") String modelId) {
		repositoryService.deleteModel(modelId);
		return "redirect:/model/list";
	}

	/**
	 * 启动一个流程实例
	 * 
	 * @param id
	 * @param mav
	 * @return
	 */
	@RequestMapping("start")
	public String start(String id) {
		RuntimeService service = engine.getRuntimeService();
		service.startProcessInstanceById(id);
		return "redirect:/model/started";
	}

	/**
	 * 删除流程实例
	 * 
	 * @param id
	 * @return
	 */
	@RequestMapping(value = "delete/instance/{id}")
	public String instanceDelete(@PathVariable("id") String id) {
		runtimeService.deleteProcessInstance(id, "删除原因");
		return "redirect:/model/started";
	}

	/**
	 * 所有已启动流程实例
	 */
	@RequestMapping("started")
	public ModelAndView started(ModelAndView mav) {
		RuntimeService service = engine.getRuntimeService();
		List<ProcessInstance> list = service.createProcessInstanceQuery()
				.list();
		mav.addObject("list", list);
		mav.setViewName("process/started_list");
		return mav;
	}

	@RequestMapping("task")
	public ModelAndView task(ModelAndView mav) {
		TaskService service = engine.getTaskService();
		List<Task> list = service.createTaskQuery().list();
		mav.addObject("list", list);
		mav.setViewName("process/task_list");
		return mav;
	}

	@RequestMapping("complete")
	public String complete(ModelAndView mav, String id) {
		TaskService service = engine.getTaskService();
		Map map = service.getVariables(id);
		/*
		 * System.out.println(map); taskService.setVariable(id, "day", 2);
		 */
		service.complete(id);
		return "redirect:/model/task";
	}

	/**
	 * 所有已启动流程实例
	 * 
	 * @throws IOException
	 */
	@RequestMapping("graphics")
	public void graphics(String definitionId, String instanceId, String taskId,
			ModelAndView mav, HttpServletResponse response) throws IOException {

		response.setContentType("image/png");
		Command<InputStream> cmd = null;

		if (definitionId != null) {
			cmd = new GetDeploymentProcessDiagramCmd(definitionId);
		}

		if (instanceId != null) {
			cmd = new ProcessInstanceDiagramCmd(instanceId);
		}

		if (taskId != null) {
			Task task = engine.getTaskService().createTaskQuery()
					.taskId(taskId).singleResult();
			cmd = new ProcessInstanceDiagramCmd(task.getProcessInstanceId());
			try {
				processTracking(task.getProcessDefinitionId(),
						task.getExecutionId(), response.getOutputStream());
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		if (cmd != null && taskId == null) {
			InputStream is = engine.getManagementService().executeCommand(cmd);
			int len = 0;
			byte[] b = new byte[1024];
			while ((len = is.read(b, 0, 1024)) != -1) {
				response.getOutputStream().write(b, 0, len);
			}
		}
	}

	/**
	 * 流程是否已经结束
	 * 
	 * @param processInstanceId
	 *            流程实例ID
	 * @return
	 */
	public boolean isFinished(String processInstanceId) {
		return historyService.createHistoricProcessInstanceQuery().finished()
				.processInstanceId(processInstanceId).count() > 0;
	}

	/**
	 * 流程跟踪图片
	 * 
	 * @param processDefinitionId
	 *            流程定义ID
	 * @param executionId
	 *            流程运行ID
	 * @param out
	 *            输出流
	 * @throws Exception
	 */
	public void processTracking(String processDefinitionId, String executionId,
			OutputStream out) throws Exception {
		// 当前活动节点、活动线
		List<String> activeActivityIds = new ArrayList<String>(), highLightedFlows = new ArrayList<String>();

		/**
		 * 获得当前活动的节点
		 */
		if (this.isFinished(executionId)) {// 如果流程已经结束，则得到结束节点
			activeActivityIds.add(historyService
					.createHistoricActivityInstanceQuery()
					.executionId(executionId).activityType("endEvent")
					.singleResult().getActivityId());
		} else {// 如果流程没有结束，则取当前活动节点
			// 根据流程实例ID获得当前处于活动状态的ActivityId合集
			activeActivityIds = runtimeService
					.getActiveActivityIds(executionId);
		}
		/**
		 * 获得当前活动的节点-结束
		 */

		/**
		 * 获得活动的线
		 */
		// 获得历史活动记录实体（通过启动时间正序排序，不然有的线可以绘制不出来）
		List<HistoricActivityInstance> historicActivityInstances = historyService
				.createHistoricActivityInstanceQuery().executionId(executionId)
				.orderByHistoricActivityInstanceStartTime().asc().list();
		// 计算活动线
		highLightedFlows = this
				.getHighLightedFlows(
						(ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
								.getDeployedProcessDefinition(processDefinitionId),
						historicActivityInstances);
		/**
		 * 获得活动的线-结束
		 */

		/**
		 * 绘制图形
		 */
		if (null != activeActivityIds) {
			InputStream imageStream = null;
			try {
				// 获得流程引擎配置
				ProcessEngineConfiguration processEngineConfiguration = engine
						.getProcessEngineConfiguration();
				// 根据流程定义ID获得BpmnModel
				BpmnModel bpmnModel = repositoryService
						.getBpmnModel(processDefinitionId);
				// 输出资源内容到相应对象
				imageStream = new DefaultProcessDiagramGenerator()
						.generateDiagram(bpmnModel, "png", activeActivityIds,
								highLightedFlows, processEngineConfiguration
										.getActivityFontName(),
								processEngineConfiguration.getLabelFontName(),
								executionId, processEngineConfiguration
										.getClassLoader(), 1.0);
				IOUtils.copy(imageStream, out);
			} finally {
				IOUtils.closeQuietly(imageStream);
			}
		}
	}

	/**
	 * 获得高亮线
	 * 
	 * @param processDefinitionEntity
	 *            流程定义实体
	 * @param historicActivityInstances
	 *            历史活动实体
	 * @return 线ID集合
	 */
	public List<String> getHighLightedFlows(
			ProcessDefinitionEntity processDefinitionEntity,
			List<HistoricActivityInstance> historicActivityInstances) {

		List<String> highFlows = new ArrayList<String>();// 用以保存高亮的线flowId
		for (int i = 0; i < historicActivityInstances.size(); i++) {// 对历史流程节点进行遍历
			ActivityImpl activityImpl = processDefinitionEntity
					.findActivity(historicActivityInstances.get(i)
							.getActivityId());// 得 到节点定义的详细信息
			List<ActivityImpl> sameStartTimeNodes = new ArrayList<ActivityImpl>();// 用以保存后需开始时间相同的节点
			if ((i + 1) >= historicActivityInstances.size()) {
				break;
			}
			ActivityImpl sameActivityImpl1 = processDefinitionEntity
					.findActivity(historicActivityInstances.get(i + 1)
							.getActivityId());// 将后面第一个节点放在时间相同节点的集合里
			sameStartTimeNodes.add(sameActivityImpl1);
			for (int j = i + 1; j < historicActivityInstances.size() - 1; j++) {
				HistoricActivityInstance activityImpl1 = historicActivityInstances
						.get(j);// 后续第一个节点
				HistoricActivityInstance activityImpl2 = historicActivityInstances
						.get(j + 1);// 后续第二个节点
				if (activityImpl1.getStartTime().equals(
						activityImpl2.getStartTime())) {// 如果第一个节点和第二个节点开始时间相同保存
					ActivityImpl sameActivityImpl2 = processDefinitionEntity
							.findActivity(activityImpl2.getActivityId());
					sameStartTimeNodes.add(sameActivityImpl2);
				} else {// 有不相同跳出循环
					break;
				}
			}
			List<PvmTransition> pvmTransitions = activityImpl
					.getOutgoingTransitions();// 取出节点的所有出去的线
			for (PvmTransition pvmTransition : pvmTransitions) {// 对所有的线进行遍历
				ActivityImpl pvmActivityImpl = (ActivityImpl) pvmTransition
						.getDestination();// 如果取出的线的目标节点存在时间相同的节点里，保存该线的id，进行高亮显示
				if (sameStartTimeNodes.contains(pvmActivityImpl)) {
					highFlows.add(pvmTransition.getId());
				}
			}
		}
		return highFlows;
	}

	public String getNextNode(String procInstanceId) {
		// 1、首先是根据流程ID获取当前任务：
		List<Task> tasks = taskService.createTaskQuery()
				.processInstanceId(procInstanceId).list();
		String nextId = "";
		for (Task task : tasks) {
			// 2、然后根据当前任务获取当前流程的流程定义，然后根据流程定义获得所有的节点：
			ProcessDefinitionEntity def = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
					.getDeployedProcessDefinition(task.getProcessDefinitionId());
			List<ActivityImpl> activitiList = def.getActivities(); // rs是指RepositoryService的实例
			// 3、根据任务获取当前流程执行ID，执行实例以及当前流程节点的ID：
			String excId = task.getExecutionId();

			ExecutionEntity execution = (ExecutionEntity) runtimeService
					.createExecutionQuery().executionId(excId).singleResult();
			String activitiId = execution.getActivityId();
			System.out.println("===========" + activitiId);
			// 4、然后循环activitiList
			// 并判断出当前流程所处节点，然后得到当前节点实例，根据节点实例获取所有从当前节点出发的路径，然后根据路径获得下一个节点实例：
			for (ActivityImpl activityImpl : activitiList) {
				String id = activityImpl.getId();
				if (activitiId.equals(id)) {
					System.out.println("当前任务："
							+ activityImpl.getProperty("name")); // 输出某个节点的某种属性
					List<PvmTransition> outTransitions = activityImpl
							.getOutgoingTransitions();// 获取从某个节点出来的所有线路
					for (PvmTransition tr : outTransitions) {
						PvmActivity ac = tr.getDestination(); // 获取线路的终点节点
						System.out.println("下一步任务任务：" + ac.getProperty("name"));
						nextId = ac.getId();
					}
					break;
				}
			}
		}
		return nextId;
	}

	@RequestMapping("nexttask")
	public TaskDefinition nextTask(String taskId) {
		// 获取流程实例Id信息
		String processInstanceId = taskService.createTaskQuery().taskId(taskId)
				.singleResult().getProcessInstanceId();
		try {
			/*
			 * List<String> list = queryActivityNode(processInstanceId); for
			 * (String name : list) { System.out.println(name); }
			 */
			getNextNode(processInstanceId);
			 processCustomService.turnTransition(taskId,"usertask5",null);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private List<TaskDefinition> nextTaskDefinition(ActivityImpl activityImpl,
			String activityId, String elString) {
		List<TaskDefinition> taskDefinitionList = new ArrayList<TaskDefinition>();// 所有的任务实例
		List<TaskDefinition> nextTaskDefinition = new ArrayList<TaskDefinition>();// 逐个获取的任务实例
		TaskDefinition taskDefinition = null;
		if ("userTask".equals(activityImpl.getProperty("type"))
				&& !activityId.equals(activityImpl.getId())) {
			taskDefinition = ((UserTaskActivityBehavior) activityImpl
					.getActivityBehavior()).getTaskDefinition();
			taskDefinitionList.add(taskDefinition);
		} else {
			List<PvmTransition> outTransitions = activityImpl
					.getOutgoingTransitions();
			List<PvmTransition> outTransitionsTemp = null;
			for (PvmTransition tr : outTransitions) {
				PvmActivity ac = tr.getDestination(); // 获取线路的终点节点
				// 如果是互斥网关或者是并行网关
				if ("exclusiveGateway".equals(ac.getProperty("type"))
						|| "parallelGateway".equals(ac.getProperty("type"))) {
					outTransitionsTemp = ac.getOutgoingTransitions();
					if (outTransitionsTemp.size() == 1) {
						nextTaskDefinition = nextTaskDefinition(
								(ActivityImpl) outTransitionsTemp.get(0)
										.getDestination(), activityId, elString);
						taskDefinitionList.addAll(nextTaskDefinition);
					} else if (outTransitionsTemp.size() > 1) {
						for (PvmTransition tr1 : outTransitionsTemp) {
							Object s = tr1.getProperty("conditionText");
							if (elString.equals(StringUtils.trim(s.toString()))) {
								nextTaskDefinition = nextTaskDefinition(
										(ActivityImpl) tr1.getDestination(),
										activityId, elString);
								taskDefinitionList.addAll(nextTaskDefinition);
							}
						}
					}
				} else if ("userTask".equals(ac.getProperty("type"))) {
					taskDefinition = ((UserTaskActivityBehavior) ((ActivityImpl) ac)
							.getActivityBehavior()).getTaskDefinition();
					taskDefinitionList.add(taskDefinition);
				} else {
					logger.debug((String) ac.getProperty("type"));
				}
			}
		}
		return taskDefinitionList;
	}

	/**
	 * 下一个任务节点信息,
	 * 
	 * 如果下一个节点为用户任务则直接返回,
	 * 
	 * 如果下一个节点为排他网关, 获取排他网关Id信息, 根据排他网关Id信息和execution获取流程实例排他网关Id为key的变量值,
	 * 根据变量值分别执行排他网关后线路中的el表达式, 并找到el表达式通过的线路后的用户任务
	 * 
	 * @param ActivityImpl
	 *            activityImpl 流程节点信息
	 * @param String
	 *            activityId 当前流程节点Id信息
	 * @param String
	 *            elString 排他网关顺序流线段判断条件
	 * @param String
	 *            processInstanceId 流程实例Id信息
	 * @return
	 */
	private TaskDefinition nextTaskDefinition(ActivityImpl activityImpl,
			String activityId, String elString, String processInstanceId) {

		PvmActivity ac = null;
		Object s = null;

		// 如果遍历节点为用户任务并且节点不是当前节点信息
		if ("userTask".equals(activityImpl.getProperty("type"))
				&& !activityId.equals(activityImpl.getId())) {
			// 获取该节点下一个节点信息
			TaskDefinition taskDefinition = ((UserTaskActivityBehavior) activityImpl
					.getActivityBehavior()).getTaskDefinition();
			return taskDefinition;
		} else if ("exclusiveGateway".equals(activityImpl.getProperty("type"))) {// 当前节点为exclusiveGateway
			List<PvmTransition> outTransitions = activityImpl
					.getOutgoingTransitions();
			// 如果网关路线判断条件为空信息
			if (StringUtils.isEmpty(elString)) {
				// 获取流程启动时设置的网关判断条件信息
				elString = getGatewayCondition(activityImpl.getId(),
						processInstanceId);
			}
			// 如果排他网关只有一条线路信息
			if (outTransitions.size() == 1) {
				return nextTaskDefinition((ActivityImpl) outTransitions.get(0)
						.getDestination(), activityId, elString,
						processInstanceId);
			} else if (outTransitions.size() > 1) { // 如果排他网关有多条线路信息
				for (PvmTransition tr1 : outTransitions) {
					s = tr1.getProperty("conditionText"); // 获取排他网关线路判断条件信息
					// 判断el表达式是否成立
					if (isCondition(activityImpl.getId(),
							StringUtils.trim(s.toString()), elString)) {
						return nextTaskDefinition(
								(ActivityImpl) tr1.getDestination(),
								activityId, elString, processInstanceId);
					}
				}
			}
		} else if ("parallelGateway".equals(activityImpl.getProperty("type"))) {
			List<PvmTransition> outTransitions = activityImpl
					.getOutgoingTransitions();
			if (outTransitions.size() > 1) { // 如果排他网关有多条线路信息
				for (PvmTransition tr1 : outTransitions) {
					PvmActivity pa = tr1.getDestination();
					if ("inclusiveGateway".equals(pa.getProperty("type"))) {
						return nextTaskDefinition((ActivityImpl) pa
								.getOutgoingTransitions().get(0)
								.getDestination(), activityId, elString,
								processInstanceId);
					}
				}
			}
		} else if ("inclusiveGateway".equals(activityImpl.getProperty("type"))) {
			List<PvmTransition> outTransitions = activityImpl
					.getOutgoingTransitions();
			if (outTransitions.size() > 1) { // 如果排他网关有多条线路信息
				for (PvmTransition tr1 : outTransitions) {
					PvmActivity pa = tr1.getDestination();
					if ("inclusiveGateway".equals(pa.getProperty("type"))) {
						return nextTaskDefinition((ActivityImpl) pa
								.getOutgoingTransitions().get(0)
								.getDestination(), activityId, elString,
								processInstanceId);
					}
				}
			}
		} else {
			// 获取节点所有流向线路信息
			List<PvmTransition> outTransitions = activityImpl
					.getOutgoingTransitions();
			List<PvmTransition> outTransitionsTemp = null;
			for (PvmTransition tr : outTransitions) {
				ac = tr.getDestination(); // 获取线路的终点节点
				// 如果流向线路为排他网关
				if ("exclusiveGateway".equals(ac.getProperty("type"))) {
					outTransitionsTemp = ac.getOutgoingTransitions();
					// 如果网关路线判断条件为空信息
					if (StringUtils.isEmpty(elString)) {
						// 获取流程启动时设置的网关判断条件信息
						elString = getGatewayCondition(ac.getId(),
								processInstanceId);
					}
					// 如果排他网关只有一条线路信息
					if (outTransitionsTemp.size() == 1) {
						return nextTaskDefinition(
								(ActivityImpl) outTransitionsTemp.get(0)
										.getDestination(), activityId,
								elString, processInstanceId);
					} else if (outTransitionsTemp.size() > 1) { // 如果排他网关有多条线路信息
						for (PvmTransition tr1 : outTransitionsTemp) {
							s = tr1.getProperty("conditionText"); // 获取排他网关线路判断条件信息
							// 判断el表达式是否成立
							if (isCondition(ac.getId(),
									StringUtils.trim(s.toString()), elString)) {
								return nextTaskDefinition(
										(ActivityImpl) tr1.getDestination(),
										activityId, elString, processInstanceId);
							}
						}
					}
				} else if ("userTask".equals(ac.getProperty("type"))) {
					return ((UserTaskActivityBehavior) ((ActivityImpl) ac)
							.getActivityBehavior()).getTaskDefinition();
				} else if ("inclusiveGateway".equals(ac.getProperty("type"))) {
					return nextTaskDefinition((ActivityImpl) ac
							.getOutgoingTransitions().get(0).getDestination(),
							activityId, elString, processInstanceId);
				} else if ("endEvent".equals(ac.getProperty("type"))) {
					return null;
				} else if ("parallelGateway".equals(ac.getProperty("type"))) {
					outTransitionsTemp = ac.getOutgoingTransitions();
					if (outTransitionsTemp.size() == 1) {
						ActivityImpl activiti = (ActivityImpl) outTransitionsTemp
								.get(0).getDestination();
						return nextTaskDefinition(activiti, activityId,
								elString, processInstanceId);
					}
				}
			}
			return null;
		}
		return null;
	}

	/**
	 * 查询流程启动时设置排他网关判断条件信息
	 * 
	 * @param String
	 *            gatewayId 排他网关Id信息, 流程启动时设置网关路线判断条件key为网关Id信息
	 * @param String
	 *            processInstanceId 流程实例Id信息
	 * @return
	 */
	public String getGatewayCondition(String gatewayId, String processInstanceId) {
		Execution execution = runtimeService.createExecutionQuery()
				.processInstanceId(processInstanceId).singleResult();
		if (execution == null) {
			return null;
		}
		Object object = runtimeService
				.getVariable(execution.getId(), gatewayId);
		return object == null ? "" : object.toString();
	}

	/**
	 * 根据key和value判断el表达式是否通过信息
	 * 
	 * @param String
	 *            key el表达式key信息
	 * @param String
	 *            el el表达式信息
	 * @param String
	 *            value el表达式传入值信息
	 * @return
	 */
	public boolean isCondition(String key, String el, String value) {
		ExpressionFactory factory = new ExpressionFactoryImpl();
		SimpleContext context = new SimpleContext();
		context.setVariable(key,
				factory.createValueExpression(value, String.class));
		ValueExpression e = factory.createValueExpression(context, el,
				boolean.class);
		return (Boolean) e.getValue(context);
	}

	/**
	 * 根据流程实例ID查询全部的usertask活动节点及其下级usertask节点
	 * 
	 * @param procInstanceId
	 * @return
	 */
	private List<String> queryActivityNode(String processInstanceId) {
		List list = new ArrayList();
		// 1、首先是根据流程ID获取当前任务：
		List<Task> tasks = taskService.createTaskQuery()
				.processInstanceId(processInstanceId).list();
		for (Task task : tasks) {
			// 2、然后根据当前任务获取当前流程的流程定义，然后根据流程定义获得所有的节点：
			ProcessDefinitionEntity def = (ProcessDefinitionEntity) ((RepositoryServiceImpl) repositoryService)
					.getDeployedProcessDefinition(task.getProcessDefinitionId());
			List<ActivityImpl> activitiList = def.getActivities();
			// 3、根据任务获取当前流程执行ID，执行实例以及当前流程节点的ID：
			String excId = task.getExecutionId();

			ExecutionEntity execution = (ExecutionEntity) runtimeService
					.createExecutionQuery().executionId(excId).singleResult();
			String activitiId = execution.getActivityId();

			// 4、然后循环activitiList
			// 并判断出当前流程所处节点，然后得到当前节点实例，根据节点实例获取所有从当前节点出发的路径，然后根据路径获得下一个节点实例：
			for (ActivityImpl activityImpl : activitiList) {
				String id = activityImpl.getId();
				if (activitiId.equals(id)) {
					String taskName = activityImpl.getProperty("name")
							.toString();
					list.add(taskName);
					TaskDefinition nextTask = nextTaskDefinition(activityImpl,
							activityImpl.getId(), null, processInstanceId);
					if (nextTask != null) {
						list.add(nextTask.getNameExpression().toString());
					}
				}
			}
		}

		return removeDuplicate(list);
	}

	/**
	 * List 去重
	 * 
	 * @param list
	 * @return
	 */
	public List removeDuplicate(List list) {
		HashSet h = new HashSet(list);
		list.clear();
		list.addAll(h);
		return list;
	}
}
