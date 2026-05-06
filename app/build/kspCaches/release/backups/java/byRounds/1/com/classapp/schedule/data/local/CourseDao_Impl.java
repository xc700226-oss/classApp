package com.classapp.schedule.data.local;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.classapp.schedule.data.model.Course;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CourseDao_Impl implements CourseDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Course> __insertionAdapterOfCourse;

  private final EntityDeletionOrUpdateAdapter<Course> __deletionAdapterOfCourse;

  private final EntityDeletionOrUpdateAdapter<Course> __updateAdapterOfCourse;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public CourseDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCourse = new EntityInsertionAdapter<Course>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `courses` (`id`,`name`,`teacher`,`classroom`,`dayOfWeek`,`startSlot`,`endSlot`,`weekStart`,`weekEnd`,`oddEven`,`colorIndex`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Course entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTeacher());
        statement.bindString(4, entity.getClassroom());
        statement.bindLong(5, entity.getDayOfWeek());
        statement.bindLong(6, entity.getStartSlot());
        statement.bindLong(7, entity.getEndSlot());
        statement.bindLong(8, entity.getWeekStart());
        statement.bindLong(9, entity.getWeekEnd());
        statement.bindLong(10, entity.getOddEven());
        statement.bindLong(11, entity.getColorIndex());
      }
    };
    this.__deletionAdapterOfCourse = new EntityDeletionOrUpdateAdapter<Course>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `courses` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Course entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfCourse = new EntityDeletionOrUpdateAdapter<Course>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `courses` SET `id` = ?,`name` = ?,`teacher` = ?,`classroom` = ?,`dayOfWeek` = ?,`startSlot` = ?,`endSlot` = ?,`weekStart` = ?,`weekEnd` = ?,`oddEven` = ?,`colorIndex` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Course entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getTeacher());
        statement.bindString(4, entity.getClassroom());
        statement.bindLong(5, entity.getDayOfWeek());
        statement.bindLong(6, entity.getStartSlot());
        statement.bindLong(7, entity.getEndSlot());
        statement.bindLong(8, entity.getWeekStart());
        statement.bindLong(9, entity.getWeekEnd());
        statement.bindLong(10, entity.getOddEven());
        statement.bindLong(11, entity.getColorIndex());
        statement.bindLong(12, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM courses";
        return _query;
      }
    };
  }

  @Override
  public Object insertCourse(final Course course, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfCourse.insertAndReturnId(course);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteCourse(final Course course, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfCourse.handle(course);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateCourse(final Course course, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfCourse.handle(course);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Course>> getAllCourses() {
    final String _sql = "SELECT * FROM courses ORDER BY dayOfWeek, startSlot";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"courses"}, new Callable<List<Course>>() {
      @Override
      @NonNull
      public List<Course> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfClassroom = CursorUtil.getColumnIndexOrThrow(_cursor, "classroom");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfEndSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "endSlot");
          final int _cursorIndexOfWeekStart = CursorUtil.getColumnIndexOrThrow(_cursor, "weekStart");
          final int _cursorIndexOfWeekEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "weekEnd");
          final int _cursorIndexOfOddEven = CursorUtil.getColumnIndexOrThrow(_cursor, "oddEven");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final List<Course> _result = new ArrayList<Course>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Course _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpClassroom;
            _tmpClassroom = _cursor.getString(_cursorIndexOfClassroom);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpEndSlot;
            _tmpEndSlot = _cursor.getInt(_cursorIndexOfEndSlot);
            final int _tmpWeekStart;
            _tmpWeekStart = _cursor.getInt(_cursorIndexOfWeekStart);
            final int _tmpWeekEnd;
            _tmpWeekEnd = _cursor.getInt(_cursorIndexOfWeekEnd);
            final int _tmpOddEven;
            _tmpOddEven = _cursor.getInt(_cursorIndexOfOddEven);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            _item = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpClassroom,_tmpDayOfWeek,_tmpStartSlot,_tmpEndSlot,_tmpWeekStart,_tmpWeekEnd,_tmpOddEven,_tmpColorIndex);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Course>> getCoursesByDay(final int dayOfWeek) {
    final String _sql = "SELECT * FROM courses WHERE dayOfWeek = ? ORDER BY startSlot";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, dayOfWeek);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"courses"}, new Callable<List<Course>>() {
      @Override
      @NonNull
      public List<Course> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfTeacher = CursorUtil.getColumnIndexOrThrow(_cursor, "teacher");
          final int _cursorIndexOfClassroom = CursorUtil.getColumnIndexOrThrow(_cursor, "classroom");
          final int _cursorIndexOfDayOfWeek = CursorUtil.getColumnIndexOrThrow(_cursor, "dayOfWeek");
          final int _cursorIndexOfStartSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "startSlot");
          final int _cursorIndexOfEndSlot = CursorUtil.getColumnIndexOrThrow(_cursor, "endSlot");
          final int _cursorIndexOfWeekStart = CursorUtil.getColumnIndexOrThrow(_cursor, "weekStart");
          final int _cursorIndexOfWeekEnd = CursorUtil.getColumnIndexOrThrow(_cursor, "weekEnd");
          final int _cursorIndexOfOddEven = CursorUtil.getColumnIndexOrThrow(_cursor, "oddEven");
          final int _cursorIndexOfColorIndex = CursorUtil.getColumnIndexOrThrow(_cursor, "colorIndex");
          final List<Course> _result = new ArrayList<Course>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Course _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpTeacher;
            _tmpTeacher = _cursor.getString(_cursorIndexOfTeacher);
            final String _tmpClassroom;
            _tmpClassroom = _cursor.getString(_cursorIndexOfClassroom);
            final int _tmpDayOfWeek;
            _tmpDayOfWeek = _cursor.getInt(_cursorIndexOfDayOfWeek);
            final int _tmpStartSlot;
            _tmpStartSlot = _cursor.getInt(_cursorIndexOfStartSlot);
            final int _tmpEndSlot;
            _tmpEndSlot = _cursor.getInt(_cursorIndexOfEndSlot);
            final int _tmpWeekStart;
            _tmpWeekStart = _cursor.getInt(_cursorIndexOfWeekStart);
            final int _tmpWeekEnd;
            _tmpWeekEnd = _cursor.getInt(_cursorIndexOfWeekEnd);
            final int _tmpOddEven;
            _tmpOddEven = _cursor.getInt(_cursorIndexOfOddEven);
            final int _tmpColorIndex;
            _tmpColorIndex = _cursor.getInt(_cursorIndexOfColorIndex);
            _item = new Course(_tmpId,_tmpName,_tmpTeacher,_tmpClassroom,_tmpDayOfWeek,_tmpStartSlot,_tmpEndSlot,_tmpWeekStart,_tmpWeekEnd,_tmpOddEven,_tmpColorIndex);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
