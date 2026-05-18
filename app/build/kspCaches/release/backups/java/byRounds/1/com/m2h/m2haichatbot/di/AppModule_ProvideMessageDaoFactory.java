package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.local.M2HAIDatabase;
import com.m2h.m2haichatbot.data.local.dao.MessageDao;
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
public final class AppModule_ProvideMessageDaoFactory implements Factory<MessageDao> {
  private final Provider<M2HAIDatabase> databaseProvider;

  public AppModule_ProvideMessageDaoFactory(Provider<M2HAIDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public MessageDao get() {
    return provideMessageDao(databaseProvider.get());
  }

  public static AppModule_ProvideMessageDaoFactory create(
      Provider<M2HAIDatabase> databaseProvider) {
    return new AppModule_ProvideMessageDaoFactory(databaseProvider);
  }

  public static MessageDao provideMessageDao(M2HAIDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideMessageDao(database));
  }
}
