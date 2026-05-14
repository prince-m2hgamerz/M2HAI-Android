package com.m2h.m2haichatbot.data.repository;

import com.m2h.m2haichatbot.data.local.dao.AIModelDao;
import com.m2h.m2haichatbot.data.remote.SupabaseService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
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
public final class ModelRepositoryImpl_Factory implements Factory<ModelRepositoryImpl> {
  private final Provider<SupabaseService> supabaseServiceProvider;

  private final Provider<AIModelDao> aiModelDaoProvider;

  public ModelRepositoryImpl_Factory(Provider<SupabaseService> supabaseServiceProvider,
      Provider<AIModelDao> aiModelDaoProvider) {
    this.supabaseServiceProvider = supabaseServiceProvider;
    this.aiModelDaoProvider = aiModelDaoProvider;
  }

  @Override
  public ModelRepositoryImpl get() {
    return newInstance(supabaseServiceProvider.get(), aiModelDaoProvider.get());
  }

  public static ModelRepositoryImpl_Factory create(
      Provider<SupabaseService> supabaseServiceProvider, Provider<AIModelDao> aiModelDaoProvider) {
    return new ModelRepositoryImpl_Factory(supabaseServiceProvider, aiModelDaoProvider);
  }

  public static ModelRepositoryImpl newInstance(SupabaseService supabaseService,
      AIModelDao aiModelDao) {
    return new ModelRepositoryImpl(supabaseService, aiModelDao);
  }
}
