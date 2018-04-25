package ru.nailsoft.files.toolkit.data;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import ru.nailsoft.files.diagnostics.Logger;
import ru.nailsoft.files.toolkit.Flags32;
import ru.nailsoft.files.toolkit.collections.Query;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;

import static ru.nailsoft.files.diagnostics.DebugUtils.safeThrow;
import static ru.nailsoft.files.toolkit.collections.Query.query;

public class DbUtils {

    public static final String COLUMN_ID = "_id";

    private static final HashMap<Class<?>, SQLiteType> TYPE_MAP = new HashMap<Class<?>, SQLiteType>() {
        {
            put(byte.class, SQLiteType.INTEGER);
            put(short.class, SQLiteType.INTEGER);
            put(int.class, SQLiteType.INTEGER);
            put(long.class, SQLiteType.INTEGER);
            put(float.class, SQLiteType.REAL);
            put(double.class, SQLiteType.REAL);
            put(boolean.class, SQLiteType.INTEGER);
            put(char.class, SQLiteType.TEXT);
            put(byte[].class, SQLiteType.BLOB);
            put(Byte.class, SQLiteType.INTEGER);
            put(Short.class, SQLiteType.INTEGER);
            put(Integer.class, SQLiteType.INTEGER);
            put(Long.class, SQLiteType.INTEGER);
            put(Float.class, SQLiteType.REAL);
            put(Double.class, SQLiteType.REAL);
            put(Boolean.class, SQLiteType.INTEGER);
            put(Character.class, SQLiteType.TEXT);
            put(String.class, SQLiteType.TEXT);
            put(Byte[].class, SQLiteType.BLOB);
        }
    };

    public static Query<Field> iterateFields(Class<?> t) {
        Query<Field> query = query(t.getDeclaredFields());
        while ((t = t.getSuperclass()) != Object.class)
            query = query.concat(t.getDeclaredFields());
        return query.where(DbUtils::checkTransient).select(f -> {
            f.setAccessible(true);
            return f;
        });
    }

    @NonNull
    public static String buildCreateScript(@NonNull Class<?> rawType) {
        String tableName = getTableName(rawType);
        return buildCreateScript(rawType, tableName);
    }

    @NonNull
    public static String buildCreateScript(@NonNull Class<?> rawType, String tableName) {
        StringBuilder sb = new StringBuilder();
        sb.append("create table ");
        sb.append(tableName);
        sb.append(" (");

        for (Field field : iterateFields(rawType)) {
            createColumnDefinition(field, sb);
            sb.append(", ");
        }

        sb.delete(sb.length() - 2, sb.length());
        sb.append(") ");

        String s = sb.toString();
        Logger.logDb("buildCreateScript %s", s);
        return s;
    }


    @NonNull
    public static String getTableName(@NonNull Class<?> rawType) {
        DbTable meta = rawType.getAnnotation(DbTable.class);
        return meta != null && !TextUtils.isEmpty(meta.name()) ? meta.name() : rawType.getSimpleName();
    }

    public static void createColumnDefinition(@NonNull Field field, @NonNull StringBuilder out) {
        Class<?> type = field.getType();
        DbColumn column = field.getAnnotation(DbColumn.class);
        final String name = getColumnName(field, column);

        SQLiteType sqLiteType = TYPE_MAP.get(type);
        if (sqLiteType == null) {
            if (type.isEnum())
                sqLiteType = SQLiteType.INTEGER;
            else if (type == Flags32.class)
                sqLiteType = SQLiteType.INTEGER;
            else
                throw new IllegalArgumentException("Can't serialize field " + name);
        }

        out.append(name);
        out.append(" ");
        out.append(sqLiteType.toString());

        if (column != null) {
            if (column.primaryKey()) {
                out.append(" PRIMARY KEY AUTOINCREMENT");
            }

            if (column.length() > -1) {
                out.append(" (").append(column.length()).append(")");
            }

            if (column.notNull()) {
                out.append(" NOT NULL ON CONFLICT ").append(column.onNullConflict());
            }

            if (column.unique()) {
                out.append(" UNIQUE ON CONFLICT ").append(column.onUniqueConflict().toString());
            }
        }

        DbForeignKey fk = field.getAnnotation(DbForeignKey.class);
        if (fk != null) {
            out.append(" REFERENCES ");
            out.append(fk.table());
            out.append('(').append(fk.column()).append(')');
            out.append(" ON DELETE ");
            out.append(fk.onDelete().toString().replace("_", " "));
            out.append(" ON UPDATE ");
            out.append(fk.onUpdate().toString().replace("_", " "));
        }
    }

