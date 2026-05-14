package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.local.M2HAIDatabase;
import com.m2h.m2haichatbot.data.local.dao.ChatDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class AppModule_ProvideChatDaoFactory implements Factory<ChatDao> {
  private final Provider<M2HAIDatabase> databaseProvider;

  public AppModule_ProvideChatDaoFactory(Provider<M2HAIDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public ChatDao get() {
    return provideChatDao(databaseProvider.get());
  }

  public static AppModule_ProvideChatDaoFactory create(Provider<M2HAIDatabase> databaseProvider) {
    return new AppModule_ProvideChatDaoFactory(databaseProvider);
  }

  public static ChatDao provideChatDao(M2HAIDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideChatDao(database));
  }
}
