package ru.nailsoft.files.toolkit.data;

import java.util.ArrayList;
import java.util.Collections;

public class SqlBuilder {
    private final ArrayList<String> selections = new ArrayList<>();
    private final StringBuilder sb;

    public SqlBuilder() {
        sb = new StringBuilder();
    }

    public SqlBuilder(SqlBuilder parent) {
        sb = parent.sb;
    }

    public SqlBuilder select(String column) {
        selections.add(column);
        return this;
    }

    public SqlBuilder select(String... columns) {
        selections.add("\n");
        Collections.addAll(selections, columns);
        return this;
    }

    public SqlBuilder delete() {
        sb.append("delete ");
        return this;
    }

    public SqlBuilder from(String table) {
        if (!selections.isEmpty()) {
            sb.append("select");
            for (String c : selections) {
                sb.append(c);
                if (!c.trim().isEmpty())
                    sb.append(", ");
            }
            sb.delete(sb.length() - 2, sb.length());
        }
        sb.append("\nfrom ").append(table);
        return this;
    }

    public SqlBuilder from(String table, String alias) {
        from(table);
        sb.append(' ').append(alias);
        return this;
    }

    public SqlBuilder join(String table) {
        sb.append("\njoin ").append(table);
        return this;
    }

    public SqlBuilder join(String table, String alias) {
        sb.append("\njoin ").append(table).append(' ').append(alias);
        return this;
    }

    public SqlBuilder leftJoin(String table) {
        sb.append("\nleft join ").append(table);
        return this;
    }

    public SqlBuilder leftJoin(String table, String alias) {
        sb.append("\nleft join ").append(table).append(' ').append(alias);
        return this;
    }

    public SqlBuilder rightJoin(String table) {
        sb.append("\nright join ").append(table);
        return this;
    }

    public SqlBuilder rightJoin(String table, String alias) {
        sb.append("\nright join ").append(table).append(' ').append(alias);
        return this;
    }

    public SqlBuilder on(String condition) {
        sb.append("on ").append(condition);
        return this;
    }

    public SqlBuilder where(String condition) {
        sb.append("\nwhere ").append(condition);
        return this;
    }

    public SqlBuilder and(String condition) {
        sb.append("\n\tand ").append(condition);
        return this;
    }

    public SqlBuilder or(String condition) {
        sb.append("\n\tor ").append(condition);
        return this;
    }

    public SqlBuilder x(String s) {
        sb.append(s);
        return this;
    }

    public String build() {
        return sb.toString();
    }

}
