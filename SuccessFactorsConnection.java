/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsConnection.java /main/2 2018/07/03 03:21:39 bkouthar Exp $ */

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
    samelgir    01/06/17 - Creation
 */

/**
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsConnection.java /main/2 2018/07/03 03:21:39 bkouthar Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */

package org.identityconnectors.successfactors;

import static org.identityconnectors.successfactors.utils.SuccessFactorsConstants.METHOD_ENTERED;
import static org.identityconnectors.successfactors.utils.SuccessFactorsConstants.METHOD_EXITING;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.successfactors.utils.SuccessFactorsConstants;
import org.identityconnectors.successfactors.utils.SuccessFactorsUtils;

public class SuccessFactorsConnection {

	private SuccessFactorsConfiguration configuration;

	private CloseableHttpClient client;

	Map<String, String> authHeaders = new HashMap<String, String>();
	/**
	 * Logger reference
	 */
	private static Log log = Log.getLog(SuccessFactorsConnection.class);

	public SuccessFactorsConnection(SuccessFactorsConfiguration config) {
		this.configuration = config;
		HttpClientBuilder clientBuilder = HttpClients.custom();
		log.info("authentication type is:{0}",
				this.configuration.getAuthenticationType());
		setProxy(clientBuilder);
		setAuthHeaders(clientBuilder);
	}

	public CloseableHttpClient getConnection() {
		return this.client;
	}

	private void setAuthHeaders(HttpClientBuilder clientBuilder) {
		try {
			String authenticationType = this.configuration
					.getAuthenticationType();
			if (authenticationType.equalsIgnoreCase("oauth_saml")) {
				authHeaders = getAuthHeaders();
			} else {
				String basicAuth = configuration.getUsername()
						+ "@"
						+ configuration.getCompanyId()
						+ ":"
						+ SuccessFactorsUtils.decryptPassword(configuration
								.getPassword());
				authHeaders.put(
						"Authorization",
						"Basic "
								+ new String(Base64.encodeBase64(basicAuth
										.getBytes())));
			}
		} catch (IOException e) {
			log.error("Exception in getting authentication header :{0} " + e);
			throw new ConnectorException(configuration.getMessage(
					SuccessFactorsConstants.EX_AUTH_HEADER, e));
		}
		this.client = getAuthClient(clientBuilder,
				this.configuration.getAuthenticationType(), authHeaders);
	}

	private CloseableHttpClient getAuthClient(HttpClientBuilder clientBuilder,
			String authType, Map<String, String> authHeader) {
		List<Header> headers = new ArrayList<Header>();
		for (String entry : authHeader.keySet()) {
			Header header = new BasicHeader(entry,
					(String) authHeader.get(entry));
			headers.add(header);
		}
		Header contentHeader = new BasicHeader("Content-Type",
				"application/json");
		Header acceptHeader = new BasicHeader("Accept", "application/json");

		headers.add(contentHeader);
		headers.add(acceptHeader);
		clientBuilder.setDefaultHeaders(headers);
		log.info("returning auth client", new Object[0]);
		log.ok("Method Exiting", new Object[0]);
		return clientBuilder.build();
	}

	public Map<String, String> getAuthHeaders() throws IOException {

		HttpRequestBase httpRequest = null;
		HttpEntity entity = null;
		String samlResponse = null;
		String tokenResponse = null;
		Map<String, String> authHeaders = new HashMap<String, String>();
		List<BasicNameValuePair> parametersBody = null;
		String authenticationUrl = this.configuration
				.getAuthenticationServerUrl() + "/oauth/token?";

		String privateKey = getPrivateKey(this.configuration
				.getPrivateKeyLocation());

		parametersBody = getSamlAssertionRequest(privateKey, authenticationUrl);

		HttpClientBuilder clientBuilder = HttpClients.custom();
		setProxy(clientBuilder);

		try {
			entity = new UrlEncodedFormEntity(parametersBody,
					SuccessFactorsConstants.UTF8);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception occured during Url encoding :{0} " + e);
			throw new ConnectorException(configuration.getMessage(
					SuccessFactorsConstants.EX_ENCODING, e));
		}

		httpRequest = new HttpPost(this.configuration.getAuthorizationUrl());
		((HttpPost) httpRequest).setEntity(entity);

		CloseableHttpResponse response = clientBuilder.build().execute(
				(HttpUriRequest) httpRequest);

		if (response.getEntity() != null) {
			samlResponse = EntityUtils.toString(response.getEntity());
		}

		parametersBody = getAccessTokenRequest(samlResponse);

		try {
			entity = new UrlEncodedFormEntity(parametersBody,
					SuccessFactorsConstants.UTF8);
		} catch (UnsupportedEncodingException e) {
			log.error("Exception occured during Url encoding :{0} " + e);
			throw new ConnectorException(configuration.getMessage(
					SuccessFactorsConstants.EX_ENCODING, e));
		}

		httpRequest = new HttpPost(authenticationUrl);
		((HttpPost) httpRequest).setEntity(entity);

		CloseableHttpResponse cTokenResponse = clientBuilder.build().execute(
				(HttpUriRequest) httpRequest);

		if (cTokenResponse.getEntity() != null) {
			tokenResponse = EntityUtils.toString(cTokenResponse.getEntity());
		}

		Map<String, Object> tokenResponseMap = SuccessFactorsUtils
				.convertJsonToMap(tokenResponse, this.configuration);

		String accessToken = (String) tokenResponseMap
				.get(SuccessFactorsConstants.ACCESS_TOKEN);

		authHeaders.put(HttpHeaders.AUTHORIZATION,
				SuccessFactorsConstants.BEARER + accessToken);

		return authHeaders;
	}

