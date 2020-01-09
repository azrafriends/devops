/* $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsFilterTranslator.java /main/2 2017/08/11 06:22:42 samelgir Exp $ */

/* Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.*/

/*
   DESCRIPTION
    <short description of component this file declares/defines>

   PRIVATE CLASSES
    <list of private classes defined - with one-line descriptions>

   NOTES
    <other useful comments, qualifications, etc.>

   MODIFIED    (MM/DD/YY)
    samelgir    02/08/17 - Creation
 */

/**
 *  @version $Header: idc/bundles/java/successfactors/src/main/java/org/identityconnectors/successfactors/SuccessFactorsFilterTranslator.java /main/2 2017/08/11 06:22:42 samelgir Exp $
 *  @author  samelgir
 *  @since   release specific (what release of product did this appear in)
 */
package org.identityconnectors.successfactors;

import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;

public class SuccessFactorsFilterTranslator extends
		AbstractFilterTranslator<String> {

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String createGreaterThanExpression(
			GreaterThanFilter filter, boolean not) {
		return AttributeUtil.getAsStringValue(filter.getAttribute());
	}
}
