package BlueCrab.com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@RestController
@RequestMapping("/api/database")
public class DatabaseController {
    
    @Autowired
    private DataSource dataSource;
    
    /**
     * 데이터베이스 정보 조회
     */
    @GetMapping("/info")
    public Map<String, Object> getDatabaseInfo() {
        Map<String, Object> info = new HashMap<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            info.put("databaseProductName", metaData.getDatabaseProductName());
            info.put("databaseProductVersion", metaData.getDatabaseProductVersion());
            info.put("driverName", metaData.getDriverName());
            info.put("driverVersion", metaData.getDriverVersion());
            info.put("url", metaData.getURL());
            info.put("userName", metaData.getUserName());
            info.put("catalog", connection.getCatalog());
            info.put("schema", connection.getSchema());
            info.put("maxConnections", metaData.getMaxConnections());
            info.put("supportsTransactions", metaData.supportsTransactions());
            
        } catch (SQLException e) {
            info.put("error", "데이터베이스 정보 조회 실패: " + e.getMessage());
        }
        
        return info;
    }
    
    /**
     * 테이블 목록 조회
     */
    @GetMapping("/tables")
    public List<Map<String, Object>> getTables() {
        List<Map<String, Object>> tables = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet rs = metaData.getTables(connection.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    Map<String, Object> table = new HashMap<>();
                    table.put("tableName", rs.getString("TABLE_NAME"));
                    table.put("tableType", rs.getString("TABLE_TYPE"));
                    table.put("remarks", rs.getString("REMARKS"));
                    tables.add(table);
                }
            }
            
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "테이블 목록 조회 실패: " + e.getMessage());
            tables.add(error);
        }
        
        return tables;
    }
    
    /**
     * 특정 테이블의 컬럼 정보 조회
     */
    @GetMapping("/tables/{tableName}/columns")
    public List<Map<String, Object>> getTableColumns(@PathVariable String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            
            try (ResultSet rs = metaData.getColumns(connection.getCatalog(), null, tableName, "%")) {
                while (rs.next()) {
                    Map<String, Object> column = new HashMap<>();
                    column.put("columnName", rs.getString("COLUMN_NAME"));
                    column.put("dataType", rs.getString("TYPE_NAME"));
                    column.put("columnSize", rs.getInt("COLUMN_SIZE"));
                    column.put("nullable", rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable);
                    column.put("defaultValue", rs.getString("COLUMN_DEF"));
                    column.put("remarks", rs.getString("REMARKS"));
                    column.put("ordinalPosition", rs.getInt("ORDINAL_POSITION"));
                    columns.add(column);
                }
            }
            
        } catch (SQLException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "컬럼 정보 조회 실패: " + e.getMessage());
            columns.add(error);
        }
        
        return columns;
    }
    
    /**
     * 특정 테이블의 샘플 데이터 조회 (최대 10행)
     */
    @GetMapping("/tables/{tableName}/sample")
    public Map<String, Object> getTableSample(@PathVariable String tableName) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        
        try (Connection connection = dataSource.getConnection()) {
            String sql = "SELECT * FROM " + tableName + " LIMIT 10";
            
            try (PreparedStatement stmt = connection.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                
                // 컬럼 정보
                List<String> columnNames = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    columnNames.add(metaData.getColumnName(i));
                }
                result.put("columns", columnNames);
                
                // 데이터 행들
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= columnCount; i++) {
                        row.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(row);
                }
                
                result.put("data", rows);
                result.put("rowCount", rows.size());
                
            }
            
        } catch (SQLException e) {
            result.put("error", "샘플 데이터 조회 실패: " + e.getMessage());
        }
        
        return result;
    }
    
    /**
     * 직접 SQL 쿼리 실행 (SELECT만 허용)
     */
    @GetMapping("/query")
    public Map<String, Object> executeQuery(@RequestParam String sql) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        
        // SELECT 쿼리만 허용
        if (!sql.trim().toUpperCase().startsWith("SELECT")) {
            result.put("error", "SELECT 쿼리만 허용됩니다.");
            return result;
        }
        
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            ResultSetMetaData metaData = rs.getMetaData();
            int columnCount = metaData.getColumnCount();
            
            // 컬럼 정보
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(metaData.getColumnName(i));
            }
            result.put("columns", columnNames);
            
            // 데이터 행들 (최대 100행 제한)
            int rowCount = 0;
            while (rs.next() && rowCount < 100) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(metaData.getColumnName(i), rs.getObject(i));
                }
                rows.add(row);
                rowCount++;
            }
            
            result.put("data", rows);
            result.put("rowCount", rows.size());
            result.put("sql", sql);
            
        } catch (SQLException e) {
            result.put("error", "쿼리 실행 실패: " + e.getMessage());
            result.put("sql", sql);
        }
        
        return result;
    }
}
