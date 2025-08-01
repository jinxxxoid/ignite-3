/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ignite.internal.jdbc;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import org.apache.ignite.internal.binarytuple.BinaryTupleReader;
import org.apache.ignite.internal.jdbc.proto.IgniteQueryErrorCode;
import org.apache.ignite.internal.jdbc.proto.JdbcQueryCursorHandler;
import org.apache.ignite.internal.jdbc.proto.SqlStateCode;
import org.apache.ignite.internal.jdbc.proto.event.JdbcColumnMeta;
import org.apache.ignite.internal.jdbc.proto.event.JdbcFetchQueryResultsRequest;
import org.apache.ignite.internal.jdbc.proto.event.JdbcQueryCloseRequest;
import org.apache.ignite.internal.jdbc.proto.event.JdbcQueryCloseResult;
import org.apache.ignite.internal.jdbc.proto.event.JdbcQueryFetchResult;
import org.apache.ignite.internal.jdbc.proto.event.JdbcQuerySingleResult;
import org.apache.ignite.internal.util.StringUtils;
import org.apache.ignite.internal.util.TransformingIterator;
import org.apache.ignite.sql.ColumnType;
import org.jetbrains.annotations.Nullable;

/**
 * Jdbc result set implementation.
 */
public class JdbcResultSet implements ResultSet {
    /** Decimal format to convert string to decimal. */
    private static final ThreadLocal<DecimalFormat> decimalFormat = new ThreadLocal<>() {
        /** {@inheritDoc} */
        @Override
        protected DecimalFormat initialValue() {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();

            symbols.setGroupingSeparator(',');
            symbols.setDecimalSeparator('.');

            DecimalFormat decimalFormat = new DecimalFormat("", symbols);

            decimalFormat.setParseBigDecimal(true);

            return decimalFormat;
        }
    };

    private final JdbcStatement stmt;
    private final @Nullable Long cursorId;
    private final boolean hasResultSet;
    private final boolean hasNextResult;

    /** Jdbc metadata. */
    private final @Nullable JdbcResultSetMetadata jdbcMeta;

    /** Column order map. */
    private @Nullable Map<String, Integer> colOrder;

    /** Rows. */
    private @Nullable List<BinaryTupleReader> rows;

    /** Rows iterator. */
    private @Nullable Iterator<List<Object>> rowsIter;

    /** Current row. */
    private @Nullable List<Object> curRow;

    /** Current position. */
    private int curPos;

    /** Finished flag. */
    private boolean finished;

    /** Closed flag. */
    private boolean closed;

    /** If {#code true} indicates that handler still holds cursor in resources. */
    private boolean holdsResource;

    /** Was {@code NULL} flag. */
    private boolean wasNull;

    /** Fetch size. */
    private int fetchSize;

    /** Update count. */
    private long updCnt;

    /** Close statement after close result set count. */
    private boolean closeStmt;

    /** Query request handler. */
    private JdbcQueryCursorHandler cursorHandler;

    /** Count of columns in resultSet row. */
    private int columnCount;

    /** Function to deserialize raw rows to list of objects. */
    private @Nullable Function<BinaryTupleReader, List<Object>> transformer;

    /**
     * Creates new result set.
     *
     * @param handler JdbcQueryCursorHandler.
     * @param stmt Statement.
     * @param cursorId Cursor ID.
     * @param fetchSize Fetch size.
     * @param finished Finished flag.
     * @param rows Rows.
     * @param hasResultSet Is Result ser for Select query.
     * @param hasNextResult Whether this result is part of multi statement and there is at least one more result available.
     * @param updCnt Update count.
     * @param closeStmt Close statement on the result set close.
     * @param columnCount Count of columns in resultSet row.
     * @param transformer Function to deserialize raw rows to list of objects.
     */
    JdbcResultSet(
            JdbcQueryCursorHandler handler,
            JdbcStatement stmt,
            @Nullable Long cursorId,
            int fetchSize,
            boolean finished,
            @Nullable List<BinaryTupleReader> rows,
            @Nullable List<JdbcColumnMeta> meta,
            boolean hasResultSet,
            boolean hasNextResult,
            long updCnt,
            boolean closeStmt,
            int columnCount,
            @Nullable Function<BinaryTupleReader, List<Object>> transformer
    ) {
        assert stmt != null;
        assert fetchSize > 0;

        this.cursorHandler = handler;
        this.stmt = stmt;
        this.cursorId = cursorId;
        this.fetchSize = fetchSize;
        this.finished = finished;
        this.hasResultSet = hasResultSet;
        this.hasNextResult = hasNextResult;
        this.closeStmt = closeStmt;
        this.columnCount = columnCount;

        if (this.hasResultSet) {
            this.transformer = Objects.requireNonNull(transformer);
            this.rows = Objects.requireNonNull(rows);
            this.jdbcMeta = new JdbcResultSetMetadata(Objects.requireNonNull(meta));

            rowsIter = new TransformingIterator<>(rows.iterator(), transformer);
        } else {
            this.updCnt = updCnt;
            this.jdbcMeta = null;
        }

        holdsResource = cursorId != null;
    }

