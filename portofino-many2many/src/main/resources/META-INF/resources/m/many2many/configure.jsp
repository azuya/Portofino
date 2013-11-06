<%@ page contentType="text/html;charset=UTF-8" language="java"
         pageEncoding="UTF-8"
%><%@ page import="org.apache.commons.lang.StringEscapeUtils"
%><%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"
%><%@ taglib prefix="stripes" uri="http://stripes.sourceforge.net/stripes-dynattr.tld"
%><%@ taglib prefix="mde" uri="/manydesigns-elements"
%><%@ taglib tagdir="/WEB-INF/tags" prefix="portofino"
%><%@ taglib prefix="fmt" uri="http://java.sun.com/jsp/jstl/fmt"
%><jsp:useBean id="actionBean" scope="request"
               type="com.manydesigns.portofino.pageactions.m2m.ManyToManyAction"
/><stripes:layout-render name="/theme/templates/${actionBean.pageInstance.layout.template}/modal.jsp">
    <stripes:layout-component name="pageTitle">
        <fmt:message key="com.manydesigns.portofino.pageactions.configure">
            <fmt:param value="<%= StringEscapeUtils.escapeHtml(actionBean.getPage().getTitle()) %>" />
        </fmt:message>
    </stripes:layout-component>
    <stripes:layout-component name="pageBody">
        <stripes:form action="${actionBean.context.actualServletPath}" method="post" enctype="multipart/form-data"
                      class="form-horizontal">
            <mde:write name="actionBean" property="pageConfigurationForm"/>
            <mde:write name="actionBean" property="configurationForm" />

            <br /><br />
            <p>
                <%
                    if(SecurityLogic.isAdministrator(request) &&
                       actionBean.getConfiguration().getActualRelationTable() != null) { %>
                    <a href="/${pageContext.request.contextPath}actions/admin/tables/${actionBean.configuration.actualRelationTable.databaseName}/${actionBean.configuration.actualRelationTable.schemaName}/${actionBean.configuration.actualRelationTable.tableName}?addSelectionProvider="
                       target="_blank">
                        <fmt:message key="com.manydesigns.portofino.pageactions.m2m.configuration.addSelectionProvider.linkText" />
                    </a>
                    <fmt:message key="com.manydesigns.portofino.pageactions.m2m.configuration.addSelectionProvider.explanation" />
                <% } %>
            </p>

            <jsp:include page="/m/pageactions/script-configuration.jsp" />
            <input type="hidden" name="returnUrl" value="<c:out value="${actionBean.returnUrl}"/>"/>
            <div class="form-actions">
                <portofino:buttons list="configuration" />
            </div>
        </stripes:form>
    </stripes:layout-component>
</stripes:layout-render>