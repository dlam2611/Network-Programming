package com.multiserver.server.dao;

import com.multiserver.common.model.User;
import com.multiserver.server.database.DBConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object thực hiện các thao tác CRUD trên bảng 'users'.
 */
public class UserDAO {
    private final DBConnection dbConnection;

    public UserDAO(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public User findByEmail(String email) {
        String sql = "SELECT user_id, name, email, password, balance, last_updated FROM users WHERE email = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.findByEmail lỗi: " + e.getMessage());
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT user_id, name, email, password, balance, last_updated FROM users WHERE user_id = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.findById lỗi: " + e.getMessage());
        }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT user_id, name, email, password, balance, last_updated FROM users ORDER BY user_id ASC";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.findAll lỗi: " + e.getMessage());
        }
        return list;
    }

    public boolean insertUser(User user) {
        String sql = "INSERT INTO users (name, email, password, balance, last_updated) VALUES (?, ?, ?, ?, ?) RETURNING user_id";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPassword());
            ps.setBigDecimal(4, user.getBalance() != null ? user.getBalance() : BigDecimal.ZERO);
            Timestamp ts = user.getLastUpdated() != null ? user.getLastUpdated() : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(5, ts);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    user.setUserId(rs.getInt(1));
                    user.setLastUpdated(ts);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.insertUser lỗi: " + e.getMessage());
        }
        return false;
    }

    /**
     * Dùng cho đồng bộ từ Server khác: Chèn người dùng hoặc cập nhật nếu đã có email,
     * đồng thời đồng bộ cả user_id nếu có.
     */
    public boolean saveOrUpdateUser(User user) {
        String checkSql = "SELECT user_id FROM users WHERE email = ?";
        try (Connection conn = dbConnection.getConnection()) {
            boolean exists = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, user.getEmail());
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next()) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String updateSql = "UPDATE users SET name = ?, password = ?, balance = ?, last_updated = ? WHERE email = ?";
                try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getPassword());
                    ps.setBigDecimal(3, user.getBalance());
                    ps.setTimestamp(4, user.getLastUpdated());
                    ps.setString(5, user.getEmail());
                    return ps.executeUpdate() > 0;
                }
            } else {
                String insertSql = "INSERT INTO users (name, email, password, balance, last_updated) VALUES (?, ?, ?, ?, ?) RETURNING user_id";
                try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                    ps.setString(1, user.getName());
                    ps.setString(2, user.getEmail());
                    ps.setString(3, user.getPassword());
                    ps.setBigDecimal(4, user.getBalance());
                    ps.setTimestamp(5, user.getLastUpdated());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            user.setUserId(rs.getInt(1));
                            return true;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.saveOrUpdateUser lỗi: " + e.getMessage());
        }
        return false;
    }

    public boolean updateBalance(String email, BigDecimal newBalance, Timestamp timestamp) {
        String sql = "UPDATE users SET balance = ?, last_updated = ? WHERE email = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, newBalance);
            ps.setTimestamp(2, timestamp != null ? timestamp : new Timestamp(System.currentTimeMillis()));
            ps.setString(3, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.updateBalance lỗi: " + e.getMessage());
            return false;
        }
    }

    /**
     * Thực hiện chuyển khoản trong 1 giao dịch SQL atomic (AutoCommit = false)
     */
    public boolean executeTransfer(String fromEmail, String toEmail, BigDecimal amount, Timestamp timestamp) {
        String deductSql = "UPDATE users SET balance = balance - ?, last_updated = ? WHERE email = ? AND balance >= ?";
        String addSql = "UPDATE users SET balance = balance + ?, last_updated = ? WHERE email = ?";

        try (Connection conn = dbConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Trừ tiền người gửi
                try (PreparedStatement ps1 = conn.prepareStatement(deductSql)) {
                    ps1.setBigDecimal(1, amount);
                    ps1.setTimestamp(2, timestamp);
                    ps1.setString(3, fromEmail);
                    ps1.setBigDecimal(4, amount);
                    int rows = ps1.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        return false; // Không đủ tiền hoặc người gửi không tồn tại
                    }
                }

                // 2. Cộng tiền người nhận
                try (PreparedStatement ps2 = conn.prepareStatement(addSql)) {
                    ps2.setBigDecimal(1, amount);
                    ps2.setTimestamp(2, timestamp);
                    ps2.setString(3, toEmail);
                    int rows = ps2.executeUpdate();
                    if (rows == 0) {
                        conn.rollback();
                        return false; // Người nhận không tồn tại
                    }
                }

                conn.commit();
                return true;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.executeTransfer lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean updateUser(User user) {
        String sql = "UPDATE users SET name = ?, balance = ?, last_updated = ? WHERE email = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getName());
            ps.setBigDecimal(2, user.getBalance());
            Timestamp ts = user.getLastUpdated() != null ? user.getLastUpdated() : new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(3, ts);
            ps.setString(4, user.getEmail());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.updateUser lỗi: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteUser(String email) {
        String sql = "DELETE FROM users WHERE email = ?";
        try (Connection conn = dbConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("UserDAO.deleteUser lỗi: " + e.getMessage());
            return false;
        }
    }

    public BigDecimal getTotalBalance() {
        String sql = "SELECT COALESCE(SUM(balance), 0) FROM users";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getBigDecimal(1);
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.getTotalBalance lỗi: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    public int getUserCount() {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = dbConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("UserDAO.getUserCount lỗi: " + e.getMessage());
        }
        return 0;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getInt("user_id"));
        user.setName(rs.getString("name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setBalance(rs.getBigDecimal("balance"));
        user.setLastUpdated(rs.getTimestamp("last_updated"));
        return user;
    }
}