    /**
     * Creates new result set.
     *
     * @param rows Rows.
     * @param meta Column metadata.
     *
     * @exception SQLException if a database access error occurs
     */
    JdbcResultSet(List<List<Object>> rows, List<JdbcColumnMeta> meta) throws SQLException {
        stmt = null;
        cursorId = null;

        finished = true;
        hasResultSet = true;
        hasNextResult = false;
        holdsResource = false;

        this.rowsIter = rows.iterator();
        this.jdbcMeta = new JdbcResultSetMetadata(meta);

        initColumnOrder(jdbcMeta);
    }

    boolean holdResults() {
        return rows != null;
    }

    @Nullable JdbcResultSet getNextResultSet() throws SQLException {
        try {
            if (hasNextResult) {
                assert cursorId != null;

                // all resources will be freed on server by `getMoreResultsAsync` call, so we need to reflect this in local result set
                closed = true;
                holdsResource = false;

                JdbcFetchQueryResultsRequest req = new JdbcFetchQueryResultsRequest(cursorId, fetchSize);
                JdbcQuerySingleResult res = cursorHandler.getMoreResultsAsync(req).get();

                if (!res.success()) {
                    throw IgniteQueryErrorCode.createJdbcSqlException(res.err(), res.status());
                }

                Long newCursorId = res.cursorId();

                List<JdbcColumnMeta> meta = res.meta();

                rows = List.of();

                Function<BinaryTupleReader, List<Object>> transformer = meta != null ? createTransformer(meta) : null;

                int colCount = meta != null ? meta.size() : 0;

                return new JdbcResultSet(cursorHandler, stmt, newCursorId, fetchSize, !res.hasMoreData(), res.items(),
                        meta, res.hasResultSet(), res.hasNextResult(), res.updateCount(), closeStmt, colCount, transformer);
            } else {
                // cursor doesn't have next result, thus let's just close current one
                close0(true);

                return null;
            }
        } catch (InterruptedException e) {
            throw new SQLException("Thread was interrupted.", e);
        } catch (ExecutionException e) {
            throw new SQLException("Fetch request failed.", e);
        } catch (CancellationException e) {
            throw new SQLException("Fetch request canceled.", SqlStateCode.QUERY_CANCELLED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean next() throws SQLException {
        ensureNotClosed();
        if ((rowsIter == null || !rowsIter.hasNext()) && !finished) {
            try {
                JdbcQueryFetchResult res = cursorHandler.fetchAsync(new JdbcFetchQueryResultsRequest(cursorId, fetchSize)).get();

                if (!res.success()) {
                    throw IgniteQueryErrorCode.createJdbcSqlException(res.err(), res.status());
                }

                rows = new ArrayList<>(res.items().size());
                for (ByteBuffer item : res.items()) {
                    rows.add(new BinaryTupleReader(columnCount, item));
                }

                finished = res.last();

                rowsIter = new TransformingIterator<>(rows.iterator(), transformer);
            } catch (InterruptedException e) {
                throw new SQLException("Thread was interrupted.", e);
            } catch (ExecutionException e) {
                throw new SQLException("Fetch request failed.", e);
            } catch (CancellationException e) {
                throw new SQLException("Fetch request canceled.", SqlStateCode.QUERY_CANCELLED);
            }
        }

        if (rowsIter != null) {
            if (rowsIter.hasNext()) {
                curRow = rowsIter.next();

                curPos++;

                return true;
            } else {
                rowsIter = null;
                curRow = null;

                return false;
            }
        } else {
            return false;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void close() throws SQLException {
        close0(!hasNextResult);

        if (closeStmt) {
            stmt.closeIfAllResultsClosed();
        }
    }

    /**
     * Close result set.
     *
     * @param removeFromResources If {@code true} cursor need to be removed from client resources.
     *
     * @throws SQLException On error.
     */
    void close0(boolean removeFromResources) throws SQLException {
        try {
            if (!holdsResource) {
                return;
            }

            holdsResource = !removeFromResources;

            assert cursorId != null;

            if (stmt != null) {
                JdbcQueryCloseResult res = cursorHandler.closeAsync(new JdbcQueryCloseRequest(cursorId, removeFromResources)).get();

                if (!res.success()) {
                    throw IgniteQueryErrorCode.createJdbcSqlException(res.err(), res.status());
                }
            }
        } catch (InterruptedException e) {
            throw new SQLException("Thread was interrupted.", e);
        } catch (ExecutionException e) {
            throw new SQLException("Unable to close result set.", e);
        } catch (CancellationException e) {
            throw new SQLException("Close result set request canceled.", e);
        } finally {
            closed = true;
        }
    }

    boolean holdsResources() {
        return holdsResource;
    }

    /** {@inheritDoc} */
    @Override
    public boolean wasNull() throws SQLException {
        ensureNotClosed();
        ensureHasCurrentRow();

        return wasNull;
    }

    /** {@inheritDoc} */
    @Override
    public String getString(int colIdx) throws SQLException {
        Object value = getValue(colIdx);

        if (value == null) {
            return null;
        } else if (value instanceof Instant) {
            LocalDateTime localDateTime = instantWithLocalTimeZone((Instant) value);
            assert jdbcMeta != null;

            return Formatters.formatDateTime(localDateTime, colIdx, jdbcMeta);
        } else if (value instanceof LocalTime) {
            assert jdbcMeta != null;

            return Formatters.formatTime((LocalTime) value, colIdx, jdbcMeta);
        } else if (value instanceof LocalDateTime) {
            assert jdbcMeta != null;

            return Formatters.formatDateTime((LocalDateTime) value, colIdx, jdbcMeta);
        } else if (value instanceof LocalDate) {
            return Formatters.formatDate((LocalDate) value);
        } else if (value instanceof byte[]) {
            return StringUtils.toHexString((byte[]) value);
        } else {
            return String.valueOf(value);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getString(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getString(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBoolean(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return false;
        }

        Class<?> cls = val.getClass();

        if (cls == Boolean.class) {
            return ((Boolean) val);
        } else if (val instanceof Number) {
            return ((Number) val).intValue() != 0;
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Integer.parseInt(val.toString()) != 0;
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to boolean: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to boolean: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean getBoolean(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getBoolean(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public byte getByte(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return 0;
        }

        Class<?> cls = val.getClass();

        if (val instanceof Number) {
            return ((Number) val).byteValue();
        } else if (cls == Boolean.class) {
            return (Boolean) val ? (byte) 1 : (byte) 0;
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Byte.parseByte(val.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to byte: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to byte: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte getByte(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getByte(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public short getShort(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return 0;
        }

        Class<?> cls = val.getClass();

        if (val instanceof Number) {
            return ((Number) val).shortValue();
        } else if (cls == Boolean.class) {
            return (Boolean) val ? (short) 1 : (short) 0;
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Short.parseShort(val.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to short: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to short: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public short getShort(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getShort(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public int getInt(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return 0;
        }

        Class<?> cls = val.getClass();

        if (val instanceof Number) {
            return ((Number) val).intValue();
        } else if (cls == Boolean.class) {
            return (Boolean) val ? 1 : 0;
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Integer.parseInt(val.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to int: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to int: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getInt(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getInt(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public long getLong(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return 0;
        }

        Class<?> cls = val.getClass();

        if (val instanceof Number) {
            return ((Number) val).longValue();
        } else if (cls == Boolean.class) {
            return ((Boolean) val ? 1 : 0);
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Long.parseLong(val.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to long: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to long: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public long getLong(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getLong(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public float getFloat(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return 0;
        }

        Class<?> cls = val.getClass();

        if (val instanceof Number) {
            return ((Number) val).floatValue();
        } else if (cls == Boolean.class) {
            return (float) ((Boolean) val ? 1 : 0);
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Float.parseFloat(val.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to float: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to float: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public float getFloat(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getFloat(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public double getDouble(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return 0d;
        }

        Class<?> cls = val.getClass();

        if (val instanceof Number) {
            return ((Number) val).doubleValue();
        } else if (cls == Boolean.class) {
            return ((Boolean) val ? 1d : 0d);
        } else if (cls == String.class || cls == Character.class) {
            try {
                return Double.parseDouble(val.toString());
            } catch (NumberFormatException e) {
                throw new SQLException("Cannot convert to double: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to double: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public double getDouble(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getDouble(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal getBigDecimal(int colIdx, int scale) throws SQLException {
        BigDecimal val = getBigDecimal(colIdx);

        return val == null ? null : val.setScale(scale, RoundingMode.HALF_UP);
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal getBigDecimal(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return null;
        }

        Class<?> cls = val.getClass();

        if (cls == BigDecimal.class) {
            return (BigDecimal) val;
        } else if (val instanceof Number) {
            return new BigDecimal(((Number) val).doubleValue());
        } else if (cls == Boolean.class) {
            return new BigDecimal((Boolean) val ? 1 : 0);
        } else if (cls == String.class || cls == Character.class) {
            try {
                return (BigDecimal) decimalFormat.get().parse(val.toString());
            } catch (ParseException e) {
                throw new SQLException("Cannot convert to BigDecimal: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to BigDecimal: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal getBigDecimal(String colLb, int scale) throws SQLException {
        int colIdx = findColumn(colLb);

        return getBigDecimal(colIdx, scale);
    }

    /** {@inheritDoc} */
    @Override
    public BigDecimal getBigDecimal(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getBigDecimal(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getBytes(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return null;
        }

        Class<?> cls = val.getClass();

        if (cls == byte[].class) {
            return (byte[]) val;
        } else if (cls == Byte.class) {
            return new byte[]{(byte) val};
        } else if (cls == Short.class) {
            short x = (short) val;

            return new byte[]{(byte) (x >> 8), (byte) x};
        } else if (cls == Integer.class) {
            int x = (int) val;

            return new byte[]{(byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) x};
        } else if (cls == Long.class) {
            long x = (long) val;

            return new byte[]{(byte) (x >> 56), (byte) (x >> 48), (byte) (x >> 40), (byte) (x >> 32),
                    (byte) (x >> 24), (byte) (x >> 16), (byte) (x >> 8), (byte) x};
        } else if (cls == Float.class) {
            return ByteBuffer.allocate(4).putFloat(((Float) val)).array();
        } else if (cls == Double.class) {
            return ByteBuffer.allocate(8).putDouble(((Double) val)).array();
        } else if (cls == String.class) {
            return ((String) val).getBytes(UTF_8);
        } else if (cls == UUID.class) {
            ByteBuffer bb = ByteBuffer.wrap(new byte[16]);

            bb.putLong(((UUID) val).getMostSignificantBits());
            bb.putLong(((UUID) val).getLeastSignificantBits());

            return bb.array();
        } else {
            throw new SQLException("Cannot convert to byte[]: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public byte[] getBytes(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getBytes(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Date getDate(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return null;
        }

        Class<?> cls = val.getClass();

        if (cls == LocalDate.class) {
            return Date.valueOf((LocalDate) val);
        } else if (cls == LocalTime.class) {
            return new Date(Time.valueOf((LocalTime) val).getTime());
        } else if (cls == Instant.class) {
            LocalDateTime localDateTime = instantWithLocalTimeZone((Instant) val);

            return Date.valueOf(localDateTime.toLocalDate());
        } else if (cls == LocalDateTime.class) {
            return Date.valueOf(((LocalDateTime) val).toLocalDate());
        } else {
            throw new SQLException("Cannot convert to date: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Date getDate(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getDate(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Date getDate(int colIdx, Calendar cal) throws SQLException {
        return getDate(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Date getDate(String colLb, Calendar cal) throws SQLException {
        int colIdx = findColumn(colLb);

        return getDate(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Time getTime(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return null;
        }

        Class<?> cls = val.getClass();

        if (cls == LocalTime.class) {
            return Time.valueOf((LocalTime) val);
        } else if (cls == LocalDate.class) {
            return new Time(Date.valueOf((LocalDate) val).getTime());
        } else if (cls == Instant.class) {
            LocalDateTime localDateTime = instantWithLocalTimeZone((Instant) val);
            LocalTime localTime = localDateTime.toLocalTime();

            return Time.valueOf(localTime);
        } else if (cls == LocalDateTime.class) {
            return Time.valueOf(((LocalDateTime) val).toLocalTime());
        } else {
            throw new SQLException("Cannot convert to time: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Time getTime(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getTime(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Time getTime(int colIdx, Calendar cal) throws SQLException {
        return getTime(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Time getTime(String colLb, Calendar cal) throws SQLException {
        int colIdx = findColumn(colLb);

        return getTime(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Timestamp getTimestamp(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return null;
        }

        Class<?> cls = val.getClass();

        if (cls == LocalTime.class) {
            return new Timestamp(Time.valueOf((LocalTime) val).getTime());
        } else if (cls == LocalDate.class) {
            return new Timestamp(Date.valueOf((LocalDate) val).getTime());
        } else if (cls == Instant.class) {
            LocalDateTime localDateTime = instantWithLocalTimeZone((Instant) val);

            return Timestamp.valueOf(localDateTime);
        } else if (cls == LocalDateTime.class) {
            return Timestamp.valueOf((LocalDateTime) val);
        } else {
            throw new SQLException("Cannot convert to timestamp: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public Timestamp getTimestamp(int colIdx, Calendar cal) throws SQLException {
        return getTimestamp(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Timestamp getTimestamp(String colLb, Calendar cal) throws SQLException {
        int colIdx = findColumn(colLb);

        return getTimestamp(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Timestamp getTimestamp(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getTimestamp(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAsciiStream(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAsciiStream(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getUnicodeStream(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getUnicodeStream(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getBinaryStream(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Stream are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getBinaryStream(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public SQLWarning getWarnings() throws SQLException {
        ensureNotClosed();

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public void clearWarnings() throws SQLException {
        ensureNotClosed();
    }

    /** {@inheritDoc} */
    @Override
    public String getCursorName() throws SQLException {
        ensureNotClosed();

        return null;
    }

    /** {@inheritDoc} */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        ensureNotClosed();

        return metaOrThrow();
    }

    /** {@inheritDoc} */
    @Override
    public int findColumn(String colLb) throws SQLException {
        ensureNotClosed();

        Objects.requireNonNull(colLb);

        Integer order = columnOrder().get(colLb.toUpperCase());

        if (order == null) {
            throw new SQLException("Column not found: " + colLb, SqlStateCode.PARSING_EXCEPTION);
        }

        assert order >= 0;

        return order + 1;
    }

    /** {@inheritDoc} */
    @Override
    public Reader getCharacterStream(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Reader getCharacterStream(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Streams are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean isBeforeFirst() throws SQLException {
        ensureNotClosed();

        return curPos == 0 && rowsIter != null && rowsIter.hasNext();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAfterLast() throws SQLException {
        ensureNotClosed();

        return finished && rowsIter == null && curRow == null;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isFirst() throws SQLException {
        ensureNotClosed();

        return curPos == 1;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isLast() throws SQLException {
        ensureNotClosed();

        return finished && rowsIter != null && !rowsIter.hasNext() && curRow != null;
    }

    /** {@inheritDoc} */
    @Override
    public void beforeFirst() throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public void afterLast() throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean first() throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean last() throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public int getRow() throws SQLException {
        ensureNotClosed();

        return isAfterLast() ? 0 : curPos;
    }

    /** {@inheritDoc} */
    @Override
    public boolean absolute(int row) throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean relative(int rows) throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public boolean previous() throws SQLException {
        ensureNotClosed();

        throw new SQLException("Result set is forward-only.");
    }

    /** {@inheritDoc} */
    @Override
    public void setFetchDirection(int direction) throws SQLException {
        ensureNotClosed();

        if (direction != FETCH_FORWARD) {
            throw new SQLFeatureNotSupportedException("Only forward direction is supported");
        }
    }

    /** {@inheritDoc} */
    @Override
    public int getFetchDirection() throws SQLException {
        ensureNotClosed();

        return FETCH_FORWARD;
    }

    /** {@inheritDoc} */
    @Override
    public void setFetchSize(int fetchSize) throws SQLException {
        ensureNotClosed();

        if (fetchSize <= 0) {
            throw new SQLException("Fetch size must be greater than zero.");
        }

        this.fetchSize = fetchSize;
    }

    /** {@inheritDoc} */
    @Override
    public int getFetchSize() throws SQLException {
        ensureNotClosed();

        return fetchSize;
    }

    /** {@inheritDoc} */
    @Override
    public int getType() throws SQLException {
        ensureNotClosed();

        return stmt.getResultSetType();
    }

    /** {@inheritDoc} */
    @Override
    public int getConcurrency() throws SQLException {
        ensureNotClosed();

        return CONCUR_READ_ONLY;
    }

    /** {@inheritDoc} */
    @Override
    public boolean rowUpdated() throws SQLException {
        ensureNotClosed();

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean rowInserted() throws SQLException {
        ensureNotClosed();

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public boolean rowDeleted() throws SQLException {
        ensureNotClosed();

        return false;
    }

    /** {@inheritDoc} */
    @Override
    public void updateNull(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNull(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBoolean(int colIdx, boolean x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBoolean(String colLb, boolean x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateByte(int colIdx, byte x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateByte(String colLb, byte x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateShort(int colIdx, short x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateShort(String colLb, short x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateInt(int colIdx, int x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateInt(String colLb, int x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateLong(int colIdx, long x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateLong(String colLb, long x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateFloat(int colIdx, float x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateFloat(String colLb, float x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateDouble(int colIdx, double x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateDouble(String colLb, double x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBigDecimal(int colIdx, BigDecimal x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBigDecimal(String colLb, BigDecimal x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateString(int colIdx, String x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateString(String colLb, String x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBytes(int colIdx, byte[] x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBytes(String colLb, byte[] x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateDate(int colIdx, Date x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateDate(String colLb, Date x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateTime(int colIdx, Time x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateTime(String colLb, Time x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateTimestamp(int colIdx, Timestamp x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateTimestamp(String colLb, Timestamp x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateAsciiStream(int colIdx, InputStream x, int len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateAsciiStream(String colLb, InputStream x, int len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateAsciiStream(int colIdx, InputStream x, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateAsciiStream(String colLb, InputStream x, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateAsciiStream(int colIdx, InputStream x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateAsciiStream(String colLb, InputStream x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBinaryStream(int colIdx, InputStream x, int len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBinaryStream(int colIdx, InputStream x, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBinaryStream(String colLb, InputStream x, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBinaryStream(int colIdx, InputStream x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBinaryStream(String colLb, InputStream x, int len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBinaryStream(String colLb, InputStream x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateCharacterStream(int colIdx, Reader x, int len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateCharacterStream(String colLb, Reader reader, int len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateCharacterStream(int colIdx, Reader x, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateCharacterStream(String colLb, Reader reader, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateCharacterStream(int colIdx, Reader x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateCharacterStream(String colLb, Reader reader) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateObject(int colIdx, Object x, int scaleOrLen) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateObject(int colIdx, Object x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateObject(String colLb, Object x, int scaleOrLen) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateObject(String colLb, Object x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void insertRow() throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateRow() throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void deleteRow() throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void refreshRow() throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Row refreshing is not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void cancelRowUpdates() throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Row updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void moveToInsertRow() throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void moveToCurrentRow() throws SQLException {
        ensureNotClosed();

        if (getConcurrency() == CONCUR_READ_ONLY) {
            throw new SQLException("The result set concurrency is CONCUR_READ_ONLY");
        }
    }

    /** {@inheritDoc} */
    @Override
    public Statement getStatement() throws SQLException {
        ensureNotClosed();

        return stmt;
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject(int colIdx, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException("SQL structured type are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getObject(int colIdx, Class<T> targetCls) throws SQLException {
        ensureNotClosed();

        return (T) getObject0(colIdx, targetCls);
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getObject(String colLb, Class<T> type) throws SQLException {
        int colIdx = findColumn(colLb);

        return getObject(colIdx, type);
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject(int colIdx) throws SQLException {
        return getValue(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getValue(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public Object getObject(String colLb, Map<String, Class<?>> map) throws SQLException {
        throw new SQLFeatureNotSupportedException("SQL structured type are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Ref getRef(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Ref getRef(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Blob getBlob(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Blob getBlob(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Clob getClob(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Clob getClob(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Array getArray(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Array getArray(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public URL getURL(int colIdx) throws SQLException {
        Object val = getValue(colIdx);

        if (val == null) {
            return null;
        }

        Class<?> cls = val.getClass();

        if (cls == URL.class) {
            return (URL) val;
        } else if (cls == String.class) {
            try {
                return new URL(val.toString());
            } catch (MalformedURLException e) {
                throw new SQLException("Cannot convert to URL: " + val, SqlStateCode.CONVERSION_FAILED, e);
            }
        } else {
            throw new SQLException("Cannot convert to URL: " + val, SqlStateCode.CONVERSION_FAILED);
        }
    }

    /** {@inheritDoc} */
    @Override
    public URL getURL(String colLb) throws SQLException {
        int colIdx = findColumn(colLb);

        return getURL(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public void updateRef(int colIdx, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateRef(String colLb, Ref x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBlob(int colIdx, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBlob(String colLb, Blob x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBlob(int colIdx, InputStream inputStream, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBlob(String colLb, InputStream inputStream, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBlob(int colIdx, InputStream inputStream) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateBlob(String colLb, InputStream inputStream) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateClob(int colIdx, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateClob(String colLb, Clob x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateClob(int colIdx, Reader reader, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateClob(String colLb, Reader reader, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateClob(int colIdx, Reader reader) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateClob(String colLb, Reader reader) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateArray(int colIdx, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateArray(String colLb, Array x) throws SQLException {
        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public RowId getRowId(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public RowId getRowId(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateRowId(int colIdx, RowId x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateRowId(String colLb, RowId x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public int getHoldability() throws SQLException {
        ensureNotClosed();

        return HOLD_CURSORS_OVER_COMMIT;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isClosed() throws SQLException {
        return closed || stmt == null || stmt.isClosed();
    }

    /** {@inheritDoc} */
    @Override
    public void updateNString(int colIdx, String val) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNString(String colLb, String val) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNClob(int colIdx, NClob val) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNClob(String colLb, NClob val) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNClob(int colIdx, Reader reader, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNClob(String colLb, Reader reader, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNClob(int colIdx, Reader reader) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNClob(String colLb, Reader reader) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public NClob getNClob(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public NClob getNClob(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public SQLXML getSQLXML(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public SQLXML getSQLXML(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateSQLXML(int colIdx, SQLXML xmlObj) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateSQLXML(String colLb, SQLXML xmlObj) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public String getNString(int colIdx) throws SQLException {
        return getString(colIdx);
    }

    /** {@inheritDoc} */
    @Override
    public String getNString(String colLb) throws SQLException {
        return getString(colLb);
    }

    /** {@inheritDoc} */
    @Override
    public Reader getNCharacterStream(int colIdx) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public Reader getNCharacterStream(String colLb) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("SQL-specific types are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNCharacterStream(int colIdx, Reader x, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNCharacterStream(String colLb, Reader reader, long len) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNCharacterStream(int colIdx, Reader x) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public void updateNCharacterStream(String colLb, Reader reader) throws SQLException {
        ensureNotClosed();

        throw new SQLFeatureNotSupportedException("Updates are not supported.");
    }

    /** {@inheritDoc} */
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        if (!isWrapperFor(iface)) {
            throw new SQLException("Result set is not a wrapper for " + iface.getName());
        }

        return (T) this;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        return iface != null && iface.isAssignableFrom(JdbcResultSet.class);
    }

    /**
     * Get the isQuery flag.
     *
     * @return Is query flag.
     */
    public boolean hasResultSet() {
        return hasResultSet;
    }

    /**
     * Gets object field value by index.
     *
     * @param colIdx Column index.
     * @return Object field value.
     * @throws SQLException In case of error.
     */
    private Object getValue(int colIdx) throws SQLException {
        ensureNotClosed();
        ensureHasCurrentRow();

        try {
            assert curRow != null;

            Object val = curRow.get(colIdx - 1);

            wasNull = val == null;

            return val;
        } catch (IndexOutOfBoundsException e) {
            throw new SQLException("Invalid column index: " + colIdx, SqlStateCode.PARSING_EXCEPTION, e);
        }
    }

    private LocalDateTime instantWithLocalTimeZone(Instant val) throws SQLException {
        JdbcConnection connection = (JdbcConnection) stmt.getConnection();
        ZoneId zoneId = connection.connectionProperties().getConnectionTimeZone();
        if (zoneId == null) {
            zoneId = ZoneId.systemDefault();
        }
        return LocalDateTime.ofInstant(val, zoneId);
    }

    /**
     * Ensures that result set is positioned on a row.
     *
     * @throws SQLException If result set is not positioned on a row.
     */
    private void ensureHasCurrentRow() throws SQLException {
        if (curRow == null) {
            throw new SQLException("Result set is not positioned on a row.");
        }
    }

    /**
     * Ensures that result set is not closed.
     *
     * @throws SQLException If result set is closed.
     */
    private void ensureNotClosed() throws SQLException {
        if (closed) {
            throw new SQLException("Result set is closed.", SqlStateCode.INVALID_CURSOR_STATE);
        }
    }

    /**
     * Get the update count.
     *
     * @return Update count for no-SELECT queries.
     */
    public long updatedCount() {
        return updCnt;
    }

    /**
     * Set the close statement flag.
     *
     * @param closeStmt Close statement on this result set close.
     */
    public void closeStatement(boolean closeStmt) {
        this.closeStmt = closeStmt;
    }

    /**
     * Get object of given class.
     *
     * @param colIdx    Column index.
     * @param targetCls Class representing the Java data type to convert the designated column to.
     * @return Converted object.
     * @throws SQLException On error.
     */
    private Object getObject0(int colIdx, Class<?> targetCls) throws SQLException {
        if (targetCls == Boolean.class) {
            return getBoolean(colIdx);
        } else if (targetCls == Byte.class) {
            return getByte(colIdx);
        } else if (targetCls == Short.class) {
            return getShort(colIdx);
        } else if (targetCls == Integer.class) {
            return getInt(colIdx);
        } else if (targetCls == Long.class) {
            return getLong(colIdx);
        } else if (targetCls == Float.class) {
            return getFloat(colIdx);
        } else if (targetCls == Double.class) {
            return getDouble(colIdx);
        } else if (targetCls == String.class) {
            return getString(colIdx);
        } else if (targetCls == BigDecimal.class) {
            return getBigDecimal(colIdx);
        } else if (targetCls == Date.class) {
            return getDate(colIdx);
        } else if (targetCls == Time.class) {
            return getTime(colIdx);
        } else if (targetCls == Timestamp.class) {
            return getTimestamp(colIdx);
        } else if (targetCls == byte[].class) {
            return getBytes(colIdx);
        } else if (targetCls == URL.class) {
            return getURL(colIdx);
        } else {
            Object val = getValue(colIdx);

            if (val == null) {
                return null;
            }

            Class<?> cls = val.getClass();

            if (targetCls.isAssignableFrom(cls)) {
                return val;
            } else {
                throw new SQLException("Cannot convert to " + targetCls.getName() + ": " + val,
                        SqlStateCode.CONVERSION_FAILED);
            }
        }
    }

    /**
     * Init if needed and return column order.
     *
     * @return Column order map.
     * @throws SQLException On error.
     */
    private Map<String, Integer> columnOrder() throws SQLException {
        if (colOrder != null) {
            return colOrder;
        }

        initColumnOrder(metaOrThrow());

        return colOrder;
    }

    /**
     * Init column order map.
     */
    private void initColumnOrder(JdbcResultSetMetadata jdbcMeta) throws SQLException {
        colOrder = new HashMap<>(jdbcMeta.getColumnCount());

        for (int i = 0; i < jdbcMeta.getColumnCount(); ++i) {
            String colName = jdbcMeta.getColumnLabel(i + 1).toUpperCase();

            if (!colOrder.containsKey(colName)) {
                colOrder.put(colName, i);
            }
        }
    }

    private JdbcResultSetMetadata metaOrThrow() throws SQLException {
        if (jdbcMeta == null) {
            throw new SQLException("Result doesn't have metadata");
        }

        return jdbcMeta;
    }

    static Function<BinaryTupleReader, List<Object>> createTransformer(List<JdbcColumnMeta> meta) {
        return (tuple) -> {
            int columnCount = meta.size();
            List<Object> row = new ArrayList<>(columnCount);
            int currentDecimalScale = -1;

            int idx = 0;
            for (JdbcColumnMeta columnMeta : meta) {
                ColumnType type = columnMeta.columnType();
                if (type == ColumnType.DECIMAL) {
                    currentDecimalScale = columnMeta.scale();
                }

                row.add(JdbcConverterUtils.deriveValueFromBinaryTuple(type, tuple, idx++, currentDecimalScale));
            }

            return row;
        };
    }

    private static class Formatters {
        static final DateTimeFormatter TIME = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter();

        static final DateTimeFormatter DATE = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .toFormatter();

        static final DateTimeFormatter DATE_TIME = new DateTimeFormatterBuilder()
                .appendValue(ChronoField.YEAR, 4)
                .appendLiteral('-')
                .appendValue(ChronoField.MONTH_OF_YEAR, 2)
                .appendLiteral('-')
                .appendValue(ChronoField.DAY_OF_MONTH, 2)
                .appendLiteral(' ')
                .appendValue(ChronoField.HOUR_OF_DAY, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                .appendLiteral(':')
                .appendValue(ChronoField.SECOND_OF_MINUTE, 2)
                .toFormatter();

        static String formatTime(LocalTime value, int colIdx, JdbcResultSetMetadata jdbcMeta) throws SQLException {
            return formatWithPrecision(TIME, value, colIdx, jdbcMeta);
        }

        static String formatDateTime(LocalDateTime value, int colIdx, JdbcResultSetMetadata jdbcMeta) throws SQLException {
            return formatWithPrecision(DATE_TIME, value, colIdx, jdbcMeta);
        }

        static String formatDate(LocalDate value) {
            return DATE.format(value);
        }

        private static String formatWithPrecision(
                DateTimeFormatter formatter, 
                TemporalAccessor value, 
                int colIdx,
                JdbcResultSetMetadata jdbcMeta
        ) throws SQLException {

            StringBuilder sb = new StringBuilder();

            formatter.formatTo(value, sb);

            int precision = jdbcMeta.getPrecision(colIdx);
            if (precision <= 0) {
                return sb.toString();
            }

            assert precision <= 9 : "Precision is out of range. Precision: " + precision + ". Column: " + colIdx;

            // Append nano seconds according to the specified precision.
            long nanos = value.getLong(ChronoField.NANO_OF_SECOND);
            long scaled  = nanos / (long) Math.pow(10, 9 - precision);

            sb.append('.');
            for (int i = 0; i < precision; i++) {
                sb.append('0');
            }

            int pos = precision - 1;
            int start = sb.length() - precision;

            do {
                int digit = (int) (scaled % 10);
                char c = (char) ('0' + digit);
                sb.setCharAt(start + pos, c);
                scaled /= 10;
                pos--;
            } while (scaled != 0 && pos >= 0);

            return sb.toString();
        }
    }
}
