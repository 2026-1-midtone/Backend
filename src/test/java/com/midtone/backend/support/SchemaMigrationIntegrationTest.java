package com.midtone.backend.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SchemaMigrationIntegrationTest extends IntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void 마이그레이션이_모두_적용되고_엔티티_매핑과_일치한다() throws Exception {
        List<String> tables = queryColumn("SHOW TABLES");

        assertTrue(tables.containsAll(List.of(
                "users", "user_settings", "routine_tasks", "nap_sessions",
                "notification_settings", "shift_schedules", "shift_patterns",
                "shift_pattern_items", "daily_coachings", "coaching_cards")));
    }

    @Test
    void 코칭_테이블에_사용자_날짜_유니크_키가_있다() throws Exception {
        List<String> indexNames = queryColumn(
                "SELECT DISTINCT index_name FROM information_schema.statistics"
                        + " WHERE table_schema = DATABASE() AND table_name = 'daily_coachings'");

        assertTrue(indexNames.contains("uk_daily_coachings_user_date"));
    }

    @Test
    void 사용자를_삭제하면_연관_데이터가_함께_삭제된다() throws Exception {
        List<String> deleteRules = queryColumn(
                "SELECT delete_rule FROM information_schema.referential_constraints"
                        + " WHERE constraint_schema = DATABASE()"
                        + " AND constraint_name = 'fk_shift_schedules_user'");

        assertEquals(List.of("CASCADE"), deleteRules);
    }

    private List<String> queryColumn(String sql) throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(sql)) {
            List<String> values = new ArrayList<>();
            while (resultSet.next()) {
                values.add(resultSet.getString(1));
            }
            return values;
        }
    }
}
