/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/utils/SuccessFactorsUtils.java /main/8 2018/09/30 22:20:46 samrgupt Exp $ */

/* Copyright (c) 2017, 2018, Oracle and/or its affiliates. 
All rights reserved.*/

/*
 DESCRIPTION
 <short description of component this file declares/defines>

 PRIVATE CLASSES
 <list of private classes defined - with one-line descriptions>

 NOTES
 <other useful comments, qualifications, etc.>

 MODIFIED    (MM/DD/YY)
 samelgir    01/05/17 - ..
 samelgir    01/05/17 - Creation
 */

/**
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/utils/SuccessFactorsUtils.java /main/8 2018/09/30 22:20:46 samrgupt Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */
package org.identityconnectors.successfactors.utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.EmbeddedObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.successfactors.SuccessFactorsConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public class SuccessFactorsUtils implements SuccessFactorsConstants {

	String payload = null;
	String targetResponse = null;
	Boolean isContinue = true;
	// START BUG 28403187 
	String personId;
	// END BUG 28403187 

	/**
	 * Logger reference
	 */
	private static Log log = Log.getLog(SuccessFactorsUtils.class);

	public static void printMethodEntered(String methodName, Log log) {
		log.info("Entered Method : {0}", methodName);
	}

	public static void printMethodExit(String methodName, Log log) {
		log.info("Exit Method : {0}", methodName);
	}

	public static LinkedHashMap<String, JSONObject> seperateAttrsPerEntity(
			Uid uid, Set<Attribute> attrs, SuccessFactorsConfiguration config,
			String operation) {
		String methodName = "seperateAttrsPerEntity";
		printMethodEntered(methodName, log);
		Map<String, Map<String, Object>> objectMap = new HashMap<String, Map<String, Object>>();
		LinkedHashMap<String, JSONObject> jsonObjectMap = new LinkedHashMap<String, JSONObject>();

		String sUserId = null;
		long startDate = 0L;
		Object attributeValue = null;
		if (operation.equalsIgnoreCase(CREATE_OP)) {
			sUserId = AttributeUtil.getNameFromAttributes(attrs).getNameValue();
			Attribute enableAttribute = AttributeUtil.find(
					OperationalAttributes.ENABLE_NAME, attrs);
			if (enableAttribute == null) {
				Map<String, Object> customEntity = new HashMap<String, Object>();
				customEntity.put(ATTR_STATUS, STATUS_ACTIVE);
				objectMap.put(USERENTITY, customEntity);
			}
		} else {
			sUserId = uid.getUidValue();
		}
		String[] token = null;

		for (Attribute attr : attrs) {
			String attrName = attr.getName();
			if (attrName.contains(DELIMITER)) {
				token = attrName.split(DELIMITER_SPLIT);
			}
			if (attrName.equals(Name.NAME) || attrName.equals(Uid.NAME)) {
				continue;
			} else if (attrName.equals(OperationalAttributes.PASSWORD_NAME)) {

				if (objectMap.size() > 0 && objectMap.containsKey(USERENTITY)) {
					attributeValue = decryptPassword(AttributeUtil
							.getPasswordValue(attrs));
					objectMap.get(token[0]).put(ATTR_PASSWORD,
							attributeValue.toString());
				} else {
					Map<String, Object> customEntity = new HashMap<String, Object>();
					attributeValue = decryptPassword(AttributeUtil
							.getPasswordValue(attrs));
					customEntity.put(ATTR_PASSWORD, attributeValue.toString());
					objectMap.put(USERENTITY, customEntity);
				}
			} else if (attrName.equals(OperationalAttributes.ENABLE_NAME)) {

				if (objectMap.size() > 0 && objectMap.containsKey(USERENTITY)) {
					if (AttributeUtil.getBooleanValue(attr)) {
						objectMap.get(USERENTITY).put(ATTR_STATUS,
								STATUS_ACTIVE);
					} else {
						objectMap.get(USERENTITY).put(ATTR_STATUS,
								STATUS_INACTIVE);
					}
				} else {
					Map<String, Object> customEntity = new HashMap<String, Object>();
					if (AttributeUtil.getBooleanValue(attr)) {
						customEntity.put(ATTR_STATUS, STATUS_ACTIVE);
					} else {
						customEntity.put(ATTR_STATUS, STATUS_INACTIVE);
					}
					objectMap.put(USERENTITY, customEntity);
				}
			} else {
				if (token[1].equals(STARTDATE)) {
					startDate = AttributeUtil.getLongValue(attr);
				}
				if (objectMap.size() > 0 && objectMap.containsKey(token[0])) {
					attributeValue = getAttributeValue(attr, token[1], config);
					objectMap.get(token[0]).put(token[1], attributeValue);
				} else {
					Map<String, Object> customEntity = new HashMap<String, Object>();
					attributeValue = getAttributeValue(attr, token[1], config);
					customEntity.put(token[1], attributeValue);
					objectMap.put(token[0], customEntity);
				}
			}
		}
		if (operation.equalsIgnoreCase(CREATE_OP)) {
			objectMap.put(SuccessFactorsConstants.PERPERSON,
					new HashMap<String, Object>());
			objectMap = addMetadataToObjects(objectMap, sUserId, startDate,
					config, CREATE_OP);
		} else {
			objectMap = addMetadataToObjects(objectMap, sUserId, startDate,
					config, UPDATE_OP);
		}
		objectMap = addMetadataToObjects(objectMap, sUserId, startDate, config,
				UPDATE_OP);

		LinkedHashSet<String> entitySet = new LinkedHashSet<String>(
				Arrays.asList(DEFAULT_ENTITY_ARRAY));
		Iterator<String> iterator = entitySet.iterator();
		while (iterator.hasNext()) {
			String entity = iterator.next().toString();
			if (objectMap.get(entity) != null) {
				jsonObjectMap.put(entity,
						createJsonObject(objectMap.get(entity), config));
			}
		}

		if (objectMap.size() > 0) {
			for (Entry<String, Map<String, Object>> entry : objectMap
					.entrySet()) {
				String key = entry.getKey();
				if (!entitySet.contains(key)) {
					jsonObjectMap.put(key,
							createJsonObject(objectMap.get(key), config));
				}
			}
		}
		printMethodExit(methodName, log);
		return jsonObjectMap;
	}

	public static Map<String, Map<String, Object>> addMetadataToObjects(
			Map<String, Map<String, Object>> customEntities, String sUserId,
			long startDate, SuccessFactorsConfiguration config, String operation) {
		String methodName = "addMetadataToObjects";
		printMethodEntered(methodName, log);
		try {
			String[] objMetadatas = config.getObjectMetadatas();
			Map<String, String> objMetadataMap = convertStringArrayToMap(objMetadatas);
			Iterator<Entry<String, Map<String, Object>>> it = customEntities
					.entrySet().iterator();
			while (it.hasNext()) {
				String metadataString = null;
				Entry<String, Map<String, Object>> item = it.next();
				String key = item.getKey();
				if (objMetadataMap.get(key).contains(URI_DATE_TIME)) {
					if (startDate == 0L) {
						startDate = new Date().getTime();
					}
					metadataString = objMetadataMap.get(key).replaceAll(
							URI_DATE_TIME, convertDate(startDate));
					metadataString = metadataString.replaceAll(URI_USERNAME,
							sUserId);
					customEntities.get(key).put(
							SuccessFactorsUtils.METADETA,
							new ObjectMapper().readValue(metadataString,
									Map.class));
				} else {
					customEntities.get(key).put(
							SuccessFactorsUtils.METADETA,
							new ObjectMapper().readValue(objMetadataMap
									.get(key).replaceAll("Username", sUserId),
									Map.class));
				}
				if (key.equalsIgnoreCase(USERENTITY)) {
					customEntities.get(key).put(
							SuccessFactorsUtils.ATTR_USERNAME, sUserId);
				}
				if (objMetadataMap.get(key).contains(ATTR_USERID)) {
					customEntities.get(key).put(
							SuccessFactorsUtils.ATTR_USERID, sUserId);
				}
				if (objMetadataMap.get(key).contains(PERSONID_EXTERNAL)) {
					customEntities.get(key).put(
							SuccessFactorsUtils.PERSONID_EXTERNAL, sUserId);
				}
			}
			if (customEntities.get(EMPJOB) != null
					&& !customEntities.get(EMPJOB).containsKey(STARTDATE)) {
				customEntities.get(EMPJOB).put(STARTDATE,
						formatLongValue(new Date().getTime()));
			}
			if (operation.equalsIgnoreCase(CREATE_OP)) {
				customEntities.get(USERENTITY).put(ATTR_FIRSTNAME,
						customEntities.get(PERPERSONAL).get(ATTR_FIRSTNAME));
				customEntities.get(USERENTITY).put(ATTR_LASTNAME,
						customEntities.get(PERPERSONAL).get(ATTR_LASTNAME));
			}
		} catch (JsonParseException jpe) {
			log.error("Exception occured while adding metadata to payload",
					jpe.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_ADD_METADATA, jpe.getMessage()));
		} catch (JsonMappingException jme) {
			log.error("Exception occured while adding metadata to payload",
					jme.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_ADD_METADATA, jme.getMessage()));
		} catch (IOException io) {
			log.error("Exception occured while adding metadata to payload",
					io.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_ADD_METADATA, io.getMessage()));
		}
		printMethodExit(methodName, log);
		return customEntities;
	}

	public static Object getAttributeValue(Attribute attr, String token,
			SuccessFactorsConfiguration config) {
		String methodName = "addMetadataToObjects";
		printMethodEntered(methodName, log);
		Object attrValue = null;
		try {
			if (token.equalsIgnoreCase(ATTR_HR)) {
				if (AttributeUtil.getSingleValue(attr) == null) {
					attrValue = NO_HR_VALUE;
				} else {
					attrValue = AttributeUtil.getSingleValue(attr);
				}
				attrValue = new ObjectMapper().readValue(String.format(
						"{\"__metadata\": {\"uri\": \"User('%s')\"}}",
						attrValue), Map.class);
			} else if (token.equalsIgnoreCase(ATTR_MANAGERID)) {
				if (AttributeUtil.getSingleValue(attr) == null) {
					attrValue = NO_MANAGER_VALUE;
				} else {
					attrValue = AttributeUtil.getSingleValue(attr);
				}
			} else if (AttributeUtil.getSingleValue(attr) == null) {
				attrValue = AttributeUtil.getSingleValue(attr);
			} else {
				attrValue = AttributeUtil.getSingleValue(attr);
				if (!token.equalsIgnoreCase(ATTR_EMPID)) {
				if (attrValue.equals("1") || attrValue.equals("0")) {
					if (AttributeUtil.getSingleValue(attr).equals("1"))
						attrValue = true;
					else
						attrValue = false;
				}
				 else {
					if (attrValue instanceof Long) {
						Long dateLong = AttributeUtil.getLongValue(attr);
						attrValue = formatLongValue(dateLong);
					}
				}
			}
			}
		} catch (JsonParseException jpe) {
			log.error("Exception occured while adding metadata to payload",
					jpe.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_ADD_METADATA, jpe.getMessage()));
		} catch (JsonMappingException jme) {
			log.error("Exception occured while adding metadata to payload",
					jme.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_ADD_METADATA, jme.getMessage()));
		} catch (IOException io) {
			log.error("Exception occured while adding metadata to payload",
					io.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_ADD_METADATA, io.getMessage()));
		}
		printMethodExit(methodName, log);
		return attrValue;
	}

	public static String convertDate(Long dateValue) {
		Date date = new Date(dateValue);
		SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String[] dateTime = formater.format(date).split(" ");
		return dateTime[0] + "T" + dateTime[1];
	}

	/** Converts long to String
	 * @param value
	 * @return
	 */
	private static String formatLongValue(Long value) {
		String longToString = value.toString();
		String formattedDate = "/Date(" + longToString + ")/";
		return formattedDate;
	}

