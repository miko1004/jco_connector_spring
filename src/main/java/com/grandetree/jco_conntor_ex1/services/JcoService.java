package com.grandetree.jco_conntor_ex1.services;

import com.grandetree.jco_conntor_ex1.vo.Element;
import com.sap.conn.jco.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
public class JcoService {

    public String getWSDL(String functionName) throws JCoException {
        JCoDestination destination = JCoDestinationManager.getDestination("ABAP_AS1");
        // ... it always has a reference to a metadata repository
        JCoFunction function = destination.getRepository().getFunction(functionName);
        if (function == null)
            throw new RuntimeException(String.format("%s not found in SAP.", functionName));

        StringBuilder stringBuilder = new StringBuilder();

        ArrayList<Element> paramters = new ArrayList<Element>();
        HashMap<String, ArrayList<Element>> complexType = new HashMap<String, ArrayList<Element>>();

        getParameters(paramters, complexType, function.getImportParameterList(), "I");
        getParameters(paramters, complexType, function.getChangingParameterList(), "C");
        getParameters(paramters, complexType, function.getExportParameterList(), "E");
        getParameters(paramters, complexType, function.getTableParameterList(), "T");

        String WSDL = generateWSDL(functionName, paramters, complexType);

        return WSDL;
    }

    private void getParameters(ArrayList<Element> paramters,
                               HashMap<String, ArrayList<Element>> complexType,
                               JCoParameterList parameterList,
                               String parameterType) {
        if (parameterList != null) {
            for (int i = 0; i < parameterList.getListMetaData().getFieldCount(); i++) {
                switch (parameterList.getField(i).getTypeAsString()) {
                    case "STRUCTURE":
                        String elementName = parameterList.getField(i).getName();
                        String type = parameterList.getField(i).getRecordMetaData().getName();

                        Element structureElement = new Element();
                        structureElement.setName(elementName);
                        structureElement.setRefType(type);
                        structureElement.setLength(0);
                        if (parameterType.equals("I")) {
                            structureElement.setMinOccur(1);
                        }
                        structureElement.setParamterType(parameterType);
                        paramters.add(structureElement);

                        JCoFieldIterator structureFieldIterator = parameterList.getStructure(i).getFieldIterator();

                        ArrayList<Element> fields = new ArrayList<Element>();
                        while (structureFieldIterator.hasNextField()) {
                            JCoField field = structureFieldIterator.nextField();
                            Element element1 = new Element();
                            element1.setName(field.getName());
                            element1.setType(field.getTypeAsString());
                            if (field.getTypeAsString().equals("BCD") || field.getTypeAsString().equals("FLOAT")) {
                                int length = (field.getLength() - 1) * 2;
                                element1.setLength(length);
                                element1.setDecimal(field.getDecimals());
                            } else {
                                element1.setLength(field.getLength());
                            }
                            //System.out.println(String.format("%s => %s", field.getName(), field.getTypeAsString()));
                            fields.add(element1);
                        }
                        complexType.put(type, fields);
                        break;
                    case "TABLE":
                        Element tableElement = new Element();
                        tableElement.setName(parameterList.getField(i).getName());
                        tableElement.setRefType(parameterList.getField(i).getRecordMetaData().getName());
                        tableElement.setLength(0);
                        tableElement.setParamterType(parameterType);
                        paramters.add(tableElement);

                        JCoFieldIterator tableFieldIterator = parameterList.getTable(i).getFieldIterator();
                        ArrayList<Element> tableFields = new ArrayList<Element>();
                        while (tableFieldIterator.hasNextField()) {
                            JCoField field = tableFieldIterator.nextField();
                            Element tableField = new Element();
                            tableField.setName(field.getName());
                            tableField.setType(field.getTypeAsString());
                            if (field.getTypeAsString().equals("BCD")) {
                                int length = field.getTypeAsString().equals("BCD") ? (field.getLength() - 1) * 2 : field.getLength();
                                tableField.setLength(length);
                                tableField.setDecimal(field.getDecimals());
                            } else {
                                tableField.setLength(field.getLength());
                            }
                            tableFields.add(tableField);
                        }
                        complexType.put(parameterList.getField(i).getRecordMetaData().getName(), tableFields);
                        break;
                    default:
                        Element element1 = new Element();
                        element1.setName(parameterList.getField(i).getName());
                        element1.setType(parameterList.getField(i).getTypeAsString());
                        element1.setLength(parameterList.getField(i).getLength());
                        element1.setParamterType(parameterType);
                        paramters.add(element1);
                        break;
                }
            }
        }
    }

    private String dataTypeToXSD(String type) {
        String xsdDataType = "";
        switch (type) {
            case "BCD":
            case "DEC":
                xsdDataType = "decimal";
                break;
            case "DATE":
                xsdDataType = "date";
                break;
            case "TIME":
                xsdDataType = "time";
                break;
            case "CHAR":
            case "STRING":
            case "NUM":
                xsdDataType = "string";
                break;
            case "FLOAT":
                xsdDataType = "float";
                break;
            case "XSTRING":
            case "BYTE":
                xsdDataType = "base64Binary";
                break;
            case "INT":
            case "INT1":
            case "INT2":
            case "INT4":
            case "INT8":
                xsdDataType = "integer";
                break;
            default:
                xsdDataType = "string";
                break;
        }
        return xsdDataType;
    }

