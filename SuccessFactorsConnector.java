/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsConnector.java /main/4 2017/11/28 03:02:01 samrgupt Exp $ */

/* Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.*/

/*
 DESCRIPTION
 <short description of component this file declares/defines>

 PRIVATE CLASSES
 <list of private classes defined - with one-line descriptions>

 NOTES
 <other useful comments, qualifications, etc.>

 MODIFIED    (MM/DD/YY)
 samelgir    01/05/17 - Creation
 */

/**
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsConnector.java /main/4 2017/11/28 03:02:01 samrgupt Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */
package org.identityconnectors.successfactors;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.EmbeddedObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.identityconnectors.successfactors.utils.SuccessFactorsConstants;
import org.identityconnectors.successfactors.utils.SuccessFactorsUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@ConnectorClass(configurationClass = SuccessFactorsConfiguration.class, displayNameKey = SuccessFactorsConstants.SUCCESS_FACTORS_CONNECTOR_DISPLAY, messageCatalogPaths = SuccessFactorsConstants.MESSAGE_CATALOG_PATH)
public class SuccessFactorsConnector implements Connector, CreateOp, UpdateOp,
		SuccessFactorsConstants, SearchOp<String>, SchemaOp, TestOp, SyncOp,
		DeleteOp ,UpdateAttributeValuesOp{

	/**
	 * Logger reference
	 */
	private static Log log = Log.getLog(SuccessFactorsConnector.class);

	private SuccessFactorsConfiguration configuration;
	private SuccessFactorsConnection connection;
	private Schema _schema;

	@Override
	public void dispose() {
		String methodName = "dispose";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		;
		log.info("disposing connection");
		this.connection.disposeConnection();
		SuccessFactorsUtils.printMethodExit(methodName, log);
	}

	@Override
	public Configuration getConfiguration() {
		String methodName = "getConfiguration";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		log.ok("Returning cfg : " + this.configuration);
		SuccessFactorsUtils.printMethodExit(methodName, log);
		return this.configuration;
	}

	@Override
	public void init(Configuration config) {
		String methodName = "init";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		log.ok("Parameters recieved : ( _cfg : " + config + " )");

		this.configuration = ((SuccessFactorsConfiguration) config);
		this.configuration.validate();
		this.connection = new SuccessFactorsConnection(this.configuration);
		if (_schema == null)
			_schema = SuccessFactorsSchema.getSchema();

		SuccessFactorsUtils.printMethodExit(methodName, log);
	}

	@Override
	public FilterTranslator<String> createFilterTranslator(
			ObjectClass objClass, OperationOptions options) {
		return new SuccessFactorsFilterTranslator();
	}

	@Override
	public void executeQuery(ObjectClass objectClass, String query,
			ResultsHandler handler, OperationOptions options) {
		String methodName = "executeQuery";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		log.ok("Parameters recieved : ( objectClass : " + objectClass
				+ ", query : " + query + ", handler : " + handler
				+ ", options : " + options + " )");

		if (objectClass == null) {
			log.error("objectClass is null");
			throw new ConnectorException(configuration.getMessage(
					SuccessFactorsConstants.EX_UNSUPPORTED_OBJ_CLASS,
					objectClass));
		}

		String reconUrl = configuration.getReconUrl();
		String lookupUrl = configuration.getLookupUrl();
		String response = null;
		JSONArray results = null;
		boolean isNext = true;
		try {
			String oClass = objectClass.getObjectClassValue();
			log.info("Object Class :: {0} ", oClass);
			log.error("Perf: SearchOp Execution Started");
			// Lookup Reconciliation
			if (oClass.equalsIgnoreCase(SuccessFactorsConstants.ATTR_JOBLEVEL)) {
				response = SuccessFactorsUtils.executeRequest(
						this.connection.getConnection(), null, lookupUrl, null,
						configuration);
				results = SuccessFactorsUtils.convertResponseToJSONArray(
						response, configuration);

				int n = results.length();
				for (int i = 0; i < n; ++i) {
					JSONObject jsonObj = results.getJSONObject(i);
					ConnectorObject co = SuccessFactorsUtils
							.createConnectorObjectForLookup(
									this.connection.getConnection(),
									objectClass, jsonObj, configuration);
					if (co != null) {
						if (!handler.handle(co))
							return;
					}
				}
				log.error("Perf: SearchOp Execution Completed");
			} else {
				while (isNext) {
					String filterReconUrl = null;
					String filterSuffix = (String) options.getOptions().get(
							SuccessFactorsConstants.FILTER_SUFFIX);
					if (query != null) {
						Long latestToken = Long.parseLong(query);
						if (latestToken > 0) {
							reconUrl = (reconUrl + "%20and%20" + String.format(
									"lastModified ge datetime'%s'",
									SuccessFactorsUtils.convertDateFormat(
											latestToken.toString(),
											configuration))).replaceAll(" ",
									"%20");
						}
					}
					filterReconUrl = (reconUrl + "%20and%20" + filterSuffix)
							.replaceAll(" ", "%20");
					if (filterSuffix != null && !filterSuffix.isEmpty()) {
						response = SuccessFactorsUtils.executeRequest(
								this.connection.getConnection(), null,
								filterReconUrl, null, configuration);
					} else {
						response = SuccessFactorsUtils.executeRequest(
								this.connection.getConnection(), null,
								reconUrl, null, configuration);
					}
					if (response.contains(PARSE_NEXT)) {
						JSONObject obj;
						obj = new JSONObject(response);
						String dObj = obj.get(SuccessFactorsConstants.PARSE_D)
								.toString();
						JSONObject resObj = new JSONObject(dObj);
						String nextQuery = resObj.get(PARSE_NEXT).toString();
						isNext = true;
						reconUrl = nextQuery;
						log.info("nextQuery :: {0}", reconUrl);

					} else {
						isNext = false;
					}
					if (oClass
							.equalsIgnoreCase(SuccessFactorsConstants.ATTR_HR)
							|| oClass
									.equalsIgnoreCase(SuccessFactorsConstants.ATTR_SUPERVISOR)) {
						results = SuccessFactorsUtils
								.convertResponseToJSONArray(response,
										configuration);
						int n = results.length();
						for (int i = 0; i < n; ++i) {
							JSONObject jsonObj = results.getJSONObject(i);
							ConnectorObject co = SuccessFactorsUtils
									.createConnectorObjectForLookup(
											this.connection.getConnection(),
											objectClass, jsonObj, configuration);
							if (co != null) {
								if (!handler.handle(co))
									return;
							}
						}
						log.error("Perf: SearchOp Execution Completed");
					} else if (oClass
							.equalsIgnoreCase(ObjectClass.ACCOUNT_NAME)) {
						ConnectorObject co = null;
						results = SuccessFactorsUtils
								.convertResponseToJSONArray(response,
										configuration);
						String[] attrsToGet = options.getAttributesToGet();
						Set<String> attrsToGetSet = new HashSet<String>(
								Arrays.asList(attrsToGet));
						log.ok("attrsToGetSet : " + attrsToGetSet);

						int userList = results.length();

						log.info("Number of accounts {0}", userList);
						for (int i = 0; i < userList; i++) {
							JSONObject jsonObj = results.getJSONObject(i);
							String user = jsonObj
									.getString(SuccessFactorsConstants.ATTR_USERID);

							co = SuccessFactorsUtils
									.createConnectorObjectForUser(
											this.connection.getConnection(),
											user, jsonObj, attrsToGetSet,
											configuration);
							log.info("connector Object {0} for user {1}", co,
									user);
							if (co != null) {
								if (!handler.handle(co))
									return;
							}
						}
					}// for loop end
					log.error("Perf: SearchOp Execution Completed");
				}// __ACCOUNT__ end
			}// While loop end
		} catch (ConnectorException connectorException) {
			throw ConnectorException.wrap(connectorException);
		} catch (JSONException e) {
			throw ConnectorException.wrap(e);
		} finally {
			log.error("Perf: SearchOp Execution Completed");
			SuccessFactorsUtils.printMethodExit(methodName, log);
		}
	}

	@Override
	public Uid update(ObjectClass objectClass, Uid uid,
			Set<Attribute> attributes, OperationOptions options) {
		String methodName = "update";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		if (objectClass == null
				|| !ObjectClass.ACCOUNT_NAME.equals(objectClass
						.getObjectClassValue())) {
			log.error("Either objectClass is null or Unsupported object class : "
					+ objectClass.getObjectClassValue());
			throw new ConnectorException(
					configuration.getMessage(EX_UNSUPPORTED_OBJ_CLASS,
							objectClass.getObjectClassValue()));
		}
		log.error("Perf: Update Entered for user {0}", uid.getUidValue());
		LinkedHashMap<String, JSONObject> updateAttrsMap = SuccessFactorsUtils
				.seperateAttrsPerEntity(uid, attributes, configuration,
						SuccessFactorsConstants.UPDATE_OP);
		String returnId = null;
		try {
			Iterator<Entry<String, JSONObject>> entries = updateAttrsMap
					.entrySet().iterator();

			while (entries.hasNext()) {
				Entry<String, JSONObject> thisEntry = (Entry<String, JSONObject>) entries
						.next();
				JSONObject jsonObj = (JSONObject) thisEntry.getValue();
				String response = null;
				if (jsonObj.length() > 0) {
					response = SuccessFactorsUtils.executeRequest(
							this.connection.getConnection(), jsonObj,
							configuration.getUpsertUrl(), null, configuration);
					LinkedHashMap<String, Object> responseMap = SuccessFactorsUtils
							.parseResponse(response, configuration);
					if (responseMap.get(RESPONSE_STATUS).toString()
							.equals(RESPONSE_OK)) {
						returnId = uid.getUidValue();
						log.info("update Succeded for user {0}", returnId);
					}
				}
			}
			log.error("Perf: Update Exiting for user {0}", uid.getUidValue());
		} catch (ConnectorException connectorException) {
			throw ConnectorException.wrap(connectorException);
		} finally {
			log.error("Perf: Update Exiting for user {0}", uid.getUidValue());
			SuccessFactorsUtils.printMethodExit(methodName, log);
		}
		return new Uid(returnId);
	}

	@Override
	public Uid create(ObjectClass objectClass, Set<Attribute> attrs,
			OperationOptions options) {
		String methodName = "create";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		log.info("attributes to create are ", attrs);

		if (objectClass == null
				|| !ObjectClass.ACCOUNT_NAME.equals(objectClass
						.getObjectClassValue())) {
			log.error("Either objectClass is null or Unsupported object class : "
					+ objectClass.getObjectClassValue());
			throw new ConnectorException(
					configuration.getMessage(EX_UNSUPPORTED_OBJ_CLASS,
							objectClass.getObjectClassValue()));
		}
		Map<String, JSONObject> createAttrsMap = SuccessFactorsUtils
				.seperateAttrsPerEntity(null, attrs, configuration,
						SuccessFactorsConstants.CREATE_OP);

		String returnId = null;
		try {
			Iterator<Entry<String, JSONObject>> entries = createAttrsMap
					.entrySet().iterator();
			log.error("Perf: Create Entered");
			while (entries.hasNext()) {
				Entry<String, JSONObject> thisEntry = (Entry<String, JSONObject>) entries
						.next();
				Object key = thisEntry.getKey();
				JSONObject jsonObj = (JSONObject) thisEntry.getValue();
				String response = null;
				if (key.equals(USERENTITY)) {
					response = SuccessFactorsUtils.executeRequest(
							this.connection.getConnection(), jsonObj,
							configuration.getUserUrl(), key.toString(),
							configuration);
					Map<String, Object> responseUserEntity = SuccessFactorsUtils
							.parseResponse(response, configuration);
					returnId = responseUserEntity.get(USERNAME).toString();
				} else {
					response = SuccessFactorsUtils.executeRequest(
							this.connection.getConnection(), jsonObj,
							configuration.getUpsertUrl(), null, configuration);
				}
			}
		} catch (ConnectorException connectorException) {
			throw ConnectorException.wrap(connectorException);
		} finally {
			log.error("Perf: Create Exiting");
			SuccessFactorsUtils.printMethodExit(methodName, log);
		}
		log.ok("Returning uid : " + returnId);
		return new Uid(returnId);
	}

	@Override
	public Schema schema() {
		return _schema;
	}

	@Override
	public void test() {
		String methodName = "test";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		String url = configuration.getLookupUrl();
		CloseableHttpResponse response = null;
		HttpRequestBase httpRequest = null;
		
		
		StringBuilder requestUrl = null;
		String requestUrlStr = null;
		try {


			if (!url.startsWith("http")) {
				requestUrl = new StringBuilder(
						configuration.isSslEnabled() ? SuccessFactorsConstants.HTTPS
								: SuccessFactorsConstants.HTTP).append(configuration
						.getHost()).append(":").append(configuration.getPort());

				requestUrlStr = requestUrl.toString() + url;
			} else {
				requestUrlStr = url;
			}
			log.error("Perf: TestOp Execution Started");
			
			httpRequest = new HttpGet(requestUrlStr);
			response = this.connection.getConnection().execute((HttpUriRequest) httpRequest);
			log.error("Perf: Performing REST call to the target Completed");
			int statusCode = response.getStatusLine().getStatusCode();
			
			if (statusCode >= 200 && statusCode < 300) {
				log.info("test Success");
			}else{
				log.error("Connection test failed");
				throw new ConnectionFailedException(configuration.getMessage(
						SuccessFactorsConstants.EX_EXECUTE_REQUEST, "Connection test failed"));			
			}
			log.error("Perf: TestOp Execution Completed");
		} catch (ClientProtocolException e) {
			throw new ConnectionFailedException(configuration.getMessage(
					SuccessFactorsConstants.EX_EXECUTE_REQUEST, e.getMessage()));
		} catch (IOException e) {
			throw new ConnectionFailedException(configuration.getMessage(
					SuccessFactorsConstants.EX_EXECUTE_REQUEST, e.getMessage()));
		} finally {
			SuccessFactorsUtils.printMethodExit(methodName, log);
		}
	}

	@Override
	public void delete(ObjectClass oClass, Uid uid, OperationOptions options) {
		String methodName = "delete";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		log.ok("Parameters recieved : ( objectClass : " + oClass + ", uid : "
				+ uid + ", options : " + options + " )");

		if (oClass == null
				|| !ObjectClass.ACCOUNT_NAME.equals(oClass
						.getObjectClassValue())) {
			log.error("Either objectClass is null or Unsupported object class : "
					+ oClass.getObjectClassValue());
			throw new ConnectorException(configuration.getMessage(
					EX_UNSUPPORTED_OBJ_CLASS, oClass.getObjectClassValue()));
		}
		Set<Attribute> attributes = new HashSet<Attribute>();
		String returnId = null;
		String response = null;
		Attribute statusAttr = AttributeBuilder.build(
				OperationalAttributes.ENABLE_NAME, false);
		attributes.add(statusAttr);
		LinkedHashMap<String, JSONObject> statusMap = SuccessFactorsUtils
				.seperateAttrsPerEntity(uid, attributes, configuration,
						UPDATE_OP);
		response = SuccessFactorsUtils.executeRequest(
				this.connection.getConnection(), statusMap.get(USERENTITY),
				configuration.getUpsertUrl(), null, configuration);
		LinkedHashMap<String, Object> responseMap = SuccessFactorsUtils
				.parseResponse(response, configuration);
		if (responseMap.get(RESPONSE_STATUS).toString().equals(RESPONSE_OK)) {
			returnId = uid.getUidValue();
			log.info(
					"Delete funtionality not supported by the taget, hence disabling the user {0}",
					returnId);
		}
	}

	@Override
	public SyncToken getLatestSyncToken(ObjectClass objectClass) {
		String methodName = "getLatestSyncToken";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		String reconUrl = configuration.getReconUrl();
		Boolean isNext = true;
		JSONArray results = null;
		Long latestToken = 0L;
		while (isNext) {
			String response = SuccessFactorsUtils.executeRequest(
					this.connection.getConnection(), null, reconUrl, null,
					configuration);

			if (response.contains(PARSE_NEXT)) {
				JSONObject obj;
				try {
					obj = new JSONObject(response);
					String dObj = obj.get(SuccessFactorsConstants.PARSE_D)
							.toString();
					JSONObject resObj = new JSONObject(dObj);
					String nextQuery = resObj.get(PARSE_NEXT).toString();
					isNext = true;
					reconUrl = nextQuery;
					log.info("nextQuery :: {0}", reconUrl);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			} else {
				isNext = false;
			}
			// Parse response
			results = SuccessFactorsUtils.convertResponseToJSONArray(response,
					configuration);
			int userList = results.length();
			log.info("Number of accounts {0}", userList);
			for (int i = 0; i < userList; i++) {
				JSONObject jsonObj;
				try {
					jsonObj = results.getJSONObject(i);

					String attrValue = jsonObj
							.getString(SuccessFactorsConstants.LAST_MODIFIED_DATETIME);

					String lastModifiedDateTime = jsonObj.getString(
							SuccessFactorsConstants.LAST_MODIFIED_DATETIME)
							.substring(attrValue.indexOf("(") + 1,
									attrValue.indexOf("+"));

					Long lastUpdatedTime = Long.valueOf(lastModifiedDateTime);

					if (lastUpdatedTime > latestToken) {
						latestToken = lastUpdatedTime;
					}
				} catch (JSONException e) {
					log.error(
							"Error occured while reading data JSON Object {0}",
							e.getMessage());
					throw new ConnectorException(configuration.getMessage(
							SuccessFactorsConstants.EX_JSON_READ,
							e.getMessage()));
				} finally {
					log.error("Perf: sync Exiting");
					SuccessFactorsUtils.printMethodExit(methodName, log);
				}
			}
		}
		log.info("latestSync Token is {0}", latestToken);
		return new SyncToken(latestToken);
	}

	@Override
	public void sync(ObjectClass objectClass, SyncToken token,
			SyncResultsHandler handler, OperationOptions options) {
		String methodName = "sync";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		String reconUrl = configuration.getReconUrl();
		Long latestToken = Long.parseLong(token.getValue().toString());
		if (latestToken > 0) {
			reconUrl = (reconUrl + "%20and%20" + String.format(
					"lastModified ge datetime'%s'",
					SuccessFactorsUtils.convertDateFormat(
							latestToken.toString(), configuration)))
					.replaceAll(" ", "%20");
		}
		String response = SuccessFactorsUtils.executeRequest(
				this.connection.getConnection(), null, reconUrl, null,
				configuration);
		SyncDeltaBuilder sdBuilder = new SyncDeltaBuilder();

		ConnectorObject co = null;
		JSONArray results = SuccessFactorsUtils.convertResponseToJSONArray(
				response, configuration);
		String[] attrsToGet = options.getAttributesToGet();
		Set<String> attrsToGetSet = new HashSet<String>(
				Arrays.asList(attrsToGet));
		log.ok("attrsToGetSet : " + attrsToGetSet);
		try {
			int userList = results.length();
			log.info("Number of accounts {0}", userList);
			for (int i = 0; i < userList; i++) {
				JSONObject userObj;

				userObj = results.getJSONObject(i);
				String user = userObj
						.getString(SuccessFactorsConstants.ATTR_USERID);
				co = SuccessFactorsUtils.createConnectorObjectForUser(
						this.connection.getConnection(), user, userObj,
						attrsToGetSet, configuration);
				if (co != null) {
					co.getAttributeByName(
							SuccessFactorsConstants.LAST_MODIFIED_DATETIME)
							.getValue().get(0).toString();
					sdBuilder.setObject(co);
					sdBuilder.setUid(co.getUid());
					sdBuilder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
					SyncToken oSyncToken = new SyncToken(
							co.getAttributeByName(
									SuccessFactorsConstants.LAST_MODIFIED_DATETIME)
									.getValue().get(0).toString());
					sdBuilder.setToken(oSyncToken);
					if (!handler.handle(sdBuilder.build()))
						return;
				}
			}
		} catch (ConnectorException connectorException) {
			throw ConnectorException.wrap(connectorException);
		} catch (JSONException e) {
			throw ConnectorException.wrap(e);
		} finally {
			log.error("Perf: sync Exiting");
			SuccessFactorsUtils.printMethodExit(methodName, log);

		}
	}

	@Override
	public Uid addAttributeValues(ObjectClass objectClass, Uid uid ,Set<Attribute> childAttrs, OperationOptions options) {
		String methodName = "addAttributeValues";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		log.ok("Parameters recieved : ( objectClass : "+objectClass+", uid : "+uid+", attributes : "+childAttrs+", options : "+options+" )");

		if((objectClass == null) || (!objectClass.getObjectClassValue().equalsIgnoreCase(ObjectClass.ACCOUNT_NAME))){
			log.error("Either objectClass is null or Unsupported object class : "+objectClass.getObjectClassValue());
			throw new ConnectorException(configuration.getMessage(EX_UNSUPPORTED_OBJ_CLASS, objectClass.getObjectClassValue()));
		}
		
		log.error("Perf: AddAttributeValues Entered for user {0}", uid.getUidValue());
		String returnId = null;
	     Iterator<Attribute> childItr = childAttrs.iterator();
	     while(childItr.hasNext()){
		Attribute attrToAdd = childItr.next();
		Map<String, Map<String, Object>> objectMap = new HashMap<String, Map<String, Object>>();
		EmbeddedObject embeddedObject = AttributeUtil.getEmbeddedObjectValue(attrToAdd);
		Map<String, Object> customEntity = new HashMap<String, Object>();
		HashMap<String, JSONObject> jsonObjectMap = new HashMap<String, JSONObject>();
		String[] token = null;

		for(Attribute innerAttr : embeddedObject.getAttributes()){

			String attrName = innerAttr.getName();
			if (attrName.contains(DELIMITER)) {
				token = attrName.split(DELIMITER_SPLIT);
			}
			Object attributeValue = SuccessFactorsUtils.getAttributeValue(innerAttr, token[1], configuration);
			customEntity.put(token[1], attributeValue);
		}
		objectMap.put(token[0], customEntity);
		String sUserId = uid.getUidValue();
		objectMap = SuccessFactorsUtils.addMetadataToObjects(objectMap, sUserId, 0L, configuration, UPDATE_OP);
		
		if (objectMap.size() > 0) {
			for (Entry<String, Map<String, Object>> entry : objectMap.entrySet()) {
				String key = entry.getKey();
				jsonObjectMap.put(entry.getKey(),SuccessFactorsUtils.createJsonObject(objectMap.get(key), configuration));
			}
		}

		try {
			Iterator<Entry<String, JSONObject>> entries = jsonObjectMap.entrySet().iterator();

			while (entries.hasNext()) {
				Entry<String, JSONObject> thisEntry = (Entry<String, JSONObject>) entries
						.next();
				JSONObject jsonObj = (JSONObject) thisEntry.getValue();
				String response = null;
				if (jsonObj.length() > 0) {
					response = SuccessFactorsUtils.executeRequest(
							this.connection.getConnection(), jsonObj,
							configuration.getUpsertUrl(), null, configuration);
					LinkedHashMap<String, Object> responseMap = SuccessFactorsUtils
							.parseResponse(response, configuration);
					if (responseMap.get(RESPONSE_STATUS).toString().equals(RESPONSE_OK)) {
						returnId = uid.getUidValue();
						log.info("AddAttributeValues Succeded for user {0}", returnId);
					}
				}
			}
		} catch (ConnectorException connectorException) {
			throw ConnectorException.wrap(connectorException);
		} finally {
			log.error("Perf: AddAttributeValues Exiting for user {0}", uid.getUidValue());
			SuccessFactorsUtils.printMethodExit(methodName, log);
		}
	     } 
		return new Uid(returnId);
	}

	@Override
	public Uid removeAttributeValues(ObjectClass objectClass, Uid uid ,Set<Attribute> childAttrs, OperationOptions options) {
		throw new ConnectorException("Remove attribute values not supported by connector");   
	}
}
