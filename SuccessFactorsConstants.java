/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/utils/SuccessFactorsConstants.java /main/6 2018/07/03 03:21:39 bkouthar Exp $ */

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
    samelgir    01/05/17 - Creation
 */

/**
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/utils/SuccessFactorsConstants.java /main/6 2018/07/03 03:21:39 bkouthar Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */
package org.identityconnectors.successfactors.utils;

public interface SuccessFactorsConstants {

	public static final String METHOD_ENTERED = "Method Entered";
	public static final String METHOD_EXITING = "Method Exiting";
	public static final String ATTR_USERID = "userId";
	public static final String ATTR_USERNAME = "username";
	public static final String ATTR_FIRSTNAME = "firstName";
	public static final String ATTR_LASTNAME = "lastName";
	public static final String ATTR_NAME = "__NAME__";
	public static final String ATTR_PASSWORD = "password";
	public static final String ATTR_EMAIL = "email";
	public static final String ATTR_STATUS = "status";
	public static final String ATTR_UID = "__UID__";
	public static final String ATTR_COUNTRY = "country";
	public static final String ATTR_SUPERVISOR = "supervisor";
	public static final String ATTR_MANAGERID = "managerId";
	public static final String ATTR_HR = "hr";
	public static final String ATTR_DEPARTMENT = "department";
	public static final String ATTR_TIMEZONE = "timeZone";
	public static final String ATTR_GENDER = "gender";
	public static final String ATTR_EMPID = "empId";
	public static final String ATTR_JOBLEVEL = "jobLevel";
	public static final String ATTR_MARRIED = "married";
	public static final String ATTR_CITY = "city";
	public static final String ATTR_STATE = "state";
	public static final String ATTR_DIVISION = "division";
	public static final String ATTR_CITIZENSHIP = "citizenship";
	public static final String ATTR_LOCATION = "location";
	public static final String ATTR_HIREDATE = "hireDate";
	public static final String ATTR_STARTDATE = "hireDate";
	public static final String ATTR_JOBCODE = "jobCode";
	public static final String ATTR_EVENT_REASON = "eventReason";
	public static final String ATTR_COMPANY = "company";
	public static final String ATTR_BUSINESS_UNIT = "businessUnit";

	public static final String FILTER_SUFFIX = "Filter Suffix";
	public static final String INCREMENTAL_SUFFIX = "Incremental Recon Attribute";
	public static final String LAST_MODIFIED_DATETIME = "lastModifiedDateTime";
	public static final String LATEST_TOKEN = "Latest Token";

	public static final String USERENTITY = "UserEntity";
	public static final String EMPJOB = "EmpJob";
	public static final String EMPEMPLOYMENT = "EmpEmployment";
	public static final String PERPERSONAL = "PerPersonal";
	public static final String PERPERSON = "PerPerson";

	public static final String USERNAME = "username";
	public static final String PERSONID_EXTERNAL = "personIdExternal";
	public static final String METADETA = "__metadata";
	public static final String STATUS_ACTIVE = "active";
	public static final String STATUS_INACTIVE = "inactive";

	public static final String PROV_STATUS = "UserEntity.status";
	public static final String PROV_EMAIL = "UserEntity.email";
	public static final String PROV_DEPARTMENT = "UserEntity.department";
	public static final String PROV_TIMEZONE = "UserEntity.timeZone";
	public static final String PROV_DIVISION = "UserEntity.division";
	public static final String PROV_LOCATION = "UserEntity.location";
	public static final String PROV_JOBLEVEL = "UserEntity.jobLevel";
	public static final String PROV_CITIZENSHIP = "UserEntity.citizenship";
	public static final String PROV_COUNTRY = "UserEntity.country";
	public static final String PROV_STATE = "UserEntity.state";
	public static final String PROV_EMPID = "UserEntity.empId";
	public static final String PROV_MARRIED = "UserEntity.married";
	public static final String PROV_CITY = "UserEntity.city";
	public static final String PROV_USERID = "UserEntity.userId";

	public static final String PROV_JOBCODE = "EmpJob.jobCode";
	public static final String PROV_EVENTREASON = "EmpJob.eventReason";
	public static final String PROV_COMPANY = "EmpJob.company";
	public static final String PROV_BUSINESSUNIT = "EmpJob.businessUnit";
	public static final String PROV_MANAGERID = "EmpJob.managerId";

	// EmpEmployment
	public static final String PROV_STARTDATE = "EmpEmployment.startDate";
	public static final String STARTDATE = "startDate";

	// PerPerson

