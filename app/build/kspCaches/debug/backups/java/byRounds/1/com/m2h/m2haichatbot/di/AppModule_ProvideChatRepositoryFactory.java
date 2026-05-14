package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.local.dao.ChatDao;
import com.m2h.m2haichatbot.data.local.dao.MessageDao;
import com.m2h.m2haichatbot.data.remote.NvidiaAIService;
import com.m2h.m2haichatbot.data.remote.SupabaseService;
import com.m2h.m2haichatbot.domain.repository.ChatRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
import io.github.jan.supabase.postgrest.Postgrest;
import io.github.jan.supabase.storage.Storage;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
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
public final class AppModule_ProvideChatRepositoryFactory implements Factory<ChatRepository> {
  private final Provider<SupabaseService> supabaseServiceProvider;

  private final Provider<NvidiaAIService> nvidiaAIServiceProvider;

  private final Provider<ChatDao> chatDaoProvider;

  private final Provider<MessageDao> messageDaoProvider;

  private final Provider<Storage> storageProvider;

  private final Provider<Auth> authProvider;

  private final Provider<Postgrest> postgrestProvider;

  public AppModule_ProvideChatRepositoryFactory(Provider<SupabaseService> supabaseServiceProvider,
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
  public ChatRepository get() {
    return provideChatRepository(supabaseServiceProvider.get(), nvidiaAIServiceProvider.get(), chatDaoProvider.get(), messageDaoProvider.get(), storageProvider.get(), authProvider.get(), postgrestProvider.get());
  }

  public static AppModule_ProvideChatRepositoryFactory create(
      Provider<SupabaseService> supabaseServiceProvider,
      Provider<NvidiaAIService> nvidiaAIServiceProvider, Provider<ChatDao> chatDaoProvider,
      Provider<MessageDao> messageDaoProvider, Provider<Storage> storageProvider,
      Provider<Auth> authProvider, Provider<Postgrest> postgrestProvider) {
    return new AppModule_ProvideChatRepositoryFactory(supabaseServiceProvider, nvidiaAIServiceProvider, chatDaoProvider, messageDaoProvider, storageProvider, authProvider, postgrestProvider);
  }

  public static ChatRepository provideChatRepository(SupabaseService supabaseService,
      NvidiaAIService nvidiaAIService, ChatDao chatDao, MessageDao messageDao, Storage storage,
      Auth auth, Postgrest postgrest) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatRepository(supabaseService, nvidiaAIService, chatDao, messageDao, storage, auth, postgrest));
  }
}