/**
 * Converts String format of date to long
 * @param dateValue
 * @return
 */
	public static Long convertLongFormat(String dateValue) {

		Date date = new Date(Long.valueOf(dateValue));
		SimpleDateFormat formater = new SimpleDateFormat("yyyyMMddHHmmss");
		return Long.valueOf(formater.format(date));
	}

	/**
	 * Converting date format
	 * @param dateValue
	 * @param config
	 * @return
	 */
	public static String convertDateFormat(String dateValue,
			SuccessFactorsConfiguration config) {
		Date oimDate;
		String[] dateTime = null;
		try {
			oimDate = new SimpleDateFormat("yyyyMMddHHmmss").parse(dateValue);
			SimpleDateFormat formater = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss");
			dateTime = formater.format(oimDate).split(" ");
		} catch (java.text.ParseException e) {
			log.error("Exception occured while converting date format {0}",
					e.getMessage());
			throw new ConnectorException(
					config.getMessage(
							SuccessFactorsConstants.EX_DATE_FORMAT_CONV,
							e.getMessage()));
		}
		return dateTime[0] + "T" + dateTime[1];
	}

	/**
	 * Creating Json Object
	 * @param userEntityAttrs
	 * @param config
	 * @return
	 */
	public static JSONObject createJsonObject(
			Map<String, Object> userEntityAttrs,
			SuccessFactorsConfiguration config) {
		JSONObject jsonObj = new JSONObject();
		Iterator<Entry<String, Object>> entries = userEntityAttrs.entrySet()
				.iterator();
		while (entries.hasNext()) {
			Entry<String, Object> thisEntry = entries.next();
			String key = thisEntry.getKey().toString();
			Object value = thisEntry.getValue();
			try {
				if (value == null)
					jsonObj.put(key, JSONObject.NULL);
				else
					jsonObj.put(key, value);
			} catch (JSONException e) {
				log.error(
						"Error occured while converting the map to JSON Payload {0}",
						e.getMessage());
				throw new ConnectorException(config.getMessage(
						SuccessFactorsConstants.EX_MAP_TO_JSON, e.getMessage()));
			}
		}
		return jsonObj;
	}

	/**
	 * This method is used to make REST calls on any given URL and analyse the
	 * response for success and failure. If the call is successful it returns
	 * the response string (if any). In case of failure it throws an exception
	 * with the HTTP Status Line and the response from the target.
	 * 
	 * @param httpClient
	 *            This take a {@link CloseableHttpClient} object
	 * @param url
	 *            URL that is to be hit.
	 * @param operationType
	 *            Operation type should be POST,PUT,PATCH,GET or DELETE
	 * @param json
	 *            The JSON payload that needs to be sent for POST,PUT and PATCH.
	 *            Should be null for other operationTypes.
	 * @param entity
	 *            Only for getting access token during getting initial access
	 *            token, otherwise entity will be null
	 * @return Response returned by the target
	 */
	public static String executeRequest(CloseableHttpClient httpClient,
			JSONObject json, String url, String userEntity,
			SuccessFactorsConfiguration config) {
		String methodName = "executeRequest";
		printMethodEntered(methodName, log);
		StringBuilder requestUrl = null;
		String requestUrlStr = null;

		if (!url.startsWith("http")) {
			requestUrl = new StringBuilder(
					config.isSslEnabled() ? SuccessFactorsConstants.HTTPS
							: SuccessFactorsConstants.HTTP).append(config
					.getHost()).append(":").append(config.getPort());

			requestUrlStr = requestUrl.toString() + url;
		} else {
			requestUrlStr = url;
		}

		CloseableHttpResponse response = null;
		HttpRequestBase httpRequest = null;

		String jsonResp = null;
		StringEntity stringEntity;
		log.error("Perf: Performing REST call to the target started");
		try {
			if (json != null) {
				if (json.has(ATTR_PASSWORD)) {
					JSONObject JsonObjForLogging = maskPasswordInLogs(json,
							config);
					log.info("payload Request :{0}", JsonObjForLogging);
				} else {
					log.info("json Request :{0}", json);
				}
				httpRequest = new HttpPost(requestUrlStr);
				stringEntity = new StringEntity(json.toString());
				((HttpPost) httpRequest).setEntity(stringEntity);
				response = httpClient.execute(httpRequest);
				log.error("Perf: Performing REST call to the target Completed");
			} else {
				httpRequest = new HttpGet(requestUrlStr);
				response = httpClient.execute((HttpUriRequest) httpRequest);
				log.error("Perf: Performing REST call to the target Completed");
			}
			if (response.getEntity() != null) {
				jsonResp = EntityUtils.toString(response.getEntity());
				if (jsonResp.contains(ATTR_PASSWORD)) {
					LinkedHashMap<String, Object> targetResponse = parseResponse(
							jsonResp, config);
					targetResponse.put(ATTR_PASSWORD, MASK_PASSWORD_VALUE);
					log.info("Resonse from the target is {0}", targetResponse);
				} else
					log.info("Resonse from the target is {0}", jsonResp);
			}

			if (jsonResp.contains("Record not found")) {
				return jsonResp;
			}

			if (jsonResp.contains("User record with Key User")) {
				return jsonResp;
			}
			if (userEntity != null) {
				checkResponseStatus(response, jsonResp, userEntity, config);
			} else {
				checkResponseStatus(response, jsonResp, null, config);
			}

		} catch (UnsupportedEncodingException e) {
			log.error("Exception occurred while executing request",
					e.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_EXECUTE_REQUEST, e.getMessage()));
		} catch (ClientProtocolException e) {
			log.error("Exception occurred while executing request",
					e.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_EXECUTE_REQUEST, e.getMessage()));
		} catch (IOException e) {
			log.error("Exception occurred while executing request",
					e.getMessage());
			throw new ConnectionFailedException(config.getMessage(
					SuccessFactorsConstants.EX_EXECUTE_REQUEST, e.getMessage()));
		} finally {
			if (httpRequest != null) {
				httpRequest.releaseConnection();
			}
			if (response != null) {
				try {
					response.close();
				} catch (IOException e) {
					log.error("Failed to close the resource CloseableResponse "
							+ e.getMessage());
					throw new ConnectorException(config.getMessage(
							SuccessFactorsConstants.EX_RESOURCE_CLOSURE,
							e.getMessage()));
				}
			}
		}
		printMethodExit(methodName, log);
		return jsonResp;
	}

	@SuppressWarnings("unchecked")
	private static JSONObject maskPasswordInLogs(JSONObject json,
			SuccessFactorsConfiguration config) {
		Map<String, Object> mapForLogging = null;
		JSONObject jsonObj = null;
		try {
			mapForLogging = new ObjectMapper().readValue(json.toString(),
					HashMap.class);
			mapForLogging.put(ATTR_PASSWORD, MASK_PASSWORD_VALUE);

			jsonObj = createJsonObject(mapForLogging, config);
			;

		} catch (JsonParseException e) {
			log.error("Error occured while reading data JSON Object",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		} catch (JsonMappingException e) {
			log.error("Error occured while reading data JSON Object",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		} catch (IOException e) {
			log.error("Error occured while reading data JSON Object",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		}
		return jsonObj;
	}

 
/**
 * Evaluates the different response status getting from  request 
 * @param response
 * @param jsonResp
 * @param entity
 * @param config
 */
	public static void checkResponseStatus(CloseableHttpResponse response,
			String jsonResp, String entity, SuccessFactorsConfiguration config) {
		String methodName = "checkResponseStatus";
		printMethodEntered(methodName, log);
		int statusCode = response.getStatusLine().getStatusCode();

		if (entity != USERENTITY) {
			LinkedHashMap<String, Object> responseMap = parseResponse(jsonResp,
					config);
			parseForErrorMessage(responseMap);
		}
		/**
		 * Check Codes for Success/Failure
		 */
		try {
			String msgKey = Integer.toString(statusCode);
			if (statusCode >= 200 && statusCode < 300) {
				/**
				 * Success Scenarios
				 */
				log.info("success", "log.httpSuccess" + msgKey);
			} else if (statusCode == 500) {
				if (jsonResp != null
						&& jsonResp.contains("User already exists")) {
					throw new AlreadyExistsException(
							config.getMessage(SuccessFactorsConstants.EX_USER_EXISTS));
				} else {
					throw new ConnectorException(config.getMessage(
							SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
							msgKey));
				}
			} else {
				/**
				 * Error Scenarios
				 */
				msgKey = "ex.httpError" + msgKey;
				log.error("Error occured while parsing the JSON Response");
				throw new ConnectorException(config.getMessage(
						SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
						jsonResp));
			}
		} catch (ParseException e) {
			log.error("Error occured while parsing the JSON Response",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
					e.getMessage()));
		}
	}

	/**
	 * Populate code key and decode in the dynamic lookups
	 * 
	 * @param httpClient
	 * @param oclass
	 * @param jsonObj
	 * @param cfg
	 * @return ConnectorObject
	 */

	public static ConnectorObject createConnectorObjectForLookup(
			CloseableHttpClient httpClient, ObjectClass oclass,
			JSONObject jsonObj, SuccessFactorsConfiguration config) {
		String methodName = "createConnectorObjectForLookup";
		printMethodEntered(methodName, log);
		ConnectorObjectBuilder objectBuilder = new ConnectorObjectBuilder();
		try {
			String oClass = oclass.getObjectClassValue();
			if (oClass.equalsIgnoreCase(SuccessFactorsConstants.ATTR_HR)
					|| oClass
							.equalsIgnoreCase(SuccessFactorsConstants.ATTR_SUPERVISOR)) {
				String user = jsonObj
						.getString(SuccessFactorsConstants.ATTR_USERID);
				objectBuilder.setUid(user);
				objectBuilder.setName(user);
				objectBuilder.setObjectClass(oclass);
			} else if (oClass
					.equalsIgnoreCase(SuccessFactorsConstants.ATTR_JOBLEVEL)) {
				objectBuilder.setUid(jsonObj
						.getString(SuccessFactorsConstants.PARSE_EXTRCODE));
				objectBuilder.setName(jsonObj
						.getString(SuccessFactorsConstants.PARSE_NAME));
				objectBuilder.setObjectClass(oclass);
			}
		} catch (JSONException e) {
			log.error("Error occured while reading data JSON Object {0}",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		}
		printMethodExit(methodName, log);
		return objectBuilder.build();
	}

	/**
	 * Building connector object for User Reconciliation
	 * 
	 * @param httpClient
	 * @param user
	 * @param jsonObj
	 * @param attrsToGetSet
	 * @param cfg
	 * @return ConnectorObject
	 */

	public static ConnectorObject createConnectorObjectForUser(
			CloseableHttpClient httpClient, String user, JSONObject userEntity,
			Set<String> attrsToGetSet, SuccessFactorsConfiguration config) {

		String methodName = "createConnectorObjectForUser";
		printMethodEntered(methodName, log);
		ConnectorObjectBuilder objectBuilder = new ConnectorObjectBuilder();
		SuccessFactorsUtils successUtil=new SuccessFactorsUtils();
		Object attrValue = null;
		Map<String, JSONArray> customEntities = new HashMap<String, JSONArray>();
		String[] customURIs = config.getCustomURIs();
		Map<String, String> customURIsMap = convertStringArrayToMap(customURIs);
		String[] childFields = config.getChildFields();
		Map<String, String> childFieldsMap = new HashMap<String, String>();
		if(childFields != null)
		childFieldsMap = convertStringArrayToMap(childFields);
		// START BUG 28403187
		attrsToGetSet = new TreeSet<String>(attrsToGetSet);
		// END BUG 28403187 
		try {
			for (String attr : attrsToGetSet) {
				String[] token = null;
				if (attr.contains(DELIMITER)) {
					token = attr.split(DELIMITER_SPLIT);
					JSONArray customEntity = null;
					if (customEntities.size() > 0
							&& customEntities.containsKey(token[0])) {
						//START BUG 28172451
						attrValue = successUtil.getValueFromResponse(
								customEntities.get(token[0]).getJSONObject(0), token[1],
								httpClient, user, config,token);
						//END BUG 28172451
					} else {
						customEntity = successUtil.getJsonObjectforEntity(httpClient,
								token[0], user, customURIsMap, config);
						if (customEntity == null || customEntity.isNull(0)) {
							continue;
						} else {
							customEntities.put(token[0], customEntity);
							//START BUG 28172451
							attrValue = successUtil.getValueFromResponse(customEntity.getJSONObject(0),
									token[1], httpClient, user, config,token);
							//END BUG 28172451
						}
					 }
					
					if (attrValue != null && attrValue instanceof Long) {
						log.info("Setting attribute:{0} with value :{1}", attr,
								((Long) attrValue).longValue());
						objectBuilder.addAttribute(AttributeBuilder.build(attr,
								((Long) attrValue).longValue()));
					} else {
						if (attrValue != null && !attrValue.equals("null")) {
							log.info("Setting attribute:{0} with value :{1}",
									attr, attrValue);
							objectBuilder.addAttribute(AttributeBuilder.build(
									attr, attrValue));
						}
					}
				}else if (attr.equals(Uid.NAME) || attr.equals(Name.NAME)) {
					continue;
				}else if (attr.equals(OperationalAttributes.ENABLE_NAME)) {
					attrValue = userEntity.get(
							SuccessFactorsConstants.ATTR_STATUS).toString();
					if (attrValue.toString().equalsIgnoreCase("T")) {
						log.info("Setting attribute:{0} with value :{1}", attr,
								true);
						objectBuilder.addAttribute(AttributeBuilder
								.buildEnabled(true));
					} else {
						log.info("Setting attribute:{0} with value :{1}", attr,
								false);
						objectBuilder.addAttribute(AttributeBuilder
								.buildEnabled(false));
					}
				}else if (attr.equals(SuccessFactorsConstants.LAST_MODIFIED_DATETIME)) {
					//START BUG 28172451
					attrValue = successUtil.getValueFromResponse(userEntity, attr,
							httpClient, user, config,token);
					//END BUG 28172451
					if (attrValue != null && attrValue instanceof Long) {
						log.info("Setting attribute:{0} with value :{1}", attr,
								convertLongFormat(attrValue.toString()));
						objectBuilder.addAttribute(AttributeBuilder.build(attr,
								convertLongFormat(attrValue.toString())));
					}
				} else {
	        		AttributeBuilder childAttrBuilder = new AttributeBuilder();
					if(childFieldsMap.get(attr) != null){
					String[] splitToken = childFieldsMap.get(attr).split(DELIMITER_TILDE);
					JSONArray customEntity = successUtil.getJsonObjectforEntity(httpClient,
							splitToken[0], user, customURIsMap, config);
	        		childAttrBuilder.setName(attr);
					for (int i = 0; i < customEntity.length(); i++) {
						EmbeddedObjectBuilder embBuilder = new EmbeddedObjectBuilder();
						embBuilder.setObjectClass(new ObjectClass(splitToken[0]));
					         for (int j = 1; j < splitToken.length; j++) {
					        	 	String childAttr = splitToken[j];
					        	 	attrValue = successUtil.getValueFromResponse(
					        	    customEntity.getJSONObject(i), childAttr,httpClient, user, config,token);//changed
					        	 	if (attrValue != null && !attrValue.equals("null"))
									embBuilder.addAttribute(childAttr, attrValue);
					}
					         childAttrBuilder.addValue(embBuilder.build());
					}
					}else{
						throw new ConnectionFailedException(config.getMessage(SuccessFactorsConstants.EX_ATTRIBUTES_TO_GET_CHILD)+ attr);
					}
					objectBuilder.addAttribute(childAttrBuilder.build());
				}
			
		
			objectBuilder.setUid(user);
			objectBuilder.setName(user);
			objectBuilder.setObjectClass(ObjectClass.ACCOUNT);
		} 
		
	}catch (JSONException e) {
			log.error("Error occured while reading data JSON Object",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		}
		printMethodExit(methodName, log);
		return objectBuilder.build();
		
	}
/**
 * Traversing the nested attributes.
 * @param entity
 * @param httpClient
 * @param user
 * @param config
 * @param nestedToken
 * @return
 */
	
	private static String getNestedAttributeValues(JSONObject entity, CloseableHttpClient httpClient, String user,
                  SuccessFactorsConfiguration config, String[] nestedToken) {
		//START BUG 28172451
		String methodName = "getNestedAttributeValues";
		printMethodEntered(methodName, log);
	    String finalNestedAttributeValue=null;
	    String value=null;
	    try {
         if(nestedToken.length>2) {
		for(int i=1;i<nestedToken.length-1;i++) {
			
				String attributeName=nestedToken[i];				
				value=entity.get(attributeName).toString();
				if(value!=null) {
					Object attrValue = null;
					attrValue=value;
					if (attrValue.toString().startsWith("{")) {
						attrValue = entity.get(attributeName).toString();
						JSONObject nestedObj = new JSONObject(attrValue.toString());
						String url = nestedObj.getJSONObject(
								SuccessFactorsConstants.PARSE_DEFERRED).getString(
								SuccessFactorsConstants.PARSE_URI);
						log.info("url :: {0}", url);
						String responseJson=executeRequest( httpClient,null, url,  user,config);
						if (responseJson.indexOf("error") > -1) {
							log.info("Nested Attribute Record {0} not found for user {1}",
									attrValue, "user");
							
							return finalNestedAttributeValue;
							
						}
						entity=null;
						//START BUG 28172451
						entity=convertStringToJSONObject(responseJson, config,attributeName);
						//END BUG 28172451

					}
				}
				else {
					break;
				}
                
			}
		}
		finalNestedAttributeValue=entity.get(nestedToken[nestedToken.length-1]).toString();

			printMethodExit(methodName, log);
		} catch (JSONException e) {
			log.error("Error occured while reading data JSON Object",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		}
         return finalNestedAttributeValue;
       //START BUG 28172451
	}
/**
 * Converting the String into JsonObject.
 * @param responseJson
 * @param config
 * @param attributeName
 * @return
 */

	private static JSONObject convertStringToJSONObject(String responseJson, SuccessFactorsConfiguration config,
			String attributeName) {
		//START BUG 28172451
		String methodName = "convertStringToJSONObject";
		printMethodEntered(methodName, log);
		JSONObject obj=null;
		JSONObject resObj=null;
		try {
			obj = new JSONObject(responseJson);
			String dObj = obj.get(SuccessFactorsConstants.PARSE_D).toString();
			resObj = new JSONObject(dObj);
		} catch (JSONException e) {
			log.error("Error occured while converting response to JSON Array",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_ARRAY_CONVERSION,
					e.getMessage()));
		}
		printMethodExit(methodName, log);
		return resObj ;
		//END BUG 28172451

	}

	/**
	 * Converting Array of String to Map
	 * @param array
	 * @return
	 */
	private static Map<String, String> convertStringArrayToMap(String[] array) {
		Map<String, String> arrayToMap = new HashMap<String, String>();
		for (String entry : array) {
			String[] keys = entry.split(SPLIT_EQUALS, 2);
			arrayToMap.put(keys[0], keys[1]);
		}
		return arrayToMap;
	}
	
/**
 * Getting the attribute value from the response 
 * @param entity
 * @param attrName
 * @param httpClient
 * @param user
 * @param config
 * @param token
 * @return
 */
	private Object getValueFromResponse(JSONObject entity,
			String attrName, CloseableHttpClient httpClient, String user,
			SuccessFactorsConfiguration config, String[] token ) {
		String value = null;
		Object attrValue = null;
		Long dateValue = 0L;
		try {
			value = entity.get(attrName).toString();
			// START BUG 28403187 
				if((!(token==null))&&(token[0].equals(EMPEMPLOYMENT))){
					personId=entity. get(PERSONID_EXTERNAL).toString();
				 
					} 	
				// END BUG 28403187 
			if (!(value.equals(null))) {
				attrValue = value;
				if (attrValue.toString().startsWith("{")) {
					//START BUG 28172451
					attrValue= getNestedAttributeValues(entity,httpClient,user,config,token);
					//END BUG 28172451
				} else if (attrValue.toString().equalsIgnoreCase("true")
						|| attrValue.toString().equalsIgnoreCase("false")) {
					if (attrValue.toString().equalsIgnoreCase("true")) {
						attrValue = "1";
					} else {
						attrValue = "0";
					}
				} else if (value.startsWith(DATE_FIELD_INDICATOR)) {
					if (value.contains("+")) {
						dateValue = Long.valueOf(value.substring(
								value.indexOf("(") + 1, value.indexOf("+")));
					} else {
						dateValue = Long.valueOf(value.substring(
								value.indexOf("(") + 1, value.indexOf(")")));
					}
					attrValue = dateValue;
				}

			}
		} catch (JSONException e) {
			log.error("Error occured while reading data JSON Object",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_READ, e.getMessage()));
		}
		return attrValue;

	}

	private JSONArray getJsonObjectforEntity(
			CloseableHttpClient httpClient, String entity, String user,
			Map<String, String> customURIsMap,
			SuccessFactorsConfiguration config) {
		String url = null;
		JSONArray entityResult = null;
		if (customURIsMap.containsKey(entity)) {
			url = customURIsMap.get(entity);
			String replaceUrl = null;
			// START BUG 28403187 
			if (url != null && entity.equalsIgnoreCase(PERPERSONAL))
				
			{
				if(personId==null) {
				replaceUrl = url.replace("(Username)",user);					
				}else {
				  replaceUrl = url.replace("(Username)", personId.toString());
				}
				log.info("PerPersonal URL :: {0}", replaceUrl);
			}else{
				replaceUrl = url.replace("(Username)", user);
				log.info("Recon URL :: {0}", replaceUrl);
			}
			// END BUG 28403187
			String targetResponse = SuccessFactorsUtils.executeRequest(
					httpClient, null, replaceUrl, null, config);
			entityResult = SuccessFactorsUtils
					.convertResponseToJSONArray(targetResponse, config);
		}else{
			throw new ConnectorException("Uri not found for Entity "+ entity);
		}
		return entityResult;
	}


	/**
	 * Method to parse the response
	 * 
	 * @param response
	 * @return
	 */
	public static JSONArray convertResponseToJSONArray(String response,
			SuccessFactorsConfiguration config) {
		JSONObject obj;
		JSONArray results = null;
		try {
			obj = new JSONObject(response);
			String dObj = obj.get(SuccessFactorsConstants.PARSE_D).toString();
			JSONObject resObj = new JSONObject(dObj);
			results = resObj
					.getJSONArray(SuccessFactorsConstants.PARSE_RESULTS);
		} catch (JSONException e) {
			log.error("Error occured while converting response to JSON Array",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_ARRAY_CONVERSION,
					e.getMessage()));
		}
		return results;
	}

	public static LinkedHashMap<String, Object> parseResponse(String response,
			SuccessFactorsConfiguration config) {
		JSONObject obj = null;
		LinkedHashMap<String, Object> lst = null;
		String dObj = null;
		try {
			if (response.startsWith("{")) {
				obj = new JSONObject(response);
				if (obj.get("d") != null) {
					dObj = obj.get("d").toString();
				} else {
					dObj = obj.toString();
				}
				if (dObj.startsWith("[")) {
					lst = (LinkedHashMap<String, Object>) convertJsonToList(
							dObj, config).get(0);
				}
				else if (dObj.contains("[")
						&& new JSONObject(dObj).get("results") != null) {
					dObj = new JSONObject(dObj).get("results").toString();
					if (!dObj.equals("[]")) {
						if (dObj.startsWith("[")) {
							lst = (LinkedHashMap<String, Object>) convertJsonToList(
									dObj, config).get(0);
						} else {
							lst = convertJsonToMap(dObj, config);
						}
					}
				} else {
					lst = convertJsonToMap(dObj, config);
				}
			} else {
				log.error("Error occured while parsing the JSON Response",
						response);
				throw new ConnectorException(config.getMessage(
						SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
						response));
			}
		} catch (JSONException e) {
			log.error("Error occured while parsing the JSON Response",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
					e.getMessage()));

		}
		return lst;

	}

	@SuppressWarnings("unchecked")
	public static LinkedHashMap<String, Object> convertJsonToMap(
			String jsonString, SuccessFactorsConfiguration config) {
		try {
			return new ObjectMapper()
					.readValue(jsonString, LinkedHashMap.class);
		} catch (IOException e) {
			log.error("Error occured while parsing the JSON Response",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
					e.getMessage()));
		}
	}

	@SuppressWarnings("unchecked")
	public static List<Map<String, Object>> convertJsonToList(
			String jsonString, SuccessFactorsConfiguration config) {
		try {
			return new ObjectMapper().readValue(jsonString, List.class);

		} catch (IOException e) {
			log.error("Error occured while parsing the JSON Response",
					e.getMessage());
			throw new ConnectorException(config.getMessage(
					SuccessFactorsConstants.EX_JSON_PARSE_RESPONSE,
					e.getMessage()));
		}
	}

	public static String decryptPassword(GuardedString password) {
		String methodName = "decryptPassword";
		printMethodEntered(methodName, log);
		final String[] passwdVal = new String[1];
		if (password != null) {
			password.access(new GuardedString.Accessor() {
				public void access(char[] clearChars) {
					passwdVal[0] = String.valueOf(clearChars);
					;
				}
			});
		}
		printMethodExit(methodName, log);
		return passwdVal[0];
	}

	public static void parseForErrorMessage(Map<String, Object> responseMap) {
		log.ok("Parsing the response for error message");
		if (responseMap != null) {
			Object errorResponse = responseMap.get("message");
			if ((errorResponse != null)) {
				String status = (String) responseMap.get(ATTR_STATUS);
				if ((!StringUtil.isBlank(status))
						&& (status.equalsIgnoreCase("ERROR"))) {
					log.error("Error: " + errorResponse, new Object[0]);
					throw new ConnectorException((String) errorResponse);
				}
			}
		}
		log.ok("Response does not contain error message");
	}

}

