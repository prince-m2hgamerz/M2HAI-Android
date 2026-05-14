package com.m2h.m2haichatbot.data.repository;

import com.m2h.m2haichatbot.data.local.dao.ChatDao;
import com.m2h.m2haichatbot.data.local.dao.MessageDao;
import com.m2h.m2haichatbot.data.remote.NvidiaAIService;
import com.m2h.m2haichatbot.data.remote.SupabaseService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.storage.Storage;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class ChatRepositoryImpl_Factory implements Factory<ChatRepositoryImpl> {
  private final Provider<SupabaseService> supabaseServiceProvider;

  private final Provider<NvidiaAIService> nvidiaAIServiceProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<Storage> storageProvider;

  private final Provider<Auth> authProvider;

  private final Provider<Postgrest> postgrestProvider;

  public ChatRepositoryImpl_Factory(Provider<SupabaseService> supabaseServiceProvider,
      Provider<NvidiaAIService> nvidiaAIServiceProvider, Provider<ChatDao> chatDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<Storage> storageProvider,
      Provider<Auth> authProvider, Provider<Postgrest> postgrestProvider) {
    this.supabaseServiceProvider = supabaseServiceProvider;
    this.nvidiaAIServiceProvider = nvidiaAIServiceProvider;
    this.chatDaoProvider = chatDaoProvider;
    this.messageDaoProvider = messageDaoProvider;
    this.storageProvider = storageProvider;
    this.authProvider = authProvider;
    this.postgrestProvider = postgrestProvider;
  }

  @Override
  public ChatRepositoryImpl get() {
    return newInstance(supabaseServiceProvider.get(), nvidiaAIServiceProvider.get(), chatDaoProvider.get(), messageDaoProvider.get(), storageProvider.get(), authProvider.get(), postgrestProvider.get());
  }

  public static ChatRepositoryImpl_Factory create(Provider<SupabaseService> supabaseServiceProvider,
      Provider<NvidiaAIService> nvidiaAIServiceProvider, Provider<ChatDao> chatDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<Storage> storageProvider,
      Provider<Auth> authProvider, Provider<Postgrest> postgrestProvider) {
    return new ChatRepositoryImpl_Factory(supabaseServiceProvider, nvidiaAIServiceProvider, chatDaoProvider, messageDaoProvider, storageProvider, authProvider, postgrestProvider);
  }

  public static ChatRepositoryImpl newInstance(SupabaseService supabaseService,
      NvidiaAIService nvidiaAIService, ChatDao chatDao, MessageDao messageDao, Storage storage,
      Auth auth, Postgrest postgrest) {
    return new ChatRepositoryImpl(supabaseService, nvidiaAIService, chatDao, messageDao, storage, auth, postgrest);
  }
}
