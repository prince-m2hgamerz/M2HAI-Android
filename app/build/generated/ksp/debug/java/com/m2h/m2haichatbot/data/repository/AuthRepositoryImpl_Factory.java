package com.m2h.m2haichatbot.data.repository;

import com.m2h.m2haichatbot.data.remote.SupabaseService;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.gotrue.Auth;
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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<SupabaseService> supabaseServiceProvider;

  private final Provider<Storage> storageProvider;

  private final Provider<Auth> authProvider;

  public AuthRepositoryImpl_Factory(Provider<SupabaseService> supabaseServiceProvider,
      Provider<Storage> storageProvider, Provider<Auth> authProvider) {
    this.supabaseServiceProvider = supabaseServiceProvider;
    this.storageProvider = storageProvider;
    this.authProvider = authProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(supabaseServiceProvider.get(), storageProvider.get(), authProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(Provider<SupabaseService> supabaseServiceProvider,
      Provider<Storage> storageProvider, Provider<Auth> authProvider) {
    return new AuthRepositoryImpl_Factory(supabaseServiceProvider, storageProvider, authProvider);
  }

  public static AuthRepositoryImpl newInstance(SupabaseService supabaseService, Storage storage,
      Auth auth) {
    return new AuthRepositoryImpl(supabaseService, storage, auth);
  }
}
