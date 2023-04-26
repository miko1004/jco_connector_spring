package com.grandetree.jco_conntor_ex1.vo;

public class Element {
    private String name;
    private String type;
    private String refType;
    private int length;
    private int decimal;
    private String paramterType;
    private int minOccur;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getParamterType() {
        return paramterType;
    }

    public void setParamterType(String paramterType) {
        this.paramterType = paramterType;
    }

    public String getRefType() {
        return refType;
    }

    public void setRefType(String refType) {
        this.refType = refType;
    }

    public int getDecimal() {
        return decimal;
    }

    public void setDecimal(int decimal) {
        this.decimal = decimal;
    }

    public int getMinOccur() {
        return minOccur;
    }

    public void setMinOccur(int minOccur) {
        this.minOccur = minOccur;
    }
}
