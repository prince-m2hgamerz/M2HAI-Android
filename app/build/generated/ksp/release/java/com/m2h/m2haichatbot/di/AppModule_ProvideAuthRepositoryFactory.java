package com.m2h.m2haichatbot.di;

import com.m2h.m2haichatbot.data.remote.SupabaseService;
import com.m2h.m2haichatbot.domain.repository.AuthRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
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
public final class AppModule_ProvideAuthRepositoryFactory implements Factory<AuthRepository> {
  private final Provider<SupabaseService> supabaseServiceProvider;

  private final Provider<Storage> storageProvider;

  private final Provider<Auth> authProvider;

  public AppModule_ProvideAuthRepositoryFactory(Provider<SupabaseService> supabaseServiceProvider,
      Provider<Storage> storageProvider, Provider<Auth> authProvider) {
    this.supabaseServiceProvider = supabaseServiceProvider;
    this.storageProvider = storageProvider;
    this.authProvider = authProvider;
  }

  @Override
  public AuthRepository get() {
    return provideAuthRepository(supabaseServiceProvider.get(), storageProvider.get(), authProvider.get());
  }

  public static AppModule_ProvideAuthRepositoryFactory create(
      Provider<SupabaseService> supabaseServiceProvider, Provider<Storage> storageProvider,
      Provider<Auth> authProvider) {
    return new AppModule_ProvideAuthRepositoryFactory(supabaseServiceProvider, storageProvider, authProvider);
  }

  public static AuthRepository provideAuthRepository(SupabaseService supabaseService,
      Storage storage, Auth auth) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideAuthRepository(supabaseService, storage, auth));
  }
}