    private String generateWSDL(String functionName, ArrayList<Element> parameters,
                                HashMap<String, ArrayList<Element>> complexTypes) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("<wsdl:definitions xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\" " +
                "xmlns:rfc=\"urn:sap-com:document:sap:rfc:functions\" " +
                "name=\"%s\" " +
                "targetNamespace=\"urn:sap-com:document:sap:rfc:functions\">\n", functionName));
        stringBuilder.append("<wsdl:types>\n");
        stringBuilder.append("<xsd:schema xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" " +
                "xmlns=\"urn:sap-com:document:sap:rfc:functions\" " +
                "targetNamespace=\"urn:sap-com:document:sap:rfc:functions\">\n");
        stringBuilder.append(String.format("<xsd:element name=\"%s\">\n", functionName));
        stringBuilder.append("<xsd:complexType>\n");
        stringBuilder.append("<xsd:all>\n");
        if (parameters.stream().filter(v -> v.getParamterType().equals("I")).count() > 0) {
            stringBuilder.append("<xsd:element name=\"INPUT\">\n" +
                    "<xsd:complexType>\n" +
                    "<xsd:sequence>\n");
            for (Element e : parameters.stream().filter(v -> v.getParamterType().equals("I")).collect(Collectors.toList())) {
                if (e.getType() == null) {
                    stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"%s\" />\n", e.getName(), e.getRefType()));
                } else {
                    if (e.getLength() > 0 && !dataTypeToXSD(e.getType()).equals("integer")) {
                        stringBuilder.append(String.format("<xsd:element name=\"%s\">\n<xsd:simpleType>\n", e.getName()));
                        stringBuilder.append(String.format("<xsd:restriction base=\"xsd:%s\">\n", dataTypeToXSD(e.getType())));
                        if (dataTypeToXSD(e.getType()).equals("decimal")) {
                            stringBuilder.append(String.format("<xsd:totalDigits value=\"%d\" />\n<xsd:fractionDigits value=\"%d\" />\n", e.getLength(), e.getDecimal()));
                        } else if (dataTypeToXSD(e.getType()).equals("string")) {
                            stringBuilder.append(String.format("<xsd:maxLength value=\"%d\" />\n", e.getLength()));
                        }
                        stringBuilder.append("</xsd:restriction>\n</xsd:simpleType>\n</xsd:element>\n");
                    } else {
                        stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"xsd:%s\" />\n", e.getName(), dataTypeToXSD(e.getType())));
                    }
                }
            }
            stringBuilder.append("</xsd:sequence>\n" +
                    "</xsd:complexType>\n" +
                    "</xsd:element>\n");
        }
        if (parameters.stream().filter(v -> v.getParamterType().equals("C")).count() > 0) {
            stringBuilder.append("<xsd:element name=\"CHANGING\">\n" +
                    "<xsd:complexType>\n" +
                    "<xsd:sequence>\n");
            for (Element e : parameters.stream().filter(v -> v.getParamterType().equals("C")).collect(Collectors.toList())) {
                if (e.getType() == null) {
                    stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"%s\" />\n", e.getName(), e.getRefType()));
                } else {
                    if (e.getLength() > 0 && !dataTypeToXSD(e.getType()).equals("integer")) {
                        stringBuilder.append(String.format("<xsd:element name=\"%s\">\n<xsd:simpleType>\n", e.getName()));
                        stringBuilder.append(String.format("<xsd:restriction base=\"xsd:%s\">\n", dataTypeToXSD(e.getType())));
                        if (dataTypeToXSD(e.getType()).equals("decimal")) {
                            stringBuilder.append(String.format("<xsd:totalDigits value=\"%d\" />\n<xsd:fractionDigits value=\"%d\" />\n", e.getLength(), e.getDecimal()));
                        } else if (dataTypeToXSD(e.getType()).equals("string")) {
                            stringBuilder.append(String.format("<xsd:maxLength value=\"%d\" />\n", e.getLength()));
                        }
                        stringBuilder.append("</xsd:restriction>\n</xsd:simpleType>\n</xsd:element>\n");
                    } else {
                        stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"xsd:%s\" />\n", e.getName(), dataTypeToXSD(e.getType())));
                    }
                }
            }
            stringBuilder.append("</xsd:sequence>\n" +
                    "</xsd:complexType>\n" +
                    "</xsd:element>\n");
        }
        if (parameters.stream().filter(v -> v.getParamterType().equals("E")).count() > 0) {
            stringBuilder.append("<xsd:element name=\"OUTPUT\">\n" +
                    "<xsd:complexType>\n" +
                    "<xsd:sequence>\n");
            for (Element e : parameters.stream().filter(v -> v.getParamterType().equals("E")).collect(Collectors.toList())) {
                if (e.getType() == null) {
                    stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"%s\" />\n", e.getName(), e.getRefType()));
                } else {
                    if (e.getLength() > 0 && !dataTypeToXSD(e.getType()).equals("integer")) {
                        stringBuilder.append(String.format("<xsd:element name=\"%s\">\n<xsd:simpleType>\n", e.getName()));
                        stringBuilder.append(String.format("<xsd:restriction base=\"xsd:%s\">\n", dataTypeToXSD(e.getType())));
                        if (dataTypeToXSD(e.getType()).equals("decimal")) {
                            stringBuilder.append(String.format("<xsd:totalDigits value=\"%d\" />\n<xsd:fractionDigits value=\"%d\" />\n", e.getLength(), e.getDecimal()));
                        } else if (dataTypeToXSD(e.getType()).equals("string")) {
                            stringBuilder.append(String.format("<xsd:maxLength value=\"%d\" />\n", e.getLength()));
                        }
                        stringBuilder.append("</xsd:restriction>\n</xsd:simpleType>\n</xsd:element>\n");
                    } else {
                        stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"xsd:%s\" />\n", e.getName(), dataTypeToXSD(e.getType())));
                    }
                }
            }
            stringBuilder.append("</xsd:sequence>\n" +
                    "</xsd:complexType>\n" +
                    "</xsd:element>\n");
        }
        if (parameters.stream().filter(v -> v.getParamterType().equals("T")).count() > 0) {
            stringBuilder.append("<xsd:element name=\"TABLE\">\n" +
                    "<xsd:complexType>\n" +
                    "<xsd:sequence>\n");
            for (Element e : parameters.stream().filter(v -> v.getParamterType().equals("T")).collect(Collectors.toList())) {
                stringBuilder.append(String.format("<xsd:element name=\"%s\" minOccurs=\"0\">\n" +
                        "<xsd:complexType>\n" +
                        "<xsd:sequence>\n" +
                        "<xsd:element name=\"item\" type=\"%s\" minOccurs=\"0\" maxOccurs=\"unbounded\" />\n" +
                        "</xsd:sequence>\n" +
                        "</xsd:complexType>\n" +
                        "</xsd:element>\n", e.getName(), e.getRefType()));
            }
            stringBuilder.append("</xsd:sequence>\n" +
                    "</xsd:complexType>\n" +
                    "</xsd:element>\n");
        }
        stringBuilder.append("</xsd:all>\n" +
                "</xsd:complexType>\n" +
                "</xsd:element>\n");
        for (String key : complexTypes.keySet()) {
            stringBuilder.append(String.format("<xsd:complexType name=\"%s\">\n<xsd:sequence>\n", key));
            for (Element e : complexTypes.get(key)) {
                if (e.getLength() > 0 && !dataTypeToXSD(e.getType()).equals("integer")) {
                    stringBuilder.append(String.format("<xsd:element name=\"%s\">\n<xsd:simpleType>\n", e.getName()));
                    stringBuilder.append(String.format("<xsd:restriction base=\"xsd:%s\">\n", dataTypeToXSD(e.getType())));
                    if (dataTypeToXSD(e.getType()).equals("decimal")) {
                        stringBuilder.append(String.format("<xsd:totalDigits value=\"%d\" />\n<xsd:fractionDigits value=\"%d\" />\n", e.getLength(), e.getDecimal()));
                    } else if (dataTypeToXSD(e.getType()).equals("string")) {
                        stringBuilder.append(String.format("<xsd:maxLength value=\"%d\" />\n", e.getLength()));
                    }
                    stringBuilder.append("</xsd:restriction>\n</xsd:simpleType>\n</xsd:element>\n");
                } else {
                    stringBuilder.append(String.format("<xsd:element name=\"%s\" type=\"xsd:%s\" />\n", e.getName(), dataTypeToXSD(e.getType())));
                }
            }
            stringBuilder.append("</xsd:sequence></xsd:complexType>\n");
        }
        stringBuilder.append("</xsd:schema>\n</wsdl:types>\n");
        stringBuilder.append(String.format("<wsdl:message name=\"%s.Input\">\n", functionName));
        stringBuilder.append(String.format("<wsdl:part name=\"parameters\" element=\"rfc:%s\" />\n", functionName));
        stringBuilder.append(String.format("</wsdl:message>\n"));
        stringBuilder.append(String.format("<wsdl:message name=\"%s.Output\">\n", functionName));
        stringBuilder.append(String.format("<wsdl:part name=\"parameters\" element=\"rfc:%s\" />\n", functionName));
        stringBuilder.append(String.format("</wsdl:message>\n"));
        stringBuilder.append(String.format("<wsdl:portType name=\"%s.PortType\">\n", functionName));
        stringBuilder.append(String.format("<wsdl:operation name=\"%s\">\n", functionName));
        stringBuilder.append(String.format("<wsdl:input message=\"rfc:%s.Input\" />\n", functionName));
        stringBuilder.append(String.format("<wsdl:output message=\"rfc:%s.Output\" />\n", functionName));
        stringBuilder.append(String.format("</wsdl:operation>\n"));
        stringBuilder.append(String.format("</wsdl:portType>\n"));
        stringBuilder.append("</wsdl:definitions>");

        return stringBuilder.toString();
    }
}
