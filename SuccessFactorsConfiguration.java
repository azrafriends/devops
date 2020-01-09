/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsConfiguration.java /main/3 2017/03/24 09:45:37 samelgir Exp $ */

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
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsConfiguration.java /main/3 2017/03/24 09:45:37 samelgir Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */
package org.identityconnectors.successfactors;

import java.util.HashSet;
import java.util.Set;

import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;
import org.identityconnectors.successfactors.utils.SuccessFactorsConstants;
import org.identityconnectors.successfactors.utils.SuccessFactorsUtils;

public class SuccessFactorsConfiguration extends AbstractConfiguration
		implements SuccessFactorsConstants {

	/**
	 * Logger reference
	 */
	private static Log log = Log.getLog(SuccessFactorsConfiguration.class);
	/**
	 * Connection parameters
	 */
	private String host;
	private int port;
	private String authenticationType;
	private boolean sslEnabled;
	/**
	 * Basic authentication / OAuth 2.0 Resource owner password parameters
	 */
	private String username;
	private GuardedString password;

	/**
	 * OAuth 2.0 Client Credential parameters
	 */
	private String clientId;
	private String authenticationServerUrl;
	private String authorizationUrl;

	private String clientUrl;
	private String companyId;
	private String privateKeyLocation;
	private String grantType;

	private String proxyHost;
	private int proxyPort;
	private String proxyUser;
	private GuardedString proxyPassword;

	// Provisioning and Reconciliation URLs
	private String reconUrl;
	private String lookupUrl;

	private String userUrl;
	private String upsertUrl;
	private String[] customURIs;
	private String[] objectMetadatas;
	private String[] childFields;
	

	/**
	 * validate() will determines whether the configuration parameters are valid
	 * or not
	 */
	@Override
	public void validate() {
		String methodName = "validate";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		Set<String> missingFields = new HashSet<String>();
		if (StringUtil.isBlank(host))
			missingFields.add(host);
		if (StringUtil.isBlank(companyId))
			missingFields.add(companyId);
		if (StringUtil.isBlank(getAuthenticationType())) {
			missingFields.add("authenticationType");
		}
		if (customURIs == null || customURIs.length < 1) {
			missingFields.add("customURIs");
		} else {
			validateAuthTypeParams(missingFields);
		}

		if (!missingFields.isEmpty())
			throw new ConfigurationException(getMessage(EX_MISSING_ATTRS,
					missingFields.toString()));
		SuccessFactorsUtils.printMethodExit(methodName, log);
	}

	private void validateAuthTypeParams(Set<String> missingFields) {
		String methodName = "validateAuthTypeParams";
		SuccessFactorsUtils.printMethodEntered(methodName, log);
		if (getAuthenticationType().equalsIgnoreCase("Basic")) {
			if (StringUtil.isBlank(getUsername())) {
				missingFields.add("username");
			}
			if (getPassword() == null
					|| StringUtil.isBlank(SuccessFactorsUtils
							.decryptPassword(getPassword()))) {
				missingFields.add("password");
			}
			if (missingFields.size() > 0) {
				throw new ConfigurationException();
			}
		} else {
			if (StringUtil.isBlank(getClientId())) {
				missingFields.add("clientId");
			}
			if (StringUtil.isBlank(getUsername())) {
				missingFields.add("username");
			}
			if (StringUtil.isBlank(getCompanyId())) {
				missingFields.add("companyId");
			}
			if (StringUtil.isBlank(getPrivateKeyLocation())) {
				missingFields.add("privateKeyLocation");
			}
			if (StringUtil.isBlank(getClientUrl())) {
				missingFields.add("clientUrl");
			}
			if (missingFields != null && missingFields.size() > 0) {
				throw new ConfigurationException(getMessage(EX_MISSING_ATTRS,
						missingFields.toString()));
			}
		}
		SuccessFactorsUtils.printMethodExit(methodName, log);
	}

	/**
	 * Format the connector message
	 * 
	 * @param key
	 *            key of the message
	 * @return return the formated message
	 */
	public String getMessage(String key) {
		final String fmt = getConnectorMessages().format(key, key);
		return fmt;
	}

	/**
	 * Format message with arguments
	 * 
	 * @param key
	 *            key of the message
	 * @param objects
	 *            arguments
	 * @return the localized message string
	 */
	public String getMessage(String key, Object... objects) {
		final String fmt = getConnectorMessages().format(key, key, objects);
		log.ok("Get for a key {0} connector message {1}", key, fmt);
		return fmt;
	}

	/**
	 * @return the host
	 */
	@ConfigurationProperty(order = 1, helpMessageKey = "successfactors.host.help", displayMessageKey = "successfactors.host.display", required = true)
	public String getHost() {
		return host;
	}

	/**
	 * @param host
	 *            the host to set
	 */
	public void setHost(String host) {
		this.host = host;
	}

	/**
	 * @return the port
	 */
	@ConfigurationProperty(order = 2, helpMessageKey = "successfactors.port.help", displayMessageKey = "successfactors.port.display", required = false)
	public int getPort() {
		return port;
	}

	/**
	 * @param port
	 *            the port to set
	 */
	public void setPort(int port) {
		this.port = port;
	}

	/**
	 * @return the authenticationType
	 */
	@ConfigurationProperty(order = 3, helpMessageKey = "successfactors.authenticationType.help", displayMessageKey = "successfactors.authenticationType.display", required = true)
	public String getAuthenticationType() {
		return authenticationType;
	}

	/**
	 * @param authenticationType
	 *            the authenticationType to set
	 */
	public void setAuthenticationType(String authenticationType) {
		this.authenticationType = authenticationType;
	}

	/**
	 * @return the isSSLEnabled
	 */
	@ConfigurationProperty(order = 4, helpMessageKey = "successfactors.sslEnabled.help", displayMessageKey = "successfactors.sslEnabled.display", required = true)
	public boolean isSslEnabled() {
		return sslEnabled;
	}

	/**
	 * @param isSSLEnabled
	 *            the isSSLEnabled is set
	 */
	public void setSslEnabled(boolean isSSLEnabled) {
		this.sslEnabled = isSSLEnabled;
	}

	/**
	 * @return the username
	 */
	@ConfigurationProperty(order = 5, helpMessageKey = "successfactors.username.help", displayMessageKey = "successfactors.username.display", required = true)
	public String getUsername() {
		return username;
	}

	/**
	 * @param username
	 *            the username to set
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * @return the password
	 */
	@ConfigurationProperty(order = 6, helpMessageKey = "successfactors.password.help", displayMessageKey = "successfactors.password.display", required = false, confidential = true)
	public GuardedString getPassword() {
		return password;
	}

	/**
	 * @param password
	 *            the password to set
	 */
	public void setPassword(GuardedString password) {
		this.password = password;
	}

	/**
	 * @return the clientId
	 */
	@ConfigurationProperty(order = 7, helpMessageKey = "successfactors.clientId.help", displayMessageKey = "successfactors.clientId.display")
	public String getClientId() {
		return clientId;
	}

	/**
	 * @param clientId
	 *            the clientId to set
	 */
	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	/**
	 * @return the authenticationServerUrl
	 */
	@ConfigurationProperty(order = 9, helpMessageKey = "successfactors.authenticationServerUrl.help", displayMessageKey = "successfactors.authenticationServerUrl.display")
	public String getAuthenticationServerUrl() {
		return authenticationServerUrl;
	}

	/**
	 * @param authenticationServerUrl
	 *            the authenticationServerUrl to set
	 */
	public void setAuthenticationServerUrl(String authenticationServerUrl) {
		this.authenticationServerUrl = authenticationServerUrl;
	}

	/**
	 * @return the authorizationUrl
	 */
	@ConfigurationProperty(order = 10, helpMessageKey = "successfactors.authorizationurl.help", displayMessageKey = "successfactors.authorizationurl.display")
	public String getAuthorizationUrl() {
		return authorizationUrl;
	}

	/**
	 * @param authorizationUrl
	 *            the authorizationUrl to set
	 */
	public void setAuthorizationUrl(String authorizationUrl) {
		this.authorizationUrl = authorizationUrl;
	}

	/**
	 * @return the clientUrl
	 */
	@ConfigurationProperty(order = 11, helpMessageKey = "successfactors.clienturl.help", displayMessageKey = "successfactors.clienturl.display")
	public String getClientUrl() {
		return clientUrl;
	}

	/**
	 * @param clientUrl
	 *            the clientUrl to set
	 */
	public void setClientUrl(String clientUrl) {
		this.clientUrl = clientUrl;
	}

	/**
	 * @return the companyId
	 */
	@ConfigurationProperty(order = 12, helpMessageKey = "successfactors.companyid.help", displayMessageKey = "successfactors.companyid.display", required = true)
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @param companyId
	 *            the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @return the privateKeyLocation
	 */
	@ConfigurationProperty(order = 13, helpMessageKey = "successfactors.privateKeyLocation.help", displayMessageKey = "successfactors.privateKeyLocation.display")
	public String getPrivateKeyLocation() {
		return privateKeyLocation;
	}

	/**
	 * @param privateKeyLocation
	 *            the privateKeyLocation to set
	 */
	public void setPrivateKeyLocation(String privateKeyLocation) {
		this.privateKeyLocation = privateKeyLocation;
	}

	/**
	 * @return the proxyHost
	 */
	@ConfigurationProperty(order = 14, helpMessageKey = "successfactors.proxyHost.help", displayMessageKey = "successfactors.proxyHost.display", required = false)
	public String getProxyHost() {
		return proxyHost;
	}

	/**
	 * @param proxyHost
	 *            the proxyHost to set
	 */
	public void setProxyHost(String proxyHost) {
		this.proxyHost = proxyHost;
	}

	/**
	 * @return the proxyPort
	 */
	@ConfigurationProperty(order = 15, helpMessageKey = "successfactors.proxyPort.help", displayMessageKey = "successfactors.proxyPort.display", required = false)
	public int getProxyPort() {
		return proxyPort;
	}

	/**
	 * @param proxyPort
	 *            the proxyPort to set
	 */
	public void setProxyPort(int proxyPort) {
		this.proxyPort = proxyPort;
	}

	/**
	 * @return the proxyUser
	 */
	@ConfigurationProperty(order = 16, helpMessageKey = "successfactors.proxyUser.help", displayMessageKey = "successfactors.proxyUser.display", required = false)
	public String getProxyUser() {
		return proxyUser;
	}

	/**
	 * @param proxyUser
	 *            the proxyUser to set
	 */
	public void setProxyUser(String proxyUser) {
		this.proxyUser = proxyUser;
	}

	/**
	 * @return the proxyPassword
	 */
	@ConfigurationProperty(order = 17, helpMessageKey = "successfactors.proxyPassword.help", displayMessageKey = "successfactors.proxyPassword.display", required = false, confidential = true)
	public GuardedString getProxyPassword() {
		return proxyPassword;
	}

	/**
	 * @param proxyPassword
	 *            the proxyPassword to set
	 */
	public void setProxyPassword(GuardedString proxyPassword) {
		this.proxyPassword = proxyPassword;
	}

	@ConfigurationProperty(order = 18, helpMessageKey = "successfactors.reconUrl.help", displayMessageKey = "successfactors.reconUrl.display")
	public String getReconUrl() {
		return reconUrl;
	}

	public void setReconUrl(String reconUrl) {
		this.reconUrl = reconUrl;
	}

	@ConfigurationProperty(order = 19, helpMessageKey = "successfactors.lookupUrl.help", displayMessageKey = "successfactors.lookupUrl.display")
	public String getLookupUrl() {
		return lookupUrl;
	}

	public void setLookupUrl(String lookupUrl) {
		this.lookupUrl = lookupUrl;
	}

	@ConfigurationProperty(order = 20, helpMessageKey = "successfactors.userUrl.help", displayMessageKey = "successfactors.userUrl.display")
	public String getUserUrl() {
		return userUrl;
	}

	public void setUserUrl(String userUrl) {
		this.userUrl = userUrl;
	}

	@ConfigurationProperty(order = 21, helpMessageKey = "successfactors.upsertUrl.help", displayMessageKey = "successfactors.upsertUrl.display")
	public String getUpsertUrl() {
		return upsertUrl;
	}

	public void setUpsertUrl(String upsertUrl) {
		this.upsertUrl = upsertUrl;
	}

	@ConfigurationProperty(order = 22, helpMessageKey = "successfactors.granttype.help", displayMessageKey = "successfactors.granttype.display")
	public String getGrantType() {
		return grantType;
	}

	public void setGrantType(String grantType) {
		this.grantType = grantType;
	}

	@ConfigurationProperty(order = 23, helpMessageKey = "successfactors.customURIs.help", displayMessageKey = "successfactors.customURIs.display")
	public String[] getCustomURIs() {
		return customURIs;
	}

	public void setCustomURIs(String[] customURIs) {
		this.customURIs = customURIs;
	}

	@ConfigurationProperty(order = 24, helpMessageKey = "successfactors.objectMetadatas.help", displayMessageKey = "successfactors.objectMetadatas.display")
	public String[] getObjectMetadatas() {
		return objectMetadatas;
	}

	public void setObjectMetadatas(String[] objectMetadatas) {
		this.objectMetadatas = objectMetadatas;
	}

	@ConfigurationProperty(order = 25, helpMessageKey = "successfactors.childFields.help", displayMessageKey = "successfactors.childFields.display")
	public String[] getChildFields() {
		return childFields;
	}

	public void setChildFields(String[] childFields) {
		this.childFields = childFields;
	}
	

}
