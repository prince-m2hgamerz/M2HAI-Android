package com.m2h.m2haichatbot.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.m2h.m2haichatbot.data.local.dao.AIModelDao;
import com.m2h.m2haichatbot.data.local.dao.AIModelDao_Impl;
import com.m2h.m2haichatbot.data.local.dao.ChatDao;
import com.m2h.m2haichatbot.data.local.dao.ChatDao_Impl;
import com.m2h.m2haichatbot.data.local.dao.MessageDao;
import com.m2h.m2haichatbot.data.local.dao.MessageDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class M2HAIDatabase_Impl extends M2HAIDatabase {
  private volatile ChatDao _chatDao;

  private volatile MessageDao _messageDao;

  private volatile AIModelDao _aIModelDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(3) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `chats` (`id` TEXT NOT NULL, `userId` TEXT NOT NULL, `title` TEXT NOT NULL, `modelId` TEXT NOT NULL, `isArchived` INTEGER NOT NULL, `isPinned` INTEGER NOT NULL, `createdAt` TEXT NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `messages` (`id` TEXT NOT NULL, `chatId` TEXT NOT NULL, `role` TEXT NOT NULL, `content` TEXT NOT NULL, `attachmentsJson` TEXT, `createdAt` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `ai_models` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `provider` TEXT NOT NULL, `description` TEXT, `isFree` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '003f6f6e6b78812aecf93cfd47ba15c5')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `chats`");
        db.execSQL("DROP TABLE IF EXISTS `messages`");
        db.execSQL("DROP TABLE IF EXISTS `ai_models`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsChats = new HashMap<String, TableInfo.Column>(8);
        _columnsChats.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("userId", new TableInfo.Column("userId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("modelId", new TableInfo.Column("modelId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("isArchived", new TableInfo.Column("isArchived", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsChats.put("updatedAt", new TableInfo.Column("updatedAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysChats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesChats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoChats = new TableInfo("chats", _columnsChats, _foreignKeysChats, _indicesChats);
        final TableInfo _existingChats = TableInfo.read(db, "chats");
        if (!_infoChats.equals(_existingChats)) {
          return new RoomOpenHelper.ValidationResult(false, "chats(com.m2h.m2haichatbot.data.local.entities.ChatEntity).\n"
                  + " Expected:\n" + _infoChats + "\n"
                  + " Found:\n" + _existingChats);
        }
        final HashMap<String, TableInfo.Column> _columnsMessages = new HashMap<String, TableInfo.Column>(6);
        _columnsMessages.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("chatId", new TableInfo.Column("chatId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("role", new TableInfo.Column("role", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("content", new TableInfo.Column("content", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("attachmentsJson", new TableInfo.Column("attachmentsJson", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMessages.put("createdAt", new TableInfo.Column("createdAt", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMessages = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMessages = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMessages = new TableInfo("messages", _columnsMessages, _foreignKeysMessages, _indicesMessages);
        final TableInfo _existingMessages = TableInfo.read(db, "messages");
        if (!_infoMessages.equals(_existingMessages)) {
          return new RoomOpenHelper.ValidationResult(false, "messages(com.m2h.m2haichatbot.data.local.entities.MessageEntity).\n"
                  + " Expected:\n" + _infoMessages + "\n"
                  + " Found:\n" + _existingMessages);
        }
        final HashMap<String, TableInfo.Column> _columnsAiModels = new HashMap<String, TableInfo.Column>(6);
        _columnsAiModels.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiModels.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiModels.put("provider", new TableInfo.Column("provider", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiModels.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiModels.put("isFree", new TableInfo.Column("isFree", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsAiModels.put("isActive", new TableInfo.Column("isActive", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysAiModels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesAiModels = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoAiModels = new TableInfo("ai_models", _columnsAiModels, _foreignKeysAiModels, _indicesAiModels);
        final TableInfo _existingAiModels = TableInfo.read(db, "ai_models");
        if (!_infoAiModels.equals(_existingAiModels)) {
          return new RoomOpenHelper.ValidationResult(false, "ai_models(com.m2h.m2haichatbot.data.local.entities.AIModelEntity).\n"
                  + " Expected:\n" + _infoAiModels + "\n"
                  + " Found:\n" + _existingAiModels);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "003f6f6e6b78812aecf93cfd47ba15c5", "d5f8a4946309e9dc6632c3059667e699");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "chats","messages","ai_models");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `chats`");
      _db.execSQL("DELETE FROM `messages`");
      _db.execSQL("DELETE FROM `ai_models`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(ChatDao.class, ChatDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MessageDao.class, MessageDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(AIModelDao.class, AIModelDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public ChatDao chatDao() {
    if (_chatDao != null) {
      return _chatDao;
    } else {
      synchronized(this) {
        if(_chatDao == null) {
          _chatDao = new ChatDao_Impl(this);
        }
        return _chatDao;
      }
    }
  }

  @Override
  public MessageDao messageDao() {
    if (_messageDao != null) {
      return _messageDao;
    } else {
      synchronized(this) {
        if(_messageDao == null) {
          _messageDao = new MessageDao_Impl(this);
        }
        return _messageDao;
      }
    }
  }

  @Override
  public AIModelDao aiModelDao() {
    if (_aIModelDao != null) {
      return _aIModelDao;
    } else {
      synchronized(this) {
        if(_aIModelDao == null) {
          _aIModelDao = new AIModelDao_Impl(this);
        }
        return _aIModelDao;
      }
    }
  }
}
