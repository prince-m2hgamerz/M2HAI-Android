package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.local.M2HAIDatabase;
import com.m2h.m2haichatbot.data.local.dao.AIModelDao;
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
public final class AppModule_ProvideAIModelDaoFactory implements Factory<AIModelDao> {
  private final Provider<M2HAIDatabase> databaseProvider;

  public AppModule_ProvideAIModelDaoFactory(Provider<M2HAIDatabase> databaseProvider) {
    this.databaseProvider = databaseProvider;
  }

  @Override
  public AIModelDao get() {
    return provideAIModelDao(databaseProvider.get());
  }

  public static AppModule_ProvideAIModelDaoFactory create(
      Provider<M2HAIDatabase> databaseProvider) {
    return new AppModule_ProvideAIModelDaoFactory(databaseProvider);
  }

  public static AIModelDao provideAIModelDao(M2HAIDatabase database) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAIModelDao(database));
  }
}
