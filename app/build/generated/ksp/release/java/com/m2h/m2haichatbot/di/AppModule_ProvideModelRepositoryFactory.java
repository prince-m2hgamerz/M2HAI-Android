package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.local.dao.AIModelDao;
import com.m2h.m2haichatbot.data.remote.SupabaseService;
import com.m2h.m2haichatbot.domain.repository.ModelRepository;
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
public final class AppModule_ProvideModelRepositoryFactory implements Factory<ModelRepository> {
  private final Provider<SupabaseService> supabaseServiceProvider;

  private final Provider<AIModelDao> aiModelDaoProvider;

  public AppModule_ProvideModelRepositoryFactory(Provider<SupabaseService> supabaseServiceProvider,
      Provider<AIModelDao> aiModelDaoProvider) {
    this.supabaseServiceProvider = supabaseServiceProvider;
    this.aiModelDaoProvider = aiModelDaoProvider;
  }

  @Override
  public ModelRepository get() {
    return provideModelRepository(supabaseServiceProvider.get(), aiModelDaoProvider.get());
  }

  public static AppModule_ProvideModelRepositoryFactory create(
      Provider<SupabaseService> supabaseServiceProvider, Provider<AIModelDao> aiModelDaoProvider) {
    return new AppModule_ProvideModelRepositoryFactory(supabaseServiceProvider, aiModelDaoProvider);
  }

  public static ModelRepository provideModelRepository(SupabaseService supabaseService,
      AIModelDao aiModelDao) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideModelRepository(supabaseService, aiModelDao));
  }
}
