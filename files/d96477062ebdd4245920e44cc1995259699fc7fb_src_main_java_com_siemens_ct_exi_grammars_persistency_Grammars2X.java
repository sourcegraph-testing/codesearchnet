package com.siemens.ct.exi.grammars.persistency;

/*
 * Copyright (c) 2007-2017 Siemens AG
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.  IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * 
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import com.siemens.ct.exi.core.Constants;
import com.siemens.ct.exi.core.context.GrammarContext;
import com.siemens.ct.exi.core.context.GrammarUriContext;
import com.siemens.ct.exi.core.context.QNameContext;
import com.siemens.ct.exi.core.datatype.BinaryBase64Datatype;
import com.siemens.ct.exi.core.datatype.BinaryHexDatatype;
import com.siemens.ct.exi.core.datatype.BooleanDatatype;
import com.siemens.ct.exi.core.datatype.BooleanFacetDatatype;
import com.siemens.ct.exi.core.datatype.Datatype;
import com.siemens.ct.exi.core.datatype.DatetimeDatatype;
import com.siemens.ct.exi.core.datatype.DecimalDatatype;
import com.siemens.ct.exi.core.datatype.EnumerationDatatype;
import com.siemens.ct.exi.core.datatype.FloatDatatype;
import com.siemens.ct.exi.core.datatype.IntegerDatatype;
import com.siemens.ct.exi.core.datatype.ListDatatype;
import com.siemens.ct.exi.core.datatype.NBitUnsignedIntegerDatatype;
import com.siemens.ct.exi.core.datatype.RestrictedCharacterSetDatatype;
import com.siemens.ct.exi.core.datatype.StringDatatype;
import com.siemens.ct.exi.core.datatype.UnsignedIntegerDatatype;
import com.siemens.ct.exi.core.datatype.charset.CodePointCharacterSet;
import com.siemens.ct.exi.core.datatype.charset.RestrictedCharacterSet;
import com.siemens.ct.exi.core.exceptions.EXIException;
import com.siemens.ct.exi.core.grammars.SchemaInformedGrammars;
import com.siemens.ct.exi.core.grammars.event.Attribute;
import com.siemens.ct.exi.core.grammars.event.AttributeGeneric;
import com.siemens.ct.exi.core.grammars.event.AttributeNS;
import com.siemens.ct.exi.core.grammars.event.Characters;
import com.siemens.ct.exi.core.grammars.event.CharactersGeneric;
import com.siemens.ct.exi.core.grammars.event.DatatypeEvent;
import com.siemens.ct.exi.core.grammars.event.EndDocument;
import com.siemens.ct.exi.core.grammars.event.EndElement;
import com.siemens.ct.exi.core.grammars.event.Event;
import com.siemens.ct.exi.core.grammars.event.EventType;
import com.siemens.ct.exi.core.grammars.event.StartDocument;
import com.siemens.ct.exi.core.grammars.event.StartElement;
import com.siemens.ct.exi.core.grammars.event.StartElementGeneric;
import com.siemens.ct.exi.core.grammars.event.StartElementNS;
import com.siemens.ct.exi.core.grammars.grammar.DocEnd;
import com.siemens.ct.exi.core.grammars.grammar.Document;
import com.siemens.ct.exi.core.grammars.grammar.Fragment;
import com.siemens.ct.exi.core.grammars.grammar.Grammar;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedDocContent;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedElement;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedFirstStartTag;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedFirstStartTagGrammar;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedFragmentContent;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedGrammar;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedStartTag;
import com.siemens.ct.exi.core.grammars.grammar.SchemaInformedStartTagGrammar;
import com.siemens.ct.exi.core.grammars.production.Production;
import com.siemens.ct.exi.core.types.BuiltIn;
import com.siemens.ct.exi.core.types.DateTimeType;
import com.siemens.ct.exi.core.values.AbstractBinaryValue;
import com.siemens.ct.exi.core.values.BinaryBase64Value;
import com.siemens.ct.exi.core.values.BinaryHexValue;
import com.siemens.ct.exi.core.values.BooleanValue;
import com.siemens.ct.exi.core.values.DateTimeValue;
import com.siemens.ct.exi.core.values.DecimalValue;
import com.siemens.ct.exi.core.values.FloatValue;
import com.siemens.ct.exi.core.values.IntegerValue;
import com.siemens.ct.exi.core.values.StringValue;
import com.siemens.ct.exi.core.values.Value;
import com.siemens.ct.exi.grammars.XSDGrammarsBuilder;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics.DateAndTime;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics.Integer.NBitUnsignedInteger;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.ExiGrammars;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.GrammarType;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.NamespaceContext;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.NamespaceContext.QnameContext;
import com.siemens.ct.exi.grammars._2017.schemaforgrammars.ObjectFactory;

public class Grammars2X {

	static ObjectFactory of = new ObjectFactory();

	public static final boolean STATS_ON = true;

	protected int statsCountTransitions = 0;
	protected int statsCountStates = 0;

	List<Datatype> listOfSimpleDatatypes = new ArrayList<Datatype>();

	GrammarsPreperation gpreps = new GrammarsPreperation();

	static Class<ExiGrammars> CLASS = ExiGrammars.class;

	static JAXBContext jc;

	public Grammars2X() {
		super();
	}

	protected void clear() {
		listOfSimpleDatatypes.clear();
		gpreps.clear();
	}

	private static JAXBContext getJAXBContext() throws JAXBException {
		if (jc == null) {
			jc = JAXBContext.newInstance(CLASS);
		}
		return jc;
	}

	public static void marshal(ExiGrammars exiGrammars, Result result)
			throws JAXBException {
		Marshaller m = getJAXBContext().createMarshaller();
		// m.setProperty("jaxb.formatted.output", Boolean.TRUE);
		m.marshal(exiGrammars, result);
	}

	public static void marshal(ExiGrammars exiGrammars,
			org.xml.sax.ContentHandler handler) throws JAXBException {
		Marshaller m = getJAXBContext().createMarshaller();
		m.marshal(exiGrammars, handler);
	}

	public static ExiGrammars unmarshal(InputStream inputStream)
			throws JAXBException {
		Unmarshaller u = getJAXBContext().createUnmarshaller();
		Object o = u.unmarshal(inputStream);
		if (o instanceof ExiGrammars) {
			return (ExiGrammars) o;
		}
		throw new JAXBException("Unmarshalled object not of instance " + CLASS
				+ ". Instead " + o.getClass());
	}

	public static ExiGrammars unmarshal(javax.xml.transform.Source source)
			throws JAXBException {
		Unmarshaller u = getJAXBContext().createUnmarshaller();
		Object o = u.unmarshal(source);
		if (o instanceof ExiGrammars) {
			return (ExiGrammars) o;
		}
		throw new JAXBException("Unmarshalled object not of instance " + CLASS
				+ ". Instead " + o.getClass());
	}

	public static ExiGrammars unmarshal(javax.xml.stream.XMLStreamReader reader)
			throws JAXBException {
		Unmarshaller u = getJAXBContext().createUnmarshaller();
		Object o = u.unmarshal(reader);
		if (o instanceof ExiGrammars) {
			return (ExiGrammars) o;
		}
		throw new JAXBException("Unmarshalled object not of instance " + CLASS
				+ ". Instead " + o.getClass());
	}

	public ExiGrammars toGrammarsX(SchemaInformedGrammars grammars)
			throws IOException, EXIException, ParserConfigurationException,
			DatatypeConfigurationException {

		ExiGrammars exiGrammars = of.createExiGrammars();

		// clear
		clear();

		// prepare grammar rules
		gpreps.prepareGrammars(grammars);

		GrammarContext grammarContext = grammars.getGrammarContext();

		// Writer w = new OutputStreamWriter(os);

		for (int i = 0; i < gpreps.getNumberOfGrammars(); i++) {
			Grammar r = gpreps.getGrammar(i);
			for (int k = 0; k < r.getNumberOfEvents(); k++) {
				Production p = r.getProduction(k);
				Event e = p.getEvent();
				if (e instanceof DatatypeEvent) {
					DatatypeEvent de = (DatatypeEvent) e;
					// System.out.println(de.getDatatype());
					if (!listOfSimpleDatatypes.contains(de.getDatatype())
							&& !de.getDatatype().equals(
									BuiltIn.getDefaultDatatype())) {
						listOfSimpleDatatypes.add(de.getDatatype());
					}
					// any simple Type !??!
					Datatype base = de.getDatatype().getBaseDatatype();
					if (base != null && !listOfSimpleDatatypes.contains(base)) {
						listOfSimpleDatatypes.add(base);
					}
				}
			}
		}

		for (int i = 0; i < grammarContext.getNumberOfGrammarUriContexts(); i++) {
			GrammarUriContext guc = grammarContext.getGrammarUriContext(i);
			for (int k = 0; k < guc.getNumberOfQNames(); k++) {
				QNameContext qnc = guc.getQNameContext(k);
				// global type
				if (qnc.getTypeGrammar() != null) {
					SchemaInformedFirstStartTagGrammar typeGrammar = qnc
							.getTypeGrammar();
					if (typeGrammar.getNumberOfEvents() == 1
							&& typeGrammar.getProduction(0).getEvent()
									.isEventType(EventType.CHARACTERS)) {
						// simple type grammar
						Characters chev = (Characters) typeGrammar
								.getProduction(0).getEvent();
						if (!listOfSimpleDatatypes.contains(chev.getDatatype())) {
							listOfSimpleDatatypes.add(chev.getDatatype());
						}
					}
				}
			}
		}

		{
			/*
			 * QNames, NameTables
			 */
			exiGrammars.setQnames(new ExiGrammars.Qnames());

			for (int i = 0; i < grammarContext.getNumberOfGrammarUriContexts(); i++) {

				GrammarUriContext guc = grammarContext.getGrammarUriContext(i);

				NamespaceContext namespaceContext = new NamespaceContext();
				exiGrammars.getQnames().getNamespaceContext()
						.add(namespaceContext);

				// namespaceUri
				namespaceContext.setNamespaceURI(guc.getNamespaceUri());

				List<QnameContext> qnameContexts = namespaceContext
						.getQnameContext();

				// qnames
				for (int k = 0; k < guc.getNumberOfQNames(); k++) {
					QNameContext qnc = guc.getQNameContext(k);
					{
						QnameContext qnameContext = new QnameContext();
						qnameContexts.add(qnameContext);

						// local-name
						qnameContext.setLocalName(qnc.getLocalName());

						// global type
						if (qnc.getTypeGrammar() != null) {
							SchemaInformedFirstStartTagGrammar typeGrammar = qnc
									.getTypeGrammar();
							if (typeGrammar.getNumberOfEvents() == 1
									&& typeGrammar.getProduction(0).getEvent()
											.isEventType(EventType.CHARACTERS)) {
								// simple type grammar
								Characters chev = (Characters) typeGrammar
										.getProduction(0).getEvent();
								qnameContext
										.setGlobalSimpleTypeDatatypeID(getDatatypeIndex(
												chev.getDatatype(), false));
							} else {
								// complex type grammar
								long gid = gpreps.getGrammarID(qnc
										.getTypeGrammar());
								qnameContext.setGlobalComplexTypeGrammarID(gid);
							}
						}

						// global element
						if (qnc.getGlobalStartElement() != null) {
							StartElement se = qnc.getGlobalStartElement();
							long l = gpreps.getGrammarID(se.getGrammar());
							qnameContext.setGlobalElementGrammarID(l);
						}

						// global attribute
						if (qnc.getGlobalAttribute() != null) {
							Attribute at = qnc.getGlobalAttribute();
							qnameContext
									.setGlobalAttributeDatatypeID(getDatatypeIndex(
											at.getDatatype(), false));
						}

					}

				}
			}

			/*
			 * Datatypes
			 */
			ExiGrammars.SimpleDatatypes simpleDatatypes = new ExiGrammars.SimpleDatatypes();
			exiGrammars.setSimpleDatatypes(simpleDatatypes);

			for (int i = 0; i < listOfSimpleDatatypes.size(); i++) {
				Datatype dt = listOfSimpleDatatypes.get(i);
				// simpleDatatype
				{
					// datatype
					com.siemens.ct.exi.grammars._2017.schemaforgrammars.Datatype d = of
							.createDatatype();
					setDatatype(d, dt);
					simpleDatatypes.getSimpleDatatype().add(d);

					// schema type
					{
						QNameContext qncSchemaType = dt.getSchemaType();
						long localNameID = qncSchemaType.getLocalNameID();
						d.setSchemaTypeLocalNameID(localNameID);
						long namespaceUriID = qncSchemaType.getNamespaceUriID();
						d.setSchemaTypeNamespaceID(namespaceUriID);

						// schema base type
						if (dt.getBaseDatatype() != null) {
							long baseDatatypeID = listOfSimpleDatatypes
									.indexOf(dt.getBaseDatatype());
							if (baseDatatypeID < 0) {
								//
							} else {
								d.setBaseDatatypeID(baseDatatypeID);
							}
						}

					}
				}
			}

			/*
			 * Grammar Rules
			 */
			ExiGrammars.Grammars grs = new ExiGrammars.Grammars();
			long documentGrammarID = gpreps.getGrammarID(grammars
					.getDocumentGrammar());
			grs.setDocumentGrammarID(documentGrammarID);
			long fragmentGrammarID = gpreps.getGrammarID(grammars
					.getFragmentGrammar());
			grs.setFragmentGrammarID(fragmentGrammarID);

			long elementFragmentGrammarID = gpreps.getGrammarID(grammars
					.getSchemaInformedElementFragmentGrammar());
			grs.setElementFragmentGrammarID(elementFragmentGrammarID);

			exiGrammars.setGrammars(grs);

			if (grammars.isBuiltInXMLSchemaTypesOnly()) {
				grs.setIsBuiltInXMLSchemaTypesOnly(of
						.createExiGrammarsGrammarsIsBuiltInXMLSchemaTypesOnly());
			}

			for (int i = 0; i < gpreps.getNumberOfGrammars(); i++) {
				Grammar r = gpreps.getGrammar(i);
				this.printGrammar((SchemaInformedGrammar) r, grs);
			}

		}

		return exiGrammars;
	}

	private static void setDatatypeBasics(DatatypeBasics d, Datatype dt) {
		switch (dt.getBuiltInType()) {
		case BINARY_BASE64:
			d.setBase64Binary(of.createDatatypeBasicsBase64Binary());
			break;
		case BINARY_HEX:
			d.setHexBinary(of.createDatatypeBasicsHexBinary());
			break;
		case BOOLEAN:
			d.setBoolean(of.createDatatypeBasicsBoolean());
			break;
		case BOOLEAN_FACET:
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics.Boolean b = of
					.createDatatypeBasicsBoolean();
			b.setPatternFacet(of.createDatatypeBasicsBooleanPatternFacet());
			d.setBoolean(b);
			break;
		case DECIMAL:
			d.setDecimal(of.createDatatypeBasicsDecimal());
			break;
		case FLOAT:
			d.setDouble(of.createDatatypeBasicsDouble());
			break;
		case NBIT_UNSIGNED_INTEGER:
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics.Integer in = of
					.createDatatypeBasicsInteger();
			NBitUnsignedInteger nb = of
					.createDatatypeBasicsIntegerNBitUnsignedInteger();
			in.setNBitUnsignedInteger(nb);
			NBitUnsignedIntegerDatatype nbit = (NBitUnsignedIntegerDatatype) dt;
			nb.setLowerBound(nbit.getLowerBound().bigIntegerValue());
			nb.setUpperBound(nbit.getUpperBound().bigIntegerValue());
			d.setInteger(in);
			break;
		case UNSIGNED_INTEGER:
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics.Integer inu = of
					.createDatatypeBasicsInteger();
			inu.setUnsignedInteger(of
					.createDatatypeBasicsIntegerUnsignedInteger());
			d.setInteger(inu);
			break;
		case INTEGER:
			d.setInteger(of.createDatatypeBasicsInteger());
			break;
		case DATETIME:
			DateAndTime dat = of.createDatatypeBasicsDateAndTime();
			// subtypes
			DatetimeDatatype dtdt = (DatetimeDatatype) dt;
			switch (dtdt.getDatetimeType()) {
			case gYear:
				dat.setGYear(of.createDatatypeBasicsDateAndTimeGYear());
				break;
			case gYearMonth:
				dat.setGYearMonth(of
						.createDatatypeBasicsDateAndTimeGYearMonth());
				break;
			case date:
				dat.setDate(of.createDatatypeBasicsDateAndTimeDate());
				break;
			case dateTime:
				dat.setDateTime(of.createDatatypeBasicsDateAndTimeDateTime());
				break;
			case gMonth:
				dat.setGMonth(of.createDatatypeBasicsDateAndTimeGMonth());
				break;
			case gMonthDay:
				dat.setGMonthDay(of.createDatatypeBasicsDateAndTimeGMonthDay());
				break;
			case gDay:
				dat.setGDay(of.createDatatypeBasicsDateAndTimeGDay());
				break;
			case time:
				dat.setTime(of.createDatatypeBasicsDateAndTimeTime());
				break;
			}
			d.setDateAndTime(dat);
			break;
		case STRING:
			d.setString(of.createDatatypeBasicsString());
			break;
		case RCS_STRING:
			RestrictedCharacterSetDatatype rcsdt = (RestrictedCharacterSetDatatype) dt;
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.DatatypeBasics.String s = of
					.createDatatypeBasicsString();
			// RCS
			RestrictedCharacterSet rcs = rcsdt.getRestrictedCharacterSet();
			for (int i = 0; i < rcs.size(); i++) {
				int cp = rcs.getCodePoint(i);
				s.getRestrictedCharSet().add((long) cp);
			}

			d.setString(s);
			break;
		default:
			throw new RuntimeException("Unsupported basic datatype: "
					+ dt.getBuiltInType());
		}
	}

	private static void setDatatype(
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Datatype d,
			Datatype dt) throws DatatypeConfigurationException {
		switch (dt.getBuiltInType()) {
		case BINARY_BASE64:
		case BINARY_HEX:
		case BOOLEAN:
		case BOOLEAN_FACET:
		case DECIMAL:
		case FLOAT:
		case NBIT_UNSIGNED_INTEGER:
		case UNSIGNED_INTEGER:
		case INTEGER:
		case DATETIME:
		case STRING:
		case RCS_STRING:
			setDatatypeBasics(d, dt);
			break;
		case ENUMERATION:
			EnumerationDatatype endt = (EnumerationDatatype) dt;
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Enumeration en = of
					.createEnumeration();
			// enum value type
			Datatype enumDT = endt.getEnumValueDatatype();
			DatatypeBasics enumdb = of.createDatatypeBasics();
			setDatatypeBasics(enumdb, enumDT);
			en.setEnumerationValueDatatype(enumdb);
			switch (enumDT.getBuiltInType()) {
			case BINARY_BASE64:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					AbstractBinaryValue v = (AbstractBinaryValue) endt
							.getEnumValue(i);
					en.getBase64BinaryValue().add(v.toBytes());
				}
				break;
			case BINARY_HEX:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					AbstractBinaryValue v = (AbstractBinaryValue) endt
							.getEnumValue(i);
					en.getBase64BinaryValue().add(v.toBytes());
				}
				break;
			case BOOLEAN:
			case BOOLEAN_FACET:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					BooleanValue v = (BooleanValue) endt.getEnumValue(i);
					en.getBooleanValue().add(v.toBoolean());
				}
				break;
			case DATETIME:
				DatetimeDatatype ddt = (DatetimeDatatype) enumDT;
				switch (ddt.getDatetimeType()) {
				case gYear:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getGYearValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case gYearMonth:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getGYearMonthValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case date:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getDateValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case dateTime:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getDateTimeValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case gMonth:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getGMonthValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case gMonthDay:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getGMonthDayValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case gDay:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getGDayValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				case time:
					for (int i = 0; i < endt.getEnumerationSize(); i++) {
						DateTimeValue v = (DateTimeValue) endt.getEnumValue(i);
						GregorianCalendar gc = new GregorianCalendar();
						gc.setTime(v.toCalendar().getTime());
						en.getTimeValue().add(
								DatatypeFactory.newInstance()
										.newXMLGregorianCalendar(gc));
					}
					break;
				}
				break;
			case DECIMAL:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					DecimalValue v = (DecimalValue) endt.getEnumValue(i);
					en.getDecimalValue().add(v.toBigDecimal());
				}
				break;
			case FLOAT:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					FloatValue v = (FloatValue) endt.getEnumValue(i);
					en.getFloatValue().add(Double.valueOf(v.toDouble()));
				}
				break;
			case INTEGER:
			case UNSIGNED_INTEGER:
			case NBIT_UNSIGNED_INTEGER:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					IntegerValue v = (IntegerValue) endt.getEnumValue(i);
					en.getIntegerValue().add(v.bigIntegerValue());
				}
				break;
			case STRING:
				for (int i = 0; i < endt.getEnumerationSize(); i++) {
					StringValue v = (StringValue) endt.getEnumValue(i);
					en.getStringValue().add(v.toString());
				}
				break;
			default:
				throw new RuntimeException("Datatype "
						+ enumDT.getBuiltInType()
						+ " for Enumeration not supported");
			}

			endt.getEnumValueDatatype();

			d.setEnumeration(en);
			break;
		case LIST:
			ListDatatype ldt = (ListDatatype) dt;
			// list datatype
			setDatatype(d, ldt.getListDatatype());
			d.setList(of.createDatatypeList());
			break;
		default:
			throw new RuntimeException("Unsupported datatype: "
					+ dt.getBuiltInType());
		}
	}

	protected void printGrammar(SchemaInformedGrammar sir,
			ExiGrammars.Grammars grs) throws IOException, EXIException {

		com.siemens.ct.exi.grammars._2017.schemaforgrammars.ExiGrammars.Grammars.Grammar g = new com.siemens.ct.exi.grammars._2017.schemaforgrammars.ExiGrammars.Grammars.Grammar();
		grs.getGrammar().add(g);

		if (sir instanceof SchemaInformedFirstStartTagGrammar) {
			SchemaInformedFirstStartTagGrammar fst = (SchemaInformedFirstStartTagGrammar) sir;

			g.setGrammarType(GrammarType.FIRST_START_TAG_CONTENT);
			g.setElementContentGrammarID(Long.valueOf(gpreps.getGrammarID(fst
					.getElementContentGrammar())));
			if (fst.isTypeCastable()) {
				g.setIsTypeCastable(of
						.createExiGrammarsGrammarsGrammarIsTypeCastable());
			}
			if (fst.isNillable()) {
				g.setIsNillable(of.createExiGrammarsGrammarsGrammarIsNillable());
			}
		} else if (sir instanceof SchemaInformedStartTagGrammar) {
			SchemaInformedStartTagGrammar st = (SchemaInformedStartTagGrammar) sir;
			g.setGrammarType(GrammarType.START_TAG_CONTENT);
			g.setElementContentGrammarID(Long.valueOf(gpreps.getGrammarID(st
					.getElementContentGrammar())));
		} else if (sir instanceof SchemaInformedElement) {
			g.setGrammarType(GrammarType.ELEMENT_CONTENT);
		} else if (sir instanceof Document) {
			g.setGrammarType(GrammarType.DOCUMENT);
		} else if (sir instanceof SchemaInformedDocContent) {
			g.setGrammarType(GrammarType.DOC_CONTENT);
		} else if (sir instanceof DocEnd) {
			g.setGrammarType(GrammarType.DOC_END);
		} else if (sir instanceof Fragment) {
			g.setGrammarType(GrammarType.FRAGMENT);
		} else if (sir instanceof SchemaInformedFragmentContent) {
			g.setGrammarType(GrammarType.FRAGMENT_CONTENT);
		} else {
			throw new RuntimeException("Unkown Rule type: " + sir);
		}

		if (STATS_ON) {
			statsCountStates++;
		}

		printGrammarProduction(sir, g.getProduction());
	}

	/**
	 * Get a datatype index from a Datatype instance using the
	 * listOfSimpleDatatypes, or null if this is the default datatype and
	 * {@code useNullAsDefaultDatatype} is true.
	 * 
	 * @param datatype
	 *            The datatype to get the index from.
	 * @param useNullAsDefaultDatatype
	 *            Tells is it should return null for the built-in default
	 *            datatype.
	 * @return The index of the datatype.
	 * @throws EXIException
	 *             if the datatype can't be found in listOfSimpleDatatypes.
	 */
	private Long getDatatypeIndex(Datatype datatype,
			boolean useNullAsDefaultDatatype) throws EXIException {
		if (datatype.equals(BuiltIn.getDefaultDatatype())
				&& useNullAsDefaultDatatype) {
			return null;
		}
		int datatypeIndex = listOfSimpleDatatypes.indexOf(datatype);
		if (datatypeIndex < 0) {
			throw new EXIException("Can't find datatype: " + datatype);
		}
		return Long.valueOf(datatypeIndex);
	}

	/**
	 * Get the datatype instance from an index in the datatypes array, or get
	 * the built-in default datatype if the {@code index} is null and
	 * {@code useNullAsDefaultDatatype} is true.
	 * 
	 * @param index
	 *            The index of the datatype.
	 * @param datatypes
	 *            The array of datatypes.
	 * @param useNullAsDefaultDatatype
	 *            Tells is it should return null for the built-in default
	 *            parameter.
	 * @return The datatype instance.
	 * @throws EXIException
	 *             if the index is out of bounds from the datatypes array.
	 */
	private static Datatype getDatatype(Long index, Datatype[] datatypes,
			boolean useNullAsDefaultDatatype) throws EXIException {
		if (index == null && useNullAsDefaultDatatype) {
			return BuiltIn.getDefaultDatatype();
		}
		if (index == null || index >= datatypes.length || index < 0) {
			throw new EXIException("Can't find datatype of index: " + index);
		}
		return datatypes[index.intValue()];
	}

	protected void printGrammarProduction(
			SchemaInformedGrammar sir,
			List<com.siemens.ct.exi.grammars._2017.schemaforgrammars.Production> productions)
			throws IOException, EXIException {

		for (int i = 0; i < sir.getNumberOfEvents(); i++) {
			if (STATS_ON) {
				statsCountTransitions++;
			}

			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Production p = new com.siemens.ct.exi.grammars._2017.schemaforgrammars.Production();
			productions.add(p);

			Production ei = sir.getProduction(i);
			Event event = ei.getEvent();
			EventType eventType = event.getEventType();
			switch (eventType) {
			case START_DOCUMENT:
				p.setStartDocument(of.createProductionStartDocument());
				break;
			case END_DOCUMENT:
				p.setEndDocument(of.createProductionEndDocument());
				break;
			case START_ELEMENT:
				StartElement se = (StartElement) event;
				QNameContext seqname = se.getQNameContext();

				p.setStartElement(of.createProductionStartElement());

				p.getStartElement().setStartElementGrammarID(
						gpreps.getGrammarID(se.getGrammar()));
				p.getStartElement().setStartElementNamespaceID(
						seqname.getNamespaceUriID());
				p.getStartElement().setStartElementLocalNameID(
						seqname.getLocalNameID());
				break;
			case START_ELEMENT_NS:
				StartElementNS seNS = (StartElementNS) event;
				long pid = seNS.getNamespaceUriID();
				p.setStartElementNS(pid);
				break;
			case END_ELEMENT:
				p.setEndElement(of.createProductionEndElement());
				break;
			case ATTRIBUTE:
				Attribute at = (Attribute) event;
				QNameContext atqname = at.getQNameContext();

				p.setAttribute(of.createProductionAttribute());

				p.getAttribute().setAttributeDatatypeID(
						getDatatypeIndex(at.getDatatype(), true));
				p.getAttribute().setAttributeNamespaceID(
						atqname.getNamespaceUriID());
				p.getAttribute().setAttributeLocalNameID(
						atqname.getLocalNameID());
				break;
			case ATTRIBUTE_NS:
				AttributeNS atNS = (AttributeNS) event;
				long atid = atNS.getNamespaceUriID();
				p.setAttributeNS(atid);
				break;
			case CHARACTERS:
				Characters ch = (Characters) event;
				p.setCharacters(of.createProductionCharacters());
				p.getCharacters().setCharactersDatatypeID(
						getDatatypeIndex(ch.getDatatype(), false));
				break;
			case START_ELEMENT_GENERIC:
				p.setStartElementGeneric(of
						.createProductionStartElementGeneric());
				break;
			case ATTRIBUTE_GENERIC:
				p.setAttributeGeneric(of.createProductionAttributeGeneric());
				break;
			case CHARACTERS_GENERIC:
				p.setCharactersGeneric(of.createProductionCharactersGeneric());
				break;
			default:
				throw new RuntimeException("Unknown Event " + ei.getEvent());
			}

			// next state ID
			{
				Grammar nextRule = ei.getNextGrammar();
				if (nextRule != null) { // && nextRule.getNumberOfEvents() > 0
					long nextGrammarID = gpreps.getGrammarID(nextRule);
					p.setNextGrammarID(nextGrammarID);
				} else {
					// No events anymore, eg. EE, ED
				}

			}
		}
	}

	private static EnumerationDatatype getEnumerationDatatype(
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Enumeration en,
			QNameContext qnc) throws EXIException {
		Value[] enumValues;
		Datatype dtEnumValues;

		DatatypeBasics enumDT = en.getEnumerationValueDatatype();
		if (enumDT.getBase64Binary() != null) {
			dtEnumValues = new BinaryBase64Datatype(qnc);
			enumValues = new BinaryBase64Value[en.getBase64BinaryValue().size()];
			for (int k = 0; k < en.getBase64BinaryValue().size(); k++) {
				enumValues[k] = new BinaryBase64Value(en.getBase64BinaryValue()
						.get(k));
			}
		} else if (enumDT.getHexBinary() != null) {
			dtEnumValues = new BinaryHexDatatype(qnc);
			enumValues = new BinaryHexValue[en.getHexBinaryValue().size()];
			for (int k = 0; k < en.getHexBinaryValue().size(); k++) {
				enumValues[k] = new BinaryHexValue(en.getHexBinaryValue()
						.get(k));
			}
		} else if (enumDT.getBoolean() != null) {
			dtEnumValues = new BooleanDatatype(qnc);
			enumValues = new BooleanValue[en.getBooleanValue().size()];
			for (int k = 0; k < en.getBooleanValue().size(); k++) {
				enumValues[k] = en.getBooleanValue().get(k) ? BooleanValue.BOOLEAN_VALUE_TRUE
						: BooleanValue.BOOLEAN_VALUE_FALSE;
			}
		} else if (enumDT.getDateAndTime() != null) {
			DateTimeType dateType;
			if (enumDT.getDateAndTime().getDateTime() != null) {
				dateType = DateTimeType.dateTime;
				enumValues = new DateTimeValue[en.getDateTimeValue().size()];
				for (int k = 0; k < en.getDateTimeValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getDateTimeValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getTime() != null) {
				dateType = DateTimeType.time;
				enumValues = new DateTimeValue[en.getTimeValue().size()];
				for (int k = 0; k < en.getTimeValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getTimeValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getDate() != null) {
				dateType = DateTimeType.date;
				enumValues = new DateTimeValue[en.getDateValue().size()];
				for (int k = 0; k < en.getDateValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getDateValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getGYearMonth() != null) {
				dateType = DateTimeType.gYearMonth;
				enumValues = new DateTimeValue[en.getGYearMonthValue().size()];
				for (int k = 0; k < en.getGYearMonthValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getGYearMonthValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getGYear() != null) {
				dateType = DateTimeType.gYear;
				enumValues = new DateTimeValue[en.getGYearValue().size()];
				for (int k = 0; k < en.getGYearValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getGYearValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getGMonthDay() != null) {
				dateType = DateTimeType.gMonthDay;
				enumValues = new DateTimeValue[en.getGMonthDayValue().size()];
				for (int k = 0; k < en.getGMonthDayValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getGMonthDayValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getGDay() != null) {
				dateType = DateTimeType.gDay;
				enumValues = new DateTimeValue[en.getGDayValue().size()];
				for (int k = 0; k < en.getGDayValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getGDayValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else if (enumDT.getDateAndTime().getGMonth() != null) {
				dateType = DateTimeType.gMonth;
				enumValues = new DateTimeValue[en.getGMonthValue().size()];
				for (int k = 0; k < en.getGMonthValue().size(); k++) {
					XMLGregorianCalendar xmlgc = en.getGMonthValue().get(k);
					DateTimeValue dtv = DateTimeValue.parse(
							xmlgc.toGregorianCalendar(), dateType);
					enumValues[k] = dtv;
				}
			} else {
				throw new EXIException("Unsupported Enum DateAndTime datatype");
			}
			dtEnumValues = new DatetimeDatatype(dateType, qnc);
		} else if (enumDT.getDecimal() != null) {
			dtEnumValues = new DecimalDatatype(qnc);
			enumValues = new DecimalValue[en.getDecimalValue().size()];
			for (int k = 0; k < en.getDecimalValue().size(); k++) {
				enumValues[k] = DecimalValue.parse(en.getDecimalValue().get(k));
			}
		} else if (enumDT.getDouble() != null) {
			dtEnumValues = new FloatDatatype(qnc);
			enumValues = new FloatValue[en.getFloatValue().size()];
			for (int k = 0; k < en.getFloatValue().size(); k++) {
				enumValues[k] = FloatValue.parse(en.getFloatValue().get(k));
			}
		} else if (enumDT.getInteger() != null) {
			dtEnumValues = new IntegerDatatype(qnc);
			enumValues = new IntegerValue[en.getIntegerValue().size()];
			for (int k = 0; k < en.getIntegerValue().size(); k++) {
				enumValues[k] = IntegerValue.valueOf(en.getIntegerValue()
						.get(k));
			}
		} else if (enumDT.getString() != null) {
			dtEnumValues = new StringDatatype(qnc);
			enumValues = new StringValue[en.getStringValue().size()];
			for (int k = 0; k < en.getStringValue().size(); k++) {
				enumValues[k] = new StringValue(en.getStringValue().get(k));
			}
		} else {
			throw new EXIException("Unsupported Enumeration type for " + enumDT);
		}

		EnumerationDatatype endt = new EnumerationDatatype(enumValues,
				dtEnumValues, qnc);
		return endt;
	}

	private static Datatype getBasicDatatype(
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Datatype dt,
			QNameContext qnc) throws EXIException {
		Datatype datatype;
		if (dt.getBase64Binary() != null) {
			datatype = new BinaryBase64Datatype(qnc);
		} else if (dt.getHexBinary() != null) {
			datatype = new BinaryHexDatatype(qnc);
		} else if (dt.getBoolean() != null) {
			if (dt.getBoolean().getPatternFacet() != null) {
				datatype = new BooleanFacetDatatype(qnc);
			} else {
				datatype = new BooleanDatatype(qnc);
			}
		} else if (dt.getDateAndTime() != null) {
			if (dt.getDateAndTime().getDateTime() != null) {
				datatype = new DatetimeDatatype(DateTimeType.dateTime, qnc);
			} else if (dt.getDateAndTime().getTime() != null) {
				datatype = new DatetimeDatatype(DateTimeType.time, qnc);
			} else if (dt.getDateAndTime().getDate() != null) {
				datatype = new DatetimeDatatype(DateTimeType.date, qnc);
			} else if (dt.getDateAndTime().getGYearMonth() != null) {
				datatype = new DatetimeDatatype(DateTimeType.gYearMonth, qnc);
			} else if (dt.getDateAndTime().getGYear() != null) {
				datatype = new DatetimeDatatype(DateTimeType.gYear, qnc);
			} else if (dt.getDateAndTime().getGMonthDay() != null) {
				datatype = new DatetimeDatatype(DateTimeType.gMonthDay, qnc);
			} else if (dt.getDateAndTime().getGDay() != null) {
				datatype = new DatetimeDatatype(DateTimeType.gDay, qnc);
			} else if (dt.getDateAndTime().getGMonth() != null) {
				datatype = new DatetimeDatatype(DateTimeType.gMonth, qnc);
			} else {
				throw new EXIException("Unsupported DateAndTime datatype: "
						+ dt.getDateAndTime());
			}
		} else if (dt.getDecimal() != null) {
			datatype = new DecimalDatatype(qnc);
		} else if (dt.getDouble() != null) {
			datatype = new FloatDatatype(qnc);
		} else if (dt.getInteger() != null) {
			if (dt.getInteger().getNBitUnsignedInteger() != null) {
				IntegerValue lowerBound = IntegerValue.valueOf(dt.getInteger()
						.getNBitUnsignedInteger().getLowerBound());
				IntegerValue upperBound = IntegerValue.valueOf(dt.getInteger()
						.getNBitUnsignedInteger().getUpperBound());
				datatype = new NBitUnsignedIntegerDatatype(lowerBound,
						upperBound, qnc);
			} else if (dt.getInteger().getUnsignedInteger() != null) {
				datatype = new UnsignedIntegerDatatype(qnc);
			} else {
				datatype = new IntegerDatatype(qnc);
			}
		} else if (dt.getString() != null) {
			if (dt.getString().getRestrictedCharSet() != null
					&& dt.getString().getRestrictedCharSet().size() > 0) {
				Iterator<Long> iter = dt.getString().getRestrictedCharSet()
						.iterator();
				Set<Integer> codePoints = new HashSet<Integer>();
				while (iter.hasNext()) {
					codePoints.add(iter.next().intValue());
				}
				RestrictedCharacterSet rcs = new CodePointCharacterSet(
						codePoints);
				datatype = new RestrictedCharacterSetDatatype(rcs, qnc);
			} else {
				datatype = new StringDatatype(qnc);
			}
		} else {
			throw new EXIException("Unsupported datatype: " + dt);
		}

		return datatype;
	}

	public static SchemaInformedGrammars toGrammars(ExiGrammars exiGrammars)
			throws EXIException {
		// GrammarContext
		ExiGrammars.Qnames qnames = exiGrammars.getQnames();
		GrammarUriContext[] grammarUriContexts = new GrammarUriContext[qnames
				.getNamespaceContext().size()];
		int numberofQNamesContexts = 0;
		for (int i = 0; i < qnames.getNamespaceContext().size(); i++) {
			NamespaceContext nsc = qnames.getNamespaceContext().get(i);

			int namespaceUriID = i;
			String namespaceUri = nsc.getNamespaceURI();

			List<NamespaceContext.QnameContext> qncs = nsc.getQnameContext();
			QNameContext[] grammarQNames = new QNameContext[qncs.size()];
			for (int k = 0; k < qncs.size(); k++) {
				numberofQNamesContexts++;
				NamespaceContext.QnameContext qnc = qncs.get(k);

				int localNameID = k;
				QName qName = new QName(namespaceUri, qnc.getLocalName());
				grammarQNames[k] = new QNameContext(namespaceUriID,
						localNameID, qName);
			}

			String[] grammarPrefixes;
			if (Constants.XML_NULL_NS_URI.equals(namespaceUri)) {
				grammarPrefixes = Constants.PREFIXES_EMPTY;
			} else if (Constants.XML_NS_URI.equals(namespaceUri)) {
				grammarPrefixes = Constants.PREFIXES_XML;
			} else if (Constants.XML_SCHEMA_INSTANCE_NS_URI
					.equals(namespaceUri)) {
				grammarPrefixes = Constants.PREFIXES_XSI;
			} else {
				grammarPrefixes = GrammarUriContext.EMPTY_PREFIXES;
			}

			grammarUriContexts[i] = new GrammarUriContext(namespaceUriID,
					namespaceUri, grammarQNames, grammarPrefixes);
		}
		GrammarContext grammarContext = new GrammarContext(grammarUriContexts,
				numberofQNamesContexts);

		// Simple Datatypes
		// 1. Init all datatypes
		Datatype[] datatypes = new Datatype[exiGrammars.getSimpleDatatypes()
				.getSimpleDatatype().size()];
		for (int i = 0; i < exiGrammars.getSimpleDatatypes()
				.getSimpleDatatype().size(); i++) {
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Datatype dt = exiGrammars
					.getSimpleDatatypes().getSimpleDatatype().get(i);

			QNameContext qnc = grammarUriContexts[(int) dt
					.getSchemaTypeNamespaceID()].getQNameContext((int) dt
					.getSchemaTypeLocalNameID());

			if (dt.getList() != null) {
				// list MUST be first (there can be a list of enums!)
				Datatype listDatatype;
				if (dt.getEnumeration() != null) {
					listDatatype = getEnumerationDatatype(dt.getEnumeration(),
							qnc);
				} else {
					listDatatype = getBasicDatatype(dt, qnc);
				}
				datatypes[i] = new ListDatatype(listDatatype, qnc);
			} else if (dt.getEnumeration() != null) {
				com.siemens.ct.exi.grammars._2017.schemaforgrammars.Enumeration en = dt
						.getEnumeration();
				EnumerationDatatype endt = getEnumerationDatatype(en, qnc);
				datatypes[i] = endt;
			} else {
				// basic datatypes
				datatypes[i] = getBasicDatatype(dt, qnc);
			}
		}
		// 2. set base datatypes
		for (int i = 0; i < exiGrammars.getSimpleDatatypes()
				.getSimpleDatatype().size(); i++) {
			com.siemens.ct.exi.grammars._2017.schemaforgrammars.Datatype dt = exiGrammars
					.getSimpleDatatypes().getSimpleDatatype().get(i);

			if (dt.getBaseDatatypeID() != null) {
				datatypes[i].setBaseDatatype(getDatatype(
						dt.getBaseDatatypeID(), datatypes, false));
			}
		}

		// Grammar Productions
		ExiGrammars.Grammars grammars = exiGrammars.getGrammars();
		Grammar[] grs = new Grammar[grammars.getGrammar().size()];
		// 1. Walk over all productions to create initial (empty) grammar type
		for (int i = 0; i < grammars.getGrammar().size(); i++) {
			ExiGrammars.Grammars.Grammar grammar = grammars.getGrammar().get(i);
			Grammar g;
			switch (grammar.getGrammarType()) {
			case DOCUMENT:
				g = new Document();
				break;
			case DOC_CONTENT:
				g = new SchemaInformedDocContent();
				break;
			case DOC_END:
				g = new DocEnd();
				break;
			case FRAGMENT:
				g = new Fragment();
				break;
			case FRAGMENT_CONTENT:
				g = new SchemaInformedFragmentContent();
				break;
			case FIRST_START_TAG_CONTENT:
				g = new SchemaInformedFirstStartTag();
				if (grammar.getIsTypeCastable() != null) {
					((SchemaInformedFirstStartTag) g).setTypeCastable(true);
				}
				if (grammar.getIsNillable() != null) {
					((SchemaInformedFirstStartTag) g).setNillable(true);
				}
				break;
			case START_TAG_CONTENT:
				g = new SchemaInformedStartTag();
				break;
			case ELEMENT_CONTENT:
				g = new SchemaInformedElement();
				break;
			default:
				throw new EXIException("Unsupported grammar type "
						+ grammar.getGrammarType());
			}

			grs[i] = g;
		}

		// 2. Walk over all productions to set the ElementContentGrammar
		// and add productions and link next grammars
		for (int i = 0; i < grammars.getGrammar().size(); i++) {
			ExiGrammars.Grammars.Grammar grammar = grammars.getGrammar().get(i);

			switch (grammar.getGrammarType()) {
			case FIRST_START_TAG_CONTENT:
				SchemaInformedFirstStartTag fst = (SchemaInformedFirstStartTag) grs[i];
				fst.setElementContentGrammar(grs[grammar
						.getElementContentGrammarID().intValue()]);
				break;
			case START_TAG_CONTENT:
				SchemaInformedStartTag st = (SchemaInformedStartTag) grs[i];
				st.setElementContentGrammar(grs[grammar
						.getElementContentGrammarID().intValue()]);
				break;
			default:
				/* no element content grammar */
				break;
			}

			for (com.siemens.ct.exi.grammars._2017.schemaforgrammars.Production prod : grammar
					.getProduction()) {
				Event event;
				if (prod.getStartDocument() != null) {
					event = new StartDocument();
				} else if (prod.getEndDocument() != null) {
					event = new EndDocument();
				} else if (prod.getStartElement() != null) {
					Grammar seGrammar = grs[(int) prod.getStartElement()
							.getStartElementGrammarID()];
					QNameContext qnc = grammarUriContexts[(int) prod
							.getStartElement().getStartElementNamespaceID()]
							.getQNameContext((int) prod.getStartElement()
									.getStartElementLocalNameID());
					event = new StartElement(qnc, seGrammar);
				} else if (prod.getStartElementNS() != null) {
					GrammarUriContext guc = grammarUriContexts[prod
							.getStartElementNS().intValue()];
					event = new StartElementNS(guc.getNamespaceUriID(),
							guc.getNamespaceUri());
				} else if (prod.getStartElementGeneric() != null) {
					event = new StartElementGeneric();
				} else if (prod.getEndElement() != null) {
					event = new EndElement();
				} else if (prod.getAttribute() != null) {
					QNameContext qnc = grammarUriContexts[(int) prod
							.getAttribute().getAttributeNamespaceID()]
							.getQNameContext((int) prod.getAttribute()
									.getAttributeLocalNameID());
					Datatype datatype = getDatatype(prod.getAttribute()
							.getAttributeDatatypeID(), datatypes, true);
					event = new Attribute(qnc, datatype);
				} else if (prod.getAttributeNS() != null) {
					GrammarUriContext guc = grammarUriContexts[prod
							.getAttributeNS().intValue()];
					event = new AttributeNS(guc.getNamespaceUriID(),
							guc.getNamespaceUri());
				} else if (prod.getAttributeGeneric() != null) {
					event = new AttributeGeneric();
				} else if (prod.getCharacters() != null) {
					Datatype datatype = getDatatype(prod.getCharacters()
							.getCharactersDatatypeID(), datatypes, false);
					event = new Characters(datatype);
				} else if (prod.getCharactersGeneric() != null) {
					event = new CharactersGeneric();
				} else {
					throw new EXIException("Unsupported event for production: "
							+ prod);
				}

				Grammar next = null;
				if (prod.getNextGrammarID() != null) {
					next = grs[prod.getNextGrammarID().intValue()];
				}
				grs[i].addProduction(event, next);
			}
		}

		// set global element / attribute / type (simple&complex)
		for (int i = 0; i < qnames.getNamespaceContext().size(); i++) {
			NamespaceContext nsc = qnames.getNamespaceContext().get(i);
			List<NamespaceContext.QnameContext> qncs = nsc.getQnameContext();
			for (int k = 0; k < qncs.size(); k++) {
				NamespaceContext.QnameContext qnc = qncs.get(k);

				// global element grammar
				if (qnc.getGlobalElementGrammarID() != null) {
					Grammar g = grs[qnc.getGlobalElementGrammarID().intValue()];
					QNameContext qncSE = grammarUriContexts[i]
							.getQNameContext(k);
					StartElement se = new StartElement(qncSE, g);
					grammarUriContexts[i].getQNameContext(k)
							.setGlobalStartElement(se);
				}

				// global attribute
				if (qnc.getGlobalAttributeDatatypeID() != null) {
					QNameContext qncAT = grammarUriContexts[i]
							.getQNameContext(k);
					Datatype dt = getDatatype(
							qnc.getGlobalAttributeDatatypeID(), datatypes,
							false);
					Attribute at = new Attribute(qncAT, dt);
					grammarUriContexts[i].getQNameContext(k)
							.setGlobalAttribute(at);
				}

				// global types
				if (qnc.getGlobalComplexTypeGrammarID() != null) {
					grammarUriContexts[i]
							.getQNameContext(k)
							.setTypeGrammar(
									(SchemaInformedFirstStartTagGrammar) grs[qnc
											.getGlobalComplexTypeGrammarID()
											.intValue()]);
				}
				if (qnc.getGlobalSimpleTypeDatatypeID() != null) {
					// Note: Simple Type grammars always look the same
					// SimpleType0 : CH(schema-types) SimpleType1
					// SimpleType1 : EE
					Datatype dt = getDatatype(
							qnc.getGlobalSimpleTypeDatatypeID(), datatypes,
							false);
					SchemaInformedFirstStartTagGrammar sistg = new SchemaInformedFirstStartTag();
					SchemaInformedElement elementContent = new SchemaInformedElement();
					SchemaInformedElement sie = new SchemaInformedElement();
					sistg.setElementContentGrammar(elementContent);
					sie.addTerminalProduction(new EndElement());
					Characters ch = new Characters(dt);
					sistg.addProduction(ch, sie);
					elementContent.addProduction(ch, sie);
					grammarUriContexts[i].getQNameContext(k).setTypeGrammar(
							sistg);
				}

			}
		}

		// Document
		Document document = (Document) grs[(int) grammars
				.getDocumentGrammarID()];

		// Fragment
		Fragment fragment = (Fragment) grs[(int) grammars
				.getFragmentGrammarID()];

		// TODO ElementFragment
		SchemaInformedGrammar elementFragmentGrammar = (SchemaInformedGrammar) grs[(int) grammars
				.getElementFragmentGrammarID()];

		SchemaInformedGrammars sig = new SchemaInformedGrammars(grammarContext,
				document, fragment, elementFragmentGrammar);

		// builtInXMLSchemaTypesOnly
		boolean builtInXMLSchemaTypesOnly = false;
		if (grammars.getIsBuiltInXMLSchemaTypesOnly() != null) {
			builtInXMLSchemaTypesOnly = true;
		}
		sig.setBuiltInXMLSchemaTypesOnly(builtInXMLSchemaTypesOnly);

		return sig;
	}

	public static void main(String[] args) throws Exception {
		String xsd = null;

		// schema-for-json.xsd, see
		// https://www.w3.org/TR/exi-for-json/schema-for-json.xsd
		// xsd = "../exificient.js/grammars/schema-for-json.xsd";
		// xsd = "../exificient.js/grammars/exi4json.xsd";
		// notebook
		xsd = "../exificient.js/grammars/notebook.xsd";
		// xsd = "../exificient.js/test/data/xml/any0.xsd";
		// xsd = "../exificient.js/test/data/xml/basic_rdf_query_v02.xsd";
		// xsd =
		// "D:\\Projects\\EXI\\EXIficient\\exificient.js\\test\\data\\xml\\unsignedInteger.xsd";
		// xsd =
		// "D:\\Projects\\EXI\\EXIficient\\exificient.js\\test\\data\\xml\\test1.xsd";

		XSDGrammarsBuilder grammarBuilder = XSDGrammarsBuilder.newInstance();

		grammarBuilder.loadGrammars(xsd);

		SchemaInformedGrammars grammarIn = grammarBuilder.toGrammars();

		Grammars2X g2j = new Grammars2X();

		/*
		 * Generate JAXB
		 */
		ExiGrammars exiGrammar = g2j.toGrammarsX(grammarIn);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		marshal(exiGrammar, new StreamResult(baos));
		System.out.println(new String(baos.toByteArray()));

		/*
		 * STATS
		 */
		if (STATS_ON) {
			System.out.println("Transitions: " + g2j.statsCountTransitions);
			System.out.println("States: " + g2j.statsCountStates);
		}

	}

}
