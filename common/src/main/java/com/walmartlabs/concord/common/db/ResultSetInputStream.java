package com.walmartlabs.concord.common.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ResultSetInputStream extends InputStream {

    public static InputStream open(Connection conn, PreparedStatement ps, int columnIndex) throws SQLException {
        ResultSet rs = null;
        try {
            rs = ps.executeQuery();
            if (!rs.next()) {
                closeSilently(rs);
                return null;
            }
        } catch (SQLException e) {
            closeSilently(rs);
            throw e;
        }
        return new ResultSetInputStream(conn, rs, columnIndex);
    }

    private static void closeSilently(AutoCloseable c) {
        if (c == null) {
            return;
        }

        try {
            c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private final Connection conn;
    private final ResultSet rs;
    private final int columnIndex;

    private InputStream delegate;

    private ResultSetInputStream(Connection conn, ResultSet rs, int columnIndex) {
        this.conn = conn;
        this.rs = rs;
        this.columnIndex = columnIndex;
    }

    @Override
    public int read() throws IOException {
        return ensureDelegate().read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return ensureDelegate().read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return ensureDelegate().read(b, off, len);
    }

    private InputStream ensureDelegate() throws IOException {
        if (delegate == null) {
            try {
                delegate = rs.getBinaryStream(1);
            } catch (SQLException e) {
                throw new IOException("Can't open a stream", e);
            }
        }
        return delegate;
    }

    @Override
    public void close() throws IOException {
        closeSilently(delegate);
        closeSilently(rs);
        closeSilently(conn);
    }
}
