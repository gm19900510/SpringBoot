<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt" %>
<%
    String path = request.getContextPath();
    String basePath = request.getScheme() + "://"
            + request.getServerName() + ":" + request.getServerPort()
            + path + "/";
%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Insert title here</title>
<style type="text/css">
table,table td,table th {
	border: 1px solid gray;
	border-collapse: collapse;
}

a {
	height: 30px;
	line-height: 30px;
	border: 1px solid black;
	background: gray;
	color: white;
	text-decoration: none;
	padding: 3px;
	font-weight: bold;
}
</style>
</head>
<body>

	<div style="margin: 0 auto; width: 1024px; padding-top: 50px;">
		<h2>流程模板</h2>
		<table width="1000px;">
			<thead>
				<tr>
					<th>流程模版ID</th>
					<th>流程模版KEY</th>
					<th>流程模版NAME</th>
					<th>流程模版元信息</th>
					<th>流程模版修订版本</th>
					<th>流程模版修订时间</th>
					<th>操作</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach var="temp" items="${list}">
					<tr>
						<td>${temp.id }</td>
						<td>${temp.key }</td>
						<td>${temp.name }</td>
						<td>${temp.metaInfo }</td>
						<td>${temp.revision }</td>
						<td><fmt:formatDate value="${temp.lastUpdateTime }" pattern="yyyy-MM-dd HH:mm:ss"/></td>
						<td>
						<a href="<%=path %>/model/deploy/${temp.id }">部署</a>
						<a href="<%=path %>/model/delete/${temp.id }">删除</a>
						<a href="<%=path %>/model/export/${temp.id }">导出</a>
						<a href="<%=path %>/modeler.html?modelId=${temp.id }">进入设计器</a>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table><br>
		<a href="deployed">已部署流程</a>
		<a href="started">已启动流程</a>
		<a href="task">任务列表</a>
	</div>
</body>
</html>