	// PerPersonal
	public static final String PROV_GENDER = "PerPersonal.gender";
	public static final String PROV_FIRSTNAME = "PerPersonal.firstName";
	public static final String PROV_LASTNAME = "PerPersonal.lastName";

	// Parser Constants
	public static final String PARSE_D = "d";
	public static final String PARSE_RESULTS = "results";
	public static final String PARSE_DEFERRED = "__deferred";
	public static final String PARSE_URI = "uri";
	public static final String PARSE_EXTRCODE = "externalCode";
	public static final String PARSE_NAME = "name";
	public static final String PARSE_NEXT = "__next";

	// Defaults Constants for trusted recon
	public static final String ORGANIZATION = "Organization";
	public static final String EMP_TYPE = "Employee Type";
	public static final String USER_TYPE = "User Type";

	// OAuth Parameters
	public static final String GRANT_TYPE = "grant_type";
	public static final String ASSERTION = "assertion";
	public static final String UTF8 = "UTF-8";
	public static final String PRIVATE_KEY = "privatekey";
	public static final String ACCESS_TOKEN = "access_token";
	public static final String BEARER = "Bearer ";
	public static final String AUTH_CLIENT_ID = "client_id";
	public static final String AUTH_CLIENTID = "clientId";
	public static final String AUTH_CLIENT_URL = "clientUrl";
	public static final String AUTH_COMPANY_ID = "companyId";
	public static final String AUTH_AUTHORIZATION_URL = "authorizationUrl";
	public static final String PRIVATE_KEY_PARAM = "private_key";
	public static final String USER_ID_PARAM = "user_id";
	public static final String TOKEN_URL_PARAM = "token_url";
	public static final String COMPANY_ID_PARAM = "company_id";

	public static final String RESPONSE_STATUS = "status";
	public static final String RESPONSE_OK = "OK";
	public static final String UPDATE_OP = "update";
	public static final String CREATE_OP = "create";
	String[] DEFAULT_ENTITY_ARRAY = {USERENTITY, PERPERSON, EMPEMPLOYMENT, EMPJOB, PERPERSONAL};

	public static final String DELIMITER = ".";
	public static final String DATE_FIELD_INDICATOR = "/Date";
	public static final String DELIMITER_SPLIT = "\\.";
	public static final String DELIMITER_TILDE = "~";
	public static final String HTTPS = "https://";
	public static final String HTTP = "http://";
	public static final String SPLIT_EQUALS = "=";
	public static final String URI_DATE_TIME = "DateTime";
	public static final String URI_USERNAME = "Username";
	public static final String MASK_PASSWORD_VALUE = "*******";
	public static final String NO_HR_VALUE = "NO_HR";
	public static final String NO_MANAGER_VALUE = "NO_MANAGER";
	
	public static final String EX_MISSING_ATTRS = "ex.missingAttribute";
	public static final String EX_UNSUPPORTED_AUTH_TYPE = "ex.unsupportedAuthenticationType";
	public static final String EX_UNSUPPORTED_OBJ_CLASS = "ex.unsupported.objectclass";
	public static final String EX_ADD_METADATA = "ex.addMetadata";
	public static final String EX_MAP_TO_JSON = "ex.mapToJSON";
	public static final String EX_EXECUTE_REQUEST = "ex.executeRequest";
	public static final String EX_RESOURCE_CLOSURE = "ex.resourceCloseFailure";
	public static final String EX_JSON_READ = "ex.jsonRead";
	public static final String EX_JSON_ARRAY_CONVERSION = "ex.jsonArrayConversion";
	public static final String EX_JSON_PARSE_RESPONSE = "ex.jsonParseResponse";
	public static final String EX_USER_EXISTS = "ex.user.alreadyExists";
	public static final String EX_CONNECTION_FAILED = "ex.connection.failed";
	public static final String EX_AUTH_HEADER = "ex.AuthHeader";
	public static final String EX_ENCODING = "ex.Encoding";
	public static final String EX_INPUT_STREAM_RELEASE = "ex.inputStreamRelease";
	public static final String EX_BUFFERED_READER_RELEASE = "ex.bufferReaderRelease";
	public static final String EX_RESOURCE_CLOSE_FAIL = "ex.bufferReaderRelease";
	public static final String EX_DATE_FORMAT_CONV = "ex.dateFormatConversion";
	public static final String EX_ATTRIBUTES_TO_GET_CHILD = "ex.attributesToGetChild";
	public static final String MESSAGE_CATALOG_PATH = "org/identityconnectors/successfactors/Messages";

	public static final String SUCCESS_FACTORS_CONNECTOR_DISPLAY = "display_SuccessFactorsConnector";
}
