/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.server.services.jbpm;

import static org.kie.server.services.jbpm.ConvertUtils.buildQueryContext;
import static org.kie.server.services.jbpm.ConvertUtils.convertQueryDefinition;
import static org.kie.server.services.jbpm.ConvertUtils.convertToErrorInstanceList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstanceCustomVarsList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstanceList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToProcessInstanceWithVarsList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToQueryDefinitionList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTaskInstanceList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTaskInstanceListPO;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTaskInstanceWithVarsList;
import static org.kie.server.services.jbpm.ConvertUtils.convertToTaskSummaryList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpm.kie.services.impl.query.SqlQueryDefinition;
import org.jbpm.services.api.model.ProcessInstanceCustomDesc;
import org.jbpm.services.api.model.ProcessInstanceDesc;
import org.jbpm.services.api.model.ProcessInstanceWithVarsDesc;
import org.jbpm.services.api.model.UserTaskInstanceDesc;
import org.jbpm.services.api.model.UserTaskInstanceWithPotOwnerDesc;
import org.jbpm.services.api.model.UserTaskInstanceWithVarsDesc;
import org.jbpm.services.api.query.QueryAlreadyRegisteredException;
import org.jbpm.services.api.query.QueryMapperRegistry;
import org.jbpm.services.api.query.QueryNotFoundException;
import org.jbpm.services.api.query.QueryParamBuilderFactory;
import org.jbpm.services.api.query.QueryResultMapper;
import org.jbpm.services.api.query.QueryService;
import org.jbpm.services.api.query.model.QueryParam;
import org.kie.api.runtime.query.AdvancedQueryContext;
import org.kie.api.runtime.query.QueryContext;
import org.kie.api.task.model.TaskSummary;
import org.kie.internal.runtime.error.ExecutionError;
import org.kie.server.api.KieServerConstants;
import org.kie.server.api.model.definition.QueryDefinition;
import org.kie.server.api.model.definition.QueryDefinitionList;
import org.kie.server.api.model.definition.QueryFilterSpec;
import org.kie.server.services.api.KieServerRegistry;
import org.kie.server.services.impl.marshal.MarshallerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryDataServiceBase {

    public static final Logger logger = LoggerFactory.getLogger(QueryDataServiceBase.class);

    private QueryService queryService;
    private MarshallerHelper marshallerHelper;
    private KieServerRegistry context;

    public QueryDataServiceBase(QueryService queryService, KieServerRegistry context) {
        this.queryService = queryService;
        this.context = context;
        this.marshallerHelper = new MarshallerHelper(context);       
    }

    public QueryDefinition registerQuery(String queryName, String payload, String marshallingType) throws QueryAlreadyRegisteredException {
        logger.debug("About to unmarshal queryDefinition from payload: '{}'", payload);
        QueryDefinition queryDefinition = marshallerHelper.unmarshal(payload, marshallingType, QueryDefinition.class);
        queryDefinition.setName(queryName);

        SqlQueryDefinition actualDefinition = build(context, queryDefinition);
        logger.debug("Built sql query definition for {} with content {}", queryName, actualDefinition);
        queryService.registerQuery(actualDefinition);
        
        return convertQueryDefinition(actualDefinition);
    }

    public QueryDefinition replaceQuery(String queryName, String payload, String marshallingType) {

        logger.debug("About to unmarshal queryDefinition from payload: '{}'", payload);
        QueryDefinition queryDefinition = marshallerHelper.unmarshal(payload, marshallingType, QueryDefinition.class);
        queryDefinition.setName(queryName);

        SqlQueryDefinition actualDefinition = build(context, queryDefinition);
        logger.debug("Built sql query definition for {} with content {}", queryName, actualDefinition);

        queryService.replaceQuery(actualDefinition);
        
        return convertQueryDefinition(actualDefinition);
    }

    public void unregisterQuery(String uniqueQueryName) throws QueryNotFoundException {

        queryService.unregisterQuery(uniqueQueryName);
    }

    public QueryDefinition getQuery(String uniqueQueryName) throws QueryNotFoundException {
        org.jbpm.services.api.query.model.QueryDefinition query = queryService.getQuery(uniqueQueryName);

        return convertQueryDefinition(query);
    }

    public QueryDefinitionList getQueries(Integer page, Integer pageSize) throws QueryNotFoundException {
        List<org.jbpm.services.api.query.model.QueryDefinition> queries = queryService.getQueries(buildQueryContext(page, pageSize));

        return convertToQueryDefinitionList(queries);
    }

    public Object query(String queryName, String mapper, String orderBy, Integer page, Integer pageSize) {

        QueryResultMapper<?> resultMapper = QueryMapperRegistry.get().mapperFor(mapper, null);
        QueryContext queryContext = buildQueryContext(page, pageSize);

        if (orderBy != null && !orderBy.isEmpty()) {
            String[] orderBySortOrderItems = orderBy.split(",");

            if (orderBySortOrderItems.length > 1) {
                logger.debug("-- query() > orderBy clause = {} ", orderBy);
                queryContext = new AdvancedQueryContext(queryContext, orderBy);
            } else {
                logger.debug("-- query() > sortBy = {}", orderBy);
                queryContext.setOrderBy(orderBy);
                queryContext.setAscending(true);
            }
        }

        logger.debug("About to perform query '{}' with sorting criteria '{}' and page {} and page size {}", queryName, orderBy, page, pageSize);
        Object result = queryService.query(queryName, resultMapper, queryContext);
        logger.debug("Result returned from the query {} mapped with {}", result, resultMapper);

        return transform(result, resultMapper);
    }

    public Object queryFiltered(String queryName, String mapper, Integer page, Integer pageSize, String payload, String marshallingType) {
        return queryFiltered(null, queryName, mapper, page, pageSize, payload, marshallingType);
    }

    public Object queryFiltered(String containerId, String queryName, String mapper, Integer page, Integer pageSize, String payload, String marshallingType) {
        QueryParam[] params = new QueryParam[0];
        Map<String, String> columnMapping = null;

        QueryContext queryContext = buildQueryContext(page, pageSize);
        QueryFilterSpec filterSpec = new QueryFilterSpec();

        if (payload != null && !payload.isEmpty()) {
            logger.debug("About to unmarshal queryDefinition from payload: '{}'", payload);
            if(containerId != null) {
                filterSpec = marshallerHelper.unmarshal(containerId, payload, marshallingType, QueryFilterSpec.class);
            } else {
                filterSpec = marshallerHelper.unmarshal(payload, marshallingType, QueryFilterSpec.class);
            }

            // build parameters for filtering the query
            if (filterSpec.getParameters() != null) {
                params = new QueryParam[filterSpec.getParameters().length];
                int index = 0;
                for (org.kie.server.api.model.definition.QueryParam param : filterSpec.getParameters()) {
                    params[index] = new QueryParam(param.getColumn(), param.getOperator(), param.getValue());
                    index++;
                }
            }

            columnMapping = filterSpec.getColumnMapping();
        }

        QueryResultMapper<?> resultMapper = QueryMapperRegistry.get().mapperFor(mapper, columnMapping);

        if (filterSpec.getOrderByClause() != null) {
            queryContext = new AdvancedQueryContext(queryContext, filterSpec.getOrderByClause());
        } else {
            queryContext.setOrderBy(filterSpec.getOrderBy());
            queryContext.setAscending(filterSpec.isAscending());
        }

        logger.debug("About to perform query '{}' with page {} and page size {}", queryName, page, pageSize);
        Object result = queryService.query(queryName, resultMapper, queryContext, params);
        logger.debug("Result returned from the query {} mapped with {}", result, resultMapper);

        return transform(result, resultMapper);
    }

    public Object queryFilteredWithBuilder(String queryName, String mapper, String builder, Integer page, Integer pageSize, String payload, String marshallingType) {
        return queryFilteredWithBuilder(null, queryName, mapper, builder, page, pageSize, payload, marshallingType);
    }

    @SuppressWarnings("unchecked")
    public Object queryFilteredWithBuilder(String containerId, String queryName, String mapper, String builder, Integer page, Integer pageSize, String payload, String marshallingType) {
        Map<String, String> columnMapping = null;
        QueryContext queryContext = buildQueryContext(page, pageSize);
        Map<String, Object> queryParameters = new HashMap<String, Object>();
        String orderBy = null;
        Boolean ascending = null;
        String orderByClause = null;

        if (payload != null && !payload.isEmpty()) {
            logger.debug("About to unmarshal query params from payload: '{}'", payload);
            if(containerId != null) {
                queryParameters = marshallerHelper.unmarshal(containerId, payload, marshallingType, Map.class);
            } else {
                queryParameters = marshallerHelper.unmarshal(payload, marshallingType, Map.class);
            }
            orderBy = (String) queryParameters.remove(KieServerConstants.QUERY_ORDER_BY);
            ascending = (Boolean) queryParameters.remove(KieServerConstants.QUERY_ASCENDING);
            orderByClause = (String) queryParameters.remove(KieServerConstants.QUERY_ORDER_BY_CLAUSE);
            columnMapping = (Map<String, String>) queryParameters.remove(KieServerConstants.QUERY_COLUMN_MAPPING);
        }
        QueryResultMapper<?> resultMapper = QueryMapperRegistry.get().mapperFor(mapper, columnMapping);
        QueryParamBuilderFactory paramBuilderFactory = QueryParamBuilderManager.get().find(builder);

        if (paramBuilderFactory == null) {
            throw new RuntimeException("No query param builder found for " + builder);
        }

        if (orderByClause != null) {
            queryContext = new AdvancedQueryContext(queryContext, orderByClause);
        } else {
            if (orderBy != null) {
                queryContext.setOrderBy(orderBy);
            }
            if (ascending != null) {
                queryContext.setAscending(ascending);
            }
        }

        logger.debug("About to perform query '{}' with page {} and page size {}", queryName, page, pageSize);
        Object result = queryService.query(queryName, resultMapper, queryContext, paramBuilderFactory.newInstance(queryParameters));
        logger.debug("Result returned from the query {} mapped with {}", result, resultMapper);

        return transform(result, resultMapper);
    }

    /*
     * helper methods
     */

    @SuppressWarnings({"rawtypes", "unchecked"})
    protected Object transform(Object result, QueryResultMapper resultMapper) {
        Object actualResult = null;
        if (result instanceof Collection) {

            if (ProcessInstanceCustomDesc.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of ProcessInstanceCustomDesc to ProcessInstanceCustomList");
                actualResult = convertToProcessInstanceCustomVarsList((Collection<ProcessInstanceCustomDesc>) result);
            } else if (ProcessInstanceWithVarsDesc.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of ProcessInstanceWithVarsDesc to ProcessInstanceList");
                actualResult = convertToProcessInstanceWithVarsList((Collection<ProcessInstanceWithVarsDesc>) result);
            } else if (ProcessInstanceDesc.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of ProcessInstanceDesc to ProcessInstanceList");
                actualResult = convertToProcessInstanceList((Collection<ProcessInstanceDesc>) result);
            } else if (UserTaskInstanceWithVarsDesc.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of UserTaskInstanceWithVarsDesc to TaskInstanceList");
                actualResult = convertToTaskInstanceWithVarsList((Collection<UserTaskInstanceWithVarsDesc>) result);
            } else if (UserTaskInstanceWithPotOwnerDesc.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of UserTaskInstanceWithPotOwnerDesc to TaskInstanceList");
                actualResult = convertToTaskInstanceListPO((Collection<UserTaskInstanceWithPotOwnerDesc>) result);
            } else if (UserTaskInstanceDesc.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of UserTaskInstanceDesc to TaskInstanceList");
                actualResult = convertToTaskInstanceList((Collection<UserTaskInstanceDesc>) result);
            } else if (TaskSummary.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of TaskSummary to TaskSummaryList");
                actualResult = convertToTaskSummaryList((Collection<TaskSummary>) result);
            } else if (ExecutionError.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of ExecutionError to ErrorInstanceList");
                actualResult = convertToErrorInstanceList((List<ExecutionError>) result);
            } else if (List.class.isAssignableFrom(resultMapper.getType())) {

                logger.debug("Converting collection of List to ArrayList");
                actualResult = new ArrayList((Collection) result);
            }else {

                logger.debug("Convert not supported for custom type {}", resultMapper.getType());
                actualResult = result;
            }

            logger.debug("Actual result after converting is {}", actualResult);
        } else {
            logger.debug("Result is not a collection - {}, skipping any conversion", result);
            actualResult = result;
        }
        return actualResult;
    }

    protected static SqlQueryDefinition build(KieServerRegistry context, QueryDefinition queryDefinition) {
        SqlQueryDefinition actualDefinition = new SqlQueryDefinition(queryDefinition.getName(), queryDefinition.getSource());
        actualDefinition.setExpression(queryDefinition.getExpression());
        actualDefinition.setTarget(org.jbpm.services.api.query.model.QueryDefinition.Target.valueOf(queryDefinition.getTarget()));

        return actualDefinition;
    }
}