    @NonNull
    public static String getColumnName(@NonNull Field field) {
        return getColumnName(field, field.getAnnotation(DbColumn.class));
    }

    @NonNull
    private static String getColumnName(@NonNull Field field, @Nullable DbColumn column) {
        return column != null && !TextUtils.isEmpty(column.name()) ? column.name() : field.getName();
    }

    @NonNull
    public static String getColumnAlias(@NonNull Field field, String tableAlias) {
        return getColumnAlias(field, field.getAnnotation(DbColumn.class), tableAlias);
    }

    @NonNull
    private static String getColumnAlias(@NonNull Field field, @Nullable DbColumn column, String tableAlias) {
        return tableAlias + "_" + (column != null && !TextUtils.isEmpty(column.name()) ? column.name() : field.getName());
    }

    @NonNull
    public static String buildInsert(@NonNull Class<?> rawType) {
        return buildInsert(rawType, null);
    }

    @NonNull
    public static String buildInsert(@NonNull Class<?> rawType, @Nullable ConflictAction onConflict) {
        StringBuilder sb = new StringBuilder();
        sb.append("insert");
        if (onConflict != null)
            sb.append(" or ").append(onConflict.name());
        sb.append(" into ");
        sb.append(getTableName(rawType));
        sb.append(" (");

        int fieldCount = 0;
        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey())
                continue;
            sb.append(getColumnName(field, column));
            fieldCount++;
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());

        sb.append(") values (");
        sb.append("?");
        for (int i = 1; i < fieldCount; i++)
            sb.append(", ?");
        sb.append(")");

        return sb.toString();
    }

    @NonNull
    public static String[] buildInsertArgs(@NonNull Object raw) {
        if (raw instanceof IDbSerializationHandlers)
            ((IDbSerializationHandlers) raw).onBeforeSerialization();

        ArrayList<String> list = new ArrayList<>(raw.getClass().getDeclaredFields().length);
        for (Field field : iterateFields(raw.getClass())) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey())
                continue;

            String value = getFieldValueAsString(raw, field, column);
            list.add(value);
        }
        return list.toArray(new String[list.size()]);
    }

    @NonNull
    public static String buildUpdate(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getTableName(rawType));
        sb.append(" set ");
        String pk = null;

        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey()) {
                pk = getColumnName(field, column);
                continue;
            }
            sb.append(getColumnName(field, column));
            sb.append(" = ?, ");
        }

        sb.delete(sb.length() - 2, sb.length());

        sb.append("\nwhere ").append(pk).append(" = ?");

        return sb.toString();
    }

    @NonNull
    public static String buildUpdate(@NonNull Class<?> rawType, String keyColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("update ");
        sb.append(getTableName(rawType));
        sb.append(" set ");
        String pk = null;

        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && (column.primaryKey() || column.equals(keyColumn)))
                continue;
            sb.append(getColumnName(field, column));
            sb.append(" = ?, ");
        }

        sb.delete(sb.length() - 2, sb.length());

        sb.append("\n where ").append(keyColumn).append(" = ?");

        return sb.toString();
    }

    public static <T> String buildDelete(Class<T> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("delete from  ");
        sb.append(getTableName(rawType));
        sb.append(" where ");

        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            if (column != null && column.primaryKey()) {
                sb.append(getColumnName(field, column));
                sb.append(" = ? ");
                return sb.toString();
            }
        }
        sb.append(COLUMN_ID + " = ? ");
        return sb.toString();
    }

    @NonNull
    public static String[] buildUpdateArgs(@NonNull Object raw) {
        if (raw instanceof IDbSerializationHandlers)
            ((IDbSerializationHandlers) raw).onBeforeSerialization();

        try {
            ArrayList<String> values = new ArrayList<>();
            String pk = null;
            for (Field field : iterateFields(raw.getClass())) {
                DbColumn column = field.getAnnotation(DbColumn.class);
                if (column != null && column.primaryKey()) {
                    pk = field.get(raw).toString();
                    continue;
                }
                String value = getFieldValueAsString(raw, field, column);
                values.add(value);
            }
            values.add(pk);
            return values.toArray(new String[values.size()]);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @Nullable
    private static String getFieldValueAsString(@NonNull Object raw, @NonNull Field field, @Nullable DbColumn column) {
        try {
            String value;
            if (field.getType().isEnum()) {
                try {
                    value = String.valueOf(((Enum<?>) field.get(raw)).ordinal());
                } catch (NullPointerException e) {
                    safeThrow(new NullPointerException("FIX ME!! " + raw + "." + field.getName()));
                    value = "0";
                }
            } else if (field.getType() == Flags32.class) {
                value = String.valueOf(((Flags32) field.get(raw)).get());
            } else if (field.getType() == boolean.class || field.getType() == Boolean.class) {
                value = field.getBoolean(raw) ? "1" : "0";
            } else if (shouldReplaceZeroWithNull(raw, field, column))
                value = null;
            else {
                Object obj = field.get(raw);
                value = obj == null ? null : obj.toString();
            }

            return value;
        } catch (IllegalAccessException e) {
            return "error";
        }
    }

    private static boolean shouldReplaceZeroWithNull(@NonNull Object raw, @NonNull Field field, @Nullable DbColumn column) throws IllegalAccessException {
        return (column != null && (column.unique() || column.nullIfDefault()) || field.isAnnotationPresent(DbForeignKey.class))
                && ((field.getType() == long.class && field.getLong(raw) == 0L)
                || (field.getType() == int.class && field.getInt(raw) == 0));
    }

    public static StringBuilder buildComplexColumnNames(@NonNull Class<?> rawType, String tableAlias, @NonNull StringBuilder out) {
        for (Field field : iterateFields(rawType)) {
            out.append(tableAlias)
                    .append('.')
                    .append(getColumnName(field))
                    .append(" as ")
                    .append(getColumnAlias(field, tableAlias))
                    .append(", ");
        }
        out.delete(out.length() - 2, out.length());
        return out;
    }

    @NonNull
    public static String buildSelectAll(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");
        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append("\nfrom ");
        sb.append(getTableName(rawType));
        sb.append('\n');
        return sb.toString();
    }

    @NonNull
    public static String buildSelectById(@NonNull Class<?> rawType) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");

        String pk = null;
        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            if (column != null && column.primaryKey()) {
                pk = name;
            }
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(" from ");
        sb.append(getTableName(rawType));
        sb.append(" where ");
        sb.append(pk);
        sb.append(" = ?");

        return sb.toString();
    }

    @NonNull
    public static String buildSelectById(@NonNull Class<?> rawType, String keyColumn) {
        StringBuilder sb = new StringBuilder();
        sb.append("select ");

        for (Field field : iterateFields(rawType)) {
            DbColumn column = field.getAnnotation(DbColumn.class);
            String name = getColumnName(field, column);
            sb.append(name);
            sb.append(", ");
        }
        sb.delete(sb.length() - 2, sb.length());
        sb.append(" from ");
        sb.append(getTableName(rawType));
        sb.append(" where ");
        sb.append(keyColumn);
        sb.append(" = ?");

        return sb.toString();
    }


    @Nullable
    public static <T> T readSingle(SQLiteDatabase db, Class<T> rawType, String query, String... args) {
        return readSingle(db, null, rawType, query, args);
    }

    @Nullable
    public static <T> T readSingle(SQLiteDatabase db, String tableAlias, Class<T> rawType, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            if (cursor.moveToFirst())
                return readObjectFromCursor(cursor, rawType.newInstance(), mapCursorForRawType(cursor, rawType, tableAlias));
            else
                return null;
        } catch (InstantiationException e) {
            safeThrow(e);
            return null;
        } catch (IllegalAccessException e) {
            safeThrow(e);
            return null;
        } finally {
            cursor.close();
        }
    }

    public static long count(SQLiteDatabase db, String tableName) {
        Cursor cursor = db.rawQuery(String.format("select count(*) from %s", tableName), null);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public static int count(SQLiteDatabase db, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public static long longCount(SQLiteDatabase db, String query, String... args) {
        Cursor cursor = db.rawQuery(query, args);
        try {
            return cursor.moveToFirst() ? cursor.getLong(0) : 0;
        } finally {
            cursor.close();
        }
    }

    public static Field[] mapCursorForRawType(Cursor cursor, Class<?> rawType, String tableAlias) {
        Field[] fields = new Field[cursor.getColumnCount()];
        if (tableAlias != null)
            for (Field field : iterateFields(rawType)) {
                field.setAccessible(true);
                int columnIndex = cursor.getColumnIndex(getColumnAlias(field, tableAlias));
                if (columnIndex >= 0) {
                    fields[columnIndex] = field;
                }
            }
        else
            for (Field field : iterateFields(rawType)) {
                field.setAccessible(true);
                int columnIndex = cursor.getColumnIndex(getColumnName(field));
                if (columnIndex >= 0) {
                    fields[columnIndex] = field;
                }
            }
        return fields;
    }

    @NonNull
    public static <T> T readObjectFromCursor(Cursor cursor, T next, Field[] cursorMap) {
        try {
            for (int i = 0; i < cursorMap.length; i++) {
                Field field = cursorMap[i];
                if (field == null)
                    continue;

                if (cursor.isNull(i))
                    continue;

                Class<?> fieldType = field.getType();
                if (fieldType.equals(Byte.class) || fieldType.equals(byte.class)) {
                    field.set(next, (byte) cursor.getInt(i));
                } else if (fieldType.equals(Short.class) || fieldType.equals(short.class)) {
                    field.set(next, (short) cursor.getInt(i));
                } else if (fieldType.equals(Integer.class) || fieldType.equals(int.class)) {
                    field.set(next, cursor.getInt(i));
                } else if (fieldType.equals(Long.class) || fieldType.equals(long.class)) {
                    field.set(next, cursor.getLong(i));
                } else if (fieldType.equals(Float.class) || fieldType.equals(float.class)) {
                    field.set(next, cursor.getFloat(i));
                } else if (fieldType.equals(Double.class) || fieldType.equals(double.class)) {
                    field.set(next, cursor.getDouble(i));
                } else if (fieldType.equals(Boolean.class) || fieldType.equals(boolean.class)) {
                    field.set(next, cursor.getInt(i) != 0);
                } else if (fieldType.equals(Character.class) || fieldType.equals(char.class)) {
                    field.set(next, cursor.getString(i).charAt(0));
                } else if (fieldType.equals(String.class)) {
                    field.set(next, cursor.getString(i));
                } else if (fieldType.equals(Byte[].class) || fieldType.equals(byte[].class)) {
                    field.set(next, cursor.getBlob(i));
                } else if (fieldType.isEnum()) {
                    field.set(next, fieldType.getEnumConstants()[cursor.getInt(i)]);
                } else if (fieldType == Flags32.class) {
                    Object value = field.get(next);
                    if (value == null) {
                        field.set(next, new Flags32(cursor.getInt(i)));
                    } else {
                        ((Flags32) value).set(cursor.getInt(i));
                    }
                } else
                    throw new IllegalArgumentException();
            }

            if (next instanceof IDbSerializationHandlers)
                ((IDbSerializationHandlers) next).onAfterDeserialization();

        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return next;
    }

    public static void bindAllArgsAsStrings(SQLiteStatement sql, String[] args) {
        if (args != null) {
            for (int i = args.length; i != 0; i--) {
                String arg = args[i - 1];
                if (arg != null) {
                    sql.bindString(i, arg);
                } else {
                    sql.bindNull(i);
                }
            }
        }
    }

    public static void leftJoin(StringBuilder sb, String fktable, final String fkalias, String fkcolumn, final String pktable, String pkcolumn) {
        sb.append("left join ").append(fktable).append(" " + fkalias + " on " + fkalias + ".").append(fkcolumn).append("=" + pktable + ".").append(pkcolumn).append("\n");
    }

    public static void join(StringBuilder sb, String fktable, final String fkalias, String fkcolumn, final String pktable, String pkcolumn) {
        sb.append("join ").append(fktable).append(" " + fkalias + " on " + fkalias + ".").append(fkcolumn).append("=" + pktable + ".").append(pkcolumn).append("\n");
    }

    private static boolean checkTransient(Field field) {
        int modifiers = field.getModifiers();

        if (Modifier.isTransient(modifiers))
            return false;

        if (Modifier.isStatic(modifiers))
            return false;

        return !Modifier.isFinal(modifiers) || field.getType() == Flags32.class;
    }

    public enum SQLiteType {
        INTEGER, REAL, TEXT, BLOB
    }

}
