package com.m2h.m2haichatbot.di;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
import io.github.jan.supabase.postgrest.Postgrest;
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
public final class SupabaseModule_ProvideSupabasePostgrestFactory implements Factory<Postgrest> {
  private final Provider<SupabaseClient> clientProvider;

  public SupabaseModule_ProvideSupabasePostgrestFactory(Provider<SupabaseClient> clientProvider) {
    this.clientProvider = clientProvider;
  }

  @Override
  public Postgrest get() {
    return provideSupabasePostgrest(clientProvider.get());
  }

  public static SupabaseModule_ProvideSupabasePostgrestFactory create(
      Provider<SupabaseClient> clientProvider) {
    return new SupabaseModule_ProvideSupabasePostgrestFactory(clientProvider);
  }

  public static Postgrest provideSupabasePostgrest(SupabaseClient client) {
    return Preconditions.checkNotNullFromProvides(SupabaseModule.INSTANCE.provideSupabasePostgrest(client));
  }
}
