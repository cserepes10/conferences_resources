package com.esri.arcgis.demo.util;

import java.util.ArrayList;
import java.util.List;

/**
 * User: shre5185
 * Date: 10/2/12
 * Time: 6:24 PM
 */
public class PreparedStatementData {
  private String queryStr;
  private ArrayList<PreparedStatementParameter> parameters;

  public PreparedStatementData(int size) {
    parameters = new ArrayList<PreparedStatementParameter>(size);
  }

  public PreparedStatementData() {
    parameters = new ArrayList<PreparedStatementParameter>();
  }

  public void setQueryStr(String queryStr) {
    this.queryStr = queryStr;
  }

  public String getQueryStr() {
    return queryStr;
  }

  public ArrayList<PreparedStatementParameter> getParameters() {
    return parameters;
  }

  public PreparedStatementData addParameter(int index, ParameterType type, Object value) {
    PreparedStatementParameter p = new PreparedStatementParameter(type, value, index);
    parameters.add(p);
    return this;
  }

  public PreparedStatementData addParameter(int index, String value) {
    PreparedStatementParameter p = new PreparedStatementParameter(ParameterType.STRING, value, index);
    parameters.add(p);
    return this;
  }

  public void addParameters(int index, List<String> values) {
    if (values == null) {
      return;
    }

    for(int i=0; i<values.size(); i++) {
      addParameter(index+i, values.get(i));
    }
  }


  /**
   * This class represents a specific parameter in a SQL statement used specifically
   * within a PreparedStatement.
   */
  protected static class PreparedStatementParameter {
    private final ParameterType type;
    private final Object value;
    private final int index;

    public PreparedStatementParameter(ParameterType type, Object value, int index) {
    	this.type = type;
    	this.value = value;
    	this.index = index;
    }

    public ParameterType getType() {
      return type;
    }

    public Object getValue() {
      return value;
    }

    public int getIndex() {
      return index;
    }

    @Override
    public String toString() {
    	return getClass().getName() + '@' + Integer.toHexString(hashCode()) + "[type=" + type + ",value=" + value + ",index=" + index + "]";
    }
  }

  @Override
  public String toString() {
  	return getClass().getName() + '@' + Integer.toHexString(hashCode()) + "[queryStr=" + queryStr + ",parameters=" + parameters.toString() + "]";
  }

  /**
   * This enumeration represents the type of parameters that are supported
   * by this abstraction of prepared statement parameters.
   */
  public enum ParameterType {
    STRING,
    LONG,
    DATE,
    INTEGER,
    FLOAT,
    DOUBLE,
    BOOLEAN,
    TIMESTAMP,
    OBJECT,
    BYTEA
  }
}
