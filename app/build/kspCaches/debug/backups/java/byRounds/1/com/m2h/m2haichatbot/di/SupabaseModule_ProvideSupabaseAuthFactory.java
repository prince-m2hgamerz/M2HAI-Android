package com.m2h.m2haichatbot.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.gotrue.Auth;
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
public final class SupabaseModule_ProvideSupabaseAuthFactory implements Factory<Auth> {
  private final Provider<SupabaseClient> clientProvider;

  public SupabaseModule_ProvideSupabaseAuthFactory(Provider<SupabaseClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public Auth get() {
    return provideSupabaseAuth(clientProvider.get());
  }

  public static SupabaseModule_ProvideSupabaseAuthFactory create(
      Provider<SupabaseClient> clientProvider) {
    return new SupabaseModule_ProvideSupabaseAuthFactory(clientProvider);
  }

  public static Auth provideSupabaseAuth(SupabaseClient client) {
    return Preconditions.checkNotNullFromProvides(SupabaseModule.INSTANCE.provideSupabaseAuth(client));
  }
}
