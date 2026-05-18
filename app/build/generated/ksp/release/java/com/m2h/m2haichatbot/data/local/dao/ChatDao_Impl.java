package com.m2h.m2haichatbot.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.m2h.m2haichatbot.data.local.entities.ChatEntity;
import java.lang.Class;
import java.lang.Exception;
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
public final class ChatDao_Impl implements ChatDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<ChatEntity> __insertionAdapterOfChatEntity;

  private final EntityDeletionOrUpdateAdapter<ChatEntity> __deletionAdapterOfChatEntity;

  private final EntityDeletionOrUpdateAdapter<ChatEntity> __updateAdapterOfChatEntity;

  private final SharedSQLiteStatement __preparedStmtOfDeleteChatById;

  public ChatDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfChatEntity = new EntityInsertionAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `chats` (`id`,`userId`,`title`,`modelId`,`isArchived`,`isPinned`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getModelId());
        final int _tmp = entity.isArchived() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final int _tmp_1 = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindString(7, entity.getCreatedAt());
        statement.bindString(8, entity.getUpdatedAt());
      }
    };
    this.__deletionAdapterOfChatEntity = new EntityDeletionOrUpdateAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `chats` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
      }
    };
    this.__updateAdapterOfChatEntity = new EntityDeletionOrUpdateAdapter<ChatEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `chats` SET `id` = ?,`userId` = ?,`title` = ?,`modelId` = ?,`isArchived` = ?,`isPinned` = ?,`createdAt` = ?,`updatedAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ChatEntity entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getUserId());
        statement.bindString(3, entity.getTitle());
        statement.bindString(4, entity.getModelId());
        final int _tmp = entity.isArchived() ? 1 : 0;
        statement.bindLong(5, _tmp);
        final int _tmp_1 = entity.isPinned() ? 1 : 0;
        statement.bindLong(6, _tmp_1);
        statement.bindString(7, entity.getCreatedAt());
        statement.bindString(8, entity.getUpdatedAt());
        statement.bindString(9, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteChatById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM chats WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertChat(final ChatEntity chat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(chat);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertChats(final List<ChatEntity> chats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfChatEntity.insert(chats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChat(final ChatEntity chat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfChatEntity.handle(chat);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateChat(final ChatEntity chat, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfChatEntity.handle(chat);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteChatById(final String chatId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteChatById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, chatId);
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
          __preparedStmtOfDeleteChatById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<ChatEntity>> getChats(final String userId) {
    final String _sql = "SELECT * FROM chats WHERE userId = ? ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, userId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chats"}, new Callable<List<ChatEntity>>() {
      @Override
      @NonNull
      public List<ChatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ChatEntity> _result = new ArrayList<ChatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpModelId;
            _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            final boolean _tmpIsPinned;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_1 != 0;
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            _item = new ChatEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpModelId,_tmpIsArchived,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Flow<List<ChatEntity>> getAllChats() {
    final String _sql = "SELECT * FROM chats ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"chats"}, new Callable<List<ChatEntity>>() {
      @Override
      @NonNull
      public List<ChatEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<ChatEntity> _result = new ArrayList<ChatEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final ChatEntity _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpModelId;
            _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            final boolean _tmpIsPinned;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_1 != 0;
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            _item = new ChatEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpModelId,_tmpIsArchived,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt);
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
  public Object getChatById(final String chatId,
      final Continuation<? super ChatEntity> $completion) {
    final String _sql = "SELECT * FROM chats WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, chatId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ChatEntity>() {
      @Override
      @Nullable
      public ChatEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfUserId = CursorUtil.getColumnIndexOrThrow(_cursor, "userId");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfModelId = CursorUtil.getColumnIndexOrThrow(_cursor, "modelId");
          final int _cursorIndexOfIsArchived = CursorUtil.getColumnIndexOrThrow(_cursor, "isArchived");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final ChatEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpUserId;
            _tmpUserId = _cursor.getString(_cursorIndexOfUserId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpModelId;
            _tmpModelId = _cursor.getString(_cursorIndexOfModelId);
            final boolean _tmpIsArchived;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsArchived);
            _tmpIsArchived = _tmp != 0;
            final boolean _tmpIsPinned;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp_1 != 0;
            final String _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getString(_cursorIndexOfCreatedAt);
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            _result = new ChatEntity(_tmpId,_tmpUserId,_tmpTitle,_tmpModelId,_tmpIsArchived,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
