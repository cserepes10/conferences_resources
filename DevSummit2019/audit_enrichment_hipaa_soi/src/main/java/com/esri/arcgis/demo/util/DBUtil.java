package com.esri.arcgis.demo.util;


import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLNonTransientException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class DBUtil {
  private static final Logger log = Logger.getLogger(DBUtil.class.getName());
  private static final String[][] RESULT_TYPE = new String[0][0];
  private static final int MAX_RETRY = 10;

  public static String[][] doQueryWithLimitAndOrderByNoRetry(
      PreparedStatementData psd, DataSource dataSource, int start, int num, String sortField, boolean desc)
      throws SQLException {
    psd.setQueryStr(psd.getQueryStr() + " ORDER BY " + sortField);
    if (desc) {
      psd.setQueryStr(psd.getQueryStr() + " desc");

    }
    return doQueryWithLimitNoRetry(psd, dataSource, start, num);
  }

  public static String[][] doQueryWithLimitNoRetry(PreparedStatementData psd,
                                                   DataSource dataSource, int start, int num) throws SQLException {
    psd.setQueryStr(psd.getQueryStr() + " LIMIT " + num + " OFFSET "
        + start);
    return doQueryNoRetry(psd, dataSource);
  }

  public static String[][] doQueryNoRetry(PreparedStatementData psd,
                                          DataSource dataSource) throws SQLException {
    Connection con = dataSource.getConnection();
    String[][] retVal = null;
    ResultSet rs = null;
    PreparedStatement ps = null;
    ArrayList<PreparedStatementData.PreparedStatementParameter> params = null;

    try {
      params = psd.getParameters();
      ps = con.prepareStatement(psd.getQueryStr());
      for (int i = 0; i < params.size(); i++) {
        setParameter(params.get(i), ps);
      }
      log.finest(psd.getQueryStr());
      rs = ps.executeQuery();

      if (rs == null) {
        retVal = null;
      } else {
        ArrayList<String[]> tempResult = new ArrayList<String[]>();
        int numCols = rs.getMetaData().getColumnCount();
        while (rs.next()) {
          String[] row = new String[numCols];
          for (int i = 0; i < numCols; i++) {
            row[i] = rs.getString(i + 1);
          }

          tempResult.add(row);
        }

        retVal = tempResult.toArray(RESULT_TYPE);
      }
    } catch (Exception e) {
      throw new RuntimeException("Unable to execute query: " + psd, e);
    } finally {
      releaseResources(rs, ps, con);
    }

    return retVal;
  }

  /**
   * @param psd
   * @param dataSource
   * @return number of rows updated
   */
  public static int doUpdateNoRetry(PreparedStatementData psd, DataSource dataSource)
      throws SQLException {
    Connection con = dataSource.getConnection();
    int retVal = -1;
    PreparedStatement ps = null;
    ArrayList<PreparedStatementData.PreparedStatementParameter> params = null;

    try {
      params = psd.getParameters();
      ps = con.prepareStatement(psd.getQueryStr());
      for (int i = 0; i < params.size(); i++) {
        setParameter(params.get(i), ps);
      }
      log.finest(psd.getQueryStr());
      int ret = ps.executeUpdate();

      if (!con.getAutoCommit()) {
        con.commit();
      }

      retVal = ret;
    } catch (Exception e) {
      throw new RuntimeException("Unable to execute update: " + psd, e);
    } finally {
      releaseResources(null, ps, con);
    }

    return retVal;
  }


  public static boolean doUpdateTransactionNoRetry(PreparedStatementData[] psdArr, DataSource dataSource) throws SQLException {
    Connection con = dataSource.getConnection();
    boolean retVal = false;
    boolean originalAtocommit = false;
    Savepoint svPt = null;
    PreparedStatement ps = null;

    try {
      originalAtocommit = con.getAutoCommit();
      log.finest("doUpdateTransaction: originalAtocommit = " + originalAtocommit);
      con.setAutoCommit(false);
      log.finest("doUpdateTransaction: after setting to \"false\", autocommit = "+ con.getAutoCommit());
      svPt = con.setSavepoint("SAVEPOINT");

      PreparedStatementData psd = null;
      ArrayList<PreparedStatementData.PreparedStatementParameter> params = null;

      for (int i = 0; i < psdArr.length; i++) {
        psd = psdArr[i];
        params = psd.getParameters();
        ps = con.prepareStatement(psd.getQueryStr());
        for (int j = 0; j < params.size(); j++) {
          setParameter(params.get(j), ps);
        }
        log.finest(psd.getQueryStr());
        ps.execute();
				ps.close();
      }
      con.commit();
      con.setAutoCommit(originalAtocommit);
      retVal = true;
    } catch (Exception ex) {
      try {
        con.rollback(svPt);
        con.setAutoCommit(originalAtocommit);
      } catch (Exception rollBackEx) {
        log.log(Level.SEVERE, "DBUtil.doUpdateTransaction(): rollback failed", rollBackEx);
      }
      throw new RuntimeException("DBUtil.doUpdateTransaction(): failed, ", ex);
    } finally {
      releaseResources(null, ps, con);
    }

    return retVal;
  }

	/**
	 * @param psds
	 * @param dataSource
	 * @return number of rows updated
	 */
	public static int doBatchUpdateNoRetry(PreparedStatementData[] psds, DataSource dataSource) throws SQLException {
		int sum = 0;
		Connection con = dataSource.getConnection();
		PreparedStatement ps = null;
		ArrayList<PreparedStatementData.PreparedStatementParameter> params = null;

		try {
			ps = con.prepareStatement(psds[0].getQueryStr());
			for (int j = 0; j < psds.length; j++) {
				params = psds[j].getParameters();
				for (int i = 0; i < params.size(); i++) {
					setParameter(params.get(i), ps);
				}

				ps.addBatch();
			}

			log.finest(psds[0].getQueryStr());
			int[] retVal = ps.executeBatch();
			for (int i = 0; i < retVal.length; i++) {
				sum += retVal[i];
			}

			if (!con.getAutoCommit()) {
				con.commit();
			}
		} catch (Exception e) {
			throw new RuntimeException("Unable to execute batch update: ", e);
		} finally {
			releaseResources(null, ps, con);
		}

		return sum;
	}

  private static void releaseResources(ResultSet rs, PreparedStatement ps,
                                       Connection con) {
    if (rs != null) {
      try {
        rs.close();
      } catch (SQLException ex) {
        log.log(Level.SEVERE, "ResultSet closing exception: ", ex);
      }
    }
    rs = null;

    if (ps != null) {
      try {
        ps.close();
      } catch (SQLException ex) {
        log.log(Level.SEVERE, "PreparedStatement closing exception: ", ex);
      }
    }
    ps = null;

    if (con != null) {
      try {
        con.close();
      } catch (SQLException ex) {
        log.log(Level.SEVERE, "Connection closing exception: ", ex);
      }
    }
    con = null;
  }

  public static PreparedStatementData getSingleUpdatePreparedStatementData(String queryStr, String whereClauseVal) {
    PreparedStatementData retVal = new PreparedStatementData(1);
    retVal.setQueryStr(queryStr);
    retVal.addParameter(0, whereClauseVal);
    return retVal;
  }

  public static PreparedStatementData getSingleUpdatePreparedStatementData(String queryStr, String paramVal, String whereClauseVal) {
    PreparedStatementData retVal = new PreparedStatementData(2);
    retVal.setQueryStr(queryStr);
    retVal.addParameter(0, paramVal);
    retVal.addParameter(1, whereClauseVal);
    return retVal;
  }

  public static PreparedStatementData getSingleUpdatePreparedStatementData(String queryStr, PreparedStatementData.ParameterType type, Object paramVal, String whereClauseVal) {
    PreparedStatementData retVal = new PreparedStatementData(2);
    retVal.setQueryStr(queryStr);
    retVal.addParameter(0, type, paramVal);
    retVal.addParameter(1, whereClauseVal);
    return retVal;
  }

  /**
   * This methods invokes the appropriate setter method on the PreparedStatement to set the type
   * of the parameter.
   * @param parameter
   * @param ps
   */
  private static void setParameter(PreparedStatementData.PreparedStatementParameter parameter, PreparedStatement ps) throws Exception {
    int index = parameter.getIndex()+1; //Note: As the Java SQL Prepared Statement index starts from One.
    switch (parameter.getType()) {
      case STRING:
        ps.setString(index, (String) parameter.getValue());
        break;
      case DATE:
        ps.setDate(index, (java.sql.Date)parameter.getValue());
        break;
      case TIMESTAMP:
        ps.setTimestamp(index, (java.sql.Timestamp)parameter.getValue());
        break;
      case INTEGER:
        ps.setInt(index, (Integer)parameter.getValue());
        break;
      case BOOLEAN:
        ps.setBoolean(index, (Boolean)parameter.getValue());
        break;
      case DOUBLE:
        ps.setDouble(index, (Double)parameter.getValue());
        break;
      case FLOAT:
        ps.setFloat(index, (Float)parameter.getValue());
        break;
      case LONG:
        // Integer cannot be casted to Long - hence the Number conversion
        Number num = (Number)parameter.getValue();
        ps.setLong(index, num.longValue());
        break;
      case OBJECT:
          ps.setObject(index, parameter.getValue());
          break;
      case BYTEA:
        ps.setBytes(index, (byte[])parameter.getValue());
        break;
      default:
        // Treat default types as strings
        ps.setString(index, (String)parameter.getValue());
        break;
    }
  }

	public static String[][] doQueryWithLimitAndOrderBy(final PreparedStatementData psd, final DataSource dataSource,
                                                      final int start, final int num, final String sortField, final boolean desc) throws SQLException {
		return retry(() -> DBUtil.doQueryWithLimitAndOrderByNoRetry(psd, dataSource, start, num, sortField, desc));
	}

	public static String[][] doQueryWithLimit(final PreparedStatementData psd, final DataSource dataSource,
                                            final int start, final int num) throws SQLException {
		return retry(() -> DBUtil.doQueryWithLimitNoRetry(psd, dataSource, start, num));
	}

	public static String[][] doQuery(final PreparedStatementData psd, final DataSource dataSource) throws SQLException {
		return retry(() -> DBUtil.doQueryNoRetry(psd, dataSource));
	}

	public static int doUpdate(final PreparedStatementData psd, final DataSource dataSource) throws SQLException {
		return retry(() -> DBUtil.doUpdateNoRetry(psd, dataSource));
	}

	public static boolean doUpdateTransaction(final PreparedStatementData[] psdArr, final DataSource dataSource)
			throws SQLException {
		return retry(() -> DBUtil.doUpdateTransactionNoRetry(psdArr, dataSource));
	}

	public static int doBatchUpdate(final PreparedStatementData[] psds, final DataSource dataSource)
			throws SQLException {
		return retry(() -> DBUtil.doBatchUpdateNoRetry(psds, dataSource));
	}

	private static <T> T retry(Retryable<T> c) throws SQLException {
		int retries = 0;
		boolean keepTrying;

		do {
			keepTrying = false;
			try {
				return c.call();
			} catch (SQLNonTransientException e) {
				log.warning("retry #" + retries + " failed. " + e.getMessage());
				if (retries == MAX_RETRY - 1) {
					throw e;
				}
				try {
					Thread.sleep((long) Math.pow(2, retries) * 100);
				} catch (InterruptedException ie) {
					// ignore
				}
				keepTrying = true;
			}
			retries++;
		} while (keepTrying && retries < MAX_RETRY);

		return null;
	}

	@FunctionalInterface
	private static interface Retryable<V> {
		V call() throws SQLException;
	}
}
