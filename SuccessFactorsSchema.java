/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsSchema.java /main/1 2017/02/16 22:40:02 samelgir Exp $ */

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
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsSchema.java /main/1 2017/02/16 22:40:02 samelgir Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */
package org.identityconnectors.successfactors;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.AttributeInfoUtil;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributeInfos;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.successfactors.utils.SuccessFactorsConstants;

public class SuccessFactorsSchema implements SuccessFactorsConstants {

	private static final Log LOG = Log.getLog(SuccessFactorsSchema.class);
	private static Schema _schema;
	private static Map<String, AttributeInfo> _accountAttributeMap;
	private static Set<String> _accountAttributeNames;

	/**
	 * private constructor, not used, everything is static
	 */
	public SuccessFactorsSchema() {
	}

	public Map<String, AttributeInfo> getAccountAttributeMap() {
		return _accountAttributeMap;
	}

	public Set<String> getAccountAttributeNames() {
		return _accountAttributeNames;
	}

	public static Schema getSchema() {
		final SchemaBuilder schemaBuilder = new SchemaBuilder(
				SuccessFactorsConnector.class);

		Set<AttributeInfo> acctAttributes = new HashSet<AttributeInfo>();

		// OPERATIONAL ATTRIBUTES
		acctAttributes.add(AttributeInfoBuilder.build(Name.NAME, String.class,
				EnumSet.of(Flags.REQUIRED))); // required
		acctAttributes.add(AttributeInfoBuilder.build(
				OperationalAttributes.PASSWORD_NAME, GuardedString.class,
				EnumSet.of(Flags.NOT_READABLE, Flags.NOT_RETURNED_BY_DEFAULT,
						Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				OperationalAttributes.CURRENT_PASSWORD_NAME,
				GuardedString.class,
				EnumSet.of(Flags.NOT_READABLE, Flags.NOT_RETURNED_BY_DEFAULT)));
		acctAttributes.add(AttributeInfoBuilder.build(
				OperationalAttributes.PASSWORD_EXPIRED_NAME, boolean.class,
				EnumSet.of(Flags.NOT_RETURNED_BY_DEFAULT)));
		acctAttributes.add(OperationalAttributeInfos.ENABLE);
		acctAttributes.add(OperationalAttributeInfos.LOCK_OUT);

		// ACCOUNT ATTRIBUTES
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_USERNAME), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_EMAIL), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_STATUS), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_COUNTRY), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_HR), String.class));

		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_DEPARTMENT), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_TIMEZONE), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_JOBLEVEL), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_MARRIED), String.class));

		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_CITY), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_STATE), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_DIVISION), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_CITIZENSHIP), String.class));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(USERENTITY, ATTR_LOCATION), String.class));

		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(EMPEMPLOYMENT, STARTDATE), String.class,
				EnumSet.of(Flags.REQUIRED)));

		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(EMPJOB, ATTR_JOBCODE), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(EMPJOB, ATTR_EVENT_REASON), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(EMPJOB, ATTR_COMPANY), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(EMPJOB, ATTR_BUSINESS_UNIT), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(EMPJOB, ATTR_MANAGERID), String.class));

		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(PERPERSONAL, ATTR_FIRSTNAME), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(PERPERSONAL, ATTR_LASTNAME), String.class,
				EnumSet.of(Flags.REQUIRED)));
		acctAttributes.add(AttributeInfoBuilder.build(
				mergeWithPeriod(PERPERSONAL, ATTR_GENDER), String.class));

		_accountAttributeMap = AttributeInfoUtil.toMap(acctAttributes);
		_accountAttributeNames = _accountAttributeMap.keySet();

		// Account Object and its Info Builder

		ObjectClassInfoBuilder objcBuilder = new ObjectClassInfoBuilder();
		objcBuilder.setType(ObjectClass.ACCOUNT_NAME);
		objcBuilder.addAllAttributeInfo(acctAttributes);
		ObjectClassInfo oci = objcBuilder.build();
		schemaBuilder.defineObjectClass(oci);

		schemaBuilder.clearSupportedObjectClassesByOperation();
		schemaBuilder.addSupportedObjectClass(SchemaOp.class, oci);
		schemaBuilder.addSupportedObjectClass(CreateOp.class, oci);
		schemaBuilder.addSupportedObjectClass(SearchOp.class, oci);
		schemaBuilder.addSupportedObjectClass(TestOp.class, oci);

		_schema = schemaBuilder.build();

		LOG.info("RETURN");
		return _schema;
	}

	public static String mergeWithPeriod(String s1, String... values) {

		StringBuilder sb = new StringBuilder();
		sb.append(s1);
		if (values != null) {
			for (String s : values) {
				sb.append(DELIMITER);
				sb.append(s);
			}
		}

		return sb.toString();
	}
}
