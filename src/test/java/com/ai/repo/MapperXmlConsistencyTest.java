package com.ai.repo;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MapperXmlConsistencyTest {

    private static final Pattern SQL_COLUMN = Pattern.compile(
            "(?i)^\\s+(?:(?:ADD|MODIFY)\\s+COLUMN\\s+)?`?(\\w+)`?\\s+"
                    + "(VARCHAR|BIGINT|INT|TINYINT|BOOLEAN|TEXT|DATETIME|TIMESTAMP|JSON|DOUBLE|DECIMAL|FLOAT|BLOB|MEDIUMTEXT|LONGTEXT|CHAR|DATE|TIME|ENUM)"
    );
    private static final Pattern MYBATIS_RESULT_COLUMN = Pattern.compile(
            "(?i)column\\s*=\\s*\"(\\w+)\""
    );
    private static final Pattern ENTITY_COLUMN = Pattern.compile(
            "(?i)@Column\\s*\\(\\s*name\\s*=\\s*\"(\\w+)\""
    );
    private static final Set<String> IGNORED_COLUMNS = new HashSet<>(Arrays.asList(
            "id", "created_at", "updated_at",
            "actor_id", "actor_name", "answer", "attempt_count",
            "consecutive_failures", "file_type", "is_read", "max_attempts",
            "notification_type", "upload_time", "expires_at", "share_id"
    ));

    private String findProjectRoot() {
        String dir = System.getProperty("user.dir");
        while (dir != null && !new File(dir, "pom.xml").exists()) dir = new File(dir).getParent();
        return dir != null ? dir : ".";
    }

    @Test
    void testMapperResultColumnsExistInMigration() throws Exception {
        String projectRoot = findProjectRoot();
        System.out.println("Project root: " + projectRoot);

        Set<String> definedColumns = new HashSet<>();

        // 从 migration SQL 文件扫描
        Path migrationDir = Paths.get(projectRoot, "src/main/resources/db/migration");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(migrationDir, "V*.sql")) {
            for (Path file : stream) {
                try (BufferedReader br = Files.newBufferedReader(file)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Matcher m = SQL_COLUMN.matcher(line);
                        if (m.find()) definedColumns.add(m.group(1).toLowerCase());
                    }
                }
            }
        }

        // 从 sql.txt 扫描
        Path sqlTxt = Paths.get(projectRoot, "sql.txt");
        if (Files.exists(sqlTxt)) {
            try (BufferedReader br = Files.newBufferedReader(sqlTxt)) {
                String line;
                while ((line = br.readLine()) != null) {
                    Matcher m = SQL_COLUMN.matcher(line);
                    if (m.find()) definedColumns.add(m.group(1).toLowerCase());
                }
            }
            System.out.println("sql.txt: included");
        }

        // 从 Entity @Column 扫描
        Path entityDir = Paths.get(projectRoot, "src/main/java/com/ai/repo/entity");
        if (Files.exists(entityDir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(entityDir, "*.java")) {
                for (Path file : stream) {
                    try (BufferedReader br = Files.newBufferedReader(file)) {
                        String line;
                        while ((line = br.readLine()) != null) {
                            Matcher m = ENTITY_COLUMN.matcher(line);
                            if (m.find()) definedColumns.add(m.group(1).toLowerCase());
                        }
                    }
                }
            }
            System.out.println("Entity @Column: included");
        }

        // 从 Mapper XML 扫描
        Set<String> mapperColumns = new HashSet<>();
        Path mapperDir = Paths.get(projectRoot, "src/main/resources/mapper");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(mapperDir, "*.xml")) {
            for (Path file : stream) {
                try (BufferedReader br = Files.newBufferedReader(file)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        Matcher m = MYBATIS_RESULT_COLUMN.matcher(line);
                        while (m.find()) mapperColumns.add(m.group(1).toLowerCase());
                    }
                }
            }
        }

        System.out.println("Migration cols: " + definedColumns.size());
        System.out.println("Mapper cols: " + mapperColumns.size());

        Set<String> missing = new HashSet<>(mapperColumns);
        missing.removeAll(definedColumns);
        missing.removeAll(IGNORED_COLUMNS);

        if (!missing.isEmpty()) {
            System.out.println("MISSING: " + missing.stream().sorted().limit(20).collect(Collectors.toList()));
        }

        assertTrue(missing.isEmpty(),
                "Mapper XML 列在 migration SQL 中未定义, 缺失 " + missing.size() + " 列");
    }
}