	private String getPrivateKey(String fileName) throws IOException {
		InputStream is = null;
		StringBuilder builder = new StringBuilder();
		BufferedReader br = null;
		try {
			is = new FileInputStream(fileName);
			br = new BufferedReader(new InputStreamReader(is));
			boolean inKey = false;
			for (String line = br.readLine(); line != null; line = br
					.readLine()) {
				if (!inKey) {
					if (line.startsWith("-----BEGIN ")
							&& line.endsWith(" PRIVATE KEY-----")) {
						inKey = true;
					}
					continue;
				} else {
					if (line.startsWith("-----END ")
							&& line.endsWith(" PRIVATE KEY-----")) {
						inKey = false;
						break;
					}
					builder.append(line);
				}
			}
		} finally {
			if (is != null) {
				try {
					is.close();
				} catch (Exception ign) {
					log.error("Exception occurred while releasing input stream {0} "
							+ ign.getMessage());
					throw new ConnectorException(configuration.getMessage(
							SuccessFactorsConstants.EX_INPUT_STREAM_RELEASE,
							ign.getMessage()));
				}
			}
			if (br != null) {
				try {
					br.close();
				} catch (Exception ign) {
					log.error("Exception occurred while releasing buffered reader {0}"
							+ ign.getMessage());
					throw new ConnectorException(configuration.getMessage(
							SuccessFactorsConstants.EX_BUFFERED_READER_RELEASE,
							ign.getMessage()));
				}
			}
		}
		return builder.toString();
	}

	private List<BasicNameValuePair> getSamlAssertionRequest(String privateKey,
			String authenticationUrl) {

		List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();

		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.AUTH_CLIENT_ID, this.configuration
						.getClientId()));
		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.PRIVATE_KEY_PARAM, privateKey));
		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.USER_ID_PARAM, this.configuration
						.getUsername()));
		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.TOKEN_URL_PARAM, authenticationUrl));
		return parametersBody;
	}

	private List<BasicNameValuePair> getAccessTokenRequest(String assertion) {

		List<BasicNameValuePair> parametersBody = new ArrayList<BasicNameValuePair>();

		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.GRANT_TYPE, this.configuration
						.getGrantType()));
		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.AUTH_CLIENT_ID, this.configuration
						.getClientId()));
		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.COMPANY_ID_PARAM, this.configuration
						.getCompanyId()));
		parametersBody.add(new BasicNameValuePair(
				SuccessFactorsConstants.ASSERTION, assertion));
		return parametersBody;
	}

    private void setProxy(HttpClientBuilder clientBuilder) {
        log.info("setting  proxy");
        log.ok(METHOD_ENTERED);
        final CredentialsProvider credsProvider = new BasicCredentialsProvider();
        if (!StringUtil.isBlank(this.configuration.getProxyHost())
                && this.configuration.getProxyPort() > 0) {
            HttpHost proxy = new HttpHost(this.configuration.getProxyHost(),
                    this.configuration.getProxyPort());
            HttpRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);

            if (this.configuration.getProxyUser() != null
                    && this.configuration.getProxyPassword() != null) {
                
                credsProvider.setCredentials(
                        new AuthScope(this.configuration.getProxyHost(),
                               this.configuration.getProxyPort()),

                        new UsernamePasswordCredentials(this.configuration
                                .getProxyUser(), SuccessFactorsUtils
                                .decryptPassword(this.configuration
                                        .getProxyPassword())));

                clientBuilder.setDefaultCredentialsProvider(credsProvider);

            }
            clientBuilder.setRoutePlanner(routePlanner);
        } else {
            log.info("proxy not set");
            return;
        }
        log.info("proxy set");
        log.ok(METHOD_EXITING);
    }

	public void disposeConnection() {
		log.info("disposing connection");
		try {
			this.client.close();
		} catch (IOException e) {
			log.error("Exception occured while closing the connection");
			throw new ConnectorException(this.configuration.getMessage(
					SuccessFactorsConstants.EX_RESOURCE_CLOSE_FAIL,
					"Exception occurred while closing the connection")
					+ " " + e.getMessage(), e);
		}
	}

